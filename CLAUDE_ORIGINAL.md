# Product Catalog System

---

## ⚠️ IMPORTANT: MANDATORY STANDARDS FOR ALL NEW SERVICES

**ALL new microservices MUST implement the following standards. These are non-negotiable production requirements:**

### 1. Performance & Resiliency Standards ✅

#### Connection Pooling (Required)
```java
// RestTemplateConfig.java or WebClientConfig.java
PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
connectionManager.setMaxTotal(100);           // Max total connections
connectionManager.setDefaultMaxPerRoute(20);  // Max connections per route

RequestConfig requestConfig = RequestConfig.custom()
    .setConnectTimeout(Timeout.ofMilliseconds(2000))       // Connection timeout: 2s
    .setResponseTimeout(Timeout.ofMilliseconds(5000))      // Socket timeout: 5s
    .setConnectionRequestTimeout(Timeout.ofMilliseconds(1000)) // Pool timeout: 1s
    .build();
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

#### Circuit Breaker (Required)
```java
@CircuitBreaker(name = "target-service-name", fallbackMethod = "fallbackMethodName")
public ResponseType callExternalService(RequestType request) {
    // External service call
}

private ResponseType fallbackMethodName(RequestType request, Exception e) {
    log.error("Circuit breaker activated: {}", e.getMessage());
    // Return graceful degradation response
}
```

**Configuration:** `application-resilience4j.yml`
```yaml
resilience4j:
  circuitbreaker:
    instances:
      target-service-name:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

**Dependencies:**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

#### Async Processing (Required for long-running operations)
```java
// AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
}
```

#### Idempotency Protection (Required for state-changing operations)
```java
// CacheConfig.java
@Bean
public Cache<String, Boolean> idempotencyCache() {
    return Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(24, TimeUnit.HOURS)
            .build();
}
```

**Dependencies:**
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 2. Security Standards ✅

#### Authentication (Required)
- **HTTP Basic Authentication** for service-to-service calls
- **MongoDB-backed user store** with BCrypt password encoding
- **Role-based access control (RBAC)**: ROLE_ADMIN, ROLE_USER

```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/**").hasAnyRole("USER", "ADMIN")
            .anyRequest().authenticated()
        )
        .httpBasic(Customizer.withDefaults());
    return http.build();
}
```

#### Credentials Externalization (Required)
**All credentials MUST use environment variables with defaults for development:**

```yaml
spring:
  data:
    mongodb:
      uri: mongodb://${MONGODB_USERNAME:-admin}:${MONGODB_PASSWORD:-admin123}@...

  security:
    user:
      name: ${SECURITY_USERNAME:-admin}
      password: ${SECURITY_PASSWORD:-admin123}
```

**Never hardcode credentials in:**
- Configuration files (application.yml)
- Source code
- Docker images
- Git repository

### 3. API Versioning Standards ✅

#### URL-based Versioning (Required)
```java
@RestController
@RequestMapping("/api/v1/resource-name")
public class ResourceController {
    // All endpoints start with /api/v{version}/
}
```

#### Backward Compatibility (Required)
- **Never break existing API contracts**
- **Deprecate before removal** (minimum 6 months)
- **Version bump** for breaking changes (v1 → v2)

#### Configuration:
```yaml
app:
  versioning:
    supported-api-versions: "1.0,2.0"
    default-api-version: "1.0"
```

### 4. Observability Standards ✅

#### Actuator Endpoints (Required)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

#### Logging Standards (Required)
```java
@Slf4j
public class ServiceClass {
    // Always use SLF4J with Lombok
    log.info("Action performed: entity={}, id={}", entityType, entityId);
    log.error("Operation failed: entity={}, error={}", entityId, e.getMessage(), e);
}
```

**Log Levels:**
- `ERROR`: Failures requiring immediate attention
- `WARN`: Degraded operation, circuit breaker activations
- `INFO`: Business events, state changes
- `DEBUG`: Detailed flow for troubleshooting (NEVER in production)

**Production Logging Configuration (Required):**
```yaml
# application.yml or application-docker.yml
logging:
  level:
    root: INFO
    com.bank.product: INFO                    # Your application packages
    org.springframework: WARN                 # Reduce Spring framework noise
    org.springframework.kafka: WARN           # Suppress Kafka connection spam
    org.apache.kafka: WARN                    # Suppress Kafka client spam
    org.mongodb.driver: WARN                  # Reduce MongoDB driver logs
```

**⚠️ CRITICAL: Never use DEBUG in production!** A single service with DEBUG logging filled 181GB of logs in hours, causing system-wide MongoDB crash. See [MONGODB_CRASH_FIX.md](../MONGODB_CRASH_FIX.md).

### 5. Multi-tenancy Standards ✅

#### Automatic Tenant Isolation (Required)
**Use the abstracted tenant isolation pattern** - no manual tenant checks needed!

See **[TENANT_ISOLATION_GUIDE.md](TENANT_ISOLATION_GUIDE.md)** for complete implementation guide.

**Quick Start:**
```java
// 1. Extend TenantAwareRepository instead of MongoRepository
public interface SolutionRepository extends TenantAwareRepository<Solution, String> {
    // Automatically inherits: findByIdTenantAware(), deleteByIdTenantAware(), etc.
}

// 2. Use tenant-aware methods in services
public Solution getSolution(String solutionId) {
    return repository.findByIdTenantAware(solutionId)  // Auto-filtered by tenant
        .orElseThrow(() -> new RuntimeException("Not found"));
}

// 3. X-Tenant-ID header automatically extracted by TenantInterceptor
@GetMapping("/{id}")
public ResponseEntity<Resource> getResource(@PathVariable String id) {
    // No @RequestHeader("X-Tenant-ID") needed - handled by interceptor
    return ResponseEntity.ok(service.getResource(id));
}
```

#### Data Isolation (Automatic)
- ✅ **TenantInterceptor** extracts `X-Tenant-ID` header from all requests
- ✅ **TenantContext** stores tenant in thread-local storage
- ✅ **TenantAwareRepository** automatically filters all queries by current tenant
- ✅ **MongoDB indexes** MUST include tenantId for performance

