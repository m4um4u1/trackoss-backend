package com.trackoss.trackoss_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoJsonService {
    
    private final ObjectMapper objectMapper;
    
    public String exportToGeoJson(Route route) throws IOException {
        log.info("Exporting route {} to GeoJSON", route.getId());
        
        Map<String, Object> featureCollection = new HashMap<>();
        featureCollection.put("type", "FeatureCollection");
        
        List<Map<String, Object>> features = new ArrayList<>();
        
        // Create LineString feature for the route
        if (!route.getRoutePoints().isEmpty()) {
            Map<String, Object> routeFeature = new HashMap<>();
            routeFeature.put("type", "Feature");
            
            // Properties
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", route.getName());
            properties.put("description", route.getDescription());
            properties.put("routeType", route.getRouteType().toString());
            properties.put("totalDistance", route.getTotalDistance());
            properties.put("totalElevationGain", route.getTotalElevationGain());
            properties.put("estimatedDuration", route.getEstimatedDuration());
            routeFeature.put("properties", properties);
            
            // Geometry - LineString
            Map<String, Object> geometry = new HashMap<>();
            geometry.put("type", "LineString");
            
            List<List<Double>> coordinates = new ArrayList<>();
            for (RoutePoint point : route.getRoutePoints()) {
                List<Double> coord = new ArrayList<>();
                coord.add(point.getLongitude());
                coord.add(point.getLatitude());
                if (point.getElevation() != null) {
                    coord.add(point.getElevation());
                }
                coordinates.add(coord);
            }
            geometry.put("coordinates", coordinates);
            routeFeature.put("geometry", geometry);
            
            features.add(routeFeature);
        }
        
        // Add waypoints as Point features
        for (RoutePoint point : route.getRoutePoints()) {
            if (point.getPointType() == RoutePoint.PointType.WAYPOINT && point.getName() != null) {
                Map<String, Object> waypointFeature = new HashMap<>();
                waypointFeature.put("type", "Feature");
                
                // Properties
                Map<String, Object> properties = new HashMap<>();
                properties.put("name", point.getName());
                properties.put("description", point.getDescription());
                properties.put("pointType", "waypoint");
                waypointFeature.put("properties", properties);
                
                // Geometry - Point
                Map<String, Object> geometry = new HashMap<>();
                geometry.put("type", "Point");
                
                List<Double> coordinates = new ArrayList<>();
                coordinates.add(point.getLongitude());
                coordinates.add(point.getLatitude());
                if (point.getElevation() != null) {
                    coordinates.add(point.getElevation());
                }
                geometry.put("coordinates", coordinates);
                waypointFeature.put("geometry", geometry);
                
                features.add(waypointFeature);
            }
        }
        
        featureCollection.put("features", features);
        
        String geoJson = objectMapper.writeValueAsString(featureCollection);
        log.info("Successfully exported route {} to GeoJSON", route.getId());
        return geoJson;
    }
    
    public RouteCreateRequest importFromGeoJson(String geoJsonData, String routeName) throws IOException {
        log.info("Importing route from GeoJSON data ({} characters)", geoJsonData.length());
        
        JsonNode rootNode = objectMapper.readTree(geoJsonData);
        RouteCreateRequest request = new RouteCreateRequest();
        List<RouteCreateRequest.RoutePointRequest> points = new ArrayList<>();
        
        // Set default name
        request.setName(routeName != null ? routeName : "Imported GeoJSON Route");
        request.setRouteType(Route.RouteType.OTHER);
        request.setIsPublic(false);
        
        // Process based on GeoJSON type
        String type = rootNode.get("type").asText();
        
        if ("FeatureCollection".equals(type)) {
            JsonNode features = rootNode.get("features");
            if (features.isArray()) {
                for (JsonNode feature : features) {
                    processFeatureNode(feature, points, request);
                }
            }
        } else if ("Feature".equals(type)) {
            processFeatureNode(rootNode, points, request);
        } else if ("LineString".equals(type)) {
            processLineStringNode(rootNode, points);
        } else if ("Point".equals(type)) {
            processPointNode(rootNode, points, null);
        }
        
        request.setPoints(points);
        
        log.info("Successfully imported GeoJSON route with {} points", points.size());
        return request;
    }
    
    private void processFeatureNode(JsonNode feature, List<RouteCreateRequest.RoutePointRequest> points, RouteCreateRequest request) {
        JsonNode properties = feature.get("properties");
        JsonNode geometry = feature.get("geometry");
        
        // Extract route information from properties
        if (properties != null) {
            if (properties.has("name") && "Imported GeoJSON Route".equals(request.getName())) {
                request.setName(properties.get("name").asText());
            }
            if (properties.has("description")) {
                request.setDescription(properties.get("description").asText());
            }
            if (properties.has("routeType")) {
                try {
                    request.setRouteType(Route.RouteType.valueOf(properties.get("routeType").asText().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Keep default
                }
            }
            
            // Extract pre-calculated statistics if available
            if (properties.has("totalDistance")) {
                request.setTotalDistance(properties.get("totalDistance").asDouble());
            }
            if (properties.has("totalElevationGain")) {
                request.setTotalElevationGain(properties.get("totalElevationGain").asDouble());
            }
            if (properties.has("estimatedDuration")) {
                request.setEstimatedDuration(properties.get("estimatedDuration").asLong());
            }
        }
        
        // Process geometry
        if (geometry != null) {
            String geometryType = geometry.get("type").asText();
            switch (geometryType) {
                case "LineString":
                    processLineStringNode(geometry, points);
                    break;
                case "MultiLineString":
                    processMultiLineStringNode(geometry, points);
                    break;
                case "Point":
                    processPointNode(geometry, points, properties);
                    break;
            }
        }
    }
    
    private void processLineStringNode(JsonNode geometry, List<RouteCreateRequest.RoutePointRequest> points) {
        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates.isArray()) {
            int sequenceOrder = points.size();
            for (JsonNode coord : coordinates) {
                if (coord.isArray() && coord.size() >= 2) {
                    RouteCreateRequest.RoutePointRequest pointRequest = new RouteCreateRequest.RoutePointRequest();
                    pointRequest.setLongitude(coord.get(0).asDouble());
                    pointRequest.setLatitude(coord.get(1).asDouble());
                    if (coord.size() > 2) {
                        pointRequest.setElevation(coord.get(2).asDouble());
                    }
                    pointRequest.setPointType("TRACK_POINT");
                    points.add(pointRequest);
                }
            }
        }
    }
    
    private void processMultiLineStringNode(JsonNode geometry, List<RouteCreateRequest.RoutePointRequest> points) {
        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates.isArray()) {
            for (JsonNode lineString : coordinates) {
                if (lineString.isArray()) {
                    int sequenceOrder = points.size();
                    for (JsonNode coord : lineString) {
                        if (coord.isArray() && coord.size() >= 2) {
                            RouteCreateRequest.RoutePointRequest pointRequest = new RouteCreateRequest.RoutePointRequest();
                            pointRequest.setLongitude(coord.get(0).asDouble());
                            pointRequest.setLatitude(coord.get(1).asDouble());
                            if (coord.size() > 2) {
                                pointRequest.setElevation(coord.get(2).asDouble());
                            }
                            pointRequest.setPointType("TRACK_POINT");
                            points.add(pointRequest);
                        }
                    }
                }
            }
        }
    }
    
    private void processPointNode(JsonNode geometry, List<RouteCreateRequest.RoutePointRequest> points, JsonNode properties) {
        JsonNode coordinates = geometry.get("coordinates");
        if (coordinates.isArray() && coordinates.size() >= 2) {
            RouteCreateRequest.RoutePointRequest pointRequest = new RouteCreateRequest.RoutePointRequest();
            pointRequest.setLongitude(coordinates.get(0).asDouble());
            pointRequest.setLatitude(coordinates.get(1).asDouble());
            if (coordinates.size() > 2) {
                pointRequest.setElevation(coordinates.get(2).asDouble());
            }
            
            // Set point type and name from properties
            if (properties != null) {
                if (properties.has("name")) {
                    pointRequest.setName(properties.get("name").asText());
                    pointRequest.setPointType("WAYPOINT");
                } else {
                    pointRequest.setPointType("TRACK_POINT");
                }
                if (properties.has("description")) {
                    pointRequest.setDescription(properties.get("description").asText());
                }
            } else {
                pointRequest.setPointType("TRACK_POINT");
            }
            
            points.add(pointRequest);
        }
    }
}