package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ConditionalStatisticsTest {
    
    private RouteStatisticsService routeStatisticsService;
    
    @BeforeEach
    void setUp() {
        routeStatisticsService = new RouteStatisticsService();
    }
    
    @Test
    void testCalculateMissingStatistics_AllMissing() {
        Route route = createTestRoute();
        
        // All statistics are null initially
        assertNull(route.getTotalDistance());
        assertNull(route.getTotalElevationGain());
        assertNull(route.getEstimatedDuration());
        
        routeStatisticsService.calculateMissingStatistics(route);
        
        // All statistics should now be calculated
        assertNotNull(route.getTotalDistance());
        assertNotNull(route.getTotalElevationGain());
        assertNotNull(route.getEstimatedDuration());
        assertTrue(route.getTotalDistance() > 0);
        assertTrue(route.getTotalElevationGain() >= 0);
        assertTrue(route.getEstimatedDuration() > 0);
    }
    
    @Test
    void testCalculateMissingStatistics_SomeProvided() {
        Route route = createTestRoute();
        
        // Pre-set some statistics
        route.setTotalDistance(50000.0); // 50km
        route.setTotalElevationGain(1200.0); // 1200m
        // Leave duration null
        
        routeStatisticsService.calculateMissingStatistics(route);
        
        // Pre-set values should remain unchanged
        assertEquals(50000.0, route.getTotalDistance());
        assertEquals(1200.0, route.getTotalElevationGain());
        
        // Duration should be calculated
        assertNotNull(route.getEstimatedDuration());
        assertTrue(route.getEstimatedDuration() > 0);
    }
    
    @Test
    void testCalculateMissingStatistics_AllProvided() {
        Route route = createTestRoute();
        
        // Pre-set all statistics
        route.setTotalDistance(50000.0);
        route.setTotalElevationGain(1200.0);
        route.setEstimatedDuration(10800L); // 3 hours
        
        routeStatisticsService.calculateMissingStatistics(route);
        
        // All values should remain unchanged
        assertEquals(50000.0, route.getTotalDistance());
        assertEquals(1200.0, route.getTotalElevationGain());
        assertEquals(10800L, route.getEstimatedDuration());
    }
    
    private Route createTestRoute() {
        Route route = new Route();
        route.setName("Test Route");
        route.setRouteType(Route.RouteType.CYCLING);
        
        // Create some test points with elevation
        RoutePoint point1 = new RoutePoint();
        point1.setLatitude(47.6062);
        point1.setLongitude(-122.3321);
        point1.setElevation(100.0);
        
        RoutePoint point2 = new RoutePoint();
        point2.setLatitude(47.6162);
        point2.setLongitude(-122.3221);
        point2.setElevation(150.0);
        
        RoutePoint point3 = new RoutePoint();
        point3.setLatitude(47.6262);
        point3.setLongitude(-122.3121);
        point3.setElevation(120.0);
        
        route.setRoutePoints(Arrays.asList(point1, point2, point3));
        
        return route;
    }
}