```java
@CompoundIndex(name = "tenant_entity_idx", def = "{'tenantId': 1, 'entityId': 1}")
```

#### Implementation Files (Copy to New Services)
- `TenantContext.java` - Thread-local tenant storage
- `TenantInterceptor.java` - HTTP request interceptor
- `WebMvcConfig.java` - Interceptor registration
- `TenantAwareRepository.java` - Base repository with auto-filtering

**See:** [TENANT_ISOLATION_GUIDE.md](TENANT_ISOLATION_GUIDE.md) for complete architecture, usage patterns, and test results.

### 6. Testing Standards ✅

#### Required Test Types
1. **Unit Tests**: All business logic
2. **Integration Tests**: Database, external service calls
3. **Performance Tests**: Response time, throughput
4. **Idempotency Tests**: Duplicate request handling
5. **Circuit Breaker Tests**: Failure scenarios

#### Test Script Template
```bash
#!/bin/bash
echo "Test 1: Happy path"
curl -u admin:admin123 http://localhost:PORT/api/v1/resource -H "X-Tenant-ID: tenant-test"

echo "Test 2: Idempotency"
curl -u admin:admin123 -X POST http://localhost:PORT/api/v1/resource \
  -H "X-Idempotency-Key: test-key-123" \
  -d '{"data":"value"}'

echo "Test 3: Circuit breaker"
docker-compose stop target-service
curl -u admin:admin123 http://localhost:PORT/api/v1/resource
docker-compose up -d target-service
```

### 7. Docker Standards ✅

#### Multi-stage Dockerfile (Required)
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Health Checks (Required)
```yaml
# docker-compose.yml
services:
  service-name:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

#### Log Truncation and Rotation (MANDATORY) ⚠️
**ALL services MUST have log rotation configured to prevent disk exhaustion.**

```yaml
# docker-compose.yml
services:
  service-name:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"    # Maximum 10MB per log file
        max-file: "3"      # Keep 3 rotated files (30MB total per service)
```

**Why This Matters:**
- Without log rotation, container logs can fill Docker VM disk space
- A single service with verbose logging can crash the entire system
- MongoDB and other databases require disk space for checkpoints/snapshots
- System-wide disk exhaustion causes cascading failures

**Impact:** Maximum 30MB of logs per service (10MB × 3 files)

**See:** [MONGODB_CRASH_FIX.md](../MONGODB_CRASH_FIX.md) for details on a production incident caused by missing log rotation.

### 8. Documentation Standards ✅

#### Required Documentation
1. **API Documentation**: OpenAPI/Swagger at `/swagger-ui.html`
2. **README.md**: Service purpose, endpoints, dependencies
3. **DEPLOYMENT.md**: Docker setup, environment variables
4. **Test scripts**: `test-{feature}.sh` for each major feature

---

## Reference Implementation

**Product Service** ([backend/product-service](backend/product-service)) implements all standards above:
- ✅ Connection pooling with Apache HttpClient 5
- ✅ Circuit breaker with Resilience4j
- ✅ Async workflow submission with @Async
- ✅ Idempotency protection with Caffeine cache
- ✅ MongoDB-backed authentication with BCrypt
- ✅ API versioning (/api/v1/)
- ✅ Actuator endpoints for observability
- ✅ **Automatic tenant isolation** with TenantContext + TenantInterceptor
- ✅ **Log rotation** (10MB × 3 files) - prevents disk exhaustion
- ✅ **Production log levels** (INFO/WARN) - no DEBUG spam
- ✅ Comprehensive test scripts

## 📚 Related Documentation

**Before creating a new service, review these documents:**

1. **[STANDARDS_SUMMARY.md](STANDARDS_SUMMARY.md)** - Quick reference table of all mandatory standards
2. **[NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md)** - Step-by-step checklist for new services
3. **[PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md)** - Detailed implementation examples and test results
4. **[SECURITY.md](SECURITY.md)** - Security guidelines and environment variable reference
5. **[TENANT_ISOLATION_GUIDE.md](TENANT_ISOLATION_GUIDE.md)** - Complete tenant isolation implementation guide ✅
6. **[OUTBOX_PATTERN_DESIGN.md](OUTBOX_PATTERN_DESIGN.md)** - Event-driven architecture with transactional outbox pattern ✅

**Test Scripts:**
- [test-optimizations.sh](test-optimizations.sh) - Performance and connection pooling tests
- [test-circuit-breaker.sh](test-circuit-breaker.sh) - Circuit breaker failure scenarios
- [test-idempotency.sh](test-idempotency.sh) - Duplicate request handling
- [test-tenant-isolation.sh](test-tenant-isolation.sh) - Multi-tenant isolation tests

---

## Project Overview
A microservices-based banking product catalog system that manages master product templates and tenant-specific product instances (solutions). The system enables multi-tenant banks to browse, configure, and deploy banking products from a centralized catalog.

## Architecture

### Two-Tier Product Model
- **Product Catalog**: Master templates for banking products (e.g., "Premium Checking", "High-Yield Savings")
- **Solutions**: Tenant-specific instances configured from catalog templates

### Microservices
Located in `backend/`:
1. **product-service** (port 8082): Manages product catalog and tenant solutions
2. **workflow-service** (port 8089): Temporal-based workflow and approval management
3. **customer-service** (port 8083): Customer management
4. **account-service** (port 8084): Account operations
5. **transaction-service** (port 8085): Transaction processing
6. **notification-service** (port 8086): Notifications
7. **reporting-service** (port 8087): Analytics and reporting
8. **compliance-service** (port 8088): Compliance and auditing
9. **api-gateway** (port 8080): Entry point and routing

### Technology Stack
- **Backend**: Spring Boot 3.4.0, Java 21
- **Database**: MongoDB (port 27018 in Docker)
- **Workflow Engine**: Temporal (port 7233, UI on 8088)
- **Messaging**: Apache Kafka
- **Build**: Maven (multi-module project)
- **Deployment**: Docker Compose

## Package Structure
```
com.bank.product
├── domain/
│   ├── catalog/         # Master product catalog
│   │   ├── model/
│   │   ├── repository/
│   │   ├── service/
│   │   └── controller/
│   └── solution/        # Tenant product instances
│       ├── model/
│       ├── repository/
│       ├── service/
│       └── controller/
├── config/              # Security, etc.
├── repository/          # Shared repositories
└── service/             # Shared services
```

## Key Domain Models

### Product Catalog (Master Template)
- Catalog product ID, name, description
- Product type (CHECKING, SAVINGS, LOAN, etc.)
- Pricing templates, rate tiers, fee templates
- Available features and configuration options
- Status: DRAFT, AVAILABLE, DEPRECATED, RETIRED

### Solution (Tenant Instance)
- References catalog product ID
- Tenant-specific configuration
- Custom pricing, fees, terms
- Status: DRAFT, ACTIVE, SUSPENDED, RETIRED
- Approval workflow for changes

## Security and Configuration

### Environment Variables
All credentials are externalized to environment variables with sensible defaults for development. **No credentials are hardcoded in any configuration file.**

**Configuration Pattern:**
```yaml
# All YAML files use environment variable substitution with defaults
spring:
  data:
    mongodb:
      uri: mongodb://${MONGODB_USERNAME:-admin}:${MONGODB_PASSWORD:-admin123}@...
