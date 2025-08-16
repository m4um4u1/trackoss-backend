package com.trackoss.trackoss_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trackoss.trackoss_backend.dto.RouteMetadataRequest;
import com.trackoss.trackoss_backend.dto.RouteMetadataResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RouteMetadataAnalysisService {
    
    private final OpenStreetMapService openStreetMapService;
    private final ObjectMapper objectMapper;
    private final RouteStatisticsService routeStatisticsService;
    
    /**
     * Analyze route metadata without persisting the route
     */
    public RouteMetadataResponse analyzeRoute(RouteMetadataRequest request) {
        log.info("Starting route metadata analysis for {} points", request.getPoints().size());
        
        RouteMetadataResponse response = new RouteMetadataResponse();
        
        try {
            // Calculate road type segments
            List<RoadTypeSegment> segments = calculateRoadTypeSegments(request.getPoints());
            
            // Calculate statistics
            double totalDistance = request.getTotalDistance() != null ? 
                request.getTotalDistance() : calculateTotalDistance(request.getPoints());
            
            Map<String, RoadTypeStats> roadTypeStats = calculateRoadTypeStats(segments, totalDistance);
            
            // Convert to response format
            response.setRoadTypeSegments(convertSegmentsToResponse(segments));
            response.setRoadTypeStats(convertStatsToResponse(roadTypeStats));
            
            // Create full metadata JSON
            ObjectNode metadata = objectMapper.createObjectNode();
            metadata.set("roadTypeSegments", convertSegmentsToJson(segments));
            metadata.set("roadTypeStats", convertStatsToJson(roadTypeStats));
            response.setMetadata(objectMapper.writeValueAsString(metadata));
            
            log.info("Route metadata analysis complete: {} segments, {} road types", 
                    segments.size(), roadTypeStats.size());
                    
        } catch (Exception e) {
            log.error("Failed to analyze route metadata: {}", e.getMessage(), e);
            // Return partial response even on error
            response.setMetadata("{}");
        }
        
        return response;
    }
    
    /**
     * Calculate road type segments from route points
     */
    private List<RoadTypeSegment> calculateRoadTypeSegments(List<RouteMetadataRequest.CoordinatePoint> points) {
        List<RoadTypeSegment> segments = new ArrayList<>();
        
        if (points.size() < 2) {
            return segments;
        }
        
        // Sort points by sequence order if provided
        List<RouteMetadataRequest.CoordinatePoint> sortedPoints = new ArrayList<>(points);
        if (sortedPoints.stream().allMatch(p -> p.getSequenceOrder() != null)) {
            sortedPoints.sort(Comparator.comparing(RouteMetadataRequest.CoordinatePoint::getSequenceOrder));
        }
        
        String currentRoadType = "PAVED_ROAD";
        int segmentStartIndex = 0;
        List<RouteMetadataRequest.CoordinatePoint> currentSegmentPoints = new ArrayList<>();
        currentSegmentPoints.add(sortedPoints.get(0));
        
        for (int i = 1; i < sortedPoints.size(); i++) {
            RouteMetadataRequest.CoordinatePoint point = sortedPoints.get(i);
            String pointRoadType = getRoadTypeForPoint(point);
            
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
                currentSegmentPoints.add(sortedPoints.get(i - 1));
            }
            
            currentSegmentPoints.add(point);
        }
        
        // Add final segment
        if (currentSegmentPoints.size() > 1) {
            RoadTypeSegment segment = createSegment(
                currentRoadType,
                segmentStartIndex,
                sortedPoints.size() - 1,
                currentSegmentPoints
            );
            segments.add(segment);
        }
        
        return segments;
    }
    
    /**
     * Get road type for a specific point using OpenStreetMap
     */
    private String getRoadTypeForPoint(RouteMetadataRequest.CoordinatePoint point) {
        try {
            Optional<OpenStreetMapService.RoadData> roadData = openStreetMapService.getRoadDataQuick(
                point.getLatitude(), 
                point.getLongitude()
            );
            
            if (roadData.isPresent() && roadData.get().highway() != null) {
                String roadType = openStreetMapService.osmHighwayToRoadType(roadData.get().highway());
                log.debug("Got road type {} for point at {},{}", 
                    roadType, point.getLatitude(), point.getLongitude());
                return roadType;
            }
        } catch (Exception e) {
            log.warn("Failed to get OSM data for point at {},{}: {}", 
                point.getLatitude(), point.getLongitude(), e.getMessage());
        }
        
        // Default fallback
        return "PAVED_ROAD";
    }
    
    /**
     * Create a road type segment
     */
    private RoadTypeSegment createSegment(String roadType, int startIndex, int endIndex, 
                                         List<RouteMetadataRequest.CoordinatePoint> points) {
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
     * Calculate total distance if not provided
     */
    private double calculateTotalDistance(List<RouteMetadataRequest.CoordinatePoint> points) {
        double distance = 0;
        for (int i = 1; i < points.size(); i++) {
            distance += routeStatisticsService.calculateDistance(
                points.get(i - 1).getLatitude(),
                points.get(i - 1).getLongitude(),
                points.get(i).getLatitude(),
                points.get(i).getLongitude()
            );
        }
        return distance;
    }
    
    /**
     * Calculate road type statistics
     */
    private Map<String, RoadTypeStats> calculateRoadTypeStats(List<RoadTypeSegment> segments, 
                                                              double totalDistance) {
        Map<String, RoadTypeStats> stats = new HashMap<>();
        
        // Group segments by road type
        Map<String, List<RoadTypeSegment>> groupedSegments = segments.stream()
            .collect(Collectors.groupingBy(s -> s.roadType));
        
        // Use actual calculated distance from segments for percentage calculation
        double actualTotalDistance = segments.stream().mapToDouble(s -> s.distance).sum();
        
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
     * Convert segments to response format
     */
    private List<RouteMetadataResponse.RoadTypeSegment> convertSegmentsToResponse(List<RoadTypeSegment> segments) {
        return segments.stream().map(segment -> {
            RouteMetadataResponse.RoadTypeSegment resp = new RouteMetadataResponse.RoadTypeSegment();
            resp.setRoadType(segment.roadType);
            resp.setStartIndex(segment.startIndex);
            resp.setEndIndex(segment.endIndex);
            resp.setDistance(segment.distance);
            resp.setColor(segment.color);
            
            // Convert coordinates
            List<double[]> coords = segment.points.stream()
                .map(p -> new double[]{p.getLongitude(), p.getLatitude()})
                .collect(Collectors.toList());
            resp.setCoordinates(coords);
            
            return resp;
        }).collect(Collectors.toList());
    }
    
    /**
     * Convert stats to response format
     */
    private RouteMetadataResponse.RoadTypeStats convertStatsToResponse(Map<String, RoadTypeStats> stats) {
        RouteMetadataResponse.RoadTypeStats response = new RouteMetadataResponse.RoadTypeStats();
        
        // Sort by percentage descending
        List<RoadTypeStats> sortedStats = new ArrayList<>(stats.values());
        sortedStats.sort((a, b) -> Double.compare(b.percentage, a.percentage));
        
        List<RouteMetadataResponse.RoadTypeStat> breakdown = sortedStats.stream().map(stat -> {
            RouteMetadataResponse.RoadTypeStat resp = new RouteMetadataResponse.RoadTypeStat();
            resp.setRoadType(stat.roadType);
            resp.setDistance(stat.totalDistance);
            resp.setPercentage(String.format("%.1f", stat.percentage));
            resp.setSegmentCount(stat.segmentCount);
            resp.setColor(stat.color);
            return resp;
        }).collect(Collectors.toList());
        
        response.setBreakdown(breakdown);
        response.setTotalDistance(sortedStats.stream().mapToDouble(s -> s.totalDistance).sum());
        response.setTotalTypes(stats.size());
        
        return response;
    }
    
    /**
     * Convert segments to JSON for metadata field
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
            
            // Add coordinate array
            ArrayNode coordinates = objectMapper.createArrayNode();
            for (RouteMetadataRequest.CoordinatePoint point : segment.points) {
                ArrayNode coord = objectMapper.createArrayNode();
                coord.add(point.getLongitude());
                coord.add(point.getLatitude());
                coordinates.add(coord);
            }
            segmentNode.set("coordinates", coordinates);
            
            segmentsArray.add(segmentNode);
        }
        
        return segmentsArray;
    }
    
    /**
     * Convert stats to JSON for metadata field
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
     * Internal class for road type segments
     */
    private static class RoadTypeSegment {
        String roadType;
        int startIndex;
        int endIndex;
        double distance;
        String color;
        List<RouteMetadataRequest.CoordinatePoint> points;
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
