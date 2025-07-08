package com.trackoss.trackoss_backend.controller;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
import com.trackoss.trackoss_backend.service.GeoJsonService;
import com.trackoss.trackoss_backend.service.GpxService;
import com.trackoss.trackoss_backend.service.RouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Configure appropriately for production
@Tag(name = "Routes", description = "Cycling route management API")
public class RouteController {
    
    private final RouteService routeService;
    private final GpxService gpxService;
    private final GeoJsonService geoJsonService;
    
    @PostMapping
    @Operation(
        summary = "Create a new cycling route",
        description = "Creates a new cycling route with track points, waypoints, and metadata. " +
                     "Automatically calculates distance, elevation gain, and estimated duration."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Route created successfully",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<RouteResponse> createRoute(
            @Parameter(description = "Route creation request with points and metadata", required = true)
            @Valid @RequestBody RouteCreateRequest request) {
        log.info("Creating new route: {}", request.getName());
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(
        summary = "Get all routes with pagination and filtering",
        description = "Retrieves a paginated list of cycling routes with optional search and filtering capabilities."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Routes retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<RouteResponse>> getAllRoutes(
            @Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable,
            @Parameter(description = "Search term for route name or description") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Show only public routes") @RequestParam(required = false, defaultValue = "false") boolean publicOnly) {
        
        Page<RouteResponse> routes;
        
        if (search != null && !search.trim().isEmpty()) {
            routes = routeService.searchRoutes(search.trim(), pageable);
        } else if (publicOnly) {
            routes = routeService.getPublicRoutes(pageable);
        } else if (userId != null && !userId.trim().isEmpty()) {
            routes = routeService.getUserRoutes(userId.trim(), pageable);
        } else {
            routes = routeService.getAllRoutes(pageable);
        }
        
        return ResponseEntity.ok(routes);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get route by ID",
        description = "Retrieves a specific cycling route by its unique identifier."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route found",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<RouteResponse> getRoute(
            @Parameter(description = "Route unique identifier", required = true) @PathVariable UUID id) {
        return routeService.getRoute(id)
                .map(route -> ResponseEntity.ok(route))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<RouteResponse> updateRoute(
            @PathVariable UUID id,
            @Valid @RequestBody RouteCreateRequest request) {
        
        try {
            RouteResponse response = routeService.updateRoute(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID id) {
        try {
            routeService.deleteRoute(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Export endpoints
    
    @GetMapping("/{id}/export/gpx")
    @Operation(
        summary = "Export route as GPX file",
        description = "Exports a cycling route as a GPX file for use with navigation apps like OsmAnd, Komoot, " +
                     "Garmin devices, Wahoo computers, and fitness platforms like Strava."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "GPX file generated successfully",
                    content = @Content(mediaType = "application/xml")),
        @ApiResponse(responseCode = "404", description = "Route not found"),
        @ApiResponse(responseCode = "500", description = "Error generating GPX file")
    })
    public ResponseEntity<byte[]> exportToGpx(
            @Parameter(description = "Route unique identifier", required = true) @PathVariable UUID id) {
        return routeService.getRoute(id)
                .map(routeResponse -> {
                    try {
                        // Convert response back to entity for export (in real app, you'd fetch entity directly)
                        Route route = convertToEntity(routeResponse);
                        byte[] gpxData = gpxService.exportToGpx(route);
                        
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_XML);
                        headers.setContentDispositionFormData("attachment", 
                                sanitizeFilename(routeResponse.getName()) + ".gpx");
                        
                        return ResponseEntity.ok()
                                .headers(headers)
                                .body(gpxData);
                                
                    } catch (IOException e) {
                        log.error("Error exporting route to GPX", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<byte[]>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{id}/export/geojson")
    @Operation(
        summary = "Export route as GeoJSON",
        description = "Exports a cycling route as GeoJSON for web mapping integration with Leaflet, Mapbox, " +
                     "OpenLayers, and custom cycling applications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "GeoJSON generated successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Route not found"),
        @ApiResponse(responseCode = "500", description = "Error generating GeoJSON")
    })
    public ResponseEntity<String> exportToGeoJson(
            @Parameter(description = "Route unique identifier", required = true) @PathVariable UUID id) {
        return routeService.getRoute(id)
                .map(routeResponse -> {
                    try {
                        Route route = convertToEntity(routeResponse);
                        String geoJsonData = geoJsonService.exportToGeoJson(route);
                        
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.setContentDispositionFormData("attachment", 
                                sanitizeFilename(routeResponse.getName()) + ".geojson");
                        
                        return ResponseEntity.ok()
                                .headers(headers)
                                .body(geoJsonData);
                                
                    } catch (IOException e) {
                        log.error("Error exporting route to GeoJSON", e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).<String>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Import endpoints
    
    @PostMapping("/import/gpx")
    @Operation(
        summary = "Import route from GPX file",
        description = "Imports a cycling route from a GPX file. Supports GPX files from Garmin, Wahoo, " +
                     "Strava, Komoot, and other cycling platforms."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Route imported successfully",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid GPX file or empty file"),
        @ApiResponse(responseCode = "500", description = "Error processing GPX file")
    })
    public ResponseEntity<RouteResponse> importFromGpx(
            @Parameter(description = "GPX file to import", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional custom name for the route") @RequestParam(required = false) String routeName) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            byte[] gpxData = file.getBytes();
            RouteCreateRequest request = gpxService.importFromGpx(gpxData, routeName);
            RouteResponse response = routeService.createRoute(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IOException e) {
            log.error("Error importing GPX file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing GPX import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/import/geojson")
    @Operation(
        summary = "Import route from GeoJSON file",
        description = "Imports a cycling route from a GeoJSON file. Perfect for routes created with " +
                     "web mapping tools or exported from other cycling applications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Route imported successfully",
                    content = @Content(schema = @Schema(implementation = RouteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid GeoJSON file or empty file"),
        @ApiResponse(responseCode = "500", description = "Error processing GeoJSON file")
    })
    public ResponseEntity<RouteResponse> importFromGeoJson(
            @Parameter(description = "GeoJSON file to import", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional custom name for the route") @RequestParam(required = false) String routeName) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            String geoJsonData = new String(file.getBytes());
            RouteCreateRequest request = geoJsonService.importFromGeoJson(geoJsonData, routeName);
            RouteResponse response = routeService.createRoute(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IOException e) {
            log.error("Error importing GeoJSON file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing GeoJSON import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/import/geojson/raw")
    public ResponseEntity<RouteResponse> importFromGeoJsonRaw(
            @RequestBody String geoJsonData,
            @RequestParam(required = false) String routeName) {
        
        try {
            RouteCreateRequest request = geoJsonService.importFromGeoJson(geoJsonData, routeName);
            RouteResponse response = routeService.createRoute(request);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IOException e) {
            log.error("Error importing GeoJSON data", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing GeoJSON import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Helper methods
    
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "route";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }
    
    private Route convertToEntity(RouteResponse response) {
        // This is a simplified conversion for export purposes
        Route route = new Route();
        route.setId(response.getId());
        route.setName(response.getName());
        route.setDescription(response.getDescription());
        route.setCreatedAt(response.getCreatedAt());
        route.setUpdatedAt(response.getUpdatedAt());
        route.setUserId(response.getUserId());
        route.setTotalDistance(response.getTotalDistance());
        route.setTotalElevationGain(response.getTotalElevationGain());
        route.setEstimatedDuration(response.getEstimatedDuration());
        route.setRouteType(response.getRouteType());
        route.setIsPublic(response.getIsPublic());
        route.setMetadata(response.getMetadata());

        // Convert points
        if (response.getPoints() != null) {
            response.getPoints().forEach(pointResponse -> {
                RoutePoint point = new RoutePoint();
                point.setId(pointResponse.getId());
                point.setSequenceOrder(pointResponse.getSequenceOrder());
                point.setLatitude(pointResponse.getLatitude());
                point.setLongitude(pointResponse.getLongitude());
                point.setElevation(pointResponse.getElevation());
                point.setTimestamp(pointResponse.getTimestamp());
                point.setPointType(RoutePoint.PointType.valueOf(pointResponse.getPointType()));
                point.setName(pointResponse.getName());
                point.setDescription(pointResponse.getDescription());
                point.setRoute(route);
                route.addRoutePoint(point);
            });
        }
        
        return route;
    }
}