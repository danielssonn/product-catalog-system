# New Microservice Checklist

Use this checklist when creating a new microservice to ensure compliance with all mandatory standards.

---

## ✅ Performance & Resiliency

### Connection Pooling
- [ ] Add `httpclient5` dependency to pom.xml
- [ ] Create `RestTemplateConfig.java` with connection pool (100 total, 20 per route)
- [ ] Configure timeouts: 2s connect, 5s response, 1s pool
- [ ] Test: Run 10+ concurrent requests

### Circuit Breaker
- [ ] Add `resilience4j-spring-boot3` and `spring-boot-starter-aop` dependencies
- [ ] Create `application-resilience4j.yml` configuration
- [ ] Add `@CircuitBreaker` annotation to external service calls
- [ ] Implement fallback methods for all circuit breakers
- [ ] Test: Stop target service, verify circuit breaker activation

### Async Processing (if needed)
- [ ] Create `AsyncConfig.java` with thread pool configuration
- [ ] Add `@EnableAsync` to config class
- [ ] Create async service methods with `@Async`
- [ ] Use `CompletableFuture` for async operations
- [ ] Test: Verify async execution in logs (thread names)

### Idempotency Protection
- [ ] Add `caffeine` dependency to pom.xml
- [ ] Create `CacheConfig.java` with idempotency cache (10K entries, 24h TTL)
- [ ] Add `X-Idempotency-Key` header support in controllers
- [ ] Check cache before processing state-changing operations
- [ ] Test: Send duplicate requests with same idempotency key

---

## ✅ Security

### Authentication
- [ ] Add `spring-boot-starter-security` dependency
- [ ] Create `SecurityConfig.java` with HTTP Basic Auth
- [ ] Configure RBAC with ROLE_ADMIN and ROLE_USER
- [ ] Permit `/actuator/health` without authentication
- [ ] Require authentication for all `/api/v1/**` endpoints

### MongoDB User Store
- [ ] Create `User` model with roles
- [ ] Create `UserRepository` interface
- [ ] Create `MongoUserDetailsService` implementing `UserDetailsService`
- [ ] Configure BCrypt password encoder
- [ ] Initialize default users in MongoDB init script

### Credentials Externalization
- [ ] Replace all hardcoded credentials with environment variables
- [ ] Use `${ENV_VAR:-default}` pattern in application.yml
- [ ] Document all environment variables in README.md
- [ ] Add credentials to `SECURITY.md`
- [ ] Test: Verify service starts with default values

---

## ✅ API Design

### Versioning
- [ ] Use `/api/v1/` prefix for all endpoints
- [ ] Add version configuration to application.yml
- [ ] Document supported versions in README.md
- [ ] Never break existing API contracts

### Multi-tenancy
- [ ] Add `X-Tenant-ID` header to all tenant-specific endpoints
- [ ] Add `X-User-ID` header for user tracking
- [ ] Filter all database queries by tenantId
- [ ] Add compound indexes with tenantId

### REST Standards
- [ ] Use appropriate HTTP methods (GET, POST, PUT, PATCH, DELETE)
- [ ] Return appropriate status codes (200, 201, 202, 400, 404, 500)
- [ ] Use DTOs for request/response bodies
- [ ] Validate all inputs with `@Valid`

---

## ✅ Observability

### Actuator
- [ ] Add `spring-boot-starter-actuator` dependency
- [ ] Expose health, info, metrics, prometheus endpoints
- [ ] Configure `show-details: always` for health endpoint
- [ ] Test: `curl http://localhost:PORT/actuator/health`

### Logging
- [ ] Add Lombok `@Slf4j` to all classes
- [ ] Use structured logging: `log.info("Action: entity={}, id={}", type, id)`
- [ ] Log all external service calls
- [ ] Log all errors with stack traces
- [ ] Use appropriate log levels (ERROR, WARN, INFO, DEBUG)

### OpenAPI Documentation
- [ ] Add `springdoc-openapi-starter-webmvc-ui` dependency
- [ ] Add `@Operation` annotations to controller methods
- [ ] Test: Access `/swagger-ui.html`

---

## ✅ Data Layer

### MongoDB Configuration
- [ ] Add `spring-boot-starter-data-mongodb` dependency
- [ ] Configure MongoDB URI with environment variables
- [ ] Set `auto-index-creation: false`
- [ ] Create repositories extending `MongoRepository`
- [ ] Add `@Document` annotation to models
- [ ] Define compound indexes with `@CompoundIndex`

