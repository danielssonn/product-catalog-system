# Version Service

## Overview

The Version Service is a comprehensive API versioning solution that enables seamless upgrades of product-service (and other microservices) interfaces. It provides version management, schema transformation, backward compatibility, and deprecation handling.

## Key Features

### 1. **Multi-Strategy Versioning**
- **URL Path**: `/api/v1/solutions`, `/api/v2/solutions`
- **Header-Based**: `X-API-Version: v2`
- **Query Parameter**: `?version=v2`
- **Content Negotiation**: `Accept: application/vnd.bank.v2+json`

### 2. **Schema Transformation**
- Automatic data transformation between API versions
- Field mapping and renaming
- Type conversions and default values
- Bidirectional transformations

### 3. **Lifecycle Management**
- Version status tracking: BETA → STABLE → DEPRECATED → SUNSET → EOL
- Automated deprecation warnings
- Sunset date enforcement
- Migration guide references

### 4. **Breaking Change Documentation**
- Detailed breaking change tracking
- Migration strategies
- Before/after examples
- Affected endpoint documentation

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client Application                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼ (Request: /api/v1/solutions)
┌─────────────────────────────────────────────────────────────────┐
│                    API Version Interceptor                       │
│  1. Detect version (URL/Header/Query/Content-Type)              │
│  2. Validate version is supported                                │
│  3. Add deprecation warnings if needed                           │
│  4. Reject if EOL                                                │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Schema Transformer                           │
│  Transform v1 request → Internal v2 format                       │
│  - Field mapping: catalogProductId → templateId                  │
│  - Add defaults: metadata = {}                                   │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Product Service (Internal)                     │
│  Process request using latest internal schema (v2)               │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Schema Transformer                           │
│  Transform Internal v2 → v1 response format                      │
│  - Field mapping: templateId → catalogProductId                  │
│  - Remove fields: metadata                                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Client Application                       │
│  Receives response in requested v1 format                        │
└─────────────────────────────────────────────────────────────────┘
```

## Domain Models

### ApiVersion
```java
{
  "serviceId": "product-service",
  "version": "v2",
  "semanticVersion": "2.0.0",
  "status": "BETA",
  "releasedAt": "2025-01-15T00:00:00Z",
  "deprecatedAt": null,
  "sunsetAt": null,
  "eolAt": null,
  "breakingChanges": [...],
  "newFeatures": [...],
  "migrationGuideUrl": "https://docs.bank.com/api/migration-v1-to-v2",
  "transformations": {
    "v1": { ... }  // Transformation rules v2 → v1
  }
}
```

### SchemaTransformation
```java
{
  "fromVersion": "v2",
  "toVersion": "v1",
  "fieldMappings": {
    "templateId": "catalogProductId",
    "name": "solutionName"
  },
  "fieldsToRemove": ["metadata"],
  "defaultValues": {},
  "type": "SIMPLE"
}
```

## API Endpoints

### Version Management

#### Register New Version
```bash
POST /api/v1/versions
Content-Type: application/json
X-User-ID: admin@bank.com

{
  "serviceId": "product-service",
  "version": "v3",
  "semanticVersion": "3.0.0",
  "status": "BETA",
  "breakingChanges": [...],
  "transformations": {...}
}
```

#### Get Version Info
```bash
GET /api/v1/versions/product-service/v2

Response:
{
  "serviceId": "product-service",
  "version": "v2",
  "status": "BETA",
  "breakingChanges": [
    {
      "type": "FIELD_RENAMED",
      "affectedField": "catalogProductId",
      "description": "Renamed to templateId",
      "migrationStrategy": "Update API clients to use 'templateId'"
    }
  ]
}
```

#### List All Versions
```bash
GET /api/v1/versions/product-service

Response:
[
  {
    "version": "v2",
    "status": "BETA",
    "releasedAt": "2025-01-15T00:00:00Z"
  },
  {
    "version": "v1",
    "status": "STABLE",
    "releasedAt": "2025-01-01T00:00:00Z"
  }
]
```

#### Get Stable Version
```bash
GET /api/v1/versions/product-service/stable

Response:
{
  "version": "v1",
  "status": "STABLE"
}
```

#### Deprecate Version
```bash
POST /api/v1/versions/product-service/v1/deprecate?reason=Replaced%20by%20v2
X-User-ID: admin@bank.com