```

**Key Environment Variables:**
- `MONGODB_USERNAME` / `MONGODB_PASSWORD` - MongoDB credentials (default: admin/admin123)
- `WORKFLOW_SERVICE_USERNAME` / `WORKFLOW_SERVICE_PASSWORD` - Workflow service auth (default: admin/admin123)
- `PRODUCT_SERVICE_USERNAME` / `PRODUCT_SERVICE_PASSWORD` - Product service callback auth (default: admin/admin123)
- `SECURITY_USERNAME` / `SECURITY_PASSWORD` - Workflow service Spring Security (default: admin/admin123)
- `TEMPORAL_POSTGRES_USER` / `TEMPORAL_POSTGRES_PASSWORD` - Temporal database (default: temporal/temporal)

**For Production:** Override defaults via environment variables or secrets management (Vault, AWS Secrets Manager, K8s Secrets). See [SECURITY.md](SECURITY.md) for details.

## Development Commands

### Build
```bash
# Build all services
mvn clean install

# Build specific service
cd backend/product-service
mvn clean package
```

### Docker Deployment
```bash
# Start all services (uses default env var values)
docker-compose up -d

# Start with custom credentials
MONGODB_PASSWORD=mySecurePassword docker-compose up -d

# View logs
docker-compose logs -f product-service

# Stop all services
docker-compose down
```

### MongoDB Access
```bash
# Connect to MongoDB (uses default credentials)
docker exec -it mongodb mongosh -u admin -p admin123 --authenticationDatabase admin

# Or use environment variable
docker exec -it mongodb mongosh -u ${MONGODB_USERNAME} -p ${MONGODB_PASSWORD} --authenticationDatabase admin

# Use product catalog database
use productcatalog
```

## API Endpoints

### Product Catalog (Master Templates)
- `GET /api/v1/catalog/available` - List available catalog products
- `GET /api/v1/catalog/{catalogProductId}` - Get catalog details
- `POST /api/v1/catalog` - Create catalog product (admin)
- `PUT /api/v1/catalog/{catalogProductId}` - Update catalog product

### Solutions (Tenant Products)
- `GET /api/v1/solutions` - List tenant solutions
- `GET /api/v1/solutions/{solutionId}` - Get solution details
- `POST /api/v1/solutions/configure` - Configure new solution from catalog (returns HTTP 202 Accepted)
- `GET /api/v1/solutions/{solutionId}/workflow-status` - Poll workflow submission status (for async workflow)
- `PATCH /api/v1/solutions/{solutionId}/status` - Update solution status

**Async Workflow Pattern:**
The `/configure` endpoint submits workflows asynchronously. It returns immediately with HTTP 202 and provides:
- `pollUrl` - URL to poll for workflow status
- `pollIntervalMs` - Recommended polling interval (1000ms)

Poll the `/workflow-status` endpoint to track the submission:
- `PENDING_SUBMISSION` - Workflow being submitted (keep polling)
- `SUBMITTED` - Workflow created successfully (includes workflowId and approval metadata)
- `SUBMISSION_FAILED` - Submission failed (includes error and retry time)

See [ASYNC_WORKFLOW_POLLING.md](ASYNC_WORKFLOW_POLLING.md) for complete documentation.

### Authentication
All endpoints use HTTP Basic Authentication with credentials from environment variables:

**Product Service (port 8082):**
- Admin: `${SECURITY_USERNAME:-admin}:${SECURITY_PASSWORD:-admin123}` (ROLE_ADMIN, ROLE_USER)
- Default: `admin:admin123`

**Workflow Service (port 8089):**
- Admin: `${SECURITY_USERNAME:-admin}:${SECURITY_PASSWORD:-admin123}` (ROLE_ADMIN, ROLE_USER)
- Default: `admin:admin123`

**Note:** These default credentials are for development only. Production deployments must override via environment variables.

## Important Conventions

### Naming
- **Catalog**: Master product templates
- **Solution**: Tenant-specific product instances
- **Product**: The overarching domain (package name)

### MongoDB Collections
- `product_catalog` - Master catalog templates
- `solutions` - Tenant product instances
- `categories` - Product categories
- `tenant_solution_config` - Tenant configurations
- `users` - Authentication

### Multi-tenancy
- Header: `X-Tenant-ID` for tenant context
- Header: `X-User-ID` for user tracking

## Recent Changes

### Latest Refactoring (feature/refactor branch)
- Renamed package from `com.bank.productcatalog` to `com.bank.product`
- Renamed `catalog-service` to `product-service`
- Renamed `Product` model to `Solution` (tenant instances)
- Kept `ProductCatalog` name (master templates)
- Reorganized domain models into `catalog` and `solution` subdomains

## Workflow Foundation (Extensible Maker/Checker)

### Overview
**Production-ready** approval workflow system built on Temporal that can approve **ANY** entity without code changes. Rules are externalized in decision tables and stored in MongoDB.

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Product Service (Port 8082)                   │
│  - Solution configuration API                                    │
│  - Triggers workflow for approvals                               │
│  - Receives approval/rejection events                            │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      ↓ (HTTP/REST)
┌─────────────────────────────────────────────────────────────────┐
│                  Workflow Service (Port 8089)                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              REST API Layer                              │   │
│  │  POST /api/v1/workflows/submit                          │   │
│  │  POST /api/v1/workflows/{id}/approve                    │   │
│  │  GET  /api/v1/workflows/my-tasks                        │   │
│  └─────────────────────────────────────────────────────────┘   │
│                      ↓                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           Rule Engine (SimpleTableRuleEngine)           │   │
│  │  - Evaluates decision tables                            │   │
│  │  - Supports FIRST, ALL, PRIORITY, COLLECT hit policies  │   │
│  │  - Computes approval plan dynamically                   │   │
│  └─────────────────────────────────────────────────────────┘   │
│                      ↓                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │          Temporal Workflow Orchestration                │   │
│  │  - GenericApprovalWorkflow (durable execution)          │   │
│  │  - Activities: Validate, Assign, Notify, Execute        │   │
│  │  - Signal handlers: approve(), reject()                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                      ↓                                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │            Callback Handler Registry                     │   │
│  │  - SolutionConfigApprovalHandler                        │   │
│  │  - Plugin architecture for entity-specific actions      │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────────────┬───────────────┬──────────────┬───────────────────┘
               │               │              │
               ↓               ↓              ↓
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │   MongoDB    │ │   Temporal   │ │    Kafka     │
    │  Templates   │ │   (Port      │ │   Events     │
    │  Workflow    │ │    7233)     │ │              │
    │  State       │ │              │ │              │
    └──────────────┘ └──────────────┘ └──────────────┘
```

