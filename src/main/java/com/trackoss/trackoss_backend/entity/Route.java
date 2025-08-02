package com.trackoss.trackoss_backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "routes")
@Data
@EqualsAndHashCode(exclude = {"routePoints"})
@ToString(exclude = {"routePoints"})
public class Route {
    
    @Id
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "user_id")
    private String userId; // For future user management
    
    @Column(name = "total_distance")
    private Double totalDistance; // in meters
    
    @Column(name = "total_elevation_gain")
    private Double totalElevationGain; // in meters
    
    @Column(name = "estimated_duration")
    private Long estimatedDuration; // in seconds
    
    @Enumerated(EnumType.STRING)
    @Column(name = "route_type")
    private RouteType routeType = RouteType.CYCLING;
    
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column(name = "difficulty")
    private Integer difficulty;
    
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @JsonManagedReference
    private List<RoutePoint> routePoints = new ArrayList<>();
    
    public enum RouteType {
        CYCLING,
        MOUNTAIN_BIKING,
        ROAD_CYCLING,
        GRAVEL,
        E_BIKE,
        HIKING,
        RUNNING,
        WALKING,
        OTHER
    }
    
    // Helper methods
    public void addRoutePoint(RoutePoint point) {
        routePoints.add(point);
        point.setRoute(this);
    }
    
    public void removeRoutePoint(RoutePoint point) {
        routePoints.remove(point);
        point.setRoute(null);
    }
    
    // Custom setter for metadata to ensure difficulty synchronization
    public void setMetadata(String metadata) {
        this.metadata = metadata;
        
        // Extract difficulty from metadata if present
        if (metadata != null && !metadata.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.JsonNode metadataJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(metadata);
                if (metadataJson.has("difficulty")) {
                    this.difficulty = metadataJson.get("difficulty").asInt();
                }
            } catch (Exception e) {
                // Log error but continue
                System.err.println("Failed to extract difficulty from metadata: " + e.getMessage());
            }
        }
    }
    
    // Custom setter for difficulty to ensure metadata synchronization
    public void setDifficulty(Integer difficulty) {
        this.difficulty = difficulty;
        
        // Update difficulty in metadata if metadata exists
        if (this.metadata != null && !this.metadata.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode metadataJson = mapper.readTree(this.metadata);
                ((com.fasterxml.jackson.databind.node.ObjectNode) metadataJson).put("difficulty", difficulty);
                this.metadata = mapper.writeValueAsString(metadataJson);
            } catch (Exception e) {
                // Log error but continue
                System.err.println("Failed to update difficulty in metadata: " + e.getMessage());
            }
        } else {
            // Create new metadata with difficulty
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode metadataJson = mapper.createObjectNode();
                metadataJson.put("difficulty", difficulty);
                this.metadata = mapper.writeValueAsString(metadataJson);
            } catch (Exception e) {
                // Log error but continue
                System.err.println("Failed to create metadata with difficulty: " + e.getMessage());
            }
        }
    }
}