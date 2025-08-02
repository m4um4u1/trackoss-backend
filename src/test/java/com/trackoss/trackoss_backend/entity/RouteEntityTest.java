package com.trackoss.trackoss_backend.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteEntityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSetDifficulty_ShouldUpdateDifficultyField() {
        // Arrange
        Route route = new Route();
        Integer difficulty = 3;
        
        // Act
        route.setDifficulty(difficulty);
        
        // Assert
        assertEquals(difficulty, route.getDifficulty());
    }
    
    @Test
    void testSetDifficulty_ShouldUpdateMetadataField() throws Exception {
        // Arrange
        Route route = new Route();
        Integer difficulty = 4;
        
        // Act
        route.setDifficulty(difficulty);
        
        // Assert
        assertNotNull(route.getMetadata());
        JsonNode metadataJson = objectMapper.readTree(route.getMetadata());
        assertTrue(metadataJson.has("difficulty"));
        assertEquals(difficulty, metadataJson.get("difficulty").asInt());
    }
    
    @Test
    void testSetDifficulty_WithExistingMetadata_ShouldUpdateDifficultyInMetadata() throws Exception {
        // Arrange
        Route route = new Route();
        String initialMetadata = "{\"surface\":\"asphalt\",\"traffic\":\"low\"}";
        route.setMetadata(initialMetadata);
        Integer difficulty = 5;
        
        // Act
        route.setDifficulty(difficulty);
        
        // Assert
        JsonNode metadataJson = objectMapper.readTree(route.getMetadata());
        assertTrue(metadataJson.has("surface"));
        assertEquals("asphalt", metadataJson.get("surface").asText());
        assertTrue(metadataJson.has("traffic"));
        assertEquals("low", metadataJson.get("traffic").asText());
        assertTrue(metadataJson.has("difficulty"));
        assertEquals(difficulty, metadataJson.get("difficulty").asInt());
    }
    
    @Test
    void testSetMetadata_WithDifficulty_ShouldUpdateDifficultyField() throws Exception {
        // Arrange
        Route route = new Route();
        String metadata = "{\"difficulty\":3,\"surface\":\"gravel\"}";
        
        // Act
        route.setMetadata(metadata);
        
        // Assert
        assertEquals(3, route.getDifficulty());
    }
    
    @Test
    void testSetMetadata_WithoutDifficulty_ShouldNotUpdateDifficultyField() {
        // Arrange
        Route route = new Route();
        route.setDifficulty(4); // Set initial difficulty
        String metadata = "{\"surface\":\"asphalt\",\"traffic\":\"medium\"}";
        
        // Act
        route.setMetadata(metadata);
        
        // Assert
        assertEquals(4, route.getDifficulty()); // Difficulty should remain unchanged
    }
    
    @Test
    void testSetMetadata_WithInvalidJson_ShouldNotThrowException() {
        // Arrange
        Route route = new Route();
        String invalidMetadata = "not a valid json";
        
        // Act & Assert
        assertDoesNotThrow(() -> route.setMetadata(invalidMetadata));
    }
    
    @Test
    void testSetDifficulty_WithNull_ShouldHandleNullValue() {
        // Arrange
        Route route = new Route();
        route.setDifficulty(3); // Set initial value
        
        // Act
        route.setDifficulty(null);
        
        // Assert
        assertNull(route.getDifficulty());
        
        // Metadata should still contain null difficulty
        assertDoesNotThrow(() -> {
            JsonNode metadataJson = objectMapper.readTree(route.getMetadata());
            assertTrue(metadataJson.has("difficulty"));
            assertTrue(metadataJson.get("difficulty").isNull());
        });
    }
    
    @Test
    void testSetMetadata_WithNullValue_ShouldHandleNullValue() {
        // Arrange
        Route route = new Route();
        route.setDifficulty(3); // Set initial difficulty
        
        // Act
        route.setMetadata(null);
        
        // Assert
        assertEquals(3, route.getDifficulty()); // Difficulty should remain unchanged
        assertNull(route.getMetadata());
    }
    
    @Test
    void testSetMetadata_WithEmptyString_ShouldHandleEmptyString() {
        // Arrange
        Route route = new Route();
        route.setDifficulty(3); // Set initial difficulty
        
        // Act
        route.setMetadata("");
        
        // Assert
        assertEquals(3, route.getDifficulty()); // Difficulty should remain unchanged
        assertEquals("", route.getMetadata());
    }
}