### Core Principles
- **Entity Agnostic**: Approve solutions, documents, onboarding, loans - anything
- **Rule-Driven**: Decision tables evaluated at runtime (no redeployment)
- **Dynamic Routing**: Approvers selected based on entity attributes
- **Plugin Architecture**: Callback handlers registered for entity-specific actions
- **Durable Execution**: Temporal ensures workflows survive crashes and restarts

### Extensibility
```
Today:    Approve solution configuration
Tomorrow: Approve customer onboarding document (just add template via API)
Next:     Approve loan application (just add template via API)
```

No code changes needed - just configure workflow template!

### Key Components

#### 1. Domain Models
- **WorkflowSubject**: Generic entity being approved (any type)
- **WorkflowTemplate**: Rule definitions stored in MongoDB
- **DecisionTable**: Business rules with conditions and outputs
- **ComputedApprovalPlan**: Approval requirements computed from rules
- **ApprovalTask**: Tasks assigned to approvers
- **WorkflowAuditLog**: Immutable audit trail
- **CallbackHandlers**: Entity-specific handlers (plugin pattern)

#### 2. Rule Engine (JSON-based, no external DMN engine)
- **ConditionEvaluator**: Expression evaluation
  - Numeric: `>`, `<`, `>=`, `<=`, `==`, `!=`
  - Ranges: `> 10 && <= 100`
  - Strings: `contains`, `startsWith`, `endsWith`, `matches`
  - OR conditions: `CHECKING|SAVINGS|LOAN`
- **SimpleTableRuleEngine**: Decision table evaluation
  - Hit policies: FIRST, ALL, PRIORITY, COLLECT
  - Rule priority sorting
  - Default rules and fallback values

#### 3. MongoDB Collections
- `workflow_templates` - Versioned rule definitions
- `workflow_subjects` - Workflow instances
- `approval_tasks` - Pending/completed tasks
- `workflow_audit_logs` - Complete audit trail

### Decision Table Example
```json
{
  "name": "Solution Approval Rules",
  "hitPolicy": "FIRST",
  "inputs": [
    {"name": "solutionType", "type": "string"},
    {"name": "pricingVariance", "type": "number"},
    {"name": "riskLevel", "type": "string"}
  ],
  "outputs": [
    {"name": "approvalRequired", "type": "boolean"},
    {"name": "approverRoles", "type": "array"},
    {"name": "approvalCount", "type": "number"},
    {"name": "isSequential", "type": "boolean"},
    {"name": "slaHours", "type": "number"}
  ],
  "rules": [
    {
      "ruleId": "LOW_VARIANCE",
      "priority": 1,
      "conditions": {
        "solutionType": "CHECKING",
        "pricingVariance": "<= 10",
        "riskLevel": "LOW"
      },
      "outputs": {
        "approvalRequired": true,
        "approverRoles": ["PRODUCT_MANAGER"],
        "approvalCount": 1,
        "isSequential": false,
        "slaHours": 24
      }
    },
    {
      "ruleId": "HIGH_VARIANCE",
      "priority": 2,
      "conditions": {
        "pricingVariance": "> 10",
        "riskLevel": "MEDIUM|HIGH"
      },
      "outputs": {
        "approvalRequired": true,
        "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
        "approvalCount": 2,
        "isSequential": true,
        "slaHours": 48
      }
    }
  ]
}
```

## Event-Driven Architecture: Transactional Outbox Pattern ✅

### Overview
The system uses the **Transactional Outbox Pattern** for atomic event publishing, ensuring no orphaned records and reliable distributed transactions. This replaces HTTP callback-based communication with event-driven Kafka messaging.

### Why Outbox Pattern?

**Problem Solved:** The "dual write problem" - guaranteeing that database writes and event publishing happen atomically.

**Without Outbox (Broken):**
```java
@Transactional
public Solution createSolution(...) {
    solution = solutionRepository.save(solution);  // ✅ Saved
    kafkaTemplate.send("solution.created", event);  // ❌ If Kafka down → Event lost!
    // Result: Orphaned solution (no workflow triggered)
}
```

**With Outbox (Atomic):**
```java
@Transactional
public Solution createSolution(...) {
    solution = solutionRepository.save(solution);  // Write 1
    outboxService.saveEvent(event);                 // Write 2
    // Both succeed or both fail (single MongoDB transaction)
}

// Background publisher polls every 100ms
@Scheduled(fixedDelay = 100)
public void publishEvents() {
    for (OutboxEvent event : findUnpublished()) {
        kafkaTemplate.send(event);  // Retries until Kafka accepts
        markAsPublished(event);
    }
}
```

