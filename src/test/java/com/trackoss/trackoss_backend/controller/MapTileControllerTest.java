package com.trackoss.trackoss_backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MapTileController.class)
@TestPropertySource(properties = {
    "maptile.service.target-base-url=https://api.maptiler.com/maps",
    "maptile.service.api.key=test-api-key"
})
class MapTileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    private String testStyleId = "basic-v2";
    private String testApiKey = "test-api-key";
    private String baseUrl = "https://api.maptiler.com/maps";

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        reset(restTemplate);
    }

    @Test
    void proxyStyle_ValidStyleId_ReturnsStyleJson() throws Exception {
        String mockStyleJson = "{\"version\":8,\"name\":\"Basic\",\"sources\":{}}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockStyleJson, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/style.json", testStyleId))
                .andExpect(status().isOk())
                .andExpect(content().string(mockStyleJson));

        verify(restTemplate).exchange(
                contains(testStyleId + "/style.json"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void proxyStyle_ServiceUnavailable_ReturnsServiceError() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Service unavailable"));

        mockMvc.perform(get("/api/map-proxy/{styleId}/style.json", testStyleId))
                .andExpect(status().is5xxServerError());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void proxyStyle_InvalidStyleId_HandlesGracefully() throws Exception {
        ResponseEntity<String> notFoundResponse = new ResponseEntity<>("Not Found", HttpStatus.NOT_FOUND);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(notFoundResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/style.json", "invalid-style"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Not Found"));

        verify(restTemplate).exchange(
                contains("invalid-style/style.json"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void proxyTile_ValidTileCoordinates_ReturnsTileData() throws Exception {
        byte[] mockTileData = new byte[]{1, 2, 3, 4, 5}; // Mock tile data
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(mockTileData, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(mockTileData));

        verify(restTemplate).exchange(
                contains(testStyleId + "/10/512/256.png"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)
        );
    }

    @Test
    void proxyTile_DifferentFormats_HandlesCorrectly() throws Exception {
        byte[] mockTileData = new byte[]{1, 2, 3, 4, 5};
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(mockTileData, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mockResponse);

        // Test PNG format
        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "png"))
                .andExpect(status().isOk());

        // Test JPEG format
        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "jpg"))
                .andExpect(status().isOk());

        // Test PBF format (vector tiles)
        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "pbf"))
                .andExpect(status().isOk());

        verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));
    }

    @Test
    void proxyTile_InvalidTileCoordinates_HandlesGracefully() throws Exception {
        ResponseEntity<byte[]> notFoundResponse = new ResponseEntity<>(new byte[0], HttpStatus.NOT_FOUND);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(notFoundResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "999", "999999", "999999", "png"))
                .andExpect(status().isNotFound());

        verify(restTemplate).exchange(
                contains("999/999999/999999.png"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)
        );
    }

    @Test
    void proxyTile_ServiceError_ReturnsError() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenThrow(new RestClientException("Tile service error"));

        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "png"))
                .andExpect(status().is5xxServerError());

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));
    }

    @Test
    void proxyStyle_VerifyApiKeyInUrl() throws Exception {
        String mockStyleJson = "{\"version\":8}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockStyleJson, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/style.json", testStyleId))
                .andExpect(status().isOk());

        verify(restTemplate).exchange(
                contains("key=" + testApiKey),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void proxyTile_VerifyApiKeyInUrl() throws Exception {
        byte[] mockTileData = new byte[]{1, 2, 3};
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(mockTileData, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "png"))
                .andExpect(status().isOk());

        verify(restTemplate).exchange(
                contains("key=" + testApiKey),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(byte[].class)
        );
    }

    @Test
    void proxyStyle_VerifyCorrectHeaders() throws Exception {
        String mockStyleJson = "{\"version\":8}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockStyleJson, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/style.json", testStyleId))
                .andExpect(status().isOk());

        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                argThat(entity -> {
                    HttpEntity<?> httpEntity = (HttpEntity<?>) entity;
                    return httpEntity.getHeaders().getAccept().contains(MediaType.APPLICATION_JSON);
                }),
                eq(String.class)
        );
    }

    @Test
    void proxyTile_VerifyCorrectHeaders() throws Exception {
        byte[] mockTileData = new byte[]{1, 2, 3};
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(mockTileData, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "10", "512", "256", "png"))
                .andExpect(status().isOk());

        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                argThat(entity -> {
                    HttpEntity<?> httpEntity = (HttpEntity<?>) entity;
                    return httpEntity.getHeaders().getAccept().contains(MediaType.IMAGE_PNG) ||
                           httpEntity.getHeaders().getAccept().contains(MediaType.IMAGE_JPEG);
                }),
                eq(byte[].class)
        );
    }

    @Test
    void proxyStyle_SpecialCharactersInStyleId_HandlesCorrectly() throws Exception {
        String specialStyleId = "style-with-dashes_and_underscores.123";
        String mockStyleJson = "{\"version\":8}";
        ResponseEntity<String> mockResponse = new ResponseEntity<>(mockStyleJson, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/map-proxy/{styleId}/style.json", specialStyleId))
                .andExpect(status().isOk());

        verify(restTemplate).exchange(
                contains(specialStyleId),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    void proxyTile_EdgeCaseCoordinates_HandlesCorrectly() throws Exception {
        byte[] mockTileData = new byte[]{1, 2, 3};
        ResponseEntity<byte[]> mockResponse = new ResponseEntity<>(mockTileData, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(mockResponse);

        // Test zoom level 0 (world view)
        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "0", "0", "0", "png"))
                .andExpect(status().isOk());

        // Test high zoom level
        mockMvc.perform(get("/api/map-proxy/{styleId}/{z}/{x}/{y}.{format}", 
                testStyleId, "18", "131072", "131072", "png"))
                .andExpect(status().isOk());

        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));
    }
}
