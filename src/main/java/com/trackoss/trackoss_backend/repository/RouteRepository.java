package com.trackoss.trackoss_backend.repository;

import com.trackoss.trackoss_backend.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RouteRepository extends JpaRepository<Route, UUID> {
    
    // Find routes by user ID
    Page<Route> findByUserId(String userId, Pageable pageable);
    
    // Find public routes
    Page<Route> findByIsPublicTrue(Pageable pageable);
    
    // Find routes by name containing (case insensitive)
    Page<Route> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Find routes by type
    Page<Route> findByRouteType(Route.RouteType routeType, Pageable pageable);
    
    // Find routes by user and public status
    @Query("SELECT r FROM Route r WHERE r.userId = :userId OR r.isPublic = true")
    Page<Route> findByUserIdOrPublic(@Param("userId") String userId, Pageable pageable);
    
    // Count routes by user
    long countByUserId(String userId);
    
    // Find routes within distance range
    @Query("SELECT r FROM Route r WHERE r.totalDistance BETWEEN :minDistance AND :maxDistance")
    Page<Route> findByDistanceRange(@Param("minDistance") Double minDistance, 
                                   @Param("maxDistance") Double maxDistance, 
                                   Pageable pageable);
}