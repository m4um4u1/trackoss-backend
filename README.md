# TrackOSS Backend

A Spring Boot backend service for managing cycling routes with GPS track data, featuring GPX/GeoJSON import/export capabilities.

## Features

- **Route Management**: Create, read, update, and delete cycling routes with GPS coordinates
- **GPX Support**: Import GPX files and export routes as GPX for GPS devices and cycling apps
- **GeoJSON Export**: Export routes as GeoJSON for web mapping applications
- **Spatial Queries**: Find routes by location using PostGIS spatial capabilities
- **Route Statistics**: Automatic calculation of distance, elevation gain, and duration
- **User Management**: Authentication and authorization for route ownership
- **Route Sharing**: Public and private route visibility options
- **REST API**: Full OpenAPI/Swagger documentation with pagination and search

## Technology Stack

- **Framework**: Spring Boot 3.5
- **Database**: PostgreSQL with PostGIS extension
- **Build Tool**: Gradle
- **Java Version**: 17

## Quick Start

### Prerequisites
- Java 17+ (for local development)
- Docker or Podman

### Development Setup
```bash
# Clone the repository
git clone https://github.com/m4um4u1/trackoss-backend.git
cd trackoss-backend

# Start PostgreSQL with PostGIS
docker compose up -d

# Run the application
./gradlew bootRun
```

### Docker Deployment
```bash
# Clone the repository
git clone https://github.com/m4um4u1/trackoss-backend.git
cd trackoss-backend

# Start the application stack
docker-compose up -d

# Or use the simplified setup with all services
docker-compose -f docker-compose.simple.yaml up -d
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

### Authentication
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh JWT token

## Configuration

Key settings in `application.properties`:

```properties
# Database (configured for Docker Compose)
spring.datasource.url=jdbc:postgresql://localhost:5432/trackossdb
spring.datasource.username=trackoss_user
spring.datasource.password=trackoss_password

# JWT Configuration
app.jwt.secret=${JWT_SECRET:your-secret-key}
app.jwt.expiration=86400000
```

## Docker

### Container Images
The application can be containerized using the provided Dockerfile:

```bash
# Build the Docker image
docker build -t trackoss-backend .

# Run with external database
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/trackossdb \
  -e SPRING_DATASOURCE_USERNAME=trackoss_user \
  -e SPRING_DATASOURCE_PASSWORD=trackoss_password \
  trackoss-backend
```

### Compose Files
- **`compose.yaml`**: Development dependencies (PostgreSQL only)
- **`docker-compose.yaml`**: Standard deployment (PostgreSQL + Backend)
- **`docker-compose.simple.yaml`**: Full stack with TileServer and Valhalla

### Environment Variables
- `SPRING_DATASOURCE_URL`: Database connection URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT token generation
- `APP_JWT_EXPIRATION`: JWT token expiration time in milliseconds

## Architecture

TrackOSS follows best practices with clean separation of concerns:

- **Backend**: Focuses solely on route management, storage, and user data
- **TileServer**: Serves map tiles directly to frontend (with CORS)
- **Valhalla**: Provides routing directly to frontend (with CORS)
- **Frontend**: Connects directly to all services


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