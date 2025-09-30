# Architecture Documentation

## System Overview

The Product Catalog System is a microservices-based application designed for commercial banks to manage product catalogs with cash management capabilities.

## Key Architectural Principles

### 1. Multi-Tenancy
- Tenant isolation at data and API level
- Tenant ID required in all API requests
- Separate data partitions per tenant in MongoDB

### 2. Multi-Channel Support
- Web, Mobile, API, Branch channels
- Channel-specific product availability
- Channel-aware routing and authorization

### 3. API & Schema Versioning
- URL-based versioning (v1, v2, etc.)
- Schema versioning for database evolution
- Product versioning for progressive rollouts
- Consumer subscription to specific versions

### 4. Event-Driven Architecture
- Kafka for asynchronous communication
- Event publishing on product changes
- Audit trail via event streaming

## Microservices

### 1. Catalog Service (Port 8081)
- Product CRUD operations
- Product versioning
- Channel availability management

### 2. Bundle Service (Port 8082)
- Product bundle management
- Discount calculation
- Bundle eligibility validation

### 3. Cross-Sell Service (Port 8083)
- Rule engine for recommendations
- Condition evaluation
- Personalized offers

### 4. Event Publisher Service (Port 8085)
- Kafka event publishing
- Event schema validation
- Guaranteed delivery

### 5. Audit Service (Port 8084)
- Audit log management
- Compliance reporting
- Event consumption for audit trails

### 6. Tenant Service (Port 8086)
- Tenant configuration
- Feature flags per tenant
- Channel configuration

### 7. Version Service (Port 8087)
- API version resolution
- Schema migration
- Consumer version tracking

### 8. API Gateway (Port 8080)
- Request routing
- Multi-tenant validation
- Authentication/Authorization
- Rate limiting

## Data Architecture

### MongoDB Collections
- `products` - Product catalog
- `product_versions` - Product version snapshots
- `bundles` - Product bundles
- `cross_sell_rules` - Cross-sell rules
- `consumers` - API consumer registry
- `audit_logs` - Audit trail
- `api_versions` - API version metadata
- `schema_versions` - Schema version metadata

### Indexing Strategy
- Tenant ID + Entity ID composite indexes
- Version indexes for temporal queries
- Status indexes for filtering

## Security

### Authentication
- JWT-based authentication
- Token contains tenant claim
- Refresh token support

### Authorization
- Role-based access control (RBAC)
- Channel-based permissions
- Tenant isolation enforcement

## Scalability

### Horizontal Scaling
- Stateless microservices
- Load balancing via API Gateway
- Kafka consumer groups for parallel processing

### Caching Strategy
- Redis for frequently accessed data
- Cache invalidation via Kafka events

## Deployment

### Containerization
- Docker containers for each service
- Docker Compose for local development
- Kubernetes for production

### CI/CD
- Automated testing
- Blue-green deployments
- Canary releases for new versions

## Monitoring & Observability

### Metrics
- Spring Boot Actuator
- Prometheus metrics
- Custom business metrics

### Logging
- Structured logging (JSON)
- Centralized log aggregation
- Correlation IDs for tracing

### Health Checks
- Liveness probes
- Readiness probes
- Dependency health checks