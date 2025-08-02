package com.trackoss.trackoss_backend.integration;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.repository.RouteRepository;
import com.trackoss.trackoss_backend.service.RouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DifficultyIntegrationTest {

    @Autowired
    private RouteService routeService;

    @Autowired
    private RouteRepository routeRepository;

    @Test
    void createRoute_WithDifficulty_ShouldSaveDifficultyInBothFields() {
        // Arrange
        RouteCreateRequest request = createTestRouteRequest();
        request.setDifficulty(3);
        request.setMetadata("{\"surface\":\"asphalt\"}"); // No difficulty in metadata

        // Act
        RouteResponse response = routeService.createRoute(request);
        
        // Assert
        assertEquals(3, response.getDifficulty());
        
        // Verify database state
        Optional<Route> savedRoute = routeRepository.findById(response.getId());
        assertTrue(savedRoute.isPresent());
        assertEquals(3, savedRoute.get().getDifficulty());
        
        // Verify metadata contains difficulty
        assertTrue(savedRoute.get().getMetadata().contains("\"difficulty\":3"));
    }

    @Test
    void createRoute_WithDifficultyInMetadata_ShouldSyncToDifficultyField() {
        // Arrange
        RouteCreateRequest request = createTestRouteRequest();
        request.setDifficulty(null); // No difficulty field
        request.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":4}"); // Difficulty in metadata

        // Act
        RouteResponse response = routeService.createRoute(request);
        
        // Assert
        assertEquals(4, response.getDifficulty());
        
        // Verify database state
        Optional<Route> savedRoute = routeRepository.findById(response.getId());
        assertTrue(savedRoute.isPresent());
        assertEquals(4, savedRoute.get().getDifficulty());
    }

    @Test
    void updateRoute_ChangingDifficulty_ShouldUpdateBothFields() {
        // Arrange - Create initial route
        RouteCreateRequest createRequest = createTestRouteRequest();
        createRequest.setDifficulty(2);
        RouteResponse createdResponse = routeService.createRoute(createRequest);
        
        // Arrange - Update request with new difficulty
        RouteCreateRequest updateRequest = createTestRouteRequest();
        updateRequest.setDifficulty(5);
        
        // Act
        RouteResponse updatedResponse = routeService.updateRoute(createdResponse.getId(), updateRequest);
        
        // Assert
        assertEquals(5, updatedResponse.getDifficulty());
        
        // Verify database state
        Optional<Route> savedRoute = routeRepository.findById(updatedResponse.getId());
        assertTrue(savedRoute.isPresent());
        assertEquals(5, savedRoute.get().getDifficulty());
        assertTrue(savedRoute.get().getMetadata().contains("\"difficulty\":5"));
    }

    @Test
    void updateRoute_ChangingMetadata_ShouldSyncToDifficultyField() {
        // Arrange - Create initial route
        RouteCreateRequest createRequest = createTestRouteRequest();
        createRequest.setDifficulty(2);
        RouteResponse createdResponse = routeService.createRoute(createRequest);
        
        // Arrange - Update request with new metadata
        RouteCreateRequest updateRequest = createTestRouteRequest();
        updateRequest.setDifficulty(null); // No difficulty field
        updateRequest.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":4}"); // New difficulty in metadata
        
        // Act
        RouteResponse updatedResponse = routeService.updateRoute(createdResponse.getId(), updateRequest);
        
        // Assert
        assertEquals(4, updatedResponse.getDifficulty());
        
        // Verify database state
        Optional<Route> savedRoute = routeRepository.findById(updatedResponse.getId());
        assertTrue(savedRoute.isPresent());
        assertEquals(4, savedRoute.get().getDifficulty());
    }

    @Test
    void findByDifficulty_ShouldReturnMatchingRoutes() {
        // Arrange - Create routes with different difficulties
        RouteCreateRequest request1 = createTestRouteRequest();
        request1.setName("Easy Route");
        request1.setDifficulty(1);
        routeService.createRoute(request1);
        
        RouteCreateRequest request2 = createTestRouteRequest();
        request2.setName("Moderate Route");
        request2.setDifficulty(3);
        routeService.createRoute(request2);
        
        RouteCreateRequest request3 = createTestRouteRequest();
        request3.setName("Hard Route");
        request3.setDifficulty(5);
        routeService.createRoute(request3);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // Act
        Page<RouteResponse> easyRoutes = routeService.findByDifficulty(1, pageable);
        Page<RouteResponse> moderateRoutes = routeService.findByDifficulty(3, pageable);
        Page<RouteResponse> hardRoutes = routeService.findByDifficulty(5, pageable);
        Page<RouteResponse> nonExistentDifficulty = routeService.findByDifficulty(10, pageable);
        
        // Assert
        assertEquals(1, easyRoutes.getTotalElements());
        assertEquals("Easy Route", easyRoutes.getContent().get(0).getName());
        
        assertEquals(1, moderateRoutes.getTotalElements());
        assertEquals("Moderate Route", moderateRoutes.getContent().get(0).getName());
        
        assertEquals(1, hardRoutes.getTotalElements());
        assertEquals("Hard Route", hardRoutes.getContent().get(0).getName());
        
        assertEquals(0, nonExistentDifficulty.getTotalElements());
    }

    @Test
    void getRoutesWithFilters_ShouldFilterByDifficultyAndOtherCriteria() {
        // Arrange - Create routes with different properties
        RouteCreateRequest request1 = createTestRouteRequest();
        request1.setName("Easy Cycling Route");
        request1.setDifficulty(2);
        request1.setRouteType(Route.RouteType.CYCLING);
        request1.setTotalDistance(5000.0);
        request1.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":2}");
        request1.setIsPublic(true);
        routeService.createRoute(request1);
        
        RouteCreateRequest request2 = createTestRouteRequest();
        request2.setName("Moderate Mountain Route");
        request2.setDifficulty(3);
        request2.setRouteType(Route.RouteType.MOUNTAIN_BIKING);
        request2.setTotalDistance(15000.0);
        request2.setMetadata("{\"surface\":\"gravel\",\"difficulty\":3}");
        request2.setIsPublic(true);
        routeService.createRoute(request2);
        
        RouteCreateRequest request3 = createTestRouteRequest();
        request3.setName("Hard Gravel Route");
        request3.setDifficulty(5);
        request3.setRouteType(Route.RouteType.GRAVEL);
        request3.setTotalDistance(25000.0);
        request3.setMetadata("{\"surface\":\"dirt\",\"difficulty\":5}");
        request3.setIsPublic(false);
        routeService.createRoute(request3);
        
        Pageable pageable = PageRequest.of(0, 10);
        
        // Act - Filter by difficulty only
        Page<RouteResponse> difficultyOnly = routeService.getRoutesWithFilters(
            3, null, null, null, null, null, pageable
        );
        
        // Act - Filter by difficulty and route type
        Page<RouteResponse> difficultyAndType = routeService.getRoutesWithFilters(
            3, Route.RouteType.MOUNTAIN_BIKING, null, null, null, null, pageable
        );
        
        // Act - Filter by difficulty and public status
        Page<RouteResponse> difficultyAndPublic = routeService.getRoutesWithFilters(
            5, null, null, null, null, false, pageable
        );
        
        // Act - Filter by difficulty and surface
        Page<RouteResponse> difficultyAndSurface = routeService.getRoutesWithFilters(
            3, null, null, null, "gravel", null, pageable
        );
        
        // Act - Combined filters with no matches
        Page<RouteResponse> noMatches = routeService.getRoutesWithFilters(
            2, Route.RouteType.GRAVEL, null, null, null, null, pageable
        );
        
        // Assert
        assertEquals(1, difficultyOnly.getTotalElements());
        assertEquals("Moderate Mountain Route", difficultyOnly.getContent().get(0).getName());
        
        assertEquals(1, difficultyAndType.getTotalElements());
        assertEquals("Moderate Mountain Route", difficultyAndType.getContent().get(0).getName());
        
        assertEquals(1, difficultyAndPublic.getTotalElements());
        assertEquals("Hard Gravel Route", difficultyAndPublic.getContent().get(0).getName());
        
        assertEquals(1, difficultyAndSurface.getTotalElements());
        assertEquals("Moderate Mountain Route", difficultyAndSurface.getContent().get(0).getName());
        
        assertEquals(0, noMatches.getTotalElements());
    }

    @Test
    void databaseTrigger_ShouldSyncDifficultyFromMetadata() {
        // This test verifies that the database trigger works correctly
        // by directly updating the metadata column and checking if the difficulty column is updated
        
        // Arrange - Create a route
        RouteCreateRequest request = createTestRouteRequest();
        request.setDifficulty(2);
        RouteResponse response = routeService.createRoute(request);
        
        // Get the route entity
        Optional<Route> routeOptional = routeRepository.findById(response.getId());
        assertTrue(routeOptional.isPresent());
        Route route = routeOptional.get();
        
        // Act - Update metadata directly
        route.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":4}");
        routeRepository.save(route);
        
        // Refresh the entity from the database
        routeRepository.flush();
        Optional<Route> updatedRouteOptional = routeRepository.findById(response.getId());
        
        // Assert
        assertTrue(updatedRouteOptional.isPresent());
        Route updatedRoute = updatedRouteOptional.get();
        assertEquals(4, updatedRoute.getDifficulty());
    }

    private RouteCreateRequest createTestRouteRequest() {
        RouteCreateRequest request = new RouteCreateRequest();
        request.setName("Test Route");
        request.setDescription("A test cycling route");
        request.setRouteType(Route.RouteType.CYCLING);
        request.setIsPublic(true);
        request.setMetadata("{\"surface\":\"asphalt\"}");
        
        // Create route points
        List<RouteCreateRequest.RoutePointRequest> points = new ArrayList<>();
        RouteCreateRequest.RoutePointRequest point1 = new RouteCreateRequest.RoutePointRequest();
        point1.setLatitude(47.6062);
        point1.setLongitude(-122.3321);
        point1.setElevation(10.0);
        point1.setPointType("START_POINT");
        points.add(point1);
        
        RouteCreateRequest.RoutePointRequest point2 = new RouteCreateRequest.RoutePointRequest();
        point2.setLatitude(47.6162);
        point2.setLongitude(-122.3221);
        point2.setElevation(20.0);
        point2.setPointType("END_POINT");
        points.add(point2);
        
        request.setPoints(points);
        return request;
    }
}