Response:
{
  "version": "v1",
  "status": "DEPRECATED",
  "deprecatedAt": "2025-06-01T00:00:00Z",
  "sunsetAt": "2025-12-01T00:00:00Z"
}
```

### Schema Transformation

#### Transform Request
```bash
POST /api/v1/transformations/request?serviceId=product-service&fromVersion=v1&toVersion=v2
Content-Type: application/json

{
  "catalogProductId": "cat-001",
  "solutionName": "Premium Checking"
}

Response:
{
  "templateId": "cat-001",
  "name": "Premium Checking",
  "metadata": {}
}
```

#### Transform Response
```bash
POST /api/v1/transformations/response?serviceId=product-service&fromVersion=v2&toVersion=v1
Content-Type: application/json

{
  "templateId": "cat-001",
  "name": "Premium Checking",
  "metadata": {"custom": "value"}
}

Response:
{
  "catalogProductId": "cat-001",
  "solutionName": "Premium Checking"
}
```

## Client Usage Examples

### Example 1: Client Using v1 API

```bash
# Client explicitly requests v1
curl -u admin:admin123 http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: john@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Premium Checking"
  }'

# Version interceptor detects v1 from URL
# Schema transformer converts v1 → v2 (internal)
# Product service processes as v2
# Schema transformer converts v2 → v1 (response)
# Client receives v1 format
```

### Example 2: Client Using v2 API (Beta)

```bash
# Client uses new v2 API
curl -u admin:admin123 http://localhost:8082/api/v2/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: john@bank.com" \
  -d '{
    "templateId": "cat-checking-001",
    "name": "Premium Checking",
    "metadata": {"segment": "enterprise"}
  }'

# Version interceptor detects v2 from URL
# No transformation needed (already v2)
# Product service processes directly
```

### Example 3: Deprecated Version Warning

```bash
# Client uses deprecated v1
curl -u admin:admin123 http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: john@bank.com" \
  -d '{...}'

# Response Headers:
HTTP/1.1 201 Created
Warning: 299 - "Deprecated API" Deprecated on: 2025-06-01T00:00:00Z Sunset date: 2025-12-01T00:00:00Z See migration guide: https://docs.bank.com/migration-v1-to-v2
Sunset: 2025-12-01T00:00:00Z
X-API-Version: v1
X-API-Status: DEPRECATED
```

### Example 4: EOL Version Rejected

```bash
# Client tries to use EOL version
curl -u admin:admin123 http://localhost:8082/api/v0/solutions/configure \
  -H "Content-Type: application/json" \
  -d '{...}'

# Response:
HTTP/1.1 410 Gone
X-Error-Message: API version v0 has reached end-of-life
```

## Version Lifecycle

### Status Progression

```
BETA → STABLE → DEPRECATED → SUNSET → EOL
```

### Status Definitions

| Status | Description | Client Impact |
|--------|-------------|---------------|
| **BETA** | Preview version, may change | Use for testing only |
| **STABLE** | Production-ready, recommended | Safe to use |
| **DEPRECATED** | Still supported, but discouraged | Plan migration |
| **SUNSET** | Read-only, no new features | Must migrate soon |
| **EOL** | No longer available | Requests rejected (410 Gone) |

### Recommended Timeline

```
v2 Release (BETA)
  ↓
  +90 days → v2 becomes STABLE
  ↓
  +180 days → v1 becomes DEPRECATED
  ↓
  +180 days → v1 enters SUNSET
  ↓
  +90 days → v1 reaches EOL
```

## Configuration

### application.yml

```yaml
api:
  versioning:
    # Default versioning strategy
    default-strategy: URL_PATH

    # Default version if not specified
    default-version: v1

    # Supported versions
    supported-versions:
      - v1
      - v2

    # Deprecation policy
    deprecation:
      # Number of days before deprecated version is sunset
      sunset-days: 180

      # Number of days before sunset version is EOL
      eol-days: 90
```

## Deployment

### Port Mapping
- **Version Service**: Port 8090
- **Product Service**: Port 8082
- **Workflow Service**: Port 8089

### Docker Compose

```yaml
version-service:
  build:
    context: ./backend
    dockerfile: version-service/Dockerfile
  ports:
    - "8090:8090"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_DATA_MONGODB_URI: mongodb://admin:admin123@mongodb:27017/productcatalog?authSource=admin
  healthcheck:
    test: wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1
```

### Build and Run

```bash
# Build all services
mvn clean install

# Build Docker images
docker-compose build version-service

# Start version service
docker-compose up -d version-service

# Check health
curl http://localhost:8090/actuator/health

