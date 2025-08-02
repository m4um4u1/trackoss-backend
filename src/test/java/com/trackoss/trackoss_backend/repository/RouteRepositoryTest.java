package com.trackoss.trackoss_backend.repository;

import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RouteRepositoryTest {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private RoutePointRepository routePointRepository;

    private List<Route> testRoutes;
    private final Pageable pageable = PageRequest.of(0, 10);

    @BeforeEach
    void setUp() {
        // Clear any existing data
        routePointRepository.deleteAll();
        routeRepository.deleteAll();

        // Create test routes with different difficulties
        testRoutes = new ArrayList<>();

        // Route 1: Difficulty 1, CYCLING, 5000m, public
        Route route1 = createRoute("Easy Route", 1, Route.RouteType.CYCLING, 5000.0, true, "{\"surface\":\"asphalt\",\"difficulty\":1}");
        
        // Route 2: Difficulty 3, MOUNTAIN_BIKING, 15000m, public
        Route route2 = createRoute("Moderate Route", 3, Route.RouteType.MOUNTAIN_BIKING, 15000.0, true, "{\"surface\":\"gravel\",\"difficulty\":3}");
        
        // Route 3: Difficulty 5, GRAVEL, 25000m, private
        Route route3 = createRoute("Hard Route", 5, Route.RouteType.GRAVEL, 25000.0, false, "{\"surface\":\"dirt\",\"difficulty\":5}");
        
        // Route 4: Difficulty 2, ROAD_CYCLING, 10000m, public
        Route route4 = createRoute("Easy Road Route", 2, Route.RouteType.ROAD_CYCLING, 10000.0, true, "{\"surface\":\"asphalt\",\"difficulty\":2}");
        
        // Route 5: Difficulty 4, HIKING, 8000m, private
        Route route5 = createRoute("Challenging Hike", 4, Route.RouteType.HIKING, 8000.0, false, "{\"surface\":\"trail\",\"difficulty\":4}");

        // Save all routes
        testRoutes = routeRepository.saveAll(List.of(route1, route2, route3, route4, route5));
    }

    private Route createRoute(String name, Integer difficulty, Route.RouteType routeType, Double distance, Boolean isPublic, String metadata) {
        Route route = new Route();
        route.setId(UUID.randomUUID());
        route.setName(name);
        route.setDescription("Test route description");
        route.setRouteType(routeType);
        route.setIsPublic(isPublic);
        route.setTotalDistance(distance);
        route.setTotalElevationGain(100.0);
        route.setEstimatedDuration(3600L);
        route.setMetadata(metadata);
        route.setDifficulty(difficulty);
        
        // Add a sample route point
        RoutePoint point = new RoutePoint();
        point.setId(UUID.randomUUID());
        point.setLatitude(47.6062);
        point.setLongitude(-122.3321);
        point.setElevation(10.0);
        point.setSequenceOrder(0);
        point.setPointType(RoutePoint.PointType.START_POINT);
        point.setRoute(route);
        
        List<RoutePoint> points = new ArrayList<>();
        points.add(point);
        route.setRoutePoints(points);
        
        return route;
    }

    @Test
    void findByDifficulty_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> easyRoutes = routeRepository.findByDifficulty(1, pageable);
        Page<Route> moderateRoutes = routeRepository.findByDifficulty(3, pageable);
        Page<Route> hardRoutes = routeRepository.findByDifficulty(5, pageable);
        Page<Route> nonExistentDifficulty = routeRepository.findByDifficulty(10, pageable);

        // Assert
        assertEquals(1, easyRoutes.getTotalElements());
        assertEquals("Easy Route", easyRoutes.getContent().get(0).getName());
        
        assertEquals(1, moderateRoutes.getTotalElements());
        assertEquals("Moderate Route", moderateRoutes.getContent().get(0).getName());
        
        assertEquals(1, hardRoutes.getTotalElements());
        assertEquals("Hard Route", hardRoutes.getContent().get(0).getName());
        
        assertEquals(0, nonExistentDifficulty.getTotalElements());
    }

    @Test
    void findWithFilters_DifficultyOnly_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            3, null, null, null, null, null, pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Moderate Route", result.getContent().get(0).getName());
    }

    @Test
    void findWithFilters_RouteTypeOnly_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            null, Route.RouteType.CYCLING, null, null, null, null, pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Easy Route", result.getContent().get(0).getName());
    }

    @Test
    void findWithFilters_DistanceRangeOnly_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            null, null, 20000.0, 30000.0, null, null, pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Hard Route", result.getContent().get(0).getName());
    }

    @Test
    void findWithFilters_SurfaceTypeOnly_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            null, null, null, null, "\"surface\":\"asphalt\"", null, pageable
        );

        // Assert
        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream().anyMatch(r -> r.getName().equals("Easy Route")));
        assertTrue(result.getContent().stream().anyMatch(r -> r.getName().equals("Easy Road Route")));
    }

    @Test
    void findWithFilters_IsPublicOnly_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            null, null, null, null, null, true, pageable
        );

        // Assert
        assertEquals(3, result.getTotalElements());
        assertTrue(result.getContent().stream().noneMatch(r -> !r.getIsPublic()));
    }

    @Test
    void findWithFilters_CombinedFilters_ShouldReturnMatchingRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            null, Route.RouteType.MOUNTAIN_BIKING, 10000.0, 20000.0, "\"surface\":\"gravel\"", true, pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Moderate Route", result.getContent().get(0).getName());
    }

    @Test
    void findWithFilters_NoMatchingRoutes_ShouldReturnEmptyPage() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            5, Route.RouteType.CYCLING, null, null, null, true, pageable
        );

        // Assert
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findWithFilters_AllParametersNull_ShouldReturnAllRoutes() {
        // Act
        Page<Route> result = routeRepository.findWithFilters(
            null, null, null, null, null, null, pageable
        );

        // Assert
        assertEquals(5, result.getTotalElements());
    }
}