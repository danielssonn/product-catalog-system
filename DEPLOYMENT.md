# Product Catalog System - Deployment Guide

## ✅ Deployment Status: COMPLETE

The Product Catalog System has been successfully deployed with MongoDB database and Kafka messaging infrastructure.

## 📊 System Architecture

### Two-Domain Model

#### **1. Catalog Domain** (`/backend/common/src/main/java/com/bank/productcatalog/common/domain/catalog/`)
Master product catalog managed by the bank - product templates/blueprints available for tenant selection.

**Models:**
- `ProductCatalog` - Master product templates
- `PricingTemplate` - Pricing with min/max configuration ranges
- `CatalogTerms` - Default terms and conditions
- `ProductConfigOptions` - Defines what tenants can customize
- `Category` - Product categorization
- Supporting: `ProductType`, `CatalogStatus`, `PricingType`, `InterestType`, etc.

**APIs:** `/api/v1/catalog` (Port 8081)

#### **2. Product Domain** (`/backend/common/src/main/java/com/bank/productcatalog/common/domain/product/`)
Tenant-specific products - configured instances of catalog products.

**Models:**
- `Product` - Tenant's active product instances
- `TenantProductConfiguration` - Tenant configuration before activation
- `PricingDetails` - Actual tenant pricing
- `ProductTerms` - Actual product terms
- `ApprovalInfo` - Approval workflow tracking
- Supporting: `ProductStatus`, `ConfigurationStatus`, `Fee`, `RateTier`, etc.

**APIs:**
- `/api/v1/products` - Tenant's active products (Port 8081)
- `/api/v1/configurations` - Tenant product configurations (Port 8081)

---

## 🗄️ Database Configuration

### MongoDB
- **Container:** `product-catalog-mongodb`
- **Port:** 27018 (mapped from internal 27017)
- **Database:** `product_catalog_db`
- **Credentials:**
  - Username: `admin`
  - Password: `admin123`
  - Auth Database: `admin`

**Note:** Port 27018 is used to avoid conflict with local MongoDB installation on port 27017.

### Collections Created
```
✅ product_catalog              - Master catalog templates
✅ catalog_categories           - Product categories
✅ products                     - Tenant active products
✅ tenant_product_configurations - Tenant configurations
✅ product_versions             - Product version history
✅ bundles                      - Product bundles
✅ cross_sell_rules             - Cross-sell recommendations
✅ consumers                    - API consumer registry
✅ audit_logs                   - Audit trail
✅ api_versions                 - API version metadata
✅ schema_versions              - Schema version metadata
```

### Indexes Created
- Compound indexes on `tenantId + entityId` for multi-tenancy
- Status indexes for filtering
- Category and type indexes for search
- Temporal indexes for versioning
- All optimized for query performance

### Sample Data
✅ 5 product categories loaded
✅ 2 sample catalog products loaded:
  - Premium Checking Account (with tiered pricing)
  - High-Yield Savings Account (with interest rate tiers)

---

## 📡 Messaging Infrastructure

### Apache Kafka
- **Container:** `product-catalog-kafka`
- **Ports:**
  - 9092 (external)
  - 9093 (external)
  - 29092 (internal)
- **Status:** ✅ Healthy

### Apache Zookeeper
- **Container:** `product-catalog-zookeeper`
- **Port:** 2181
- **Status:** ✅ Healthy

### Kafka UI (Optional Monitoring)
- **Port:** 8090
- **URL:** http://localhost:8090
- Access Kafka topics, consumers, and messages through web interface

---

## 🐳 Docker Services

### Infrastructure Services
| Service | Container | Status | Ports |
|---------|-----------|--------|-------|
| MongoDB | product-catalog-mongodb | ✅ Healthy | 27017 |
| Zookeeper | product-catalog-zookeeper | ✅ Healthy | 2181 |
| Kafka | product-catalog-kafka | ✅ Healthy | 9092, 9093 |

### Application Services (Ready to Build)
| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 8080 | Request routing, auth, rate limiting |
| Catalog Service | 8081 | Product catalog management |
| Bundle Service | 8082 | Product bundle management |
| Cross-Sell Service | 8083 | Recommendation engine |
| Audit Service | 8084 | Audit logging |
| Event Publisher | 8085 | Kafka event publishing |
| Tenant Service | 8086 | Tenant configuration |
| Version Service | 8087 | API & schema versioning |

---

## 🚀 Quick Start Commands

### Start Infrastructure
```bash
docker-compose up -d mongodb zookeeper kafka
```

### Start All Services
```bash
docker-compose up -d
```

### View Running Containers
```bash
docker ps --filter "name=product-catalog"
```

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f mongodb
docker-compose logs -f catalog-service
```

### Stop All Services
```bash
docker-compose down
```

### Stop and Remove Volumes
```bash
docker-compose down -v
```

---

## 🔗 Connection Strings

### MongoDB URI (from host)
```
mongodb://admin:admin123@localhost:27017/product_catalog_db?authSource=admin
```

### MongoDB URI (from containers)
```
mongodb://admin:admin123@mongodb:27017/product_catalog_db?authSource=admin
```

### Kafka Bootstrap Servers (from host)
```
localhost:9092
```

### Kafka Bootstrap Servers (from containers)
```
kafka:29092
```

---

## 📝 API Workflow Example

### 1. Bank Admin Creates Catalog Product
```bash
POST /api/v1/catalog
X-User-ID: admin123