# Initialize version data
docker exec -it mongodb mongosh -u admin -p admin123 --authenticationDatabase admin
> use productcatalog
> load('/path/to/init-version-data.js')
```

## Integration with Product Service

### Option 1: Interceptor Integration

Add version interceptor to product-service:

```java
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ApiVersionInterceptor apiVersionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**");
    }
}
```

### Option 2: API Gateway Integration

Route versioned requests through API Gateway:

```yaml
# API Gateway routes
spring:
  cloud:
    gateway:
      routes:
        - id: product-v1
          uri: http://product-service:8082
          predicates:
            - Path=/api/v1/solutions/**
          filters:
            - RewritePath=/api/v1/solutions/(?<segment>.*), /api/v2/solutions/${segment}
            - AddRequestHeader=X-Original-Version, v1
```

## Benefits

### For API Consumers
- **Seamless Upgrades**: Existing clients continue working with v1
- **Gradual Migration**: Migrate to v2 at your own pace
- **Clear Communication**: Deprecation warnings and sunset dates
- **No Breaking Changes**: Automatic schema transformation

### For API Providers
- **Continuous Evolution**: Release new features without breaking existing clients
- **Controlled Deprecation**: Manage version lifecycle systematically
- **Audit Trail**: Complete tracking of version changes
- **Reduced Support**: Fewer production issues from breaking changes

## Example Migration Scenario

### Initial State (v1 Only)
```
Product Service v1.0.0 (STABLE)
├─ /api/v1/solutions/configure
│  └─ Fields: catalogProductId, solutionName
└─ 100 active clients
```

### New Version Release (v2 BETA)
```
Product Service v2.0.0 (BETA)
├─ /api/v2/solutions/configure
│  └─ Fields: templateId, name, metadata
└─ 5 beta testers
```

### v2 Stabilization
```
Product Service v2.0.0 (STABLE)
Product Service v1.0.0 (STABLE → DEPRECATED)

Warning headers added to v1 responses:
"Deprecated on: 2025-06-01, Sunset: 2025-12-01"

Client migration: 30/100 migrated to v2
```

### v1 Sunset
```
Product Service v2.0.0 (STABLE)
Product Service v1.0.0 (SUNSET)

v1 is read-only, no new features
Client migration: 95/100 migrated to v2
```

### v1 EOL
```
Product Service v2.0.0 (STABLE)
Product Service v1.0.0 (EOL - Requests rejected)

All clients on v2
```

## Testing

### Test Version Detection
```bash
# URL path version
curl http://localhost:8090/api/v1/versions/product-service/v1

# Header version
curl -H "X-API-Version: v2" http://localhost:8090/api/versions/product-service/v2

# Content negotiation
curl -H "Accept: application/vnd.bank.v2+json" http://localhost:8090/api/...
```

### Test Schema Transformation
```bash
# Transform v1 → v2
curl -X POST http://localhost:8090/api/v1/transformations/request \
  ?serviceId=product-service&fromVersion=v1&toVersion=v2 \
  -H "Content-Type: application/json" \
  -d '{"catalogProductId": "cat-001", "solutionName": "Test"}'

# Expected output:
{
  "templateId": "cat-001",
  "name": "Test",
  "metadata": {}
}
```

### Test Deprecation Warnings
```bash
# Deprecate v1
curl -u admin:admin123 -X POST \
  http://localhost:8090/api/v1/versions/product-service/v1/deprecate \
  ?reason=Replaced%20by%20v2

# Call deprecated endpoint
curl -u admin:admin123 http://localhost:8082/api/v1/solutions

# Check response headers for Warning and Sunset headers
```

## Monitoring

### Health Check
```bash
curl http://localhost:8090/actuator/health
```

### Metrics
```bash
curl http://localhost:8090/actuator/metrics
```

### Version Usage Analytics
```bash
# Get all versions
GET /api/v1/versions/product-service

# Check active versions
GET /api/v1/versions/product-service?status=STABLE

# Get deprecated versions needing migration
GET /api/v1/versions/product-service?status=DEPRECATED
```

## Summary

The Version Service enables **zero-downtime API evolution** for the Product Catalog System. It allows:

✅ **Multiple API versions** to coexist simultaneously
✅ **Automatic schema transformation** between versions
✅ **Controlled deprecation** with clear timelines
✅ **Backward compatibility** for existing clients
✅ **Seamless upgrades** without breaking changes

This architecture supports the requirement for product-service to be **consumed by multiple clients** while allowing the service to **evolve its interfaces continuously**.