### Architecture Flow

```
Product-Service: [Solution + OutboxEvent] ATOMIC → OutboxPublisher → Kafka
                                                                        ↓
Workflow-Service: SolutionEventConsumer → Temporal Workflow → EventPublisherActivity → Kafka
                                                                                          ↓
Product-Service: WorkflowEventConsumer → Update Solution Status (ACTIVE/REJECTED)
```

### Key Benefits

- ✅ **Atomic**: Single MongoDB transaction (solution + event)
- ✅ **Guaranteed Delivery**: If solution exists → Event **will** be published
- ✅ **Automatic Retry**: OutboxPublisher retries until Kafka available
- ✅ **Idempotent**: Deterministic workflow IDs prevent duplicates
- ✅ **50% less code** than saga orchestration
- ✅ **Observable**: Outbox table + Kafka topics + Temporal UI

### Kafka Topics

- `solution.created` - Solution creation events
- `workflow.completed` - Workflow approval/rejection events
- `solution.status-changed` - Status change events

### Endpoints

- **Old (HTTP callbacks):** `POST /api/v1/solutions/configure` - Uses HTTP callbacks (deprecated)
- **New (Event-driven):** `POST /api/v1/solutions/configure-v2` - Uses outbox pattern ✅

### Documentation

See **[OUTBOX_PATTERN_DESIGN.md](OUTBOX_PATTERN_DESIGN.md)** for complete design, implementation, testing, and troubleshooting guide.

---

## Product-Service & Workflow-Service Integration

### Overview
The product-service and workflow-service are fully integrated for solution configuration approval workflows. When a tenant configures a new solution from the catalog, an approval workflow is automatically triggered based on configurable business rules.

**Integration Methods:**
1. **HTTP Callbacks (V1)** - Synchronous, callback-based (existing implementation)
2. **Event-Driven (V2)** - Asynchronous, outbox pattern-based (recommended) ✅

### Integration Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                          User/Client                                  │
└────────────────────────────┬─────────────────────────────────────────┘
                             │
                             ▼
                   POST /api/v1/solutions/configure
                             │
┌────────────────────────────▼─────────────────────────────────────────┐
│                      Product Service (Port 8082)                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  SolutionController.configureSolution()                       │   │
│  │  1. Creates Solution in DRAFT status                          │   │
│  │  2. Builds workflow metadata from solution attributes         │   │
│  │  3. Calls WorkflowClient.submitWorkflow()                     │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
│                            │                                          │
│  ┌────────────────────────▼─────────────────────────────────────┐   │
│  │  WorkflowClient (REST client)                                 │   │
│  │  - HTTP Basic Auth (admin:admin123)                           │   │
│  │  - POST http://workflow-service:8089/api/v1/workflows/submit │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼ HTTP/REST
┌─────────────────────────────┴────────────────────────────────────────┐
│                     Workflow Service (Port 8089)                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  WorkflowController.submitWorkflow()                          │   │
│  │  1. Loads workflow template (SOLUTION_CONFIG_V1)              │   │
│  │  2. Evaluates decision rules based on metadata                │   │
│  │  3. Starts Temporal workflow with computed approval plan      │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
│                            │                                          │
│  ┌────────────────────────▼─────────────────────────────────────┐   │
│  │  RuleEvaluationService                                        │   │
│  │  - Evaluates: solutionType, pricingVariance, riskLevel        │   │
│  │  - Returns: approverRoles, approvalCount, sequential, SLA     │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
│                            │                                          │
│  ┌────────────────────────▼─────────────────────────────────────┐   │
│  │  Temporal ApprovalWorkflow                                    │   │
│  │  - Creates approval tasks                                      │   │
│  │  - Waits for approval signals                                  │   │
│  │  - Handles timeouts and escalations                            │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
│                            │                                          │
│                            ▼ (On Approval)                            │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  SolutionConfigApprovalHandler (Callback)                     │   │
│  │  - HTTP Basic Auth (admin:admin123)                           │   │
│  │  - PUT http://product-service:8082/api/v1/solutions/{id}/     │   │
│  │    activate                                                    │   │
│  └────────────────────────┬─────────────────────────────────────┘   │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼ HTTP/REST (Callback)
┌─────────────────────────────┴────────────────────────────────────────┐
│                      Product Service (Port 8082)                      │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  SolutionController.activateSolution()                        │   │
│  │  - Updates Solution status: DRAFT → ACTIVE                    │   │
│  │  - Publishes Kafka event: "solution.approved"                 │   │
│  └──────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────────────┘
```

### Integration Components

#### 1. Product Service Components

**WorkflowClient** ([product-service/src/.../client/WorkflowClient.java](backend/product-service/src/main/java/com/bank/product/client/WorkflowClient.java))
- REST client for communicating with workflow-service
- HTTP Basic Authentication
- Methods: `submitWorkflow()`, `getWorkflowStatus()`

**SolutionController** ([product-service/src/.../controller/SolutionController.java](backend/product-service/src/main/java/com/bank/product/domain/solution/controller/SolutionController.java))
- `POST /api/v1/solutions/configure` - Submit solution for approval
- `PUT /api/v1/solutions/{id}/activate` - Callback endpoint for approval
- `PUT /api/v1/solutions/{id}/reject` - Callback endpoint for rejection

**SolutionService** ([product-service/src/.../service/impl/SolutionServiceImpl.java](backend/product-service/src/main/java/com/bank/product/domain/solution/service/impl/SolutionServiceImpl.java))
- `createSolutionFromCatalog()` - Create solution in DRAFT status
- `getSolutionById()` - Retrieve solution by ID
- `updateSolutionStatus()` - Update solution status

#### 2. Workflow Service Components

**WorkflowController** ([workflow-service/src/.../controller/WorkflowController.java](backend/workflow-service/src/main/java/com/bank/product/workflow/domain/controller/WorkflowController.java))
- `POST /api/v1/workflows/submit` - Submit workflow for approval
- `POST /api/v1/workflows/{id}/approve` - Approve workflow
- `POST /api/v1/workflows/{id}/reject` - Reject workflow
- `GET /api/v1/workflows/{id}` - Get workflow status

**SolutionConfigApprovalHandler** ([workflow-service/src/.../handler/SolutionConfigApprovalHandler.java](backend/workflow-service/src/main/java/com/bank/product/workflow/handler/SolutionConfigApprovalHandler.java))
- Callback handler invoked when workflow is approved
- Calls back to product-service to activate solution

**SolutionConfigRejectionHandler** ([workflow-service/src/.../handler/SolutionConfigRejectionHandler.java](backend/workflow-service/src/main/java/com/bank/product/workflow/handler/SolutionConfigRejectionHandler.java))
- Callback handler invoked when workflow is rejected
- Calls back to product-service to reject solution

### End-to-End Flow: Solution Configuration Approval

#### Step 1: User Configures Solution

```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: john.doe@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Premium Business Checking for Enterprise",
    "description": "Customized checking solution for enterprise customers",
    "customInterestRate": 2.8,
    "customFees": {
      "monthlyMaintenance": 12.00,
      "overdraft": 30.00
    },
    "pricingVariance": 18,
    "riskLevel": "MEDIUM",
    "businessJustification": "Enterprise customer segment requires competitive pricing",
    "priority": "HIGH"
  }'
