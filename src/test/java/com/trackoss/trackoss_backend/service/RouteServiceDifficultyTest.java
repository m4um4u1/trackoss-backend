package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceDifficultyTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteStatisticsService routeStatisticsService;

    @InjectMocks
    private RouteService routeService;

    private Route testRoute;
    private RouteCreateRequest testRequest;
    private UUID testRouteId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testRouteId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
        
        // Create test route with difficulty
        testRoute = new Route();
        testRoute.setId(testRouteId);
        testRoute.setName("Test Route");
        testRoute.setDescription("Test Description");
        testRoute.setRouteType(Route.RouteType.CYCLING);
        testRoute.setIsPublic(true);
        testRoute.setTotalDistance(10000.0);
        testRoute.setTotalElevationGain(500.0);
        testRoute.setEstimatedDuration(3600L);
        testRoute.setDifficulty(3);
        testRoute.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":3}");
        testRoute.setRoutePoints(new ArrayList<>());
        
        // Create test request with difficulty
        testRequest = new RouteCreateRequest();
        testRequest.setName("Test Route");
        testRequest.setDescription("Test Description");
        testRequest.setRouteType(Route.RouteType.CYCLING);
        testRequest.setIsPublic(true);
        testRequest.setDifficulty(3);
        testRequest.setMetadata("{\"surface\":\"asphalt\",\"difficulty\":3}");
        testRequest.setPoints(new ArrayList<>());
    }

    @Test
    void findByDifficulty_ShouldReturnMatchingRoutes() {
        // Arrange
        Integer difficulty = 3;
        List<Route> routes = Collections.singletonList(testRoute);
        Page<Route> routePage = new PageImpl<>(routes, pageable, 1);
        
        when(routeRepository.findByDifficulty(eq(difficulty), eq(pageable)))
                .thenReturn(routePage);

        // Act
        Page<RouteResponse> result = routeService.findByDifficulty(difficulty, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(testRoute.getName(), result.getContent().get(0).getName());
        assertEquals(testRoute.getDifficulty(), result.getContent().get(0).getDifficulty());
        
        verify(routeRepository).findByDifficulty(difficulty, pageable);
    }

    @Test
    void getRoutesWithFilters_ShouldReturnMatchingRoutes() {
        // Arrange
        Integer difficulty = 3;
        Route.RouteType routeType = Route.RouteType.CYCLING;
        Double minDistance = 5000.0;
        Double maxDistance = 15000.0;
        String surfaceType = "asphalt";
        Boolean isPublic = true;
        
        List<Route> routes = Collections.singletonList(testRoute);
        Page<Route> routePage = new PageImpl<>(routes, pageable, 1);
        
        when(routeRepository.findWithFilters(
                eq(difficulty),
                eq(routeType),
                eq(minDistance),
                eq(maxDistance),
                contains(surfaceType),
                eq(isPublic),
                eq(pageable)
        )).thenReturn(routePage);

        // Act
        Page<RouteResponse> result = routeService.getRoutesWithFilters(
                difficulty,
                routeType,
                minDistance,
                maxDistance,
                surfaceType,
                isPublic,
                pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(testRoute.getName(), result.getContent().get(0).getName());
        assertEquals(testRoute.getDifficulty(), result.getContent().get(0).getDifficulty());
        
        verify(routeRepository).findWithFilters(
                difficulty,
                routeType,
                minDistance,
                maxDistance,
                "\"surface\":\"" + surfaceType + "\"",
                isPublic,
                pageable
        );
    }

    @Test
    void createRoute_WithDifficulty_ShouldSetDifficultyField() {
        // Arrange
        when(routeRepository.save(any(Route.class))).thenReturn(testRoute);
        doNothing().when(routeStatisticsService).calculateMissingStatistics(any(Route.class));

        // Act
        RouteResponse result = routeService.createRoute(testRequest);

        // Assert
        assertEquals(testRequest.getDifficulty(), result.getDifficulty());
        
        verify(routeRepository).save(argThat(route -> 
            route.getDifficulty() != null && 
            route.getDifficulty().equals(testRequest.getDifficulty())
        ));
    }

    @Test
    void updateRoute_WithDifficulty_ShouldUpdateDifficultyField() {
        // Arrange
        when(routeRepository.findById(eq(testRouteId))).thenReturn(Optional.of(testRoute));
        when(routeRepository.save(any(Route.class))).thenReturn(testRoute);
        
        // Change difficulty in request
        testRequest.setDifficulty(4);

        // Act
        RouteResponse result = routeService.updateRoute(testRouteId, testRequest);

        // Assert
        assertEquals(testRequest.getDifficulty(), result.getDifficulty());
        
        verify(routeRepository).save(argThat(route -> 
            route.getDifficulty() != null && 
            route.getDifficulty().equals(testRequest.getDifficulty())
        ));
    }

    @Test
    void getRoutesWithFilters_NullParameters_ShouldHandleNullValues() {
        // Arrange
        List<Route> routes = Collections.singletonList(testRoute);
        Page<Route> routePage = new PageImpl<>(routes, pageable, 1);
        
        when(routeRepository.findWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(routePage);

        // Act
        Page<RouteResponse> result = routeService.getRoutesWithFilters(
                null,
                null,
                null,
                null,
                null,
                null,
                pageable
        );

        // Assert
        assertEquals(1, result.getTotalElements());
        
        verify(routeRepository).findWithFilters(
                null,
                null,
                null,
                null,
                null,
                null,
                pageable
        );
    }
}