{
  "catalogProductId": "savings-premium-001",
  "name": "Premium Savings Account",
  "type": "SAVINGS_ACCOUNT",
  "status": "AVAILABLE",
  "pricingTemplate": {
    "minInterestRate": 1.0,
    "maxInterestRate": 5.0,
    "defaultInterestRate": 3.0
  },
  "configOptions": {
    "canCustomizePricing": true,
    "canCustomizeName": true
  }
}
```

### 2. Tenant Browses Available Catalog
```bash
GET /api/v1/catalog/available
```

### 3. Tenant Creates Configuration
```bash
POST /api/v1/configurations?catalogProductId=savings-premium-001
X-Tenant-ID: tenant123
X-User-ID: user456

{
  "customName": "ACME Corp Premium Savings",
  "customPricing": {
    "interestRate": 4.0
  },
  "enabledChannels": ["WEB", "MOBILE"]
}
```

### 4. Submit for Approval (if required)
```bash
POST /api/v1/configurations/{configId}/submit
X-Tenant-ID: tenant123
X-User-ID: user456
```

### 5. Approve Configuration
```bash
POST /api/v1/configurations/{configId}/approve
X-Tenant-ID: tenant123
X-User-ID: approver789
```

### 6. Activate Product
```bash
POST /api/v1/configurations/{configId}/activate
X-Tenant-ID: tenant123
X-User-ID: user456
```

### 7. View Active Products
```bash
GET /api/v1/products
X-Tenant-ID: tenant123
```

---

## 🛠️ Next Steps

### To Build and Start Catalog Service
```bash
docker-compose up -d catalog-service
```

### To Build All Services
```bash
docker-compose build
docker-compose up -d
```

### To Access MongoDB Shell
```bash
docker exec -it product-catalog-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db
```

### To Query Sample Data
```javascript
// List all catalog products
db.product_catalog.find().pretty()

// List all categories
db.catalog_categories.find().pretty()

// Check indexes
db.product_catalog.getIndexes()
```

---

## 📦 Project Structure

```
product-catalog-system/
├── backend/
│   ├── common/
│   │   └── src/main/java/com/bank/productcatalog/common/
│   │       └── domain/
│   │           ├── catalog/model/    # Catalog domain models
│   │           └── product/model/    # Product domain models
│   ├── catalog-service/
│   │   ├── src/main/java/com/bank/productcatalog/catalog/domain/
│   │   │   ├── catalog/             # Catalog service layer
│   │   │   └── product/             # Product service layer
│   │   ├── Dockerfile
│   │   └── pom.xml
│   ├── api-gateway/
│   ├── bundle-service/
│   ├── cross-sell-service/
│   ├── audit-service/
│   ├── event-publisher-service/
│   ├── tenant-service/
│   └── version-service/
├── infrastructure/
│   └── mongodb/
│       ├── Dockerfile
│       └── init-mongo.js            # Database initialization
├── docker-compose.yml
└── DEPLOYMENT.md                    # This file
```

---

## 🔐 Security Notes

⚠️ **IMPORTANT:** The current configuration uses default credentials for development only.

**For Production:**
1. Change MongoDB credentials
2. Use Docker secrets or environment-specific configs
3. Enable TLS/SSL for MongoDB
4. Configure Kafka security (SASL/SSL)
5. Implement API authentication/authorization
6. Use secrets management (Vault, AWS Secrets Manager, etc.)

---

## ✅ Deployment Checklist

- [x] MongoDB container built and running
- [x] MongoDB database initialized with schema
- [x] Sample data loaded
- [x] All collections created
- [x] All indexes created
- [x] Zookeeper running and healthy
- [x] Kafka running and healthy
- [x] Docker networking configured
- [x] Dockerfiles created for all services
- [x] Application properties configured
- [x] Domain models separated (Catalog vs Product)
- [x] Service layers organized by domain
- [ ] Application services built (ready to build)
- [ ] Integration tests
- [ ] API documentation (Swagger/OpenAPI)

---

## 📊 Health Checks

### Check MongoDB
```bash
docker exec product-catalog-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin --eval "db.adminCommand('ping')"
```

### Check Kafka
```bash
docker exec product-catalog-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

### Check All Container Health
```bash
docker ps --format "table {{.Names}}\t{{.Status}}" --filter "name=product-catalog"
```

---

## 📞 Support

For issues or questions:
1. Check container logs: `docker-compose logs -f <service-name>`
2. Verify container health: `docker ps`
3. Check MongoDB connectivity
4. Verify Kafka is accessible

---

**Deployment completed successfully! 🎉**

The MongoDB database is initialized with all collections, indexes, and sample catalog data. The messaging infrastructure (Kafka & Zookeeper) is running and healthy. Application services are ready to build and deploy.