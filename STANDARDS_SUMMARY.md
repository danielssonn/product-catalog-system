# Microservice Standards - Quick Reference

## 📋 Overview

This document provides a quick reference for the **mandatory standards** that ALL microservices in this system must implement. These standards are non-negotiable for production deployment.

**Full details**: See [CLAUDE.md](CLAUDE.md) - "MANDATORY STANDARDS FOR ALL NEW SERVICES" section (top of file)

**Implementation checklist**: See [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md)

**Reference implementation**: [backend/product-service](backend/product-service/)

---

## 🚀 1. Performance & Resiliency

| Standard | Required? | Purpose | Test Command |
|----------|-----------|---------|--------------|
| **Connection Pooling** | ✅ Yes | Reuse HTTP connections, prevent socket exhaustion | `test-optimizations.sh` |
| **Circuit Breaker** | ✅ Yes | Fail fast when dependencies are down | `test-circuit-breaker.sh` |
| **Async Processing** | ⚠️ If long-running | Non-blocking operations, better throughput | Check logs for `async-` threads |
| **Idempotency** | ✅ Yes | Prevent duplicate processing | `test-idempotency.sh` |

### Key Metrics
- **Response time**: <500ms (target), <200ms (ideal)
- **Throughput**: 1000+ req/sec
- **Circuit breaker**: Fail fast in <5s
- **Connection pool**: 100 total, 20 per route

---

## 🔒 2. Security

| Standard | Required? | Implementation |
|----------|-----------|----------------|
| **HTTP Basic Auth** | ✅ Yes | All endpoints except `/actuator/health` |
| **RBAC** | ✅ Yes | ROLE_ADMIN, ROLE_USER |
| **MongoDB User Store** | ✅ Yes | BCrypt password encoding |
| **No Hardcoded Credentials** | ✅ Yes | Use `${ENV_VAR:-default}` pattern |

### Environment Variables Pattern
```yaml
mongodb:
  uri: mongodb://${MONGODB_USERNAME:-admin}:${MONGODB_PASSWORD:-admin123}@...
security:
  user:
    name: ${SECURITY_USERNAME:-admin}
    password: ${SECURITY_PASSWORD:-admin123}
```

---

## 🌐 3. API Design

| Standard | Required? | Format |
|----------|-----------|--------|
| **Versioning** | ✅ Yes | `/api/v1/resource-name` |
| **Multi-tenancy** | ✅ Yes | `X-Tenant-ID` header |
| **User Tracking** | ✅ Yes | `X-User-ID` header |
| **REST Standards** | ✅ Yes | Proper HTTP methods & status codes |

### Status Codes
- `200` OK - Success
- `201` Created - Resource created
- `202` Accepted - Async processing started
- `400` Bad Request - Validation error
- `404` Not Found - Resource not found
- `500` Internal Server Error - Unexpected error

---

## 📊 4. Observability

| Feature | Required? | Endpoint/Configuration |
|---------|-----------|----------------------|
| **Health Check** | ✅ Yes | `/actuator/health` |
| **Metrics** | ✅ Yes | `/actuator/metrics` |
| **Prometheus** | ✅ Yes | `/actuator/prometheus` |
| **Structured Logging** | ✅ Yes | `@Slf4j` with placeholders |
| **OpenAPI Docs** | ✅ Yes | `/swagger-ui.html` |

### Logging Standards
```java
log.info("Action performed: entity={}, id={}", entityType, entityId);
log.error("Operation failed: entity={}, error={}", entityId, e.getMessage(), e);
```

**Production Log Levels (application.yml):**
```yaml
logging:
  level:
    root: INFO
    com.bank.product: INFO
    org.springframework: WARN
    org.springframework.kafka: WARN
    org.apache.kafka: WARN
    org.mongodb.driver: WARN
```

⚠️ **Never use DEBUG in production** - can fill 100GB+ of logs in hours

---

## 💾 5. Data Layer

| Standard | Required? | Implementation |
|----------|-----------|----------------|
| **MongoDB** | ✅ Yes | Spring Data MongoDB |
| **Tenant Isolation** | ✅ Yes | Filter all queries by `tenantId` |
| **Indexes** | ✅ Yes | Compound indexes with `tenantId` |
| **Audit Fields** | ✅ Yes | createdAt, updatedAt, createdBy, updatedBy |

