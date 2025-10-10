# Multi-Channel API Gateway

Enterprise-grade API Gateway for the Product Catalog System, supporting multiple channels with channel-specific authentication, rate limiting, and transformation capabilities.

---

## 🌟 Overview

The API Gateway provides a unified entry point for all external systems and channels, enforcing:
- **Multi-tenancy isolation**
- **Party-aware routing**
- **RBAC authorization**
- **API versioning**
- **Rate limiting per channel**
- **Circuit breakers for resiliency**
- **Request/response transformation**
- **Comprehensive audit logging**

---

## 📡 Supported Channels

### 1. **Public API** (`PUBLIC_API`)
- **Protocol**: REST (JSON/XML over HTTPS)
- **Authentication**: JWT Bearer tokens
- **Rate Limit**: 1,000 requests/min per tenant
- **Use Cases**: Mobile apps, third-party integrations, partner APIs

### 2. **Host-to-Host File Processing** (`HOST_TO_HOST`)
- **Protocol**: File upload (SFTP/S3/HTTP Multipart)
- **Authentication**: Mutual TLS + API Key
- **File Formats**: CSV, Fixed-width, ISO20022 XML
- **Processing**: Asynchronous with callback
- **Rate Limit**: 100 uploads/min
- **Use Cases**: Batch product configurations, bulk payment origination

### 3. **ERP Integration** (`ERP_INTEGRATION`)
- **Protocol**: REST + SOAP fallback
- **Authentication**: OAuth 2.0 Client Credentials
- **Rate Limit**: 5,000 requests/min (batch operations)
- **Use Cases**: Kyriba, SAP Treasury, Oracle TMS integration

### 4. **Client Self-Service Portal** (`CLIENT_PORTAL`)
- **Protocol**: REST (JSON over HTTPS)
- **Authentication**: OAuth 2.0 Authorization Code Flow
- **Session**: Redis-backed
- **Rate Limit**: 500 requests/min per user
- **Use Cases**: Customer web portal, account opening, product browsing

### 5. **Salesforce Operations Workbench** (`SALESFORCE_OPS`)
- **Protocol**: REST (JSON)
- **Authentication**: Salesforce OAuth + Connected App
- **Sync**: Real-time + batch
- **Rate Limit**: 2,000 requests/min
- **Use Cases**: CRM integration, relationship manager tools, workflow management

### 6. **Internal Admin** (`INTERNAL_ADMIN`)
- **Protocol**: REST (JSON)
- **Authentication**: Internal SSO (LDAP/AD)
- **Rate Limit**: Unlimited
- **Use Cases**: Internal operations, system administration

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (Port 8080)                 │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Spring Cloud Gateway (Reactive/WebFlux)              │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │   Channel    │  │Multi-Tenancy │  │ Party-Aware      │  │
│  │Identification│→ │   Filter     │→ │   Routing        │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ Rate Limiter │  │Circuit Breaker│ │ Audit Logging    │  │
│  │  (Redis)     │  │(Resilience4j) │  │   (MongoDB)      │  │
│  └──────────────┘  └──────────────┘  └──────────────────┘  │
└─────────────────────────┬────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
  ┌──────────┐      ┌──────────┐     ┌──────────┐
  │ Product  │      │ Workflow │     │  Party   │
  │ Service  │      │ Service  │     │ Service  │
  │  (8082)  │      │  (8089)  │     │  (8083)  │
  └──────────┘      └──────────┘     └──────────┘
```

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- MongoDB (for audit logs)
- Redis (for rate limiting)
- Kafka (for event publishing)

### Build

```bash
# Build with Maven
cd backend/api-gateway
mvn clean package

# Build Docker image
docker build -t api-gateway:latest .
```

### Run Locally

```bash
# Start dependencies
docker-compose up -d mongodb redis kafka

# Run application
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run with Docker

```bash
docker-compose up -d api-gateway
```

---

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Gateway port | 8080 |
| `MONGODB_URI` | MongoDB connection | mongodb://admin:admin123@localhost:27018/... |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | localhost:9092 |
| `PRODUCT_SERVICE_URL` | Product service URL | http://product-service:8082 |
| `WORKFLOW_SERVICE_URL` | Workflow service URL | http://workflow-service:8089 |
| `PARTY_SERVICE_URL` | Party service URL | http://party-service:8083 |
| `LOG_LEVEL_APP` | Application log level | INFO |
| `CORS_ALLOWED_ORIGINS` | CORS allowed origins | http://localhost:4200 |

### Channel Configuration

Channels can be enabled/disabled in `application.yml`:

