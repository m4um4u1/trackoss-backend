# TrackOSS API Collection

This Bruno collection contains all the API endpoints for testing the TrackOSS cycling route saving functionality.

## Setup

1. **Start the application with PostgreSQL/PostGIS**:
   ```bash
   docker-compose up -d
   ./gradlew bootRun
   ```

2. **Open Bruno** and import this collection

3. **Select the Local environment** which sets:
   - `baseUrl`: http://localhost:8080
   - `routeId`: 1 (will be updated by tests)

## Collection Structure

### RouteController
Contains all route-related API endpoints:

1. **Create Route** - Creates a new cycling route with sample Lake Washington loop data
2. **Get Route by ID** - Retrieves a specific route
3. **List All Routes** - Gets paginated list of all routes
4. **Update Route** - Updates route metadata
5. **Get Public Routes** - Lists only public routes
6. **Find Routes Nearby** - Spatial search for routes near a location
7. **Export Route as GPX** - Downloads route in GPX format
8. **Export Route as GeoJSON** - Downloads route in GeoJSON format
9. **Import GPX File** - Uploads and imports a GPX file
10. **Import GeoJSON** - Imports route from GeoJSON data
11. **Delete Route** - Permanently deletes a route

## Testing Flow

### Basic CRUD Operations
1. Run **Create Route** - This will set the `routeId` variable
2. Run **Get Route by ID** - Verify the route was created
3. Run **Update Route** - Modify the route
4. Run **List All Routes** - See all routes
5. Run **Delete Route** - Clean up (optional)

### Import/Export Testing
1. Run **Create Route** first to have a route to export
2. Run **Export Route as GPX** - Download GPX file
3. Run **Export Route as GeoJSON** - Download GeoJSON file
4. Run **Import GPX File** - Upload the sample GPX file
5. Run **Import GeoJSON** - Import from JSON data

### Spatial Features
1. Run **Create Route** with Seattle cycling route coordinates
2. Run **Find Routes Nearby** - Search for cycling routes near Seattle
3. Run **Get Public Routes** - Filter by visibility

## Sample Data

The collection includes:
- **sample_route.gpx**: A sample Burke-Gilman Trail GPX file for import testing
- Sample GeoJSON data for Green Lake cycling loop in the Import GeoJSON request
- Seattle-area cycling route coordinates for spatial testing

## Environment Variables

- `baseUrl`: API base URL (default: http://localhost:8080)
- `routeId`: Current route ID (automatically set by Create Route test)

## Database Requirements

The application requires PostgreSQL with PostGIS extension. The docker-compose.yaml is configured with:
- Image: `postgis/postgis:16-3.4`
- Database: `trackossdb`
- User: `trackoss_user`
- Password: `trackoss_password`

## File Formats Supported

### GPX (GPS Exchange Format)
- Standard GPS format
- Supports tracks, waypoints, and metadata
- Compatible with Garmin, Strava, Komoot, etc.

### GeoJSON
- Web-friendly geographic data format
- Supports points, lines, and polygons
- Compatible with web mapping libraries

### Database Storage
- Normalized relational structure
- PostGIS spatial data types
- Efficient spatial indexing and queries

## Error Handling

All endpoints include proper error handling:
- 400: Bad Request (validation errors)
- 404: Not Found (route doesn't exist)
- 500: Internal Server Error

## Testing Tips

1. **Sequential Testing**: Run Create Route first to set up data
2. **Variable Usage**: The `routeId` variable is automatically set by successful create operations
3. **File Uploads**: Ensure sample_route.gpx is in the same directory as the request
4. **Spatial Queries**: Use realistic coordinates for nearby searches
5. **Cleanup**: Use Delete Route to clean up test data

## Integration Examples

### With Cycling Navigation Apps
1. Export route as GPX
2. Transfer file to device
3. Import in OsmAnd, Komoot, or other cycling apps

### With Web Cycling Applications
1. Export route as GeoJSON
2. Load in Leaflet/Mapbox cycling maps
3. Display cycling routes on web map

### With Cycling Fitness Apps
1. Export route as GPX
2. Import to Strava, Garmin Connect, or Wahoo
3. Use for cycling training and navigation