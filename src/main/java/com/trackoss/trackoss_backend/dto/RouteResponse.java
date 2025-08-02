package com.trackoss.trackoss_backend.dto;

import com.trackoss.trackoss_backend.entity.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Schema(description = "Response object containing cycling route information")
public class RouteResponse {

    @Schema(description = "Unique route identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Name of the cycling route", example = "Lake Washington Loop")
    private String name;

    @Schema(description = "Detailed description of the route",
            example = "Scenic 50km loop around Lake Washington with bike lanes and beautiful views")
    private String description;

    @Schema(description = "Route creation timestamp", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Route last update timestamp", example = "2024-01-15T14:45:00")
    private LocalDateTime updatedAt;

    @Schema(description = "ID of the user who created the route", example = "user123")
    private String userId;

    @Schema(description = "Total distance in meters", example = "50000.0")
    private Double totalDistance;

    @Schema(description = "Total elevation gain in meters", example = "1200.0")
    private Double totalElevationGain;

    @Schema(description = "Estimated duration in seconds", example = "10800")
    private Long estimatedDuration;

    @Schema(description = "Type of cycling route", example = "CYCLING")
    private Route.RouteType routeType;

    @Schema(description = "Whether the route is publicly visible", example = "true")
    private Boolean isPublic;
    
    @Schema(description = "Difficulty level (1-5)", example = "3")
    private Integer difficulty;

    @Schema(description = "Additional metadata as JSON string",
            example = "{\"surface\": \"asphalt\", \"difficulty\": 3, \"traffic\": \"low\"}")
    private String metadata;

    @Schema(description = "List of route points (track points and waypoints)")
    private List<RoutePointResponse> points;

    @Schema(description = "Total number of points in the route", example = "150")
    private Integer pointCount;
    
    @Data
    @Schema(description = "Individual point on the cycling route")
    public static class RoutePointResponse {
        @Schema(description = "Unique point identifier", example = "456e7890-e89b-12d3-a456-426614174001")
        private UUID id;

        @Schema(description = "Order of the point in the route sequence", example = "5")
        private Integer sequenceOrder;

        @Schema(description = "Latitude coordinate", example = "47.6062")
        private Double latitude;

        @Schema(description = "Longitude coordinate", example = "-122.3321")
        private Double longitude;

        @Schema(description = "Elevation in meters", example = "56.0")
        private Double elevation;

        @Schema(description = "Timestamp when the point was recorded", example = "2024-01-15T10:35:00")
        private LocalDateTime timestamp;

        @Schema(description = "Type of point", example = "TRACK_POINT",
                allowableValues = {"WAYPOINT", "TRACK_POINT", "ROUTE_POINT", "START_POINT", "END_POINT"})
        private String pointType;

        @Schema(description = "Name of the point (for waypoints)", example = "Rest Stop")
        private String name;

        @Schema(description = "Description of the point", example = "Good place for water refill")
        private String description;
    }
}