# Mock Core Banking API Server

Mock implementation of core banking system APIs (Temenos T24, Finacle, FIS Profile) for testing product provisioning through the abstraction layer.

## Overview

This mock server simulates three core banking systems:

| System | Port | Base Path | Purpose |
|--------|------|-----------|---------|
| **Temenos T24** | 9190 | `/mock-temenos-api` | Most complete implementation, reference for adapter |
| **Finacle** | 9191 | `/mock-finacle-api` | Alternative core system with different field names |
| **FIS Profile** | 9192 | `/mock-fis-api` | Third vendor for multi-core testing |

All mock products are persisted to MongoDB collection: `mock_core_products`

## Quick Start

### Prerequisites

**1. MongoDB Running (Port 27018)**
```bash
docker-compose ps mongodb
# Should show: Up X days (healthy)
```

If not running:
```bash
docker-compose up -d mongodb
```

**2. Node.js Installed**
```bash
node --version
# Should be v18 or higher
```

### Setup Steps

**1. Set Up Environment**

```bash
cd infrastructure/mock-core-api

# Copy environment template
cp .env.example .env

# Edit .env and set your MongoDB credentials
nano .env  # or vim .env
```

Edit `.env`:
```bash
# REQUIRED: MongoDB credentials
MONGODB_USERNAME=admin
MONGODB_PASSWORD=admin123  # Replace with your actual password

# Optional: Override defaults
MONGODB_HOST=localhost
MONGODB_PORT=27018
MONGODB_DATABASE=product_catalog_db
```

**2. Install Dependencies**

```bash
npm install
```

**3. Start Mock APIs**

**Option A: Using startup script (Recommended)**
```bash
./start-mock-apis.sh
```

This script will:
- ✅ Check `.env` exists
- ✅ Validate MongoDB credentials
- ✅ Test MongoDB connection
- ✅ Install dependencies if needed
- ✅ Start all 3 mock APIs

**Option B: Manual start**
```bash
npm start
```

**Expected Output:**
```
Connecting to MongoDB...
URI: mongodb://admin:***@localhost:27018/product_catalog_db?authSource=admin
✅ Successfully connected to MongoDB
✅ Indexes created/verified
✅ Temenos T24 Mock API running on http://localhost:9190/mock-temenos-api
✅ Finacle Mock API running on http://localhost:9191/mock-finacle-api
✅ FIS Profile Mock API running on http://localhost:9192/mock-fis-api

📋 Mock Core Banking Systems Ready!
```

**4. Verify APIs**

```bash
# Test health endpoints
curl http://localhost:9190/mock-temenos-api/health
curl http://localhost:9191/mock-finacle-api/health
curl http://localhost:9192/mock-fis-api/health
```

**5. Initialize Test Data**

From project root:

```bash
mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --file infrastructure/mongodb/init-core-systems.js
```

This creates:
- **3 tenant core mappings** (tenant-001, tenant-002, acme-bank)
- **3 test solutions** ready for provisioning
- **Indexes** on collections

**6. Run Provisioning Tests**

```bash
./test-core-provisioning.sh
```

## API Endpoints

### Temenos T24 Mock API

**Base URL**: `http://localhost:9190/mock-temenos-api`

#### Health Check
```bash
GET /health

Response:
{
  "status": "UP",
  "system": "Temenos T24",
  "timestamp": "2025-01-15T10:00:00.000Z"
}
```

#### Create Product
```bash
POST /products
Content-Type: application/json

Request:
{
  "name": "Premium Checking",
  "description": "Premium checking account",
  "type": "CHECKING",
  "monthlyFee": 15.00,
  "minimumBalance": 1000.00,
  "features": {
    "overdraftProtection": true,
    "debitCard": true
  },
  "metadata": {
    "catalogSolutionId": "sol-123",
    "tenantId": "tenant-001"
  }
}

Response: 201 Created
{
  "productId": "T24-1705318800000-456",
  "name": "Premium Checking",
  "status": "ACTIVE",
  "createdAt": "2025-01-15T10:00:00.000Z",
  ...
}
```

#### Update Product
```bash
PUT /products/{productId}
Content-Type: application/json

Request:
{
  "name": "Updated Premium Checking",
  "monthlyFee": 20.00,
  "minimumBalance": 1500.00
}

Response: 200 OK
{
  "productId": "T24-1705318800000-456",
  "name": "Updated Premium Checking",
  "monthlyFee": 20.00,
  "updatedAt": "2025-01-15T11:00:00.000Z"
}
```

