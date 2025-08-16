package com.trackoss.trackoss_backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Cache configuration for storing OSM data and reducing API calls
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "osmRoadData",      // Cache for individual road data points
            "osmRouteData",     // Cache for route segments
            "mapTiles"          // Cache for map tile URLs
        );
    }
    
    // RestTemplate bean moved to main application class to avoid conflicts
}
