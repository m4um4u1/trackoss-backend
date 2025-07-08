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
    @GeneratedValue(strategy = GenerationType.UUID)
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
    
    @Column(columnDefinition = "jsonb")
    private String metadata; // JSON for flexible additional data
    
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
        DRIVING,
        MOTORCYCLE,
        PUBLIC_TRANSPORT,
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
}