package com.trackoss.trackoss_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackoss.trackoss_backend.dto.RouteCreateRequest;
import com.trackoss.trackoss_backend.dto.RouteResponse;
import com.trackoss.trackoss_backend.entity.Route;
import com.trackoss.trackoss_backend.repository.RouteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Transactional
class RouteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RouteRepository routeRepository;

    private RouteCreateRequest validRouteRequest;

    @BeforeEach
    void setUp() {
        // Clean up database before each test
        routeRepository.deleteAll();
        
        // Create valid route request
        validRouteRequest = new RouteCreateRequest();
        validRouteRequest.setName("Integration Test Route");
        validRouteRequest.setDescription("A test route for integration testing");
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
        
        RouteCreateRequest.RoutePointRequest point3 = new RouteCreateRequest.RoutePointRequest();
        point3.setLatitude(47.6262);
        point3.setLongitude(-122.3121);
        point3.setElevation(70.0);
        point3.setPointType("END_POINT");
        point3.setName("End Point");
        
        validRouteRequest.setPoints(Arrays.asList(point1, point2, point3));
    }

    @Test
    void fullRouteLifecycle_CreateReadUpdateDelete_WorksCorrectly() throws Exception {
        // 1. Create route
        MvcResult createResult = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Test Route"))
                .andExpect(jsonPath("$.routeType").value("CYCLING"))
                .andExpect(jsonPath("$.isPublic").value(true))
                .andExpect(jsonPath("$.pointCount").value(3))
                .andExpect(jsonPath("$.totalDistance").exists())
                .andExpect(jsonPath("$.totalElevationGain").exists())
                .andExpect(jsonPath("$.estimatedDuration").exists())
                .andReturn();

        RouteResponse createdRoute = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), RouteResponse.class);
        UUID routeId = createdRoute.getId();

        // Verify route was saved to database
        assertThat(routeRepository.findById(routeId)).isPresent();

        // 2. Read route
        mockMvc.perform(get("/api/routes/{id}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(routeId.toString()))
                .andExpect(jsonPath("$.name").value("Integration Test Route"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").isNotEmpty());

        // 3. Update route
        validRouteRequest.setName("Updated Integration Test Route");
        validRouteRequest.setDescription("Updated description");
        validRouteRequest.setRouteType(Route.RouteType.ROAD_CYCLING);

        mockMvc.perform(put("/api/routes/{id}", routeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRouteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Integration Test Route"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.routeType").value("ROAD_CYCLING"));

        // 4. List routes (should contain our route)
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(routeId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));

        // 5. Delete route
        mockMvc.perform(delete("/api/routes/{id}", routeId))
                .andExpect(status().isNoContent());

        // Verify route was deleted from database
        assertThat(routeRepository.findById(routeId)).isEmpty();

        // Verify route is no longer in list
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void searchAndFilteringFunctionality_WorksCorrectly() throws Exception {
        // Create multiple routes with different properties
        RouteCreateRequest publicRoute = createRouteRequest("Public Cycling Route", true, Route.RouteType.CYCLING);
        RouteCreateRequest privateRoute = createRouteRequest("Private Mountain Route", false, Route.RouteType.MOUNTAIN_BIKING);
        RouteCreateRequest roadRoute = createRouteRequest("Road Cycling Adventure", true, Route.RouteType.ROAD_CYCLING);

        // Create all routes
        UUID publicRouteId = createRouteAndGetId(publicRoute);
        UUID privateRouteId = createRouteAndGetId(privateRoute);
        UUID roadRouteId = createRouteAndGetId(roadRoute);

        // Test search by name
        mockMvc.perform(get("/api/routes")
                .param("search", "cycling"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // Should find 2 routes with "cycling"

        // Test public only filter
        mockMvc.perform(get("/api/routes")
                .param("publicOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)) // Should find 2 public routes
                .andExpect(jsonPath("$.content[*].isPublic").value(everyItem(is(true))));

        // Test pagination
        mockMvc.perform(get("/api/routes")
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void exportImportFunctionality_WorksCorrectly() throws Exception {
        // Create a route first
        UUID routeId = createRouteAndGetId(validRouteRequest);

        // Test GPX export
        MvcResult gpxResult = mockMvc.perform(get("/api/routes/{id}/export/gpx", routeId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/xml"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(containsString("<?xml")))
                .andExpect(content().string(containsString("<gpx")))
                .andReturn();

        // Test GeoJSON export
        mockMvc.perform(get("/api/routes/{id}/export/geojson", routeId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().string(containsString("FeatureCollection")))
                .andExpect(jsonPath("$.type").value("FeatureCollection"))
                .andExpect(jsonPath("$.features").isArray());

        // Test GPX import
        String sampleGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="Test">
              <trk>
                <name>Test Track</name>
                <trkseg>
                  <trkpt lat="47.6815" lon="-122.2654">
                    <ele>15</ele>
                  </trkpt>
                  <trkpt lat="47.6825" lon="-122.2644">
                    <ele>18</ele>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """;

        MockMultipartFile gpxFile = new MockMultipartFile(
                "file", "test.gpx", "application/gpx+xml", sampleGpx.getBytes());

        mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(gpxFile)
                .param("routeName", "Imported GPX Route"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Imported GPX Route"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").isNotEmpty());

        // Verify the imported route was saved
        long routeCount = routeRepository.count();
        assertThat(routeCount).isEqualTo(2); // Original + imported
    }

    @Test
    void gpxImportWithSampleFile_WorksCorrectly() throws Exception {
        // Test with the actual sample GPX file from test resources
        String sampleGpxFromResources = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="TrackOSS Test" xmlns="http://www.topografix.com/GPX/1/1">
              <metadata>
                <name>Test Route for Unit Testing</name>
                <desc>A sample cycling route for testing GPX import functionality</desc>
                <time>2025-01-10T10:00:00Z</time>
              </metadata>

              <wpt lat="47.6815" lon="-122.2654">
                <ele>15</ele>
                <name>Test Start Point</name>
                <desc>Starting point for test route</desc>
              </wpt>

              <trk>
                <name>Test Track</name>
                <desc>Main track for testing</desc>
                <trkseg>
                  <trkpt lat="47.6815" lon="-122.2654">
                    <ele>15</ele>
                    <time>2025-01-10T10:00:00Z</time>
                  </trkpt>
                  <trkpt lat="47.6825" lon="-122.2644">
                    <ele>18</ele>
                    <time>2025-01-10T10:02:00Z</time>
                  </trkpt>
                  <trkpt lat="47.6835" lon="-122.2634">
                    <ele>20</ele>
                    <time>2025-01-10T10:04:00Z</time>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
            """;

        MockMultipartFile gpxFile = new MockMultipartFile(
                "file", "sample_route.gpx", "application/gpx+xml", sampleGpxFromResources.getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/routes/import/gpx")
                .file(gpxFile)
                .param("routeName", "Sample GPX Import Test"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sample GPX Import Test"))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.points").isNotEmpty())
                .andExpect(jsonPath("$.totalDistance").exists())
                .andExpect(jsonPath("$.totalElevationGain").exists())
                .andReturn();

        RouteResponse importedRoute = objectMapper.readValue(
                result.getResponse().getContentAsString(), RouteResponse.class);

        // Verify the route was saved to database
        assertThat(routeRepository.findById(importedRoute.getId())).isPresent();

        // Verify we can export it back
        mockMvc.perform(get("/api/routes/{id}/export/gpx", importedRoute.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/xml"))
                .andExpect(content().string(containsString("<?xml")))
                .andExpect(content().string(containsString("<gpx")));
    }

    @Test
    void validationErrors_ReturnProperErrorResponses() throws Exception {
        // Test empty name
        RouteCreateRequest invalidRequest = new RouteCreateRequest();
        invalidRequest.setName("");
        invalidRequest.setPoints(Arrays.asList(createValidPoint()));

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Test no points
        invalidRequest.setName("Valid Name");
        invalidRequest.setPoints(Arrays.asList());

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        // Test name too long
        invalidRequest.setName("A".repeat(256));
        invalidRequest.setPoints(Arrays.asList(createValidPoint()));

        mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonExistentRouteOperations_ReturnNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        // Test get non-existent route
        mockMvc.perform(get("/api/routes/{id}", nonExistentId))
                .andExpect(status().isNotFound());

        // Test export non-existent route
        mockMvc.perform(get("/api/routes/{id}/export/gpx", nonExistentId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/routes/{id}/export/geojson", nonExistentId))
                .andExpect(status().isNotFound());
    }

    // Helper methods
    private RouteCreateRequest createRouteRequest(String name, boolean isPublic, Route.RouteType routeType) {
        RouteCreateRequest request = new RouteCreateRequest();
        request.setName(name);
        request.setDescription("Test route: " + name);
        request.setRouteType(routeType);
        request.setIsPublic(isPublic);
        request.setPoints(Arrays.asList(createValidPoint(), createValidPoint()));
        return request;
    }

    private RouteCreateRequest.RoutePointRequest createValidPoint() {
        RouteCreateRequest.RoutePointRequest point = new RouteCreateRequest.RoutePointRequest();
        point.setLatitude(47.6062 + Math.random() * 0.01);
        point.setLongitude(-122.3321 + Math.random() * 0.01);
        point.setElevation(50.0 + Math.random() * 100);
        point.setPointType("TRACK_POINT");
        return point;
    }

    private UUID createRouteAndGetId(RouteCreateRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/routes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        RouteResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), RouteResponse.class);
        return response.getId();
    }
}
