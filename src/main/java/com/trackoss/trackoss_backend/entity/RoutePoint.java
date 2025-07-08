package com.trackoss.trackoss_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "route_points")
@Data
@EqualsAndHashCode(exclude = {"route"})
@ToString(exclude = {"route"})
public class RoutePoint {
    
    @Id
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    @JsonBackReference
    private Route route;
    
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;
    
    @Column(nullable = false)
    private Double latitude;
    
    @Column(nullable = false)
    private Double longitude;
    
    private Double elevation; // in meters, optional
    
    private LocalDateTime timestamp; // optional, for track points
    
    @Enumerated(EnumType.STRING)
    @Column(name = "point_type")
    private PointType pointType = PointType.TRACK_POINT;
    
    private String name; // optional, for waypoints
    
    @Column(columnDefinition = "TEXT")
    private String description; // optional, for waypoints
    
    public enum PointType {
        WAYPOINT,      // Named points of interest
        TRACK_POINT,   // GPS track points
        ROUTE_POINT,   // Calculated route points
        START_POINT,   // Starting point of the route
        END_POINT      // Ending point of the route
    }
}