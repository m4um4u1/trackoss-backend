package com.trackoss.trackoss_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.service.GeoJsonService;
import com.trackoss.trackoss_backend.service.GpxService;
import com.trackoss.trackoss_backend.service.RouteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteController.class)
class RouteControllerDifficultyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private GpxService gpxService;

    @MockitoBean
    private GeoJsonService geoJsonService;

    private RouteCreateRequest validRouteRequest;
    private RouteResponse mockRouteResponse;
    private UUID testRouteId;

    @BeforeEach
    void setUp() {
        testRouteId = UUID.randomUUID();

        // Create valid route request with difficulty
        validRouteRequest = new RouteCreateRequest();
        validRouteRequest.setName("Test Route");
        validRouteRequest.setDescription("A test cycling route");
        validRouteRequest.setRouteType(Route.RouteType.CYCLING);
        validRouteRequest.setIsPublic(true);
        validRouteRequest.setDifficulty(3);
        validRouteRequest.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":3}");

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

        validRouteRequest.setPoints(points);

        // Create mock response
        mockRouteResponse = new RouteResponse();
        mockRouteResponse.setId(testRouteId);
        mockRouteResponse.setName("Test Route");
        mockRouteResponse.setDescription("A test cycling route");
        mockRouteResponse.setRouteType(Route.RouteType.CYCLING);
        mockRouteResponse.setIsPublic(true);
        mockRouteResponse.setDifficulty(3);
        mockRouteResponse.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":3}");
        mockRouteResponse.setTotalDistance(1000.0);
        mockRouteResponse.setTotalElevationGain(100.0);
        mockRouteResponse.setEstimatedDuration(3600L);
        mockRouteResponse.setCreatedAt(LocalDateTime.now());
        mockRouteResponse.setUpdatedAt(LocalDateTime.now());
        mockRouteResponse.setPointCount(2);

        // Create mock route points response
        List<RouteResponse.RoutePointResponse> pointResponses = new ArrayList<>();
        RouteResponse.RoutePointResponse pointResponse1 = new RouteResponse.RoutePointResponse();
        pointResponse1.setId(UUID.randomUUID());
        pointResponse1.setLatitude(47.6062);
        pointResponse1.setLongitude(-122.3321);
        pointResponse1.setElevation(10.0);
        pointResponse1.setPointType("START_POINT");
        pointResponses.add(pointResponse1);

        RouteResponse.RoutePointResponse pointResponse2 = new RouteResponse.RoutePointResponse();
        pointResponse2.setId(UUID.randomUUID());
        pointResponse2.setLatitude(47.6162);
        pointResponse2.setLongitude(-122.3221);
        pointResponse2.setElevation(20.0);
        pointResponse2.setPointType("END_POINT");
        pointResponses.add(pointResponse2);

        mockRouteResponse.setPoints(pointResponses);
    }

    @Test
    void getAllRoutes_WithDifficultyParam_FiltersByDifficulty() throws Exception {
        // Arrange
        Integer difficulty = 3;
        Pageable pageable = PageRequest.of(0, 20);
        List<RouteResponse> routes = Collections.singletonList(mockRouteResponse);
        Page<RouteResponse> routePage = new PageImpl<>(routes, pageable, 1);

        when(routeService.getRoutesWithFilters(
                eq(difficulty), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(routePage);

        // Act & Assert
        mockMvc.perform(get("/api/routes")
                .param("difficulty", "3")
                .with(user("user").roles("USER"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].difficulty", is(3)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(routeService).getRoutesWithFilters(
                eq(difficulty), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAllRoutes_WithCombinedFilters_FiltersCorrectly() throws Exception {
        // Arrange
        Integer difficulty = 3;
        Route.RouteType routeType = Route.RouteType.CYCLING;
        Double minDistance = 500.0;
        Double maxDistance = 1500.0;
        String surfaceType = "asphalt";
        Boolean publicOnly = true;
        Pageable pageable = PageRequest.of(0, 20);
        
        List<RouteResponse> routes = Collections.singletonList(mockRouteResponse);
        Page<RouteResponse> routePage = new PageImpl<>(routes, pageable, 1);

        when(routeService.getRoutesWithFilters(
                eq(difficulty), eq(routeType), eq(minDistance), eq(maxDistance), 
                eq(surfaceType), eq(publicOnly), any(Pageable.class)))
                .thenReturn(routePage);

        // Act & Assert
        mockMvc.perform(get("/api/routes")
                .param("difficulty", "3")
                .param("routeType", "CYCLING")
                .param("minDistance", "500.0")
                .param("maxDistance", "1500.0")
                .param("surfaceType", "asphalt")
                .param("publicOnly", "true")
                .with(user("user").roles("USER"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].difficulty", is(3)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(routeService).getRoutesWithFilters(
                eq(difficulty), eq(routeType), eq(minDistance), eq(maxDistance), 
                eq(surfaceType), eq(publicOnly), any(Pageable.class));
    }

    @Test
    void createRoute_WithDifficulty_CreatesRouteWithDifficulty() throws Exception {
        // Arrange
        when(routeService.createRoute(any(RouteCreateRequest.class))).thenReturn(mockRouteResponse);

        // Act & Assert
        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest))
                .with(user("user").roles("USER"))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.difficulty", is(3)));

        verify(routeService).createRoute(argThat(request -> 
            request.getDifficulty() != null && 
            request.getDifficulty().equals(3)));
    }

    @Test
    void updateRoute_WithDifficulty_UpdatesRouteWithDifficulty() throws Exception {
        // Arrange
        when(routeService.updateRoute(eq(testRouteId), any(RouteCreateRequest.class))).thenReturn(mockRouteResponse);

        // Change difficulty in request
        validRouteRequest.setDifficulty(4);
        mockRouteResponse.setDifficulty(4);

        // Act & Assert
        mockMvc.perform(put("/api/routes/{id}", testRouteId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest))
                .with(user("user").roles("USER"))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty", is(4)));

        verify(routeService).updateRoute(eq(testRouteId), argThat(request -> 
            request.getDifficulty() != null && 
            request.getDifficulty().equals(4)));
    }

    @Test
    void createRoute_WithoutDifficulty_HandlesNullDifficulty() throws Exception {
        // Arrange
        validRouteRequest.setDifficulty(null);
        mockRouteResponse.setDifficulty(null);
        
        when(routeService.createRoute(any(RouteCreateRequest.class))).thenReturn(mockRouteResponse);

        // Act & Assert
        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest))
                .with(user("user").roles("USER"))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.difficulty").doesNotExist());

        verify(routeService).createRoute(argThat(request -> 
            request.getDifficulty() == null));
    }

    @Test
    void getAllRoutes_WithInvalidDifficulty_ReturnsBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/routes")
                .param("difficulty", "invalid")
                .with(user("user").roles("USER"))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(routeService, never()).getRoutesWithFilters(
                any(), any(), any(), any(), any(), any(), any());
    }
}