### Data Models
- [ ] Use Lombok `@Data` for models
- [ ] Add `@Id` to primary key field
- [ ] Add `@Indexed` to frequently queried fields
- [ ] Include `tenantId` in all tenant-specific models
- [ ] Add audit fields: createdAt, updatedAt, createdBy, updatedBy

---

## ✅ Testing

### Unit Tests
- [ ] Test all service methods
- [ ] Mock external dependencies
- [ ] Test error scenarios
- [ ] Achieve >80% code coverage

### Integration Tests
- [ ] Test database operations
- [ ] Test external service calls
- [ ] Test security configuration

### Performance Tests
- [ ] Create `test-performance.sh` script
- [ ] Test response time (<500ms target)
- [ ] Test concurrent requests (10+)
- [ ] Test circuit breaker activation

### Feature Tests
- [ ] Create `test-{feature}.sh` for each major feature
- [ ] Test happy path
- [ ] Test error scenarios
- [ ] Test idempotency
- [ ] Document test results

---

## ✅ Docker

### Dockerfile
- [ ] Use multi-stage build (Maven + JRE)
- [ ] Base image: `eclipse-temurin:21-jre`
- [ ] Build image: `maven:3.9-eclipse-temurin-21`
- [ ] Copy only JAR file to final image
- [ ] Expose service port
- [ ] Use `ENTRYPOINT` for Java command

### Docker Compose
- [ ] Add service to `docker-compose.yml`
- [ ] Configure depends_on for dependencies
- [ ] Add health check with curl to `/actuator/health`
- [ ] Pass environment variables
- [ ] Expose service port
- [ ] Connect to `product-catalog-network`

### Docker Testing
- [ ] Build image: `docker-compose build service-name`
- [ ] Start service: `docker-compose up -d service-name`
- [ ] Check logs: `docker-compose logs service-name`
- [ ] Test health: `curl http://localhost:PORT/actuator/health`

---

## ✅ Documentation

### README.md
- [ ] Service name and purpose
- [ ] Port number
- [ ] Key endpoints
- [ ] Dependencies (MongoDB, Kafka, other services)
- [ ] Environment variables
- [ ] Build instructions
- [ ] Test instructions

### DEPLOYMENT.md (if complex)
- [ ] Docker setup instructions
- [ ] Environment variable reference
- [ ] Production deployment guide
- [ ] Monitoring and alerting setup

### Code Documentation
- [ ] JavaDoc for public APIs
- [ ] Inline comments for complex logic
- [ ] Architecture decision records (ADR) for major decisions

---

## ✅ CI/CD (Future)

### GitHub Actions Workflow
- [ ] Build on pull request
- [ ] Run tests
- [ ] Build Docker image
- [ ] Push to registry
- [ ] Deploy to staging

---

## Reference Files

Before starting, review these reference implementations:

1. **Product Service** - Full implementation of all standards
   - [RestTemplateConfig.java](backend/product-service/src/main/java/com/bank/product/config/RestTemplateConfig.java)
   - [AsyncConfig.java](backend/product-service/src/main/java/com/bank/product/config/AsyncConfig.java)
   - [CacheConfig.java](backend/product-service/src/main/java/com/bank/product/config/CacheConfig.java)
   - [SecurityConfig.java](backend/product-service/src/main/java/com/bank/product/config/SecurityConfig.java)
   - [WorkflowClient.java](backend/product-service/src/main/java/com/bank/product/client/WorkflowClient.java)
   - [SolutionController.java](backend/product-service/src/main/java/com/bank/product/domain/solution/controller/SolutionController.java)

2. **Documentation**
   - [CLAUDE.md](CLAUDE.md) - Mandatory standards (top section)
   - [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) - Implementation details
   - [SECURITY.md](SECURITY.md) - Security guidelines

3. **Test Scripts**
   - [test-optimizations.sh](test-optimizations.sh)
   - [test-circuit-breaker.sh](test-circuit-breaker.sh)
   - [test-idempotency.sh](test-idempotency.sh)

---

## Final Verification

Before deploying, verify:

- [ ] All items in this checklist are complete
- [ ] Service passes all tests (unit, integration, performance)
- [ ] Service starts successfully in Docker
- [ ] Health endpoint returns "UP"
- [ ] API documentation is accessible
- [ ] README.md is complete and accurate
- [ ] Code is committed with descriptive commit message

---

**Questions?** Review [CLAUDE.md](CLAUDE.md) section "MANDATORY STANDARDS FOR ALL NEW SERVICES"
