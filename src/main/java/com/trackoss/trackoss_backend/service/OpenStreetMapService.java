package com.trackoss.trackoss_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Service for fetching road type and surface data from OpenStreetMap
 * Uses Overpass API for detailed road information
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenStreetMapService {
    
    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";
    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/reverse";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    
    /**
     * Road type data for a specific point
     */
    public record RoadData(
        String highway,      // OSM highway tag (primary, secondary, cycleway, etc.)
        String surface,      // Surface type (asphalt, gravel, dirt, etc.)
        String bicycle,      // Bicycle access (yes, no, designated, etc.)
        String name,         // Road name
        Integer maxSpeed,    // Speed limit if available
        String lanes,        // Number of lanes
        boolean isTunnel,
        boolean isBridge,
        Map<String, String> additionalTags
    ) {}
    
    /**
     * Get road data for a specific coordinate using reverse geocoding
     * This is faster but less detailed than Overpass API
     */
    @Cacheable(value = "osmRoadData", key = "#lat + ',' + #lon")
    public Optional<RoadData> getRoadDataQuick(double lat, double lon) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(NOMINATIM_API_URL)
                .queryParam("format", "json")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("zoom", 17)  // Street level detail
                .queryParam("extratags", 1)
                .queryParam("addressdetails", 1)
                .toUriString();
            
            log.debug("Fetching OSM data from: {}", url);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            
            // First try extratags
            JsonNode extratags = json.get("extratags");
            String highway = null;
            String surface = null;
            String bicycle = null;
            
            if (extratags != null && extratags.size() > 0) {
                highway = extratags.has("highway") ? extratags.get("highway").asText() : null;
                surface = extratags.has("surface") ? extratags.get("surface").asText() : null;
                bicycle = extratags.has("bicycle") ? extratags.get("bicycle").asText() : null;
            }
            
            // If no highway tag in extratags, check the type field
            if (highway == null && json.has("type")) {
                String type = json.get("type").asText();
                if ("road".equals(type) || "way".equals(type)) {
                    // Try to infer from the display name or class
                    if (json.has("class")) {
                        String classType = json.get("class").asText();
                        if ("highway".equals(classType)) {
                            highway = json.has("osm_value") ? json.get("osm_value").asText() : "unclassified";
                        }
                    }
                }
            }
            
            // Create RoadData even if some fields are null
            return Optional.of(new RoadData(
                highway,
                surface,
                bicycle,
                json.has("display_name") ? json.get("display_name").asText() : null,
                extratags != null && extratags.has("maxspeed") ? parseMaxSpeed(extratags.get("maxspeed").asText()) : null,
                extratags != null && extratags.has("lanes") ? extratags.get("lanes").asText() : null,
                extratags != null && extratags.has("tunnel") && "yes".equals(extratags.get("tunnel").asText()),
                extratags != null && extratags.has("bridge") && "yes".equals(extratags.get("bridge").asText()),
                extratags != null ? extractAdditionalTags(extratags) : new HashMap<>()
            ));
            
        } catch (Exception e) {
            log.error("Error fetching road data from Nominatim for lat={}, lon={}: {}", lat, lon, e.getMessage());
            // Return a default RoadData with null highway to trigger fallback
            return Optional.of(new RoadData(
                null, null, null, null, null, null, false, false, new HashMap<>()
            ));
        }
    }
    
    /**
     * Get detailed road data for a route segment using Overpass API
     * This queries all ways within a bounding box
     */
    @Cacheable(value = "osmRouteData", key = "#minLat + ',' + #minLon + ',' + #maxLat + ',' + #maxLon")
    public List<RoadSegment> getRouteRoadData(double minLat, double minLon, double maxLat, double maxLon) {
        try {
            // Overpass QL query to get all ways with highway tags in the bounding box
            String query = String.format("""
                [out:json][timeout:25];
                (
                  way["highway"](%f,%f,%f,%f);
                );
                out body;
                >;
                out skel qt;
                """, minLat, minLon, maxLat, maxLon);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                OVERPASS_API_URL, 
                query, 
                String.class
            );
            
            return parseOverpassResponse(response.getBody());
            
        } catch (Exception e) {
            log.error("Error fetching route data from Overpass API: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Match route points to OSM ways and get road types
     */
    public Map<RoutePoint, RoadData> matchRoutePointsToRoads(List<RoutePoint> points) {
        Map<RoutePoint, RoadData> roadDataMap = new HashMap<>();
        
        // Process in batches to avoid overwhelming the API
        for (RoutePoint point : points) {
            getRoadDataQuick(point.getLatitude(), point.getLongitude())
                .ifPresent(roadData -> roadDataMap.put(point, roadData));
            
            // Add small delay to respect API rate limits
            try {
                Thread.sleep(100); // 100ms delay between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return roadDataMap;
    }
    
    /**
     * Convert OSM highway tag to our road type classification
     */
    public String osmHighwayToRoadType(String highway) {
        if (highway == null || highway.isEmpty()) {
            log.debug("No highway tag provided, returning PAVED_ROAD as default");
            return "PAVED_ROAD";
        }
        
        String roadType = switch (highway.toLowerCase()) {
            case "motorway", "motorway_link" -> "HIGHWAY";
            case "trunk", "trunk_link" -> "HIGHWAY";
            case "primary", "primary_link" -> "ARTERIAL";
            case "secondary", "secondary_link" -> "ARTERIAL";
            case "tertiary", "tertiary_link" -> "PAVED_ROAD";
            case "residential" -> "RESIDENTIAL";
            case "living_street" -> "RESIDENTIAL";
            case "service" -> "PAVED_ROAD";
            case "cycleway" -> "BIKE_PATH";
            case "path" -> "SHARED_USE_PATH";
            case "track" -> "GRAVEL_ROAD";
            case "footway", "pedestrian" -> "PEDESTRIAN_ONLY";
            case "steps" -> "STAIRS";
            case "bridleway" -> "TRAIL";
            case "unclassified" -> "PAVED_ROAD";
            case "road" -> "PAVED_ROAD";
            default -> "PAVED_ROAD";
        };
        
        log.debug("Mapped OSM highway '{}' to road type '{}'", highway, roadType);
        return roadType;
    }
    
    /**
     * Determine path type based on additional tags
     */
    private String determinatePathType(Map<String, String> tags) {
        if (tags == null) return "TRAIL";
        
        String bicycle = tags.get("bicycle");
        String foot = tags.get("foot");
        String surface = tags.get("surface");
        
        if ("designated".equals(bicycle) || "yes".equals(bicycle)) {
            return "SHARED_USE_PATH";
        }
        if ("asphalt".equals(surface) || "concrete".equals(surface)) {
            return "SHARED_USE_PATH";
        }
        
        return "TRAIL";
    }
    
    /**
     * Parse Overpass API response
     */
    private List<RoadSegment> parseOverpassResponse(String json) {
        List<RoadSegment> segments = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root.get("elements");
            
            if (elements != null && elements.isArray()) {
                for (JsonNode element : elements) {
                    if ("way".equals(element.get("type").asText())) {
                        RoadSegment segment = parseWayElement(element);
                        if (segment != null) {
                            segments.add(segment);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing Overpass response: {}", e.getMessage());
        }
        
        return segments;
    }
    
    /**
     * Parse a way element from Overpass response
     */
    private RoadSegment parseWayElement(JsonNode way) {
        try {
            JsonNode tags = way.get("tags");
            if (tags == null) return null;
            
            RoadSegment segment = new RoadSegment();
            segment.osmId = way.get("id").asLong();
            segment.highway = tags.has("highway") ? tags.get("highway").asText() : null;
            segment.surface = tags.has("surface") ? tags.get("surface").asText() : null;
            segment.name = tags.has("name") ? tags.get("name").asText() : null;
            segment.bicycle = tags.has("bicycle") ? tags.get("bicycle").asText() : null;
            segment.maxSpeed = tags.has("maxspeed") ? parseMaxSpeed(tags.get("maxspeed").asText()) : null;
            
            // Parse nodes if available
            JsonNode nodes = way.get("nodes");
            if (nodes != null && nodes.isArray()) {
                segment.nodeIds = new ArrayList<>();
                for (JsonNode node : nodes) {
                    segment.nodeIds.add(node.asLong());
                }
            }
            
            segment.roadType = osmHighwayToRoadType(segment.highway);
            
            return segment;
            
        } catch (Exception e) {
            log.error("Error parsing way element: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse max speed from various formats (e.g., "50", "50 mph", "50 km/h")
     */
    private Integer parseMaxSpeed(String maxSpeed) {
        if (maxSpeed == null) return null;
        
        try {
            // Remove units and parse
            String cleaned = maxSpeed.replaceAll("[^0-9]", "");
            int speed = Integer.parseInt(cleaned);
            
            // Convert mph to km/h if needed
            if (maxSpeed.contains("mph")) {
                speed = (int) (speed * 1.60934);
            }
            
            return speed;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Extract additional useful tags
     */
    private Map<String, String> extractAdditionalTags(JsonNode tags) {
        Map<String, String> additional = new HashMap<>();
        
        String[] usefulTags = {
            "smoothness", "trail_visibility", "sac_scale", 
            "mtb:scale", "width", "incline", "lit", "segregated"
        };
        
        for (String tag : usefulTags) {
            if (tags.has(tag)) {
                additional.put(tag, tags.get(tag).asText());
            }
        }
        
        return additional;
    }
    
    /**
     * Road segment from OSM
     */
    public static class RoadSegment {
        public long osmId;
        public String highway;
        public String surface;
        public String name;
        public String bicycle;
        public Integer maxSpeed;
        public List<Long> nodeIds;
        public String roadType;
    }
}
