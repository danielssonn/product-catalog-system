# Mock Core Banking System - Complete Setup Guide

## Overview

This guide explains how to test the **Core Banking Integration** using mock core banking systems. You can now provision products through the abstraction layer without connecting to real core banking APIs.

## 🎯 What's Included

### 1. Mock Core API Servers (3 systems)

| System | Port | Technology | Purpose |
|--------|------|------------|---------|
| **Temenos T24** | 8090 | Node.js/Express | Reference implementation |
| **Finacle** | 8091 | Node.js/Express | Alternative vendor (different field names) |
| **FIS Profile** | 8092 | Node.js/Express | Third vendor for multi-core testing |

**Location**: `infrastructure/mock-core-api/`

### 2. MongoDB Test Data

- **Tenant Core Mappings**: 3 tenants with core system configurations
- **Test Solutions**: 3 ready-to-provision solutions
- **Mock Products Collection**: Persistent storage for provisioned products

**Script**: `infrastructure/mongodb/init-core-systems.js`

### 3. Automated Test Suite

Complete end-to-end test script that validates:
- Auto-provisioning via change streams
- Configuration sync to cores
- Multi-core routing
- Health monitoring

**Script**: `test-core-provisioning.sh`

## 🚀 Quick Start (5 Minutes)

### Step 1: Start Mock Core APIs

```bash
# Navigate to mock API directory
cd infrastructure/mock-core-api

# Install dependencies (first time only)
npm install

# Start all 3 mock core systems
npm start
```

**Expected Output**:
```
Connected to MongoDB
✅ Temenos T24 Mock API running on http://localhost:9190/mock-temenos-api
✅ Finacle Mock API running on http://localhost:9191/mock-finacle-api
✅ FIS Profile Mock API running on http://localhost:9192/mock-fis-api

📋 Mock Core Banking Systems Ready!
```

### Step 2: Initialize Test Data

```bash
# Return to project root
cd ../..

# Load test data into MongoDB
mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --file infrastructure/mongodb/init-core-systems.js
```

**This creates**:
- ✅ 3 tenant-to-core mappings
- ✅ 3 test solutions (ready for provisioning)
- ✅ Indexes on all collections

### Step 3: Start Product Service

```bash
# Make sure MongoDB and Kafka are running
docker-compose up -d mongodb kafka

# Start product service (or rebuild if needed)
cd backend/product-service
mvn spring-boot:run
```

The change stream listener will start automatically and watch for solution changes.

### Step 4: Run Tests

```bash
# From project root
./test-core-provisioning.sh
```

**Test Coverage**:
1. ✅ Health checks (product service + mock cores)
2. ✅ Tenant core mappings verification
3. ✅ Auto-provisioning via change stream
4. ✅ Configuration update & sync
5. ✅ Mock product verification
6. ✅ Health monitoring status

## 📊 Test Scenarios

### Scenario 1: Auto-Provisioning (Change Stream)

**What happens**:
1. Create a solution with complete fields
2. MongoDB change stream detects INSERT
3. Readiness evaluator validates business rules
4. Solution auto-provisions to configured cores
5. `coreProvisioningRecords` updated with core product IDs

**Test it**:
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Auto-Provision Test",
    "category": "CHECKING",
    "pricing": {
      "monthlyFee": 10.00,
      "minimumBalance": 500.00
    },
    "availableChannels": ["WEB", "MOBILE"],
    "terms": {
      "minimumAge": 18,
      "termsUrl": "https://bank.com/terms"
    }
  }'

# Wait 3-5 seconds for change stream processing

# Check provisioning status
mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
  --eval 'db.solutions.findOne({solutionName: "Auto-Provision Test"}).coreProvisioningRecords'
```

**Expected Result**:
```javascript
[
  {
    coreSystemId: "temenos-us-east-1",
    coreSystemType: "TEMENOS_T24",
    coreProductId: "T24-1705318800000-789",
    status: "PROVISIONED",
    provisionedAt: ISODate("2025-01-15T10:00:00Z")
  }
]
```

### Scenario 2: Configuration Update & Sync

**What happens**:
1. Update an already-provisioned solution
2. Change stream detects UPDATE
3. Listener detects provisioning records exist
4. Orchestrator syncs update to all provisioned cores

**Test it**:
```bash
# Get solution ID from previous test
SOLUTION_ID="<solution-id-from-scenario-1>"

