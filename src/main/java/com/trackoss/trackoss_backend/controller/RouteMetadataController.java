package com.trackoss.trackoss_backend.controller;

import com.trackoss.trackoss_backend.dto.RouteMetadataRequest;
import com.trackoss.trackoss_backend.dto.RouteMetadataResponse;
import com.trackoss.trackoss_backend.service.RouteMetadataAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routes/metadata")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Route Metadata", description = "Route metadata analysis endpoints")
public class RouteMetadataController {

    private final RouteMetadataAnalysisService metadataAnalysisService;

    @PostMapping("/analyze")
    @Operation(summary = "Analyze route metadata without saving", 
               description = "Fetches OpenStreetMap road type data for a route without persisting it")
    public ResponseEntity<RouteMetadataResponse> analyzeRoute(@RequestBody RouteMetadataRequest request) {
        log.info("Analyzing route metadata for {} points", request.getPoints().size());
        
        RouteMetadataResponse response = metadataAnalysisService.analyzeRoute(request);
        
        return ResponseEntity.ok(response);
    }
}
