package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteCalculationServiceTest {
    
    private RouteCalculationService routeCalculationService;
    
    @BeforeEach
    void setUp() {
        routeCalculationService = new RouteCalculationService();
    }
    
    @Test
    void testCalculateDistance() {
        // Test distance calculation between two known points
        // Berlin to Munich (approximately 504 km)
        double berlinLat = 52.5200;
        double berlinLon = 13.4050;
        double munichLat = 48.1351;
        double munichLon = 11.5820;
        
        double distance = routeCalculationService.calculateDistance(berlinLat, berlinLon, munichLat, munichLon);
        
        // Should be approximately 504,000 meters (504 km)
        assertTrue(distance > 500000 && distance < 510000, 
                "Distance should be approximately 504km, but was: " + distance);
    }
    
    @Test
    void testCalculateRouteStatisticsWithTwoPoints() {
        Route route = new Route();
        route.setRouteType(Route.RouteType.CYCLING);
        
        // Add two points
        RoutePoint point1 = new RoutePoint();
        point1.setLatitude(52.5200);
        point1.setLongitude(13.4050);
        point1.setElevation(100.0);
        point1.setSequenceOrder(0);
        route.addRoutePoint(point1);
        
        RoutePoint point2 = new RoutePoint();
        point2.setLatitude(52.5300);
        point2.setLongitude(13.4150);
        point2.setElevation(150.0);
        point2.setSequenceOrder(1);
        route.addRoutePoint(point2);
        
        routeCalculationService.calculateRouteStatistics(route);
        
        assertNotNull(route.getTotalDistance());
        assertTrue(route.getTotalDistance() > 0);
        
        assertNotNull(route.getTotalElevationGain());
        assertEquals(50.0, route.getTotalElevationGain(), 0.1); // 150 - 100 = 50m gain
        
        assertNotNull(route.getEstimatedDuration());
        assertTrue(route.getEstimatedDuration() > 0);
    }
    
    @Test
    void testCalculateRouteStatisticsWithOnePoint() {
        Route route = new Route();
        route.setRouteType(Route.RouteType.CYCLING);
        
        RoutePoint point1 = new RoutePoint();
        point1.setLatitude(52.5200);
        point1.setLongitude(13.4050);
        point1.setSequenceOrder(0);
        route.addRoutePoint(point1);
        
        routeCalculationService.calculateRouteStatistics(route);
        
        assertEquals(0.0, route.getTotalDistance());
        assertEquals(0.0, route.getTotalElevationGain());
        assertEquals(0L, route.getEstimatedDuration());
    }
    
    @Test
    void testCalculateRouteStatisticsWithElevationLoss() {
        Route route = new Route();
        route.setRouteType(Route.RouteType.CYCLING);
        
        RoutePoint point1 = new RoutePoint();
        point1.setLatitude(52.5200);
        point1.setLongitude(13.4050);
        point1.setElevation(200.0);
        point1.setSequenceOrder(0);
        route.addRoutePoint(point1);
        
        RoutePoint point2 = new RoutePoint();
        point2.setLatitude(52.5300);
        point2.setLongitude(13.4150);
        point2.setElevation(100.0); // Lower elevation
        point2.setSequenceOrder(1);
        route.addRoutePoint(point2);
        
        routeCalculationService.calculateRouteStatistics(route);
        
        // Should not count elevation loss as gain
        assertEquals(0.0, route.getTotalElevationGain());
    }
}