#### Get Product
```bash
GET /products/{productId}

Response: 200 OK
{
  "productId": "T24-1705318800000-456",
  "name": "Premium Checking",
  "type": "CHECKING",
  "status": "ACTIVE",
  ...
}
```

#### Check Product Exists
```bash
HEAD /products/{productId}

Response: 200 OK (if exists) or 404 Not Found
```

#### Deactivate Product
```bash
POST /products/{productId}/deactivate

Response: 200 OK
{
  "productId": "T24-1705318800000-456",
  "status": "INACTIVE"
}
```

#### Delete Product (Sunset)
```bash
DELETE /products/{productId}

Response: 200 OK
{
  "productId": "T24-1705318800000-456",
  "status": "DELETED"
}
```

### Finacle Mock API

**Base URL**: `http://localhost:9191/mock-finacle-api`

Similar endpoints with different field names:
- `productName` instead of `name`
- `intRate` instead of `interestRate`
- `monthlyCharge` instead of `monthlyFee`
- `productStatus`: "A" (active) or "I" (inactive)

### FIS Profile Mock API

**Base URL**: `http://localhost:9192/mock-fis-api`

Similar endpoints with FIS-specific field names:
- `productCode` instead of `productId`
- `productTitle` instead of `name`
- `serviceFee` instead of `monthlyFee`
- `interestRatePercent` instead of `interestRate`

## Testing with cURL

### Test Temenos Health
```bash
curl http://localhost:9190/mock-temenos-api/health
```

### Create Product in Temenos
```bash
curl -X POST http://localhost:9190/mock-temenos-api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Checking",
    "description": "Test account",
    "type": "CHECKING",
    "monthlyFee": 10.00,
    "minimumBalance": 500.00,
    "metadata": {
      "catalogSolutionId": "sol-test-001",
      "tenantId": "tenant-001"
    }
  }'
```

### Get Product from Temenos
```bash
# Replace {productId} with actual ID from create response
curl http://localhost:9190/mock-temenos-api/products/T24-1705318800000-456
```

## MongoDB Persistence

All mock products are stored in: `mock_core_products`

**Document Structure**:
```javascript
{
  _id: ObjectId("..."),
  coreSystemId: "temenos-us-east-1",
  coreSystemType: "TEMENOS_T24",
  coreProductId: "T24-1705318800000-456",
  catalogSolutionId: "sol-123",
  tenantId: "tenant-001",
  productData: {
    // Core system specific product data
    productId: "T24-1705318800000-456",
    name: "Premium Checking",
    status: "ACTIVE",
    ...
  },
  createdAt: ISODate("2025-01-15T10:00:00Z"),
  updatedAt: ISODate("2025-01-15T11:00:00Z")
}
```

**Indexes**:
- `{ coreSystemId: 1, coreProductId: 1 }` (unique)
- `{ catalogSolutionId: 1 }`
- `{ tenantId: 1 }`

### Query Mock Products

```javascript
// Find all products for a tenant
db.mock_core_products.find({ tenantId: "tenant-001" })

// Find product by core product ID
db.mock_core_products.findOne({ coreProductId: "T24-1705318800000-456" })

// Find products by core system type
db.mock_core_products.find({ coreSystemType: "TEMENOS_T24" })

// Count products per core system
db.mock_core_products.aggregate([
  { $group: { _id: "$coreSystemType", count: { $sum: 1 } } }
])
```

## Test Data

### Tenant Core Mappings

**tenant-001**:
- Temenos T24 (US-EAST): `http://localhost:9190/mock-temenos-api`
- Finacle (EU-WEST): `http://localhost:9191/mock-finacle-api`

**tenant-002**:
- FIS Profile (US-WEST): `http://localhost:9192/mock-fis-api`

**acme-bank**:
- Temenos T24 (US-EAST): `http://localhost:9190/mock-temenos-api`
- Temenos T24 (EU-WEST): `http://localhost:9190/mock-temenos-api`

### Pre-loaded Test Solutions

1. **SOL-TEST-001** (tenant-001)
   - Premium Checking Account
   - Monthly fee: $15, Min balance: $1,000
   - Ready for auto-provisioning

2. **SOL-TEST-002** (tenant-001)
   - High-Yield Savings Account
   - Interest: 4.5% APY, Min balance: $500
   - Ready for auto-provisioning

3. **SOL-ACME-001** (acme-bank)
   - ACME Premium Checking
   - Monthly fee: $25, Min balance: $5,000
   - Region: US-EAST (routes to temenos-prod-us)

## Integration with Product Service

The Product Service's [TemenosT24Adapter](../../backend/product-service/src/main/java/com/bank/product/core/adapter/impl/TemenosT24Adapter.java) is configured to call these mock endpoints during testing.

