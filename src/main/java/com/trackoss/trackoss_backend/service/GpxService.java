package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import io.jenetics.jpx.*;
import io.jenetics.jpx.GPX.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GpxService {
    
    /**
     * Export a route to GPX format
     */
    public byte[] exportToGpx(Route route) throws IOException {
        log.info("Exporting route {} to GPX", route.getId());
        
        GPX.Builder gpxBuilder = GPX.builder()
                .creator("TrackOSS")
                .version(Version.V11);
        
        // Add metadata
        if (route.getName() != null || route.getDescription() != null) {
            Metadata.Builder metadataBuilder = Metadata.builder();
            if (route.getName() != null) {
                metadataBuilder.name(route.getName());
            }
            if (route.getDescription() != null) {
                metadataBuilder.desc(route.getDescription());
            }
            gpxBuilder.metadata(metadataBuilder.build());
        }
        
        // Create track from route points
        if (!route.getRoutePoints().isEmpty()) {
            Track.Builder trackBuilder = Track.builder();
            
            if (route.getName() != null) {
                trackBuilder.name(route.getName());
            }
            if (route.getDescription() != null) {
                trackBuilder.desc(route.getDescription());
            }
            
            // Create track segment
            TrackSegment.Builder segmentBuilder = TrackSegment.builder();
            
            for (RoutePoint point : route.getRoutePoints()) {
                WayPoint.Builder wpBuilder = WayPoint.builder()
                        .lat(point.getLatitude())
                        .lon(point.getLongitude());
                
                if (point.getElevation() != null) {
                    wpBuilder.ele(point.getElevation());
                }
                
                if (point.getTimestamp() != null) {
                    wpBuilder.time(point.getTimestamp().toInstant(ZoneOffset.UTC));
                }
                
                if (point.getName() != null) {
                    wpBuilder.name(point.getName());
                }
                
                if (point.getDescription() != null) {
                    wpBuilder.desc(point.getDescription());
                }
                
                segmentBuilder.addPoint(wpBuilder.build());
            }
            
            trackBuilder.addSegment(segmentBuilder.build());
            gpxBuilder.addTrack(trackBuilder.build());
        }
        
        // Add waypoints for named points
        for (RoutePoint point : route.getRoutePoints()) {
            if (point.getPointType() == RoutePoint.PointType.WAYPOINT && point.getName() != null) {
                WayPoint.Builder wpBuilder = WayPoint.builder()
                        .lat(point.getLatitude())
                        .lon(point.getLongitude())
                        .name(point.getName());
                
                if (point.getElevation() != null) {
                    wpBuilder.ele(point.getElevation());
                }
                
                if (point.getDescription() != null) {
                    wpBuilder.desc(point.getDescription());
                }
                
                gpxBuilder.addWayPoint(wpBuilder.build());
            }
        }
        
        GPX gpx = gpxBuilder.build();
        
        // Write to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GPX.Writer.DEFAULT.write(gpx, outputStream);
        
        log.info("Successfully exported route {} to GPX ({} bytes)", route.getId(), outputStream.size());
        return outputStream.toByteArray();
    }
    
    /**
     * Import a route from GPX format
     */
    public RouteCreateRequest importFromGpx(byte[] gpxData, String routeName) throws IOException {
        log.info("Importing route from GPX data ({} bytes)", gpxData.length);
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(gpxData);
        GPX gpx = GPX.Reader.DEFAULT.read(inputStream);
        
        RouteCreateRequest request = new RouteCreateRequest();
        List<RouteCreateRequest.RoutePointRequest> points = new ArrayList<>();
        
        // Set route name
        if (routeName != null && !routeName.trim().isEmpty()) {
            request.setName(routeName.trim());
        } else if (gpx.getMetadata().isPresent() && gpx.getMetadata().get().getName().isPresent()) {
            request.setName(gpx.getMetadata().get().getName().get());
        } else if (!gpx.getTracks().isEmpty() && gpx.getTracks().get(0).getName().isPresent()) {
            request.setName(gpx.getTracks().get(0).getName().get());
        } else {
            request.setName("Imported Route");
        }
        
        // Set description from metadata
        if (gpx.getMetadata().isPresent() && gpx.getMetadata().get().getDescription().isPresent()) {
            request.setDescription(gpx.getMetadata().get().getDescription().get());
        }
        
        // Try to extract statistics from GPX metadata or track extensions
        extractStatisticsFromGpx(gpx, request);
        
        // Process tracks
        for (Track track : gpx.getTracks()) {
            for (TrackSegment segment : track.getSegments()) {
                for (WayPoint wayPoint : segment.getPoints()) {
                    RouteCreateRequest.RoutePointRequest pointRequest = new RouteCreateRequest.RoutePointRequest();
                    pointRequest.setLatitude(wayPoint.getLatitude().doubleValue());
                    pointRequest.setLongitude(wayPoint.getLongitude().doubleValue());
                    pointRequest.setPointType("TRACK_POINT");
                    
                    if (wayPoint.getElevation().isPresent()) {
                        pointRequest.setElevation(wayPoint.getElevation().get().doubleValue());
                    }
                    
                    if (wayPoint.getName().isPresent()) {
                        pointRequest.setName(wayPoint.getName().get());
                    }
                    
                    // TODO: Fix description extraction from waypoint
                    
                    points.add(pointRequest);
                }
            }
        }
        
        // Process waypoints
        for (WayPoint wayPoint : gpx.getWayPoints()) {
            RouteCreateRequest.RoutePointRequest pointRequest = new RouteCreateRequest.RoutePointRequest();
            pointRequest.setLatitude(wayPoint.getLatitude().doubleValue());
            pointRequest.setLongitude(wayPoint.getLongitude().doubleValue());
            pointRequest.setPointType("WAYPOINT");
            
            if (wayPoint.getElevation().isPresent()) {
                pointRequest.setElevation(wayPoint.getElevation().get().doubleValue());
            }
            
            if (wayPoint.getName().isPresent()) {
                pointRequest.setName(wayPoint.getName().get());
            }
            
            // TODO: Fix description extraction from waypoint
            
            points.add(pointRequest);
        }
        
        // Process routes (GPX routes are different from tracks)
        for (io.jenetics.jpx.Route gpxRoute : gpx.getRoutes()) {
            for (WayPoint wayPoint : gpxRoute.getPoints()) {
                RouteCreateRequest.RoutePointRequest pointRequest = new RouteCreateRequest.RoutePointRequest();
                pointRequest.setLatitude(wayPoint.getLatitude().doubleValue());
                pointRequest.setLongitude(wayPoint.getLongitude().doubleValue());
                pointRequest.setPointType("ROUTE_POINT");
                
                if (wayPoint.getElevation().isPresent()) {
                    pointRequest.setElevation(wayPoint.getElevation().get().doubleValue());
                }
                
                if (wayPoint.getName().isPresent()) {
                    pointRequest.setName(wayPoint.getName().get());
                }
                
                if (wayPoint.getDescription().isPresent()) {
                    pointRequest.setDescription(wayPoint.getDescription().get());
                }
                
                points.add(pointRequest);
            }
        }
        
        if (points.isEmpty()) {
            throw new IllegalArgumentException("GPX file contains no valid track points, waypoints, or route points");
        }
        
        request.setPoints(points);
        request.setRouteType(Route.RouteType.HIKING); // Default, can be changed by user
        request.setIsPublic(false);
        
        log.info("Successfully imported route with {} points", points.size());
        return request;
    }
    
    /**
     * Extract statistics from GPX metadata or extensions if available
     */
    private void extractStatisticsFromGpx(GPX gpx, RouteCreateRequest request) {
        // Try to extract from metadata extensions (common in Garmin, Strava exports)
        if (gpx.getMetadata().isPresent()) {
            Metadata metadata = gpx.getMetadata().get();
            
            // Some GPX files store total distance in metadata
            // This is not standard but some apps do it
            if (metadata.getExtensions().isPresent()) {
                // Extensions parsing would require XML parsing
                // For now, we'll skip this and rely on calculation
                log.debug("GPX metadata extensions found but not parsed for statistics");
            }
        }
        
        // Try to extract from track extensions
        for (Track track : gpx.getTracks()) {
            if (track.getExtensions().isPresent()) {
                // Track extensions might contain total distance, elevation gain
                // This would require custom XML parsing
                log.debug("GPX track extensions found but not parsed for statistics");
            }
        }
        
        // Note: Most GPX files don't contain pre-calculated statistics
        // They typically only contain raw track points with lat/lon/elevation/time
        // Statistics are usually calculated by the importing application
        log.debug("No pre-calculated statistics found in GPX, will calculate from track points");
    }
}