### Model Template
```java
@Data
@Document(collection = "resource_name")
@CompoundIndex(name = "tenant_resource_idx", def = "{'tenantId': 1, 'resourceId': 1}")
public class Resource {
    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
```

---

## 🧪 6. Testing

| Test Type | Required? | Location | Command |
|-----------|-----------|----------|---------|
| **Unit Tests** | ✅ Yes | `src/test/java` | `mvn test` |
| **Integration Tests** | ✅ Yes | `src/test/java` | `mvn verify` |
| **Performance Tests** | ✅ Yes | `test-performance.sh` | `./test-performance.sh` |
| **Feature Tests** | ✅ Yes | `test-{feature}.sh` | `./test-feature.sh` |

### Test Coverage Target
- **Minimum**: 80%
- **Target**: 90%+

---

## 🐳 7. Docker

| Component | Required? | File |
|-----------|-----------|------|
| **Multi-stage Dockerfile** | ✅ Yes | `Dockerfile` |
| **Health Check** | ✅ Yes | `docker-compose.yml` |
| **Log Rotation** | ✅ Yes | `docker-compose.yml` (10m × 3 files) |
| **Environment Variables** | ✅ Yes | `docker-compose.yml` |
| **Network** | ✅ Yes | `product-catalog-network` |

### Dockerfile Template
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

### Docker Compose Template
```yaml
services:
  service-name:
    build:
      context: ./backend
      dockerfile: service-name/Dockerfile
    container_name: service-name
    depends_on:
      mongodb:
        condition: service_healthy
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: docker
    networks:
      - product-catalog-network
    healthcheck:
      test: wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: "json-file"
      options:
        max-size: "10m"    # Maximum 10MB per log file
        max-file: "3"      # Keep 3 rotated files (30MB total)
```

---

## 📝 8. Documentation

| Document | Required? | Location |
|----------|-----------|----------|
| **README.md** | ✅ Yes | Service root |
| **API Docs** | ✅ Yes | `/swagger-ui.html` |
| **DEPLOYMENT.md** | ⚠️ If complex | Service root |
| **Test Scripts** | ✅ Yes | Repository root |

---

## 📦 Required Dependencies

### pom.xml (Maven)
```xml
<!-- Performance & Resiliency -->
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.2.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>

<!-- Core -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Documentation -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>

<!-- Utilities -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

---

## 🎯 Quick Start: New Service

1. **Review Standards**: Read [CLAUDE.md](CLAUDE.md) top section
2. **Use Checklist**: Follow [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md)
3. **Reference Implementation**: Copy patterns from [product-service](backend/product-service/)
4. **Run Tests**: Execute all test scripts
5. **Verify**: All checklist items complete
6. **Deploy**: `docker-compose up -d service-name`

---

## 📈 Success Metrics

A service is **production-ready** when:

✅ All checklist items are complete
✅ Response time < 500ms (P95)
✅ Test coverage > 80%
✅ Circuit breaker tested and working
✅ Idempotency verified
✅ Health endpoint returns "UP"
✅ All dependencies use connection pooling
✅ No hardcoded credentials
✅ API documentation accessible
✅ Docker deployment successful

---

## 🆘 Getting Help

| Question | Resource |
|----------|----------|
| Standards details? | [CLAUDE.md](CLAUDE.md) - Top section |
| Implementation examples? | [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) |
| Security guidelines? | [SECURITY.md](SECURITY.md) |
| Step-by-step checklist? | [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md) |
| Reference code? | [backend/product-service](backend/product-service/) |

---

## 📅 Standards Version

- **Version**: 1.0
- **Last Updated**: October 2, 2025
- **Based on**: Product Service implementation
- **Reviewed by**: Performance optimization analysis

**Changes require**: Team approval, update to all services

---

## ⚠️ Non-Compliance

Services that do NOT meet these standards:

❌ Will not be approved for production deployment
❌ May experience performance issues
❌ May have security vulnerabilities
❌ May cause cascading failures
❌ Will block merge requests

**All standards are mandatory for production.**
