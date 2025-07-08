# TrackOSS Backend

A Spring Boot backend service for managing cycling routes with GPS track data, featuring GPX/GeoJSON import/export capabilities.

## Features

- **Route Management**: Create, read, update, and delete cycling routes with GPS coordinates
- **GPX Support**: Import GPX files and export routes as GPX for GPS devices and cycling apps
- **GeoJSON Export**: Export routes as GeoJSON for web mapping applications
- **Spatial Queries**: Find routes by location using PostGIS spatial capabilities
- **Route Statistics**: Automatic calculation of distance, elevation gain, and duration
- **Map Tile Proxy**: Built-in proxy for map tile services
- **REST API**: Full OpenAPI/Swagger documentation with pagination and search

## Technology Stack

- **Framework**: Spring Boot 3.5
- **Database**: PostgreSQL with PostGIS extension
- **Build Tool**: Gradle
- **Java Version**: 17

## Quick Start

### Prerequisites
- Java 17+
- Docker or Podman (for database)

### Setup
```bash
# Clone the repository
git clone https://github.com/m4um4u1/trackoss-backend.git
cd trackoss-backend

# Start PostgreSQL with PostGIS
docker compose up -d

# Run the application
./gradlew bootRun
```

The application starts on `http://localhost:8080`

**API Documentation**: `http://localhost:8080/swagger-ui.html`

## API Endpoints

### Routes
- `POST /api/routes` - Create route
- `GET /api/routes` - List routes (with search, pagination)
- `GET /api/routes/{id}` - Get route by ID
- `PUT /api/routes/{id}` - Update route
- `DELETE /api/routes/{id}` - Delete route

### Import/Export
- `POST /api/routes/import/gpx` - Import GPX file
- `GET /api/routes/{id}/export/gpx` - Export as GPX
- `GET /api/routes/{id}/export/geojson` - Export as GeoJSON

### Map Tiles
- `GET /api/map-proxy/{styleId}/style.json` - Proxy map style
- `GET /api/map-proxy/{styleId}/{z}/{x}/{y}.{format}` - Proxy map tiles

## Configuration

Key settings in `application.properties`:

```properties
# Database (configured for Docker Compose)
spring.datasource.url=jdbc:postgresql://localhost:5432/trackossdb
spring.datasource.username=trackoss_user
spring.datasource.password=trackoss_password

# Map Tile Proxy (optional)
maptile.service.target-base-url=https://api.maptiler.com/maps
maptile.service.api.key=${MAPTILE_SERVICE_API_KEY:dummy_key}
```

## Development

### Running Tests
```bash
./gradlew test
```

### Building
```bash
./gradlew build
```

### Testing API
Use the Bruno collection in `trackoss-api/` directory for comprehensive API testing.

## License

MIT License - see LICENSE file for details.