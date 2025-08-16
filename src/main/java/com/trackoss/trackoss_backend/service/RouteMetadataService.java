package com.trackoss.trackoss_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteMetadataService {
    
    private final ObjectMapper objectMapper;
    private final RouteStatisticsService routeStatisticsService;
    private final OpenStreetMapService openStreetMapService;
    
    /**
     * Enhance route metadata with road type segments and percentages
     */
    public void enhanceRouteMetadata(Route route) {
        try {
            // Parse existing metadata or create new
            String metadataStr = route.getMetadata();
            ObjectNode metadata = metadataStr != null && !metadataStr.isEmpty() 
                ? (ObjectNode) objectMapper.readTree(metadataStr)
                : objectMapper.createObjectNode();
            
            // Calculate road type segments
            List<RoadTypeSegment> segments = calculateRoadTypeSegments(route);
            
            // Calculate percentages
            Map<String, RoadTypeStats> roadTypeStats = calculateRoadTypeStats(segments, route.getTotalDistance());
            
            // Add to metadata
            metadata.set("roadTypeSegments", convertSegmentsToJson(segments));
            metadata.set("roadTypeStats", convertStatsToJson(roadTypeStats));
            
            // Save updated metadata
            route.setMetadata(objectMapper.writeValueAsString(metadata));
            
            log.info("Enhanced metadata for route {} with {} road type segments", 
                    route.getId(), segments.size());
                    
        } catch (Exception e) {
            log.error("Failed to enhance route metadata: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Calculate road type segments from route points
     */
    private List<RoadTypeSegment> calculateRoadTypeSegments(Route route) {
        List<RoadTypeSegment> segments = new ArrayList<>();
        List<RoutePoint> points = route.getRoutePoints();
        
        if (points.size() < 2) {
            return segments;
        }
        
        // Start with default road type
        String currentRoadType = "PAVED_ROAD";
        int segmentStartIndex = 0;
        List<RoutePoint> currentSegmentPoints = new ArrayList<>();
        currentSegmentPoints.add(points.get(0));
        
        for (int i = 1; i < points.size(); i++) {
            RoutePoint point = points.get(i);
            String pointRoadType = extractRoadTypeFromPoint(point);
            
            if (!pointRoadType.equals(currentRoadType)) {
                // Road type changed, create segment for previous type
                if (currentSegmentPoints.size() > 1) {
                    RoadTypeSegment segment = createSegment(
                        currentRoadType, 
                        segmentStartIndex, 
                        i - 1,
                        new ArrayList<>(currentSegmentPoints)
                    );
                    segments.add(segment);
                }
                
                // Start new segment
                currentRoadType = pointRoadType;
                segmentStartIndex = i - 1;
                currentSegmentPoints.clear();
                currentSegmentPoints.add(points.get(i - 1));
            }
            
            currentSegmentPoints.add(point);
        }
        
        // Add final segment
        if (currentSegmentPoints.size() > 1) {
            RoadTypeSegment segment = createSegment(
                currentRoadType,
                segmentStartIndex,
                points.size() - 1,
                currentSegmentPoints
            );
            segments.add(segment);
        }
        
        return segments;
    }
    
    /**
     * Extract road type from a route point's properties or metadata
     */
    private String extractRoadTypeFromPoint(RoutePoint point) {
        // Check if point has metadata with road type
        if (point.getDescription() != null && !point.getDescription().isEmpty()) {
            try {
                JsonNode metadata = objectMapper.readTree(point.getDescription());
                if (metadata.has("roadType")) {
                    return metadata.get("roadType").asText();
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }
        
        // Try to get road type from OpenStreetMap
        try {
            Optional<OpenStreetMapService.RoadData> roadData = openStreetMapService.getRoadDataQuick(
                point.getLatitude(), 
                point.getLongitude()
            );
            
            if (roadData.isPresent() && roadData.get().highway() != null) {
                String roadType = openStreetMapService.osmHighwayToRoadType(roadData.get().highway());
                log.info("Got road type {} from OSM highway {} for point at {},{}", 
                    roadType, roadData.get().highway(), point.getLatitude(), point.getLongitude());
                return roadType;
            } else {
                log.debug("No OSM highway data found for point at {},{}", 
                    point.getLatitude(), point.getLongitude());
            }
        } catch (Exception e) {
            log.warn("Failed to get OSM data for point at {},{}: {}", 
                point.getLatitude(), point.getLongitude(), e.getMessage());
        }
        
        // Fallback to heuristics if OSM data not available
        return determineRoadTypeHeuristically(point);
    }
    
    /**
     * Determine road type based on heuristics
     */
    private String determineRoadTypeHeuristically(RoutePoint point) {
        // Use a more intelligent fallback based on context clues
        
        // Check point type for hints
        if (point.getPointType() == RoutePoint.PointType.START_POINT ||
            point.getPointType() == RoutePoint.PointType.END_POINT) {
            // Start/end points are often on accessible roads
            return "PAVED_ROAD";
        }
        
        // Check description for keywords
        if (point.getDescription() != null) {
            String desc = point.getDescription().toLowerCase();
            if (desc.contains("trail") || desc.contains("path")) {
                return "TRAIL";
            } else if (desc.contains("urban") || desc.contains("city")) {
                return "BIKE_LANE";
            } else if (desc.contains("residential")) {
                return "RESIDENTIAL";
            } else if (desc.contains("park")) {
                return "BIKE_PATH";
            } else if (desc.contains("gravel") || desc.contains("dirt")) {
                return "GRAVEL_ROAD";
            }
        }
        
        // Default to PAVED_ROAD as the most common type
        log.debug("Using default road type PAVED_ROAD for point at {},{}", 
            point.getLatitude(), point.getLongitude());
        return "PAVED_ROAD";
    }
    
    /**
     * Create a road type segment
     */
    private RoadTypeSegment createSegment(String roadType, int startIndex, int endIndex, 
                                         List<RoutePoint> points) {
        RoadTypeSegment segment = new RoadTypeSegment();
        segment.roadType = roadType;
        segment.startIndex = startIndex;
        segment.endIndex = endIndex;
        segment.points = points;
        
        // Calculate segment distance
        double distance = 0;
        for (int i = 1; i < points.size(); i++) {
            distance += routeStatisticsService.calculateDistance(
                points.get(i - 1).getLatitude(),
                points.get(i - 1).getLongitude(),
                points.get(i).getLatitude(),
                points.get(i).getLongitude()
            );
        }
        segment.distance = distance;
        
        // Assign color based on road type
        segment.color = getRoadTypeColor(roadType);
        
        return segment;
    }
    
    /**
     * Get color for a road type
     */
    private String getRoadTypeColor(String roadType) {
        Map<String, String> colorMap = Map.ofEntries(
            Map.entry("HIGHWAY", "#FF0000"),          // Red
            Map.entry("ARTERIAL", "#FF4500"),         // Orange Red
            Map.entry("PAVED_ROAD", "#FFA500"),       // Orange
            Map.entry("RESIDENTIAL", "#FFFF00"),      // Yellow
            Map.entry("BIKE_LANE", "#00FF00"),        // Green
            Map.entry("BIKE_PATH", "#32CD32"),        // Lime Green
            Map.entry("SHARED_USE_PATH", "#00CED1"),  // Dark Turquoise
            Map.entry("GRAVEL_ROAD", "#8B4513"),      // Saddle Brown
            Map.entry("DIRT_ROAD", "#A0522D"),        // Sienna
            Map.entry("TRAIL", "#228B22"),            // Forest Green
            Map.entry("SINGLE_TRACK", "#006400"),     // Dark Green
            Map.entry("BRIDGE", "#4169E1"),           // Royal Blue
            Map.entry("TUNNEL", "#483D8B"),           // Dark Slate Blue
            Map.entry("FERRY", "#1E90FF"),            // Dodger Blue
            Map.entry("BOARDWALK", "#DEB887"),        // Burlywood
            Map.entry("STAIRS", "#808080"),           // Gray
            Map.entry("PEDESTRIAN_ONLY", "#FF69B4")   // Hot Pink
        );
        
        return colorMap.getOrDefault(roadType, "#808080");
    }
    
    /**
     * Calculate road type statistics (percentages)
     */
    private Map<String, RoadTypeStats> calculateRoadTypeStats(List<RoadTypeSegment> segments, 
                                                              Double totalDistance) {
        Map<String, RoadTypeStats> stats = new HashMap<>();
        
        // Group segments by road type
        Map<String, List<RoadTypeSegment>> groupedSegments = segments.stream()
            .collect(Collectors.groupingBy(s -> s.roadType));
        
        // Always use the actual calculated distance from segments for percentage calculation
        // This ensures percentages add up to 100%
        double actualTotalDistance = segments.stream().mapToDouble(s -> s.distance).sum();
        
        // Log if there's a discrepancy with the route's total distance
        if (totalDistance != null && Math.abs(totalDistance - actualTotalDistance) > 1.0) {
            log.warn("Route total distance ({}) differs from calculated segment distance ({})", 
                    totalDistance, actualTotalDistance);
        }
        
        for (Map.Entry<String, List<RoadTypeSegment>> entry : groupedSegments.entrySet()) {
            String roadType = entry.getKey();
            List<RoadTypeSegment> typeSegments = entry.getValue();
            
            RoadTypeStats stat = new RoadTypeStats();
            stat.roadType = roadType;
            stat.totalDistance = typeSegments.stream().mapToDouble(s -> s.distance).sum();
            stat.percentage = (stat.totalDistance / actualTotalDistance) * 100;
            stat.segmentCount = typeSegments.size();
            stat.color = getRoadTypeColor(roadType);
            
            stats.put(roadType, stat);
        }
        
        return stats;
    }
    
    /**
     * Convert segments to JSON
     */
    private ArrayNode convertSegmentsToJson(List<RoadTypeSegment> segments) {
        ArrayNode segmentsArray = objectMapper.createArrayNode();
        
        for (RoadTypeSegment segment : segments) {
            ObjectNode segmentNode = objectMapper.createObjectNode();
            segmentNode.put("roadType", segment.roadType);
            segmentNode.put("startIndex", segment.startIndex);
            segmentNode.put("endIndex", segment.endIndex);
            segmentNode.put("distance", segment.distance);
            segmentNode.put("color", segment.color);
            
            // Add coordinate array for the segment
            ArrayNode coordinates = objectMapper.createArrayNode();
            for (RoutePoint point : segment.points) {
                ArrayNode coord = objectMapper.createArrayNode();
                coord.add(point.getLongitude());
                coord.add(point.getLatitude());
                if (point.getElevation() != null) {
                    coord.add(point.getElevation());
                }
                coordinates.add(coord);
            }
            segmentNode.set("coordinates", coordinates);
            
            segmentsArray.add(segmentNode);
        }
        
        return segmentsArray;
    }
    
    /**
     * Convert stats to JSON
     */
    private ObjectNode convertStatsToJson(Map<String, RoadTypeStats> stats) {
        ObjectNode statsNode = objectMapper.createObjectNode();
        
        // Sort by percentage descending
        List<RoadTypeStats> sortedStats = new ArrayList<>(stats.values());
        sortedStats.sort((a, b) -> Double.compare(b.percentage, a.percentage));
        
        ArrayNode statsArray = objectMapper.createArrayNode();
        for (RoadTypeStats stat : sortedStats) {
            ObjectNode statNode = objectMapper.createObjectNode();
            statNode.put("roadType", stat.roadType);
            statNode.put("distance", stat.totalDistance);
            statNode.put("percentage", String.format("%.1f", stat.percentage));
            statNode.put("segmentCount", stat.segmentCount);
            statNode.put("color", stat.color);
            statsArray.add(statNode);
        }
        
        statsNode.set("breakdown", statsArray);
        statsNode.put("totalTypes", stats.size());
        
        return statsNode;
    }
    
    /**
     * Internal class for road type segments
     */
    private static class RoadTypeSegment {
        String roadType;
        int startIndex;
        int endIndex;
        double distance;
        String color;
        List<RoutePoint> points;
    }
    
    /**
     * Internal class for road type statistics
     */
    private static class RoadTypeStats {
        String roadType;
        double totalDistance;
        double percentage;
        int segmentCount;
        String color;
    }
}
