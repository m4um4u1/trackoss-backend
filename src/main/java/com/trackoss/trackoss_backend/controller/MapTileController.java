package com.trackoss.trackoss_backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Collections;

@RestController
@RequestMapping("/api/map-proxy")
@Tag(name = "Map Tiles", description = "Map tile proxy service for cycling route visualization")
public class MapTileController {

    Logger logger = LoggerFactory.getLogger(MapTileController.class);

    @Value("${maptile.service.target-base-url}")
    private String targetBaseUrl;

    @Value("${maptile.service.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    // Constructor injection for RestTemplate
    public MapTileController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{styleId}/style.json")
    @Operation(
        summary = "Proxy map style configuration",
        description = "Proxies map style JSON configuration from the map tile service. " +
                     "This provides the style definition needed for map rendering libraries like Mapbox GL JS or Leaflet."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Style JSON retrieved successfully",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Style not found"),
        @ApiResponse(responseCode = "500", description = "Error retrieving style from upstream service")
    })
    public ResponseEntity<String> proxyStyle(
            @Parameter(description = "Map style identifier", required = true, example = "osm-bright")
            @PathVariable String styleId) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(targetBaseUrl)
                .pathSegment(styleId, "style.json")
                .queryParam("key", apiKey);
        String url = builder.toUriString();

        // Set headers to accept JSON
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        logger.debug("Requesting style.json from: " + url);

        // Fetching as String because style.json is a JSON text file
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    // Endpoint for tiles
    @GetMapping("/{styleId}/{z}/{x}/{y}.{format}")
    @Operation(
        summary = "Proxy map tiles",
        description = "Proxies map tiles from the map tile service. Supports various formats including PNG, JPEG, " +
                     "and vector tiles (PBF). Used for rendering cycling route maps in web applications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Map tile retrieved successfully",
                    content = @Content(mediaType = "image/png")),
        @ApiResponse(responseCode = "404", description = "Tile not found"),
        @ApiResponse(responseCode = "500", description = "Error retrieving tile from upstream service")
    })
    public ResponseEntity<byte[]> proxyTile(
            @Parameter(description = "Map style identifier", required = true, example = "osm-bright")
            @PathVariable String styleId,
            @Parameter(description = "Zoom level", required = true, example = "10")
            @PathVariable String z,
            @Parameter(description = "Tile X coordinate", required = true, example = "163")
            @PathVariable String x,
            @Parameter(description = "Tile Y coordinate", required = true, example = "357")
            @PathVariable String y,
            @Parameter(description = "Tile format (png, jpg, jpeg, pbf)", required = true, example = "png")
            @PathVariable String format) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(targetBaseUrl)
                .pathSegment(styleId, z, x, y + "." + format)
                .queryParam("key", apiKey);
        String url = builder.toUriString();

        // Set headers to accept common image/vector tile formats
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(
                MediaType.IMAGE_PNG,
                MediaType.IMAGE_JPEG,
                MediaType.valueOf("application/vnd.mapbox-vector-tile"), // for .pbf
                MediaType.valueOf("application/x-protobuf") // another common one for pbf
        ));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        logger.info("Requesting tile: " + url);

        // Fetching as byte[] because tiles are binary data
        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
    }
}