```yaml
gateway:
  channels:
    public-api:
      enabled: true
      rate-limit: 1000
    host-to-host:
      enabled: true
      rate-limit: 100
    # ... other channels
```

---

## 📖 API Examples

### 1. Public API - Configure Product

```bash
curl -X POST http://localhost:8080/api/v1/solutions/configure \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: user@example.com" \
  -H "X-Channel: PUBLIC_API" \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-savings-001",
    "solutionName": "Premium Savings Account",
    "pricingVariance": 10
  }'
```

### 2. Host-to-Host - Upload File

```bash
curl -X POST http://localhost:8080/channel/host-to-host/files/upload \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: system@example.com" \
  -H "X-File-Format: CSV" \
  -H "X-Callback-URL: https://your-system.com/callback" \
  -F "file=@products.csv"
```

Response:
```json
{
  "fileId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "PROCESSING",
  "message": "File uploaded successfully and processing started",
  "callbackConfigured": true
}
```

### 3. Host-to-Host - Check File Status

```bash
curl http://localhost:8080/channel/host-to-host/files/{fileId}/status \
  -H "X-Tenant-ID: tenant-001"
```

Response:
```json
{
  "fileId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "status": "PROCESSING",
  "recordsTotal": 100,
  "recordsProcessed": 75,
  "recordsSucceeded": 70,
  "recordsFailed": 5,
  "progressPercent": 75
}
```

### 4. ERP Integration - Configure Product (Batch)

```bash
curl -X POST http://localhost:8080/channel/erp/products/configure \
  -H "Authorization: Bearer YOUR_OAUTH_TOKEN" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: erp-system" \
  -H "Content-Type: application/json" \
  -d '{
    "products": [...]
  }'
```

### 5. Client Portal - Browse Products

```bash
curl http://localhost:8080/channel/portal/products/available \
  -H "Authorization: Bearer YOUR_USER_TOKEN" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: customer@example.com" \
  -H "X-Party-ID: party-12345"
```

### 6. Salesforce - Get Workflows

```bash
curl http://localhost:8080/channel/salesforce/workflows \
  -H "Authorization: Bearer YOUR_SALESFORCE_TOKEN" \
  -H "X-Tenant-ID: tenant-001"
```

---

## 🔒 Security

### Authentication Methods

#### 1. JWT Bearer (Public API, Client Portal)
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 2. OAuth 2.0 Client Credentials (ERP Integration)
```bash
# Get access token
curl -X POST https://auth-server/oauth/token \
  -d grant_type=client_credentials \
  -d client_id=YOUR_CLIENT_ID \
  -d client_secret=YOUR_CLIENT_SECRET
```

#### 3. Mutual TLS + API Key (Host-to-Host)
- Client certificate required
- API Key in header: `X-API-Key: YOUR_API_KEY`

#### 4. Salesforce OAuth (Salesforce Operations)
- Connected App authentication
- Token obtained from Salesforce

### RBAC (Role-Based Access Control)

| Channel | Required Role | Permissions |
|---------|---------------|-------------|
| Public API | USER | Read/Write own tenant data |
| Host-to-Host | SYSTEM | Bulk operations |
| ERP Integration | ERP_USER, SYSTEM | Batch operations |
| Client Portal | USER, CUSTOMER | Self-service operations |
| Salesforce | SALESFORCE | CRM operations |
| Internal Admin | ADMIN | All operations |

---

## 📊 Monitoring & Observability

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "mongo": {"status": "UP"},
    "ping": {"status": "UP"},
    "redis": {"status": "UP"}
  }
}
```

### Metrics (Prometheus)

```bash
curl http://localhost:8080/actuator/prometheus
```

Key metrics:
- `gateway_requests_total` - Total requests per channel
- `gateway_request_duration_seconds` - Request latency
- `gateway_circuit_breaker_state` - Circuit breaker state
- `gateway_rate_limit_hits` - Rate limit violations

### Audit Logs

All requests are logged to MongoDB in the `api_audit_logs` collection:

```javascript
// Query audit logs
db.api_audit_logs.find({
  tenantId: "tenant-001",
  timestamp: {$gte: ISODate("2025-01-01")}
}).sort({timestamp: -1})

// Analyze by channel
db.api_audit_logs.aggregate([
  {$group: {
    _id: "$channel",
    count: {$sum: 1},
    avgDuration: {$avg: "$durationMs"}
  }}
])
```

---

## 🔄 Circuit Breaker

Circuit breakers protect downstream services from cascading failures.

### Configuration

```yaml
resilience4j:
  circuitbreaker:
    instances:
      product-service-cb:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