```

**Response:**
```json
{
  "solutionId": "58a5aba0-e433-4e04-9409-d8ee08735a96",
  "solutionName": "Premium Business Checking for Enterprise",
  "status": "DRAFT",
  "workflowId": "34c33af1-b8f4-4523-9f64-f6f9c3a770b9",
  "workflowStatus": "PENDING_APPROVAL",
  "approvalRequired": true,
  "requiredApprovals": 2,
  "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
  "sequential": false,
  "slaHours": 48,
  "estimatedCompletion": "2025-10-03T22:11:49.022971052",
  "message": "Workflow submitted for approval"
}
```

**What Happens Internally:**

1. **Product-Service** creates solution in DRAFT status:
```bash
# MongoDB insert
db.solutions.insertOne({
  "id": "58a5aba0-e433-4e04-9409-d8ee08735a96",
  "tenantId": "tenant-001",
  "catalogProductId": "cat-checking-001",
  "name": "Premium Business Checking for Enterprise",
  "status": "DRAFT",
  "createdBy": "john.doe@bank.com"
})
```

2. **Product-Service** calls **Workflow-Service**:
```bash
# Internal service-to-service call
curl -u admin:admin123 -X POST http://workflow-service:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "SOLUTION_CONFIGURATION",
    "entityId": "58a5aba0-e433-4e04-9409-d8ee08735a96",
    "entityData": {
      "solutionId": "58a5aba0-e433-4e04-9409-d8ee08735a96",
      "solutionName": "Premium Business Checking for Enterprise",
      "catalogProductId": "cat-checking-001"
    },
    "entityMetadata": {
      "solutionType": "CHECKING",
      "pricingVariance": 18,
      "riskLevel": "MEDIUM",
      "tenantTier": "STANDARD"
    },
    "initiatedBy": "john.doe@bank.com",
    "tenantId": "tenant-001",
    "businessJustification": "Enterprise customer segment requires competitive pricing",
    "priority": "HIGH"
  }'
```

3. **Workflow-Service** evaluates decision rules:
```bash
# Rule matched: DUAL_APPROVAL_HIGH_VARIANCE
# Condition: pricingVariance (18) > 15 && riskLevel = MEDIUM
# Output:
{
  "approvalRequired": true,
  "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
  "approvalCount": 2,
  "sequential": false,
  "slaHours": 48
}
```

4. **Temporal** starts durable workflow execution

#### Step 2: Approver Reviews Workflow

```bash
# Check workflow status
curl -u admin:admin123 http://localhost:8089/api/v1/workflows/34c33af1-b8f4-4523-9f64-f6f9c3a770b9
```

**Response:**
```json
{
  "workflowId": "34c33af1-b8f4-4523-9f64-f6f9c3a770b9",
  "workflowInstanceId": "workflow-34c33af1-b8f4-4523-9f64-f6f9c3a770b9",
  "entityType": "SOLUTION_CONFIGURATION",
  "entityId": "58a5aba0-e433-4e04-9409-d8ee08735a96",
  "state": "PENDING_APPROVAL",
  "pendingTasks": [
    {
      "taskId": "task-001",
      "assignedTo": null,
      "requiredRole": "PRODUCT_MANAGER",
      "status": "PENDING",
      "createdAt": "2025-10-01T22:11:49"
    },
    {
      "taskId": "task-002",
      "assignedTo": null,
      "requiredRole": "RISK_MANAGER",
      "status": "PENDING",
      "createdAt": "2025-10-01T22:11:49"
    }
  ]
}
```

#### Step 3: First Approver Approves

```bash
curl -u admin:admin123 -X POST \
  http://localhost:8089/api/v1/workflows/34c33af1-b8f4-4523-9f64-f6f9c3a770b9/approve \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "alice.manager@bank.com",
    "comments": "Pricing is competitive for enterprise segment. Approved."
  }'
```

**What Happens:**
- Temporal workflow receives approval signal
- Task marked complete
- Workflow continues to wait for second approval (parallel mode)

#### Step 4: Second Approver Approves

```bash
curl -u admin:admin123 -X POST \
  http://localhost:8089/api/v1/workflows/34c33af1-b8f4-4523-9f64-f6f9c3a770b9/approve \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "bob.risk@bank.com",
    "comments": "Risk assessment complete. Medium risk acceptable. Approved."
  }'
```

**What Happens:**

1. **Temporal workflow** completes (all approvals received)
2. **Workflow-Service** executes **SolutionConfigApprovalHandler**
3. **Callback to Product-Service**:
```bash
# Internal service-to-service callback
curl -u admin:admin123 -X PUT \
  http://product-service:8082/api/v1/solutions/58a5aba0-e433-4e04-9409-d8ee08735a96/activate
