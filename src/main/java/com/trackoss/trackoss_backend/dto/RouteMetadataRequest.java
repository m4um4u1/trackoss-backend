package com.trackoss.trackoss_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteMetadataRequest {
    private List<CoordinatePoint> points;
    private Double totalDistance; // Optional: total route distance in meters
    
    @Data
    public static class CoordinatePoint {
        private double latitude;
        private double longitude;
        private Integer sequenceOrder;
    }
}