### States

- **CLOSED**: Normal operation
- **OPEN**: Circuit breaker triggered, requests fail fast
- **HALF_OPEN**: Testing if service recovered

### Fallback Responses

When circuit breaker is open:
```json
{
  "error": "Service temporarily unavailable",
  "service": "product-service",
  "fallback": true,
  "retryAfter": "10s"
}
```

---

## ⚡ Rate Limiting

Rate limits are enforced per channel and per tenant/user using Redis.

### Limits

| Channel | Limit | Window |
|---------|-------|--------|
| Public API | 1,000 req | 1 minute |
| Host-to-Host | 100 req | 1 minute |
| ERP Integration | 5,000 req | 1 minute |
| Client Portal | 500 req | 1 minute |
| Salesforce Ops | 2,000 req | 1 minute |
| Internal Admin | Unlimited | - |

### Response Headers

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 847
X-RateLimit-Reset: 1672531200
```

### Rate Limit Exceeded

HTTP 429:
```json
{
  "error": "Rate limit exceeded",
  "limit": 1000,
  "window": "1 minute",
  "retryAfter": 42
}
```

---

## 🧪 Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
mvn verify
```

### Load Testing

```bash
# Test Public API channel
./test-gateway-load.sh PUBLIC_API

# Test Host-to-Host channel
./test-gateway-load.sh HOST_TO_HOST
```

---

## 🐳 Docker Deployment

### Docker Compose

```yaml
api-gateway:
  build: ./backend/api-gateway
  ports:
    - "8080:8080"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
    - MONGODB_URI=mongodb://admin:admin123@mongodb:27017/product-catalog
    - REDIS_HOST=redis
    - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
  depends_on:
    - mongodb
    - redis
    - kafka
    - product-service
    - workflow-service
    - party-service
  networks:
    - product-catalog-network
```

---

## 📚 Key Features

### ✅ Multi-Tenancy
- Tenant isolation enforced at gateway level
- Tenant ID required in all requests
- Tenant-specific rate limiting

### ✅ Party-Aware Routing
- Party context extracted from headers or user identity
- Party relationships validated
- Party roles propagated to downstream services

### ✅ API Versioning
- URL-based versioning: `/api/v1/...`, `/api/v2/...`
- Header-based versioning: `X-API-Version: 1.0`
- Backward compatibility maintained

### ✅ File Processing
- Async file upload and processing
- Multiple file formats (CSV, Fixed-width, ISO20022)
- Progress tracking
- Callback on completion

### ✅ Transformation
- Request/response transformation per channel
- Format conversion (JSON ↔ XML)
- Field mapping and validation

### ✅ Audit Trail
- Complete request/response logging
- MongoDB storage for compliance
- Kafka publishing for real-time analytics
- Queryable audit history

---

## 🔗 Integration Points

| Service | Endpoint | Purpose |
|---------|----------|---------|
| Product Service | http://product-service:8082 | Product catalog, solutions |
| Workflow Service | http://workflow-service:8089 | Approval workflows |
| Party Service | http://party-service:8083 | Party management, relationships |
| File Processing Service | http://file-processing-service:8094 | Host-to-host file processing |
| Payment Service | http://payment-service:8095 | Payment origination |
| Application Service | http://application-service:8096 | Customer applications |

---

## 🐛 Troubleshooting

### Issue: Circuit Breaker Always Open

**Cause**: Downstream service is down or slow

**Solution**:
1. Check downstream service health
2. Review circuit breaker metrics
3. Adjust `failureRateThreshold` if needed

### Issue: Rate Limit Errors

**Cause**: Too many requests from tenant/user

**Solution**:
1. Check Redis connectivity
2. Review rate limit configuration
3. Consider increasing limits for specific tenants

### Issue: Missing Tenant ID

**Cause**: `X-Tenant-ID` header not provided

**Solution**:
Ensure all requests include required headers:
- `X-Tenant-ID`
- `X-User-ID`

---

## 📝 Development

### Adding a New Channel

1. Add channel to `Channel.java` enum
2. Configure route in `GatewayRoutes.java`
3. Set rate limit in `application.yml`
4. Add authentication in `SecurityConfig.java`
5. Create channel-specific controller if needed
6. Add tests

### Adding a New Filter

1. Create filter class implementing `GatewayFilter`
2. Add filter to route in `GatewayRoutes.java`
3. Configure filter in `application.yml`
4. Add tests

---

## 📄 License

Proprietary - All rights reserved

---

## 👥 Support

For questions or issues, contact the platform team.
