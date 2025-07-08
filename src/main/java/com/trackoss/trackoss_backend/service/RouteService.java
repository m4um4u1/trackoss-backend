package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import com.trackoss.trackoss_backend.repository.RouteRepository;
import com.trackoss.trackoss_backend.repository.RoutePointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final RoutePointRepository routePointRepository;
    private final RouteCalculationService routeCalculationService;
    
    public RouteResponse createRoute(RouteCreateRequest request) {
        log.info("Creating new route: {}", request.getName());
        
        Route route = new Route();
        route.setName(request.getName());
        route.setDescription(request.getDescription());
        route.setRouteType(request.getRouteType());
        route.setIsPublic(request.getIsPublic());
        route.setMetadata(request.getMetadata());
        
        // Create route points
        List<RoutePoint> points = IntStream.range(0, request.getPoints().size())
                .mapToObj(i -> {
                    RouteCreateRequest.RoutePointRequest pointReq = request.getPoints().get(i);
                    RoutePoint point = new RoutePoint();
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
        
        // Calculate route statistics
        routeCalculationService.calculateRouteStatistics(route);
        
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
    
    public RouteResponse updateRoute(UUID id, RouteCreateRequest request) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        
        route.setName(request.getName());
        route.setDescription(request.getDescription());
        route.setRouteType(request.getRouteType());
        route.setIsPublic(request.getIsPublic());
        route.setMetadata(request.getMetadata());
        
        // Clear existing points and add new ones
        route.getRoutePoints().clear();
        
        List<RoutePoint> points = IntStream.range(0, request.getPoints().size())
                .mapToObj(i -> {
                    RouteCreateRequest.RoutePointRequest pointReq = request.getPoints().get(i);
                    RoutePoint point = new RoutePoint();
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
        routeCalculationService.calculateRouteStatistics(route);
        
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