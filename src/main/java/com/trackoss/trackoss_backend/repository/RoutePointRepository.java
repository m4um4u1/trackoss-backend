package com.trackoss.trackoss_backend.repository;

import com.trackoss.trackoss_backend.entity.RoutePoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoutePointRepository extends JpaRepository<RoutePoint, UUID> {
    
    // Find points by route ID ordered by sequence
    List<RoutePoint> findByRouteIdOrderBySequenceOrderAsc(UUID routeId);
    
    // Find waypoints for a route
    List<RoutePoint> findByRouteIdAndPointTypeOrderBySequenceOrderAsc(UUID routeId, RoutePoint.PointType pointType);
    
    // Get the maximum sequence order for a route
    @Query("SELECT COALESCE(MAX(rp.sequenceOrder), 0) FROM RoutePoint rp WHERE rp.route.id = :routeId")
    Integer findMaxSequenceOrderByRouteId(@Param("routeId") UUID routeId);
    
    // Count points in a route
    long countByRouteId(UUID routeId);
    
    // Delete all points for a route
    void deleteByRouteId(UUID routeId);
}