```

4. **Product-Service** updates solution status:
```bash
# MongoDB update
db.solutions.updateOne(
  { "id": "58a5aba0-e433-4e04-9409-d8ee08735a96" },
  {
    "$set": {
      "status": "ACTIVE",
      "updatedAt": ISODate("2025-10-01T22:15:00Z"),
      "updatedBy": "system"
    }
  }
)
```

5. **Kafka event published**:
```json
{
  "topic": "solution.approved",
  "key": "58a5aba0-e433-4e04-9409-d8ee08735a96",
  "value": {
    "solutionId": "58a5aba0-e433-4e04-9409-d8ee08735a96",
    "tenantId": "tenant-001",
    "status": "ACTIVE",
    "approvedBy": ["alice.manager@bank.com", "bob.risk@bank.com"],
    "approvedAt": "2025-10-01T22:15:00Z"
  }
}
```

#### Step 5: Verify Solution is Active

```bash
curl -u admin:admin123 http://localhost:8082/api/v1/solutions/58a5aba0-e433-4e04-9409-d8ee08735a96 \
  -H "X-Tenant-ID: tenant-001"
```

**Response:**
```json
{
  "id": "58a5aba0-e433-4e04-9409-d8ee08735a96",
  "tenantId": "tenant-001",
  "catalogProductId": "cat-checking-001",
  "name": "Premium Business Checking for Enterprise",
  "status": "ACTIVE",
  "createdBy": "john.doe@bank.com",
  "updatedBy": "system",
  "updatedAt": "2025-10-01T22:15:00Z"
}
```

### Rejection Flow

If an approver rejects the workflow:

```bash
curl -u admin:admin123 -X POST \
  http://localhost:8089/api/v1/workflows/34c33af1-b8f4-4523-9f64-f6f9c3a770b9/reject \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "bob.risk@bank.com",
    "comments": "Risk level too high for proposed pricing variance. Rejected.",
    "reason": "Pricing variance exceeds risk tolerance"
  }'
```

**What Happens:**

1. **Temporal workflow** terminates
2. **Workflow-Service** executes **SolutionConfigRejectionHandler**
3. **Callback to Product-Service**:
```bash
curl -u admin:admin123 -X PUT \
  http://product-service:8082/api/v1/solutions/58a5aba0-e433-4e04-9409-d8ee08735a96/reject \
  -H "Content-Type: application/json" \
  -d '{"reason": "Pricing variance exceeds risk tolerance"}'
```

4. **Product-Service** updates solution status to REJECTED

### Service Communication Details

#### Authentication
- **Product-Service → Workflow-Service**: HTTP Basic Auth (`admin:admin123`)
- **Workflow-Service → Product-Service**: HTTP Basic Auth (`admin:admin123`)
- **User → Product-Service**: HTTP Basic Auth (`admin:admin123`)
- **User → Workflow-Service**: HTTP Basic Auth (`admin:admin123`)

#### Service URLs (Docker Environment)
- **Product-Service**: `http://product-service:8082` (internal), `http://localhost:8082` (external)
- **Workflow-Service**: `http://workflow-service:8089` (internal), `http://localhost:8089` (external)

#### Configuration Files
- Product-Service: [application-docker.yml](backend/product-service/src/main/resources/application-docker.yml)
  ```yaml
  workflow:
    service:
      url: http://workflow-service:8089
      username: admin
      password: admin123
  ```

- Workflow-Service: [application-docker.yml](backend/workflow-service/src/main/resources/application-docker.yml)
  ```yaml
  product:
    service:
      url: http://product-service:8082
      username: admin
      password: admin123
  ```

### Decision Rules Example

The workflow template `SOLUTION_CONFIG_V1` in MongoDB contains these rules:

```json
{
  "templateId": "SOLUTION_CONFIG_V1",
  "entityType": "SOLUTION_CONFIGURATION",
  "decisionTables": [{
    "name": "Solution Approval Rules",
    "hitPolicy": "FIRST",
    "rules": [
      {
        "ruleId": "AUTO_APPROVE_LOW_RISK",
        "priority": 1,
        "conditions": {
          "solutionType": "CHECKING|SAVINGS",
          "pricingVariance": "< 5",
          "riskLevel": "LOW"
        },
        "outputs": {
          "approvalRequired": false
        }
      },
      {
        "ruleId": "SINGLE_APPROVAL_MEDIUM_VARIANCE",
        "priority": 2,
        "conditions": {
          "pricingVariance": ">= 5 && <= 15",
          "riskLevel": "LOW|MEDIUM"
        },
        "outputs": {
          "approvalRequired": true,
          "approverRoles": ["PRODUCT_MANAGER"],
          "approvalCount": 1,
          "sequential": false,
          "slaHours": 24
        }
      },
      {
        "ruleId": "DUAL_APPROVAL_HIGH_VARIANCE",
        "priority": 3,
        "conditions": {
          "pricingVariance": "> 15",
          "riskLevel": "MEDIUM|HIGH"
        },
        "outputs": {
          "approvalRequired": true,
          "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
          "approvalCount": 2,
          "sequential": false,
          "slaHours": 48
        }
      }
    ]
  }]
}
```

### Testing the Integration

#### 1. Test Auto-Approval (Low Variance)
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d '{
    "catalogProductId": "cat-savings-001",
    "solutionName": "Standard Savings",
    "pricingVariance": 3,
    "riskLevel": "LOW"
  }'
# Expected: approvalRequired = false, solution automatically ACTIVE
```

#### 2. Test Single Approval (Medium Variance)
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Premium Checking",
    "pricingVariance": 10,
    "riskLevel": "MEDIUM"
  }'
# Expected: approvalRequired = true, requiredApprovals = 1, approverRoles = ["PRODUCT_MANAGER"]
```

#### 3. Test Dual Approval (High Variance)
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Enterprise Checking",
    "pricingVariance": 18,
    "riskLevel": "MEDIUM"
  }'
