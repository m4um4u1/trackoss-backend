package com.trackoss.trackoss_backend.controller;

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
    public ResponseEntity<String> proxyStyle(
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
    public ResponseEntity<byte[]> proxyTile(
            @PathVariable String styleId,
            @PathVariable String z,
            @PathVariable String x,
            @PathVariable String y,
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