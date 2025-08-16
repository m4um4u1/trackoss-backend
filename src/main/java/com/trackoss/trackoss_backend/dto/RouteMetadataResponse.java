package com.trackoss.trackoss_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteMetadataResponse {
    private List<RoadTypeSegment> roadTypeSegments;
    private RoadTypeStats roadTypeStats;
    private String metadata; // Full metadata JSON string
    
    @Data
    public static class RoadTypeSegment {
        private String roadType;
        private int startIndex;
        private int endIndex;
        private double distance;
        private String color;
        private List<double[]> coordinates;
        private String surface;
    }
    
    @Data
    public static class RoadTypeStats {
        private List<RoadTypeStat> breakdown;
        private double totalDistance;
        private int totalTypes;
    }
    
    @Data
    public static class RoadTypeStat {
        private String roadType;
        private double distance;
        private String percentage;
        private int segmentCount;
        private String color;
    }
}
