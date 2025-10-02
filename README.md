# Product Catalog System

A comprehensive Product Catalog Management System for Commercial Banks with Cash Management capabilities.

---

## 🚀 Quick Start

### For Developers
1. **Setup**: See [CLAUDE.md](CLAUDE.md) for development commands
2. **Standards**: Review [MANDATORY STANDARDS](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services) (top of CLAUDE.md)
3. **New Service?**: Follow [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md)

### For Code Review
- ✅ All [mandatory standards](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services) implemented?
- ✅ [Checklist](NEW_SERVICE_CHECKLIST.md) complete?
- ✅ Tests passing? (unit, integration, performance)
- ✅ Documentation updated?

---

## Architecture Overview

### Technology Stack

**Frontend:**
- Angular with Angular Material
- Multi-tenant and multi-channel enabled

**Backend:**
- Java Spring Boot Microservices
- RESTful APIs with versioning support
- Multi-tenant architecture

**Database:**
- MongoDB (NoSQL)
- Versioned schemas
- Tenant isolation

**Messaging:**
- Apache Kafka
- Event-driven architecture

**Infrastructure:**
- Docker & Docker Compose
- Kubernetes-ready

## Features

### Core Capabilities
- **Product Catalog Management**: CRUD operations for banking products
- **Product Bundling**: Create and manage product bundles with discounts
- **Cross-Sell Rules**: Intelligent product recommendations
- **Multi-Tenancy**: Support for multiple banks/organizations
- **Multi-Channel**: Web, Mobile, API, Branch channel support
- **Versioning**: API, Schema, and Product versioning for progressive adoption
- **Audit Trail**: Complete audit history for compliance
- **Event Publishing**: Real-time event notifications via Kafka

### Initial Products
1. **Business Checking Account** - Core transactional account
2. **Sweep Account** - Automatic liquidity management
3. **Merchant Services** - Payment processing and receivables

## Project Structure

```
product-catalog-system/
├── frontend/                    # Angular application
│   └── product-catalog-ui/
├── backend/                     # Spring Boot microservices
│   ├── catalog-service/        # Product catalog management
│   ├── bundle-service/         # Product bundling
│   ├── cross-sell-service/     # Cross-sell recommendations
│   ├── event-publisher-service/# Kafka event publishing
│   ├── audit-service/          # Audit trail management
│   ├── tenant-service/         # Multi-tenant management
│   ├── version-service/        # Version resolution & migration
│   ├── api-gateway/            # API Gateway & routing
│   └── common/                 # Shared libraries
├── infrastructure/              # Infrastructure configuration
│   ├── docker/
│   └── kubernetes/
└── docs/                       # Documentation
```

## Getting Started

### Prerequisites
- Java 17+
- Node.js 18+ & npm
- Maven 3.8+
- Docker & Docker Compose
- MongoDB
- Apache Kafka

### Quick Start

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd product-catalog-system
   ```

2. **Start infrastructure services**
   ```bash
   docker-compose up -d
   ```

3. **Build and run backend services**
   ```bash
   cd backend
   ./build-all.sh
   ./run-all.sh
   ```

4. **Start frontend application**
   ```bash
   cd frontend/product-catalog-ui
   npm install
   npm start
   ```

5. **Access the application**
   - Frontend: http://localhost:4200
   - API Gateway: http://localhost:8080
   - API Documentation: http://localhost:8080/swagger-ui.html

## API Versioning

All APIs support versioning through:
- URL path: `/api/v1/products`
- Header: `X-API-Version: 1.0`
- Content negotiation: `Accept: application/vnd.productcatalog.v1+json`

## Multi-Tenancy

All API requests must include:
- `X-Tenant-ID` header
- Valid JWT token with tenant claim

## Development

### Backend Development
```bash
cd backend/<service-name>
mvn spring-boot:run
```

### Frontend Development
```bash
cd frontend/product-catalog-ui
ng serve
```

### Running Tests
```bash
# Backend
mvn test

# Frontend
ng test
```

## Configuration

### Environment Variables
- `SPRING_PROFILES_ACTIVE`: Active Spring profile (dev, test, prod)
- `MONGODB_URI`: MongoDB connection string
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka broker addresses
- `JWT_SECRET`: JWT signing secret

## Documentation

- [API Documentation](docs/api/README.md)
- [Data Models](docs/models/README.md)
- [Architecture Guide](docs/architecture/README.md)
- [Deployment Guide](docs/deployment/README.md)

## Contributing

1. Create a feature branch
2. Make your changes
3. Run tests
4. Submit a pull request

## License

Proprietary - All rights reserved

## Contact

For questions or support, contact the development team.