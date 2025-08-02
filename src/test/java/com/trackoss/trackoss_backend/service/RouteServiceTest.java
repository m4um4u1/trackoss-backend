package com.trackoss.trackoss_backend.service;

import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.entity.RoutePoint;
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
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteService routeService;

    private Route testRoute;
    private RouteCreateRequest testRequest;
    private UUID testRouteId;

    @BeforeEach
    void setUp() {
        testRouteId = UUID.randomUUID();
        
        // Create test route
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

        // Create test route points
        RoutePoint point1 = new RoutePoint();
        point1.setId(UUID.randomUUID());
        point1.setLatitude(47.6062);
        point1.setLongitude(-122.3321);
        point1.setElevation(100.0);
        point1.setSequenceOrder(0);
        point1.setPointType(RoutePoint.PointType.START_POINT);
        point1.setRoute(testRoute);

        RoutePoint point2 = new RoutePoint();
        point2.setId(UUID.randomUUID());
        point2.setLatitude(47.6162);
        point2.setLongitude(-122.3221);
        point2.setElevation(150.0);
        point2.setSequenceOrder(1);
        point2.setPointType(RoutePoint.PointType.END_POINT);
        point2.setRoute(testRoute);

        testRoute.setRoutePoints(Arrays.asList(point1, point2));

        // Create test request
        testRequest = new RouteCreateRequest();
        testRequest.setName("Test Route");
        testRequest.setDescription("Test Description");
        testRequest.setRouteType(Route.RouteType.CYCLING);
        testRequest.setIsPublic(true);

        RouteCreateRequest.RoutePointRequest pointReq1 = new RouteCreateRequest.RoutePointRequest();
        pointReq1.setLatitude(47.6062);
        pointReq1.setLongitude(-122.3321);
        pointReq1.setElevation(100.0);
        pointReq1.setPointType("START_POINT");

        RouteCreateRequest.RoutePointRequest pointReq2 = new RouteCreateRequest.RoutePointRequest();
        pointReq2.setLatitude(47.6162);
        pointReq2.setLongitude(-122.3221);
        pointReq2.setElevation(150.0);
        pointReq2.setPointType("END_POINT");

        testRequest.setPoints(Arrays.asList(pointReq1, pointReq2));
    }

    @Test
    void findNearbyRoutes_ValidCoordinates_ReturnsRoutes() {
        // Arrange
        Double latitude = 47.6062;
        Double longitude = -122.3321;
        Double radiusKm = 10.0;
        Pageable pageable = PageRequest.of(0, 20);
        
        List<Route> routes = Collections.singletonList(testRoute);
        Page<Route> routePage = new PageImpl<>(routes, pageable, 1);
        
        when(routeRepository.findNearbyRoutes(eq(latitude), eq(longitude), eq(radiusKm), eq(pageable)))
                .thenReturn(routePage);

        // Act
        Page<RouteResponse> result = routeService.findNearbyRoutes(latitude, longitude, radiusKm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        RouteResponse routeResponse = result.getContent().get(0);
        assertEquals(testRoute.getId(), routeResponse.getId());
        assertEquals(testRoute.getName(), routeResponse.getName());
        assertEquals(testRoute.getIsPublic(), routeResponse.getIsPublic());

        verify(routeRepository).findNearbyRoutes(eq(latitude), eq(longitude), eq(radiusKm), eq(pageable));
    }

    @Test
    void findNearbyRoutes_NoRoutesFound_ReturnsEmptyPage() {
        // Arrange
        Double latitude = 47.6062;
        Double longitude = -122.3321;
        Double radiusKm = 1.0; // Small radius
        Pageable pageable = PageRequest.of(0, 20);
        
        Page<Route> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        
        when(routeRepository.findNearbyRoutes(eq(latitude), eq(longitude), eq(radiusKm), eq(pageable)))
                .thenReturn(emptyPage);

        // Act
        Page<RouteResponse> result = routeService.findNearbyRoutes(latitude, longitude, radiusKm, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        verify(routeRepository).findNearbyRoutes(eq(latitude), eq(longitude), eq(radiusKm), eq(pageable));
    }

    @Test
    void getPublicRoutes_ReturnsOnlyPublicRoutes() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        List<Route> publicRoutes = Collections.singletonList(testRoute);
        Page<Route> routePage = new PageImpl<>(publicRoutes, pageable, 1);
        
        when(routeRepository.findByIsPublicTrue(eq(pageable))).thenReturn(routePage);

        // Act
        Page<RouteResponse> result = routeService.getPublicRoutes(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        
        RouteResponse routeResponse = result.getContent().get(0);
        assertTrue(routeResponse.getIsPublic());

        verify(routeRepository).findByIsPublicTrue(eq(pageable));
    }

    @Test
    void getRoute_ExistingRoute_ReturnsRouteResponse() {
        // Arrange
        when(routeRepository.findById(testRouteId)).thenReturn(Optional.of(testRoute));

        // Act
        Optional<RouteResponse> result = routeService.getRoute(testRouteId);

        // Assert
        assertTrue(result.isPresent());
        RouteResponse routeResponse = result.get();
        assertEquals(testRoute.getId(), routeResponse.getId());
        assertEquals(testRoute.getName(), routeResponse.getName());
        assertEquals(2, routeResponse.getPoints().size());

        verify(routeRepository).findById(testRouteId);
    }

    @Test
    void getRoute_NonExistentRoute_ReturnsEmpty() {
        // Arrange
        when(routeRepository.findById(testRouteId)).thenReturn(Optional.empty());

        // Act
        Optional<RouteResponse> result = routeService.getRoute(testRouteId);

        // Assert
        assertFalse(result.isPresent());

        verify(routeRepository).findById(testRouteId);
    }

    @Test
    void deleteRoute_ExistingRoute_DeletesSuccessfully() {
        // Arrange
        when(routeRepository.existsById(testRouteId)).thenReturn(true);

        // Act
        assertDoesNotThrow(() -> routeService.deleteRoute(testRouteId));

        // Assert
        verify(routeRepository).existsById(testRouteId);
        verify(routeRepository).deleteById(testRouteId);
    }

    @Test
    void deleteRoute_NonExistentRoute_ThrowsException() {
        // Arrange
        when(routeRepository.existsById(testRouteId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> routeService.deleteRoute(testRouteId));
        assertEquals("Route not found", exception.getMessage());

        verify(routeRepository).existsById(testRouteId);
        verify(routeRepository, never()).deleteById(any());
    }
}