# Update pricing
curl -u admin:admin123 -X PUT http://localhost:8082/api/v1/solutions/$SOLUTION_ID \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{
    "pricing": {
      "monthlyFee": 15.00,
      "minimumBalance": 1000.00
    }
  }'

# Wait 3 seconds for sync

# Verify in mock core
CORE_PRODUCT_ID=$(mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
  --quiet --eval "db.solutions.findOne({_id: ObjectId('$SOLUTION_ID')}).coreProvisioningRecords[0].coreProductId" | tr -d '"')

curl http://localhost:9190/mock-temenos-api/products/$CORE_PRODUCT_ID
```

**Expected Result**:
```json
{
  "productId": "T24-1705318800000-789",
  "name": "Auto-Provision Test",
  "monthlyFee": 15.00,
  "minimumBalance": 1000.00,
  "updatedAt": "2025-01-15T10:05:00.000Z"
}
```

### Scenario 3: Multi-Core Provisioning (Geo-Distributed)

**What happens**:
1. Create solution for tenant with multiple cores (acme-bank)
2. Solution metadata includes region: US-EAST
3. Router selects cores matching region
4. Provisions to matching core(s)

**Test it**:
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: acme-bank" \
  -d '{
    "catalogProductId": "cat-checking-premium",
    "solutionName": "ACME Geo Test",
    "category": "CHECKING",
    "pricing": {
      "monthlyFee": 25.00,
      "minimumBalance": 5000.00
    },
    "availableChannels": ["WEB", "MOBILE"],
    "terms": {
      "minimumAge": 21,
      "termsUrl": "https://acmebank.com/terms"
    },
    "metadata": {
      "region": "US-EAST"
    }
  }'
```

**Expected**: Provisions to `temenos-prod-us` (US-EAST) only, not EU core.

### Scenario 4: Readiness Failure (Missing Required Fields)

**What happens**:
1. Create solution missing required fields
2. Readiness evaluator detects failure
3. Auto-provisioning does NOT trigger
4. Logs show failure reasons

**Test it**:
```bash
# Missing pricing - should NOT provision
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Incomplete Solution",
    "category": "CHECKING",
    "availableChannels": ["WEB"]
  }'

# Check logs - should see:
# LOG: Solution <id> is NOT ready for provisioning. Reasons: Pricing configuration missing, Terms are missing
```

### Scenario 5: Circuit Breaker Test

**What happens**:
1. Stop mock Temenos API
2. Try to provision (will fail)
3. After 5 failures, circuit breaker opens
4. Subsequent calls fail fast

**Test it**:
```bash
# Stop Temenos mock (Ctrl+C in mock server terminal)

# Create 6 solutions - circuit should open after 5 failures
for i in {1..6}; do
  curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: tenant-001" \
    -d "{\"catalogProductId\":\"cat-checking-001\",\"solutionName\":\"Circuit Test $i\",\"category\":\"CHECKING\",\"pricing\":{\"monthlyFee\":10},\"availableChannels\":[\"WEB\"],\"terms\":{\"minimumAge\":18,\"termsUrl\":\"https://bank.com/terms\"}}"
done

# Check logs - should see:
# LOG: Circuit breaker state transition: CLOSED -> OPEN
```

## 🗄️ MongoDB Collections

### tenant_core_mappings

Stores tenant-to-core system mappings.

```javascript
db.tenant_core_mappings.find().pretty()

// Sample:
{
  tenantId: "tenant-001",
  coreSystems: [
    {
      coreSystemId: "temenos-us-east-1",
      coreSystemType: "TEMENOS_T24",
      priority: 100,
      region: "US-EAST",
      active: true,
      config: {
        apiEndpoint: "http://localhost:9190/mock-temenos-api",
        apiKey: "test-api-key-temenos-001"
      }
    }
  ],
  defaultCoreSystemId: "temenos-us-east-1"
}
```

### solutions

Solutions with provisioning records.

```javascript
// Find provisioned solutions
db.solutions.find({
  "coreProvisioningRecords.0": { $exists: true }
}).pretty()

// Sample:
{
  _id: ObjectId("..."),
  tenantId: "tenant-001",
  name: "Premium Checking Account",
  category: "CHECKING",
  status: "ACTIVE",
  coreProvisioningRecords: [
    {
      coreSystemId: "temenos-us-east-1",
      coreSystemType: "TEMENOS_T24",
      coreProductId: "T24-1705318800000-456",
      status: "PROVISIONED",
      provisionedAt: ISODate("2025-01-15T10:00:00Z")
    }
  ]
}
```

