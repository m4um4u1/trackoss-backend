package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class RouteCalculationService {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * Calculate route statistics including total distance and elevation gain
     */
    public void calculateRouteStatistics(Route route) {
        List<RoutePoint> points = route.getRoutePoints();
        
        if (points.size() < 2) {
            route.setTotalDistance(0.0);
            route.setTotalElevationGain(0.0);
            route.setEstimatedDuration(0L);
            return;
        }
        
        double totalDistance = 0.0;
        double totalElevationGain = 0.0;
        
        for (int i = 1; i < points.size(); i++) {
            RoutePoint prev = points.get(i - 1);
            RoutePoint curr = points.get(i);
            
            // Calculate distance between consecutive points
            double segmentDistance = calculateDistance(
                prev.getLatitude(), prev.getLongitude(),
                curr.getLatitude(), curr.getLongitude()
            );
            totalDistance += segmentDistance;
            
            // Calculate elevation gain
            if (prev.getElevation() != null && curr.getElevation() != null) {
                double elevationDiff = curr.getElevation() - prev.getElevation();
                if (elevationDiff > 0) {
                    totalElevationGain += elevationDiff;
                }
            }
        }
        
        route.setTotalDistance(totalDistance);
        route.setTotalElevationGain(totalElevationGain);
        route.setEstimatedDuration(calculateEstimatedDuration(route));
        
        log.debug("Calculated route statistics - Distance: {}m, Elevation gain: {}m, Duration: {}s", 
                totalDistance, totalElevationGain, route.getEstimatedDuration());
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c * 1000; // Convert to meters
    }
    
    /**
     * Calculate estimated duration based on route type and characteristics
     */
    private long calculateEstimatedDuration(Route route) {
        if (route.getTotalDistance() == null) {
            return 0L;
        }
        
        double distanceKm = route.getTotalDistance() / 1000.0;
        double elevationGainKm = (route.getTotalElevationGain() != null ? route.getTotalElevationGain() : 0.0) / 1000.0;
        
        // Base speed in km/h depending on route type
        double baseSpeed = switch (route.getRouteType()) {
            case CYCLING -> 25.0;
            case MOUNTAIN_BIKING -> 15.0;
            case ROAD_CYCLING -> 35.0;
            case GRAVEL -> 20.0;
            case E_BIKE -> 30.0;
            case HIKING -> 4.0;
            case RUNNING -> 10.0;
            case WALKING -> 3.0;
            case DRIVING -> 50.0;
            case MOTORCYCLE -> 60.0;
            default -> 25.0; // Default to cycling
        };
        
        // Adjust for elevation gain
        double adjustedDistance = distanceKm;
        switch (route.getRouteType()) {
            case HIKING, WALKING -> {
                // Naismith's rule: Add 1 hour for every 600m of elevation gain
                adjustedDistance += elevationGainKm * 1000 / 600 * baseSpeed;
            }
            case CYCLING, MOUNTAIN_BIKING, GRAVEL -> {
                // Cycling: Add time for elevation gain (less impact than hiking)
                adjustedDistance += elevationGainKm * 1000 / 1000 * baseSpeed; // 1 hour per 1000m gain
            }
            case ROAD_CYCLING -> {
                // Road cycling: Minimal elevation impact due to higher speeds
                adjustedDistance += elevationGainKm * 1000 / 1500 * baseSpeed; // 1 hour per 1500m gain
            }
            case E_BIKE -> {
                // E-bike: Very minimal elevation impact
                adjustedDistance += elevationGainKm * 1000 / 2000 * baseSpeed; // 1 hour per 2000m gain
            }
        }
        
        // Calculate duration in seconds
        return Math.round((adjustedDistance / baseSpeed) * 3600);
    }
}