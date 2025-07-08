package com.trackoss.trackoss_backend.dto;

import com.trackoss.trackoss_backend.entity.Route;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request object for creating a new cycling route")
public class RouteCreateRequest {
    
    @NotBlank(message = "Route name is required")
    @Size(max = 255, message = "Route name must not exceed 255 characters")
    @Schema(description = "Name of the cycling route", example = "Lake Washington Loop", required = true)
    private String name;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Detailed description of the route", 
            example = "Scenic 50km loop around Lake Washington with bike lanes and beautiful views")
    private String description;
    
    @Schema(description = "Type of cycling route", example = "CYCLING", 
            allowableValues = {"CYCLING", "MOUNTAIN_BIKING", "ROAD_CYCLING", "GRAVEL", "E_BIKE"})
    private Route.RouteType routeType = Route.RouteType.CYCLING;
    
    @Schema(description = "Whether the route is publicly visible", example = "true")
    private Boolean isPublic = false;
    
    @NotEmpty(message = "Route must have at least one point")
    @Valid
    @Schema(description = "List of route points (track points and waypoints)", required = true)
    private List<RoutePointRequest> points;
    
    @Schema(description = "Additional metadata as JSON string", 
            example = "{\"surface\": \"asphalt\", \"difficulty\": 3, \"traffic\": \"low\"}")
    private String metadata; // JSON string for additional data
    
    @Data
    @Schema(description = "Individual point on the cycling route")
    public static class RoutePointRequest {
        @Schema(description = "Latitude coordinate", example = "47.6062", required = true)
        private Double latitude;
        
        @Schema(description = "Longitude coordinate", example = "-122.3321", required = true)
        private Double longitude;
        
        @Schema(description = "Elevation in meters", example = "56.0")
        private Double elevation;
        
        @Schema(description = "Name of the point (for waypoints)", example = "Rest Stop")
        private String name;
        
        @Schema(description = "Description of the point", example = "Good place for water refill")
        private String description;
        
        @Schema(description = "Type of point", example = "TRACK_POINT", 
                allowableValues = {"WAYPOINT", "TRACK_POINT", "ROUTE_POINT"})
        private String pointType = "TRACK_POINT"; // WAYPOINT, TRACK_POINT, ROUTE_POINT
    }
}