### mock_core_products

Mock core system product storage.

```javascript
db.mock_core_products.find().pretty()

// Sample:
{
  _id: ObjectId("..."),
  coreSystemId: "temenos-us-east-1",
  coreSystemType: "TEMENOS_T24",
  coreProductId: "T24-1705318800000-456",
  catalogSolutionId: "sol-123",
  tenantId: "tenant-001",
  productData: {
    productId: "T24-1705318800000-456",
    name: "Premium Checking Account",
    status: "ACTIVE",
    monthlyFee: 15.00,
    minimumBalance: 1000.00
  },
  createdAt: ISODate("2025-01-15T10:00:00Z")
}
```

## 📈 Useful Queries

### Count Products by Core System
```javascript
db.mock_core_products.aggregate([
  { $group: { _id: "$coreSystemType", count: { $sum: 1 } } },
  { $sort: { count: -1 } }
])
```

### Find All Provisioned Solutions
```javascript
db.solutions.find({
  coreProvisioningRecords: { $exists: true, $ne: [] }
})
```

### Find Failed Provisioning Attempts
```javascript
db.solutions.find({
  "coreProvisioningRecords.status": "PROVISION_FAILED"
})
```

### Get Provisioning History for Tenant
```javascript
db.solutions.aggregate([
  { $match: { tenantId: "tenant-001" } },
  { $unwind: "$coreProvisioningRecords" },
  { $group: {
      _id: "$coreProvisioningRecords.coreSystemType",
      count: { $sum: 1 },
      statuses: { $addToSet: "$coreProvisioningRecords.status" }
  }}
])
```

## 🔧 Troubleshooting

### Issue: Auto-provisioning not working

**Check 1: Change stream listener**
```bash
# Check product service logs
tail -f backend/product-service/logs/application.log | grep -i "change stream"

# Expected:
# Watching change stream for collection: solutions
```

**Check 2: Mock API is running**
```bash
curl http://localhost:9190/mock-temenos-api/health
```

**Check 3: Tenant has core mappings**
```javascript
db.tenant_core_mappings.findOne({ tenantId: "tenant-001" })
```

### Issue: Products not syncing to cores

**Check 1: Solution is provisioned**
```javascript
db.solutions.findOne({ _id: ObjectId("...") }).coreProvisioningRecords
```

**Check 2: Update detected by change stream**
```bash
# Check logs for:
# LOG: Handling update for already provisioned solution: <id>
```

### Issue: Circuit breaker stuck open

**Restart mock API** then wait 60 seconds for circuit to go half-open:
```bash
cd infrastructure/mock-core-api
npm start
```

**Or reset programmatically** (requires code change to expose endpoint):
```bash
curl -X POST http://localhost:8082/api/v1/core-systems/circuit-breaker/reset
```

## 📚 Related Documentation

- [Core Banking Complete Guide](CORE_BANKING_COMPLETE_GUIDE.md) - Full integration architecture with auto-provisioning details
- [Core Banking Index](CORE_BANKING_INDEX.md) - Quick navigation
- [Mock API README](infrastructure/mock-core-api/README.md) - Mock server API docs

## 🎯 Next Steps

1. ✅ **Run the test suite**: `./test-core-provisioning.sh`
2. ✅ **Explore provisioned products**: Query `mock_core_products` collection
3. ✅ **Test multi-core**: Create solutions for acme-bank tenant
4. ✅ **Test circuit breaker**: Stop mock API and trigger failures
5. ✅ **Monitor health**: Check logs every 30 seconds for health checks

## 🏆 Success Criteria

Your mock core system setup is working correctly if:

- [x] All 3 mock APIs respond to health checks
- [x] Test data loaded successfully (3 tenant mappings, 3 solutions)
- [x] Auto-provisioning creates `coreProvisioningRecords`
- [x] Mock products persist to `mock_core_products` collection
- [x] Configuration updates sync to cores
- [x] Circuit breaker opens after 5 consecutive failures
- [x] Health monitor logs status every 30 seconds

---

*Ready to test? Run: `./test-core-provisioning.sh`*