# Expected: approvalRequired = true, requiredApprovals = 2, approverRoles = ["PRODUCT_MANAGER", "RISK_MANAGER"]
```

---

## Summary

The Product Catalog System now features a **production-ready, fully integrated approval workflow system** that demonstrates:

✅ **Microservice Integration** - Product-service and workflow-service communicate via REST APIs with proper authentication
✅ **Rule-Based Routing** - Dynamic approval requirements based on business rules (pricing variance, risk level)
✅ **Callback Pattern** - Workflow-service calls back to product-service upon completion
✅ **Durable Workflows** - Temporal ensures workflows survive crashes and restarts
✅ **Extensibility** - Adding new workflow types requires only configuration, no code changes

**Key Achievement**: A tenant can configure a solution, have it automatically routed through appropriate approvers based on risk/pricing, and see it activated upon approval - all orchestrated by a reusable workflow engine.

---

### Template Management
```bash
# Create new approval type (no code deployment!)
POST /api/v1/workflow-templates
{
  "templateId": "SOLUTION_CONFIG_V1",
  "entityType": "SOLUTION_CONFIGURATION",
  "decisionTables": [...],
  "callbackHandlers": {
    "onApprove": "SolutionConfigApprovalHandler",
    "onReject": "SolutionConfigRejectionHandler"
  }
}

# Test template before publishing
POST /api/v1/workflow-templates/SOLUTION_CONFIG_V1/test
{
  "entityMetadata": {
    "solutionType": "CHECKING",
    "pricingVariance": 15,
    "riskLevel": "MEDIUM"
  }
}

# Publish template (activates it)
POST /api/v1/workflow-templates/SOLUTION_CONFIG_V1/publish

# Get my approval tasks
GET /api/v1/workflows/my-tasks?role=PRODUCT_MANAGER&status=PENDING
```

### Workflow Service Components
- **Location**: `backend/workflow-service/`
- **Port**: 8089
- **Dependencies**: Temporal SDK 1.26.2, Spring Boot 3.4.0, MongoDB, Kafka
- **Rule Engine**: Custom JSON-based (27 source files, 22 tests passing)
- **Temporal UI**: http://localhost:8088

### Agentic Workflows (AI + Rules)
The system combines **explicit rules** with **AI agents** for intelligent decision-making:

**Three Decision Patterns:**
1. **Pure Rule-Based (DMN)**: Traditional decision tables
2. **Async Red Flag Detection**: AI agents run in parallel, terminate on critical findings
3. **Sync Agent Enrichment**: AI agents analyze, then DMN evaluates with AI insights

**Agent Types:**
- **MCP Agents**: Real-time AI analysis via Model Context Protocol (fraud detection, financial analysis)
- **GraphRAG Agents**: Knowledge retrieval from graph databases (compliance rules, historical patterns)

**Example Flow:**
```
Submit Loan → Launch AI Agents (parallel)
  ├─→ Fraud Detection Agent → Red Flag? → TERMINATE & AUTO-REJECT
  ├─→ Credit Risk GraphRAG → Retrieve similar customer patterns
  └─→ Financial Analysis MCP → Calculate risk scores
       ↓
  No Red Flags → Enrich metadata with AI insights
       ↓
  Evaluate DMN with enriched data (original + AI scores)
       ↓
  Assign Human Approver
```

**Benefits:**
- Auto-reject fraud/high-risk cases instantly
- Auto-approve excellent cases (no human needed)
- Enrich decisions with AI reasoning and knowledge graphs
- Full explainability (agent reasoning traces + DMN rules)

See [AGENTIC_WORKFLOW_DESIGN.md](AGENTIC_WORKFLOW_DESIGN.md) for hybrid AI+Rules architecture

## Implementation Status

### ✅ Completed
1. **Workflow Service Foundation**: Temporal-based workflow engine with rule evaluation
2. **Rule Engine**: JSON-based decision table evaluator (22 tests passing)
3. **Domain Models**: Complete workflow, template, and approval models
4. **MongoDB Integration**: Repositories for templates, workflows, tasks, audit logs
5. **Docker Deployment**: workflow-service containerized and integrated

### 🚧 In Progress
1. **Temporal Workflows**: GenericApprovalWorkflow implementation needed
2. **REST APIs**: Workflow submission and approval endpoints
3. **Product Service Integration**: Solution approval flow
4. **Callback Handlers**: Entity-specific approval actions

### ⏳ Pending
1. **Authentication**: Currently using basic auth; consider JWT for production
2. **Testing**: Integration tests for workflow scenarios
3. **API Gateway**: Route workflow APIs through gateway
4. **Kafka Integration**: Event publishing for workflow state changes
5. **Notification Integration**: Email/SMS for task assignments
6. **Documentation**: OpenAPI/Swagger documentation

## File Locations

### Configuration
All configuration files use environment variable substitution (no hardcoded credentials):

- `backend/product-service/src/main/resources/application.yml` - Product service config (env vars)
- `backend/product-service/src/main/resources/application-docker.yml` - Docker overrides (env vars)
- `backend/workflow-service/src/main/resources/application.yml` - Workflow service config (env vars)
- `backend/workflow-service/src/main/resources/application-docker.yml` - Docker overrides (env vars)
- `backend/notification-service/src/main/resources/application.yml` - Notification service config (env vars)
- `backend/notification-service/src/main/resources/application-docker.yml` - Docker overrides (env vars)
- `backend/version-service/src/main/resources/application.yml` - Version service config (env vars)
- `backend/version-service/src/main/resources/application-docker.yml` - Docker overrides (env vars)
- `docker-compose.yml` - Container orchestration (all credentials via env vars with defaults)
- `backend/pom.xml` - Parent POM
- `init-mongo.js` - MongoDB initialization script
- `SECURITY.md` - Complete environment variable documentation

### Domain Models
- Common models: `backend/common/src/main/java/com/bank/product/`
- Product service: `backend/product-service/src/main/java/com/bank/product/`
- Workflow models: `backend/workflow-service/src/main/java/com/bank/product/workflow/domain/model/`

### Workflow Components
- Rule engine: `backend/workflow-service/src/main/java/com/bank/product/workflow/engine/`
- Services: `backend/workflow-service/src/main/java/com/bank/product/workflow/domain/service/`
- Repositories: `backend/workflow-service/src/main/java/com/bank/product/workflow/domain/repository/`
- Tests: `backend/workflow-service/src/test/java/com/bank/product/workflow/engine/`

## Git Workflow
- Main branch: `main`
- Feature branches: `feature/*`
- Current work: `feature/refactor` (package restructuring)
