package com.trackoss.trackoss_backend.dto;

import com.trackoss.trackoss_backend.entity.Route;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class RouteResponse {
    
    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String userId;
    private Double totalDistance;
    private Double totalElevationGain;
    private Long estimatedDuration;
    private Route.RouteType routeType;
    private Boolean isPublic;
    private String metadata;
    private List<RoutePointResponse> points;
    private Integer pointCount;
    
    @Data
    public static class RoutePointResponse {
        private UUID id;
        private Integer sequenceOrder;
        private Double latitude;
        private Double longitude;
        private Double elevation;
        private LocalDateTime timestamp;
        private String pointType;
        private String name;
        private String description;
    }
}