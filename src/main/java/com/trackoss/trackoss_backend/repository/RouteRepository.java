package com.trackoss.trackoss_backend.repository;

import com.trackoss.trackoss_backend.entity.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    // Find routes near a location using spatial search
    // This is a simplified implementation using bounding box approximation
    // For production, consider using PostGIS spatial functions
    @Query("SELECT DISTINCT r FROM Route r JOIN r.routePoints rp " +
           "WHERE rp.latitude BETWEEN :minLat AND :maxLat " +
           "AND rp.longitude BETWEEN :minLon AND :maxLon")
    Page<Route> findNearbyRoutes(@Param("minLat") Double minLat,
                                @Param("maxLat") Double maxLat,
                                @Param("minLon") Double minLon,
                                @Param("maxLon") Double maxLon,
                                Pageable pageable);

    // Helper method to find nearby routes with radius calculation
    default Page<Route> findNearbyRoutes(Double latitude, Double longitude, Double radiusKm, Pageable pageable) {
        // Convert radius from km to approximate degrees (rough approximation)
        // 1 degree ≈ 111 km at equator
        Double radiusDegrees = radiusKm / 111.0;

        Double minLat = latitude - radiusDegrees;
        Double maxLat = latitude + radiusDegrees;
        Double minLon = longitude - radiusDegrees;
        Double maxLon = longitude + radiusDegrees;

        return findNearbyRoutes(minLat, maxLat, minLon, maxLon, pageable);
    }
}