**Flow**:
1. User creates/updates solution via Product Service API
2. MongoDB change stream detects change
3. Readiness evaluator checks business rules
4. Provisioning orchestrator calls adapter
5. Adapter makes HTTP request to mock core API (this server)
6. Mock API persists to `mock_core_products` collection
7. Mock API returns success response
8. Adapter updates solution's `coreProvisioningRecords`

## Troubleshooting

### Error: "MongoDB credentials not configured"

**Solution**: Edit `.env` and set `MONGODB_USERNAME` and `MONGODB_PASSWORD`

### Error: "Cannot connect to MongoDB"

**Check 1**: Is MongoDB running on port 27018?
```bash
docker-compose ps mongodb
mongosh "mongodb://admin:admin123@localhost:27018/admin?authSource=admin" --eval "db.runCommand({ ping: 1 })"
```

**Check 2**: Test connection with test script
```bash
node test-connection.js
```

**Check 3**: Verify port in `.env`
```bash
# Should be 27018 for the docker setup
MONGODB_PORT=27018
```

**Check 4**: Restart MongoDB container
```bash
docker-compose restart mongodb
```

### Error: "Port already in use"

**Solution 1**: Kill existing process
```bash
# Find process using the port
lsof -ti:9190

# Kill it
kill -9 $(lsof -ti:9190)
```

**Solution 2**: Change ports in `.env`
```bash
TEMENOS_PORT=9193
FINACLE_PORT=9194
FIS_PORT=9195
```

### Error: "Module not found"

**Solution**: Install dependencies
```bash
rm -rf node_modules package-lock.json
npm install
```

### MongoDB connection timeout

**Solution**: Use explicit host connection
```bash
export MONGODB_URI="mongodb://admin:admin123@127.0.0.1:27018/product_catalog_db?authSource=admin&directConnection=true"
npm start
```

### Mock API not responding

**Check if running**:
```bash
curl http://localhost:9190/mock-temenos-api/health
```

**Restart**:
```bash
cd infrastructure/mock-core-api
npm start
```

### Products not persisting

**Check MongoDB connection**:
```bash
mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
  --eval "db.mock_core_products.countDocuments({})"
```

## Development

### Add New Endpoint

Edit `mock-core-server.js`:

```javascript
// In createTemenosServer()
app.post('/mock-temenos-api/products/:productId/freeze', async (req, res) => {
    console.log('[Temenos T24] Freezing product:', req.params.productId);

    await mockProducts.updateOne(
        { coreProductId: req.params.productId },
        { $set: { 'productData.status': 'FROZEN' } }
    );

    res.json({ productId: req.params.productId, status: 'FROZEN' });
});
```

### Enable Debug Logging

```bash
DEBUG=* npm start
```

### Watch Mode (Auto-restart)

```bash
npm run dev
```

### Run in Background

```bash
npm start > mock-apis.log 2>&1 &
echo $! > mock-apis.pid

# To stop later:
kill $(cat mock-apis.pid)

# View logs:
tail -f mock-apis.log
```

### Stop Mock APIs

**If running in foreground**: Press `Ctrl+C`

**If running in background**:
```bash
kill $(cat mock-apis.pid)
```

**Force kill all instances**:
```bash
pkill -f mock-core-server
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `MONGODB_USERNAME` | Yes* | - | MongoDB username |
| `MONGODB_PASSWORD` | Yes* | - | MongoDB password |
| `MONGODB_URI` | Yes* | - | Full MongoDB connection string |
| `MONGODB_HOST` | No | localhost | MongoDB host |
| `MONGODB_PORT` | No | 27018 | MongoDB port |
| `MONGODB_DATABASE` | No | product_catalog_db | Database name |
| `MONGODB_AUTH_SOURCE` | No | admin | Auth database |
| `TEMENOS_PORT` | No | 9190 | Temenos API port |
| `FINACLE_PORT` | No | 9191 | Finacle API port |
| `FIS_PORT` | No | 9192 | FIS API port |

*Either provide `MONGODB_USERNAME` + `MONGODB_PASSWORD` OR `MONGODB_URI`

## Security Note

⚠️ **Never commit `.env` file to version control!**

The `.env` file contains sensitive credentials. It's already in `.gitignore`.

## Production Deployment

⚠️ **Warning**: This is a MOCK server for testing only. Do NOT deploy to production.

For production:
1. Use real core banking system endpoints
2. Implement proper authentication (OAuth2, mutual TLS)
3. Add request validation and rate limiting
4. Use connection pooling for MongoDB
5. Add comprehensive error handling
6. Implement audit logging

## License

MIT
