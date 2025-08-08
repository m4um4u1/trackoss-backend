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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import org.mockito.Mockito;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private RouteService routeService;

    @MockBean
    private GpxService gpxService;

    @MockBean
    private GeoJsonService geoJsonService;

    @Autowired
    private RouteController routeController;

    private RouteCreateRequest validRouteRequest;
    private RouteResponse mockRouteResponse;
    private UUID testRouteId;

    @BeforeEach
    void setUp() {
        testRouteId = UUID.randomUUID();
        
        // Create valid route request
        validRouteRequest = new RouteCreateRequest();
        validRouteRequest.setName("Test Route");
        validRouteRequest.setDescription("A test cycling route");
        validRouteRequest.setRouteType(Route.RouteType.CYCLING);
        validRouteRequest.setIsPublic(true);
        
        // Create route points
        RouteCreateRequest.RoutePointRequest point1 = new RouteCreateRequest.RoutePointRequest();
        point1.setLatitude(47.6062);
        point1.setLongitude(-122.3321);
        point1.setElevation(50.0);
        point1.setPointType("START_POINT");
        point1.setName("Start Point");
        
        RouteCreateRequest.RoutePointRequest point2 = new RouteCreateRequest.RoutePointRequest();
        point2.setLatitude(47.6162);
        point2.setLongitude(-122.3221);
        point2.setElevation(60.0);
        point2.setPointType("TRACK_POINT");
        
        validRouteRequest.setPoints(Arrays.asList(point1, point2));
        
        // Create mock response
        mockRouteResponse = new RouteResponse();
        mockRouteResponse.setId(testRouteId);
        mockRouteResponse.setName("Test Route");
        mockRouteResponse.setDescription("A test cycling route");
        mockRouteResponse.setRouteType(Route.RouteType.CYCLING);
        mockRouteResponse.setIsPublic(true);
        mockRouteResponse.setCreatedAt(LocalDateTime.now());
        mockRouteResponse.setUpdatedAt(LocalDateTime.now());
        mockRouteResponse.setTotalDistance(1000.0);
        mockRouteResponse.setTotalElevationGain(100.0);
        mockRouteResponse.setEstimatedDuration(3600L);
        mockRouteResponse.setPointCount(2);
    }

    @Test
    void createRoute_ValidRequest_ReturnsCreated() throws Exception {
        when(routeService.createRoute(any(RouteCreateRequest.class), isNull())).thenReturn(mockRouteResponse);

        ResponseEntity<RouteResponse> response = routeController.createRoute(validRouteRequest, null);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals("Test Route", response.getBody().getName());
        assertEquals(Route.RouteType.CYCLING, response.getBody().getRouteType());
        assertTrue(response.getBody().getIsPublic());
        assertEquals(2, response.getBody().getPointCount());

        verify(routeService).createRoute(any(RouteCreateRequest.class), isNull());
    }

    @Test
    void createRoute_InvalidRequest_ReturnsBadRequest() throws Exception {
        RouteCreateRequest invalidRequest = new RouteCreateRequest();
        // Missing required fields

        mockMvc.perform(post("/api/routes")
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(routeService, never()).createRoute(any());
    }

    @Test
    void createRoute_EmptyName_ReturnsBadRequest() throws Exception {
        validRouteRequest.setName("");

        mockMvc.perform(post("/api/routes")
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoute_EmptyPoints_ReturnsBadRequest() throws Exception {
        validRouteRequest.setPoints(List.of());

        mockMvc.perform(post("/api/routes")
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRouteById_ExistingRoute_ReturnsRoute() throws Exception {
        when(routeService.getRoute(testRouteId)).thenReturn(Optional.of(mockRouteResponse));

        mockMvc.perform(get("/api/routes/{id}", testRouteId)
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRouteId.toString()))
                .andExpect(jsonPath("$.name").value("Test Route"));

        verify(routeService).getRoute(testRouteId);
    }

    @Test
    void getRouteById_NonExistentRoute_ReturnsNotFound() throws Exception {
        when(routeService.getRoute(testRouteId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/routes/{id}", testRouteId)
                .with(user("testuser")))
                .andExpect(status().isNotFound());

        verify(routeService).getRoute(testRouteId);
    }

    @Test
    void getAllRoutes_DefaultPagination_ReturnsPagedRoutes() throws Exception {
        List<RouteResponse> routes = Collections.singletonList(mockRouteResponse);
        Page<RouteResponse> page = new PageImpl<>(routes, PageRequest.of(0, 20), 1);
        
        when(routeService.getAllRoutes(any())).thenReturn(page);

        mockMvc.perform(get("/api/routes")
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testRouteId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(routeService).getAllRoutes(any());
    }

    @Test
    void getAllRoutes_WithSearch_ReturnsFilteredRoutes() throws Exception {
        List<RouteResponse> routes = Collections.singletonList(mockRouteResponse);
        Page<RouteResponse> page = new PageImpl<>(routes, PageRequest.of(0, 20), 1);
        
        when(routeService.searchRoutes(eq("test"), any())).thenReturn(page);

        mockMvc.perform(get("/api/routes")
                .param("search", "test")
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(routeService).searchRoutes(eq("test"), any());
    }

    @Test
    void getAllRoutes_PublicOnly_ReturnsPublicRoutes() throws Exception {
        List<RouteResponse> routes = Collections.singletonList(mockRouteResponse);
        Page<RouteResponse> page = new PageImpl<>(routes, PageRequest.of(0, 20), 1);
        
        when(routeService.getPublicRoutes(any())).thenReturn(page);

        mockMvc.perform(get("/api/routes")
                .param("publicOnly", "true")
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(routeService).getPublicRoutes(any());
    }

    @Test
    void getAllRoutes_WithUserId_ReturnsUserRoutes() throws Exception {
        List<RouteResponse> routes = Collections.singletonList(mockRouteResponse);
        Page<RouteResponse> page = new PageImpl<>(routes, PageRequest.of(0, 20), 1);
        
        when(routeService.getUserRoutes(eq("user123"), any())).thenReturn(page);

        mockMvc.perform(get("/api/routes")
                .param("userId", "user123")
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(routeService).getUserRoutes(eq("user123"), any());
    }

    @Test
    void updateRoute_ValidRequest_ReturnsUpdatedRoute() throws Exception {
        RouteResponse updatedResponse = new RouteResponse();
        updatedResponse.setId(testRouteId);
        updatedResponse.setName("Updated Route");
        
        when(routeService.updateRoute(eq(testRouteId), any(RouteCreateRequest.class)))
                .thenReturn(updatedResponse);

        validRouteRequest.setName("Updated Route");

        mockMvc.perform(put("/api/routes/{id}", testRouteId)
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Route"));

        verify(routeService).updateRoute(eq(testRouteId), any(RouteCreateRequest.class));
    }

    @Test
    void updateRoute_NonExistentRoute_ReturnsNotFound() throws Exception {
        when(routeService.updateRoute(eq(testRouteId), any(RouteCreateRequest.class)))
                .thenThrow(new RuntimeException("Route not found"));

        mockMvc.perform(put("/api/routes/{id}", testRouteId)
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRoute_ExistingRoute_ReturnsNoContent() throws Exception {
        doNothing().when(routeService).deleteRoute(testRouteId);

        mockMvc.perform(delete("/api/routes/{id}", testRouteId)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isNoContent());

        verify(routeService).deleteRoute(testRouteId);
    }

    @Test
    void deleteRoute_NonExistentRoute_ReturnsNotFound() throws Exception {
        doThrow(new RuntimeException("Route not found")).when(routeService).deleteRoute(testRouteId);

        mockMvc.perform(delete("/api/routes/{id}", testRouteId)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportToGpx_ExistingRoute_ReturnsGpxFile() throws Exception {
        when(routeService.getRouteEntityForExport(testRouteId)).thenReturn(Optional.of(new Route()));
        when(gpxService.exportToGpx(any(Route.class))).thenReturn("<?xml version=\"1.0\"?><gpx></gpx>".getBytes());

        mockMvc.perform(get("/api/routes/{id}/export/gpx", testRouteId)
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/xml"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes("<?xml version=\"1.0\"?><gpx></gpx>".getBytes()));

        verify(routeService).getRouteEntityForExport(testRouteId);
        verify(gpxService).exportToGpx(any(Route.class));
    }

    @Test
    void exportToGpx_NonExistentRoute_ReturnsNotFound() throws Exception {
        when(routeService.getRouteEntityForExport(testRouteId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/routes/{id}/export/gpx", testRouteId)
                .with(user("testuser")))
                .andExpect(status().isNotFound());

        verify(routeService).getRouteEntityForExport(testRouteId);
        verify(gpxService, never()).exportToGpx(any());
    }

    @Test
    void exportToGeoJson_ExistingRoute_ReturnsGeoJsonFile() throws Exception {
        when(routeService.getRouteEntityForExport(testRouteId)).thenReturn(Optional.of(new Route()));
        when(geoJsonService.exportToGeoJson(any(Route.class))).thenReturn("{\"type\":\"FeatureCollection\"}");

        mockMvc.perform(get("/api/routes/{id}/export/geojson", testRouteId)
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(containsString("FeatureCollection")));

        verify(routeService).getRouteEntityForExport(testRouteId);
        verify(geoJsonService).exportToGeoJson(any(Route.class));
    }

    @Test
    void importFromGpx_ValidFile_ReturnsCreatedRoute() throws Exception {
        MockMultipartFile gpxFile = new MockMultipartFile(
                "file",
                "test.gpx",
                "application/gpx+xml",
                "<?xml version=\"1.0\"?><gpx></gpx>".getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), anyString())).thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class))).thenReturn(mockRouteResponse);

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(gpxFile)
                .param("routeName", "Imported Route")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testRouteId.toString()));

        verify(gpxService).importFromGpx(any(byte[].class), eq("Imported Route"));
        verify(routeService).createRoute(any(RouteCreateRequest.class));
    }

    @Test
    void importFromGpx_EmptyFile_ReturnsBadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/gpx+xml", new byte[0]);

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(emptyFile)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isBadRequest());

        verify(gpxService, never()).importFromGpx(any(), any());
        verify(routeService, never()).createRoute(any());
    }

    @Test
    void importFromGeoJson_ValidData_ReturnsCreatedRoute() throws Exception {
        String geoJsonData = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        MockMultipartFile geoJsonFile = new MockMultipartFile(
                "file",
                "test.geojson",
                "application/json",
                geoJsonData.getBytes()
        );

        when(geoJsonService.importFromGeoJson(anyString(), eq("GeoJSON Route"))).thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class))).thenReturn(mockRouteResponse);

        mockMvc.perform(multipart("/api/routes/import/geojson")
                .file(geoJsonFile)
                .param("routeName", "GeoJSON Route")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testRouteId.toString()));

        verify(geoJsonService).importFromGeoJson(anyString(), eq("GeoJSON Route"));
        verify(routeService).createRoute(any(RouteCreateRequest.class));
    }



    @Test
    void createRoute_LongName_ReturnsBadRequest() throws Exception {
        validRouteRequest.setName("A".repeat(256)); // Exceeds 255 character limit

        mockMvc.perform(post("/api/routes")
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoute_LongDescription_ReturnsBadRequest() throws Exception {
        validRouteRequest.setDescription("A".repeat(2001)); // Exceeds 2000 character limit

        mockMvc.perform(post("/api/routes")
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoute_InvalidRouteType_ReturnsBadRequest() throws Exception {
        String invalidJson = objectMapper.writeValueAsString(validRouteRequest)
                .replace("\"CYCLING\"", "\"INVALID_TYPE\"");

        mockMvc.perform(post("/api/routes")
                .with(csrf())
                .with(user("testuser"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importFromGpx_ValidSampleFile_ReturnsCreatedRoute() throws Exception {
        // Load the sample GPX file from test resources
        String gpxContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="TrackOSS Test" xmlns="http://www.topografix.com/GPX/1/1">
              <metadata>
                <name>Test Route</name>
                <desc>A sample cycling route for testing</desc>
              </metadata>
              <trk>
                <name>Test Track</name>
                <trkseg>
                  <trkpt lat="47.6815" lon="-122.2654">
                    <ele>15</ele>
                  </trkpt>
                  <trkpt lat="47.6825" lon="-122.2644">
                    <ele>18</ele>
                  </trkpt>
                  <trkpt lat="47.6835" lon="-122.2634">
                    <ele>20</ele>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """;

        MockMultipartFile gpxFile = new MockMultipartFile(
                "file",
                "test_route.gpx",
                "application/gpx+xml",
                gpxContent.getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), eq("Test GPX Import")))
                .thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class)))
                .thenReturn(mockRouteResponse);

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(gpxFile)
                .param("routeName", "Test GPX Import")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testRouteId.toString()))
                .andExpect(jsonPath("$.name").value("Test Route"));

        verify(gpxService).importFromGpx(any(byte[].class), eq("Test GPX Import"));
        verify(routeService).createRoute(any(RouteCreateRequest.class));
    }

    @Test
    void importFromGpx_WithoutRouteName_UsesDefaultName() throws Exception {
        String gpxContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk>
                <name>Track Name</name>
                <trkseg>
                  <trkpt lat="47.6815" lon="-122.2654">
                    <ele>15</ele>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """;

        MockMultipartFile gpxFile = new MockMultipartFile(
                "file",
                "test.gpx",
                "application/gpx+xml",
                gpxContent.getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), isNull()))
                .thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class)))
                .thenReturn(mockRouteResponse);

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(gpxFile)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated());

        verify(gpxService).importFromGpx(any(byte[].class), isNull());
    }

    @Test
    void importFromGpx_InvalidGpxContent_ReturnsError() throws Exception {
        String invalidGpxContent = "This is not valid GPX content";

        MockMultipartFile gpxFile = new MockMultipartFile(
                "file",
                "invalid.gpx",
                "application/gpx+xml",
                invalidGpxContent.getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("Invalid GPX format"));

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(gpxFile)
                .param("routeName", "Invalid GPX")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isInternalServerError());

        verify(gpxService).importFromGpx(any(byte[].class), eq("Invalid GPX"));
        verify(routeService, never()).createRoute(any());
    }

    @Test
    void importFromGpx_LargeFile_HandlesCorrectly() throws Exception {
        // Create a large GPX file content
        StringBuilder largeGpxContent = new StringBuilder();
        largeGpxContent.append("""
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk>
                <name>Large Track</name>
                <trkseg>
            """);

        // Add many track points
        for (int i = 0; i < 100; i++) {  // Reduced to 100 for test performance
            double lat = 47.6815 + (i * 0.0001);
            double lon = -122.2654 + (i * 0.0001);
            largeGpxContent.append(String.format(
                "          <trkpt lat=\"%.4f\" lon=\"%.4f\">\n" +
                "            <ele>%d</ele>\n" +
                "          </trkpt>\n", lat, lon, 15 + (i % 100)
            ));
        }

        largeGpxContent.append("""
                </trkseg>
              </trk>
            </gpx>
            """);

        MockMultipartFile largeGpxFile = new MockMultipartFile(
                "file",
                "large.gpx",
                "application/gpx+xml",
                largeGpxContent.toString().getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), eq("Large Route")))
                .thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class)))
                .thenReturn(mockRouteResponse);

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(largeGpxFile)
                .param("routeName", "Large Route")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated());

        verify(gpxService).importFromGpx(any(byte[].class), eq("Large Route"));
    }

    @Test
    void importFromGpx_WrongFileType_ReturnsError() throws Exception {
        String textContent = "This is just a text file, not GPX";

        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "not_gpx.txt",
                "text/plain",
                textContent.getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("Not a valid GPX file"));

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(textFile)
                .param("routeName", "Text File")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void importFromGpx_EmptyGpxFile_ReturnsError() throws Exception {
        String emptyGpxContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
            </gpx>
            """;

        MockMultipartFile emptyGpxFile = new MockMultipartFile(
                "file",
                "empty.gpx",
                "application/gpx+xml",
                emptyGpxContent.getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), anyString()))
                .thenThrow(new RuntimeException("GPX file contains no track data"));

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(emptyGpxFile)
                .param("routeName", "Empty GPX")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void importFromGpx_GpxWithWaypointsOnly_HandlesCorrectly() throws Exception {
        String waypointsOnlyGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <wpt lat="47.6815" lon="-122.2654">
                <ele>15</ele>
                <name>Waypoint 1</name>
              </wpt>
              <wpt lat="47.6825" lon="-122.2644">
                <ele>18</ele>
                <name>Waypoint 2</name>
              </wpt>
            </gpx>
            """;

        MockMultipartFile waypointsFile = new MockMultipartFile(
                "file",
                "waypoints.gpx",
                "application/gpx+xml",
                waypointsOnlyGpx.getBytes()
        );

        when(gpxService.importFromGpx(any(byte[].class), eq("Waypoints Route")))
                .thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class)))
                .thenReturn(mockRouteResponse);

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(waypointsFile)
                .param("routeName", "Waypoints Route")
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated());

        verify(gpxService).importFromGpx(any(byte[].class), eq("Waypoints Route"));
    }

    // Tests for new endpoints added for Swagger documentation improvements

    @Test
    void findNearbyRoutes_InvalidLatitude_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/routes/nearby")
                .param("latitude", "91.0") // Invalid latitude > 90
                .param("longitude", "-122.3321")
                .param("radiusKm", "10")
                .with(user("testuser")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findNearbyRoutes_InvalidLongitude_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/routes/nearby")
                .param("latitude", "47.6062")
                .param("longitude", "181.0") // Invalid longitude > 180
                .param("radiusKm", "10")
                .with(user("testuser")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findNearbyRoutes_InvalidRadius_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/routes/nearby")
                .param("latitude", "47.6062")
                .param("longitude", "-122.3321")
                .param("radiusKm", "-5") // Negative radius
                .with(user("testuser")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findNearbyRoutes_MissingParameters_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/routes/nearby")
                .param("latitude", "47.6062")
                .with(user("testuser")))
                // Missing longitude and radiusKm
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPublicRoutes_ReturnsOnlyPublicRoutes() throws Exception {
        List<RouteResponse> publicRoutes = Arrays.asList(mockRouteResponse);
        Page<RouteResponse> page = new PageImpl<>(publicRoutes, PageRequest.of(0, 20), 1);

        when(routeService.getPublicRoutes(any())).thenReturn(page);

        mockMvc.perform(get("/api/routes/public")
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].isPublic").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(routeService).getPublicRoutes(any());
    }

    @Test
    void getPublicRoutes_WithPagination_ReturnsCorrectPage() throws Exception {
        List<RouteResponse> publicRoutes = Arrays.asList(mockRouteResponse);
        Page<RouteResponse> page = new PageImpl<>(publicRoutes, PageRequest.of(1, 5), 10);

        when(routeService.getPublicRoutes(any())).thenReturn(page);

        mockMvc.perform(get("/api/routes/public")
                .param("page", "1")
                .param("size", "5")
                .with(user("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));

        verify(routeService).getPublicRoutes(any());
    }

    @Test
    void importFromGeoJsonRaw_ValidData_ReturnsCreatedRoute() throws Exception {
        String geoJsonData = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-122.3321, 47.6062]
                  },
                  "properties": {
                    "pointType": "START_POINT",
                    "elevation": 15
                  }
                }
              ]
            }
            """;

        when(geoJsonService.importFromGeoJson(eq(geoJsonData), eq("Raw Import Route")))
                .thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class)))
                .thenReturn(mockRouteResponse);

        mockMvc.perform(post("/api/routes/import/geojson/raw")
                .param("routeName", "Raw Import Route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(geoJsonData)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testRouteId.toString()));

        verify(geoJsonService).importFromGeoJson(eq(geoJsonData), eq("Raw Import Route"));
        verify(routeService).createRoute(any(RouteCreateRequest.class));
    }

    @Test
    void importFromGeoJsonRaw_WithoutRouteName_UsesDefault() throws Exception {
        String geoJsonData = """
            {
              "type": "FeatureCollection",
              "features": []
            }
            """;

        when(geoJsonService.importFromGeoJson(eq(geoJsonData), isNull()))
                .thenReturn(validRouteRequest);
        when(routeService.createRoute(any(RouteCreateRequest.class)))
                .thenReturn(mockRouteResponse);

        mockMvc.perform(post("/api/routes/import/geojson/raw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(geoJsonData)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isCreated());

        verify(geoJsonService).importFromGeoJson(eq(geoJsonData), isNull());
    }

    @Test
    void importFromGeoJsonRaw_InvalidGeoJson_ReturnsError() throws Exception {
        String invalidGeoJson = "{ invalid json }";

        when(geoJsonService.importFromGeoJson(eq(invalidGeoJson), anyString()))
                .thenThrow(new IOException("Invalid GeoJSON format"));

        mockMvc.perform(post("/api/routes/import/geojson/raw")
                .param("routeName", "Invalid Route")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidGeoJson)
                .with(csrf())
                .with(user("testuser")))
                .andExpect(status().isBadRequest());

        verify(geoJsonService).importFromGeoJson(eq(invalidGeoJson), eq("Invalid Route"));
        verify(routeService, never()).createRoute(any());
    }
}
