
package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import com.trackoss.trackoss_backend.entity.User;
import com.trackoss.trackoss_backend.repository.RouteRepository;
import com.trackoss.trackoss_backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteStatisticsService routeStatisticsService;

    public RouteResponse createRoute(RouteCreateRequest request) {
        return createRoute(request, null);
    }

    public RouteResponse createRoute(RouteCreateRequest request, Authentication authentication) {
        log.info("Creating new route: {}", request.getName());

        Route route = new Route();
        route.setId(UUID.randomUUID()); // Set UUID manually
        route.setName(request.getName());
        route.setDescription(request.getDescription());
        route.setRouteType(request.getRouteType());
        route.setIsPublic(request.getIsPublic());
        route.setMetadata(request.getMetadata());

        // Set userId from authentication if available
        if (authentication != null && authentication.isAuthenticated()) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userPrincipal.getUser();
            route.setUserId(user.getId().toString());
            log.info("Setting route userId to: {}", user.getId());
        }

        // Set difficulty from request only if provided, otherwise keep metadata-extracted value
        Integer requestDifficulty = request.getDifficulty();
        if (requestDifficulty != null) {
            route.setDifficulty(requestDifficulty);
        }

        // Create route points
        List<RoutePoint> points = IntStream.range(0, request.getPoints().size())
                .mapToObj(i -> {
                    RouteCreateRequest.RoutePointRequest pointReq = request.getPoints().get(i);
                    RoutePoint point = new RoutePoint();
                    point.setId(UUID.randomUUID()); // Set UUID manually
                    point.setSequenceOrder(i);
                    point.setLatitude(pointReq.getLatitude());
                    point.setLongitude(pointReq.getLongitude());
                    point.setElevation(pointReq.getElevation());
                    point.setName(pointReq.getName());
                    point.setDescription(pointReq.getDescription());
                    point.setPointType(RoutePoint.PointType.valueOf(pointReq.getPointType()));
                    point.setRoute(route);
                    return point;
                })
                .toList();

        route.setRoutePoints(points);

        // Set pre-calculated statistics if provided, otherwise calculate them
        if (request.getTotalDistance() != null) {
            route.setTotalDistance(request.getTotalDistance());
        }
        if (request.getTotalElevationGain() != null) {
            route.setTotalElevationGain(request.getTotalElevationGain());
        }
        if (request.getEstimatedDuration() != null) {
            route.setEstimatedDuration(request.getEstimatedDuration());
        }

        // Calculate missing statistics
        routeStatisticsService.calculateMissingStatistics(route);

        Route savedRoute = routeRepository.save(route);
        log.info("Route created with ID: {}", savedRoute.getId());

        return convertToResponse(savedRoute);
    }

    @Transactional(readOnly = true)
    public Optional<RouteResponse> getRoute(UUID id) {
        return routeRepository.findById(id)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Optional<Route> getRouteEntityForExport(UUID id) {
        // Fetch route with eager loading of route points for export
        return routeRepository.findById(id)
                .map(route -> {
                    // Force loading of route points to avoid lazy loading issues
                    route.getRoutePoints().size();
                    return route;
                });
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> getAllRoutes(Pageable pageable) {
        return routeRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> getPublicRoutes(Pageable pageable) {
        return routeRepository.findByIsPublicTrue(pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> getUserRoutes(String userId, Pageable pageable) {
        return routeRepository.findByUserId(userId, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> searchRoutes(String name, Pageable pageable) {
        return routeRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> findNearbyRoutes(Double latitude, Double longitude, Double radiusKm, Pageable pageable) {
        return routeRepository.findNearbyRoutes(latitude, longitude, radiusKm, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> findByDifficulty(Integer difficulty, Pageable pageable) {
        return routeRepository.findByDifficulty(difficulty, pageable)
                .map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<RouteResponse> getRoutesWithFilters(
            Integer difficulty,
            Route.RouteType routeType,
            Double minDistance,
            Double maxDistance,
            String surfaceType,
            Boolean isPublic,
            Pageable pageable) {

        // Convert surfaceType string to proper format for LIKE query if needed
        String surfaceTypeParam = null;
        if (surfaceType != null && !surfaceType.isEmpty()) {
            surfaceTypeParam = "\"surface\":\"" + surfaceType + "\"";
        }

        Page<Route> routes = routeRepository.findWithFilters(
            difficulty,
            routeType,
            minDistance,
            maxDistance,
            surfaceTypeParam,
            isPublic,
            pageable
        );

        return routes.map(this::convertToResponse);
    }

    public RouteResponse updateRoute(UUID id, RouteCreateRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        route.setName(request.getName());
        route.setDescription(request.getDescription());
        route.setRouteType(request.getRouteType());
        route.setIsPublic(request.getIsPublic());

        // Set metadata first to allow difficulty extraction from metadata
        route.setMetadata(request.getMetadata());

        // Set difficulty from request only if provided, otherwise keep metadata-extracted value
        Integer requestDifficulty = request.getDifficulty();
        if (requestDifficulty != null) {
            route.setDifficulty(requestDifficulty);
        }

        // Clear existing points and add new ones
        route.getRoutePoints().clear();

        List<RoutePoint> points = IntStream.range(0, request.getPoints().size())
                .mapToObj(i -> {
                    RouteCreateRequest.RoutePointRequest pointReq = request.getPoints().get(i);
                    RoutePoint point = new RoutePoint();
                    point.setId(UUID.randomUUID()); // Set UUID manually
                    point.setSequenceOrder(i);
                    point.setLatitude(pointReq.getLatitude());
                    point.setLongitude(pointReq.getLongitude());
                    point.setElevation(pointReq.getElevation());
                    point.setName(pointReq.getName());
                    point.setDescription(pointReq.getDescription());
                    point.setPointType(RoutePoint.PointType.valueOf(pointReq.getPointType()));
                    point.setRoute(route);
                    return point;
                })
                .toList();

        route.getRoutePoints().addAll(points);

        // Recalculate route statistics
        routeStatisticsService.calculateRouteStatistics(route);

        Route savedRoute = routeRepository.save(route);
        log.info("Route updated: {}", savedRoute.getId());

        return convertToResponse(savedRoute);
    }

    public void deleteRoute(UUID id) {
        if (!routeRepository.existsById(id)) {
            throw new RuntimeException("Route not found");
        }
        routeRepository.deleteById(id);
        log.info("Route deleted: {}", id);
    }

    private RouteResponse convertToResponse(Route route) {
        RouteResponse response = new RouteResponse();
        response.setId(route.getId());
        response.setName(route.getName());
        response.setDescription(route.getDescription());
        response.setCreatedAt(route.getCreatedAt());
        response.setUpdatedAt(route.getUpdatedAt());
        response.setUserId(route.getUserId());
        response.setTotalDistance(route.getTotalDistance());
        response.setTotalElevationGain(route.getTotalElevationGain());
        response.setEstimatedDuration(route.getEstimatedDuration());
        response.setRouteType(route.getRouteType());
        response.setIsPublic(route.getIsPublic());
        response.setDifficulty(route.getDifficulty());
        response.setMetadata(route.getMetadata());
        response.setPointCount(route.getRoutePoints().size());

        List<RouteResponse.RoutePointResponse> pointResponses = route.getRoutePoints().stream()
                .map(point -> {
                    RouteResponse.RoutePointResponse pointResponse = new RouteResponse.RoutePointResponse();
                    pointResponse.setId(point.getId());
                    pointResponse.setSequenceOrder(point.getSequenceOrder());
                    pointResponse.setLatitude(point.getLatitude());
                    pointResponse.setLongitude(point.getLongitude());
                    pointResponse.setElevation(point.getElevation());
                    pointResponse.setTimestamp(point.getTimestamp());
                    pointResponse.setPointType(point.getPointType().name());
                    pointResponse.setName(point.getName());
                    pointResponse.setDescription(point.getDescription());
                    return pointResponse;
                })
                .toList();

        response.setPoints(pointResponses);
        return response;
    }
}
