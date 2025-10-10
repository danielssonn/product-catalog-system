# Mock Core Banking System - Test Data Reference

Quick reference guide for all test data loaded by `init-core-systems.js`

## 🏦 Tenant Core System Mappings

### Tenant: `tenant-001`

| Core System ID | Type | Region | Priority | Endpoint | Product Types |
|----------------|------|--------|----------|----------|---------------|
| `temenos-us-east-1` | TEMENOS_T24 | US-EAST | 100 | http://localhost:9190/mock-temenos-api | CHECKING, SAVINGS |
| `finacle-eu-west-1` | FINACLE | EU-WEST | 90 | http://localhost:9191/mock-finacle-api | (all) |

**Default**: `temenos-us-east-1`

**Auth**:
- Temenos: API Key = `test-api-key-temenos-001`
- Finacle: Username = `catalog-service`, Password = `test-password`

---

### Tenant: `tenant-002`

| Core System ID | Type | Region | Priority | Endpoint | Product Types |
|----------------|------|--------|----------|----------|---------------|
| `fis-us-west-1` | FIS_PROFILE | US-WEST | 100 | http://localhost:9192/mock-fis-api | CHECKING, SAVINGS, LOAN |

**Default**: `fis-us-west-1`

**Auth**:
- FIS: API Key = `test-api-key-fis-001`

---

### Tenant: `acme-bank`

| Core System ID | Type | Region | Priority | Endpoint | Product Types |
|----------------|------|--------|----------|----------|---------------|
| `temenos-prod-us` | TEMENOS_T24 | US-EAST | 100 | http://localhost:9190/mock-temenos-api | CHECKING, SAVINGS, CREDIT_CARD |
| `temenos-prod-eu` | TEMENOS_T24 | EU-WEST | 90 | http://localhost:9190/mock-temenos-api | CHECKING, SAVINGS |

**Default**: `temenos-prod-us`

**Auth**:
- Temenos US: API Key = `acme-temenos-key-123`, Institution ID = `ACME001`
- Temenos EU: API Key = `acme-temenos-eu-key-456`, Institution ID = `ACME002`

**Multi-Core Strategy**:
- Solutions with `metadata.region = "US-EAST"` → routes to `temenos-prod-us`
- Solutions with `metadata.region = "EU-WEST"` → routes to `temenos-prod-eu`
- Solutions without region → routes to default (`temenos-prod-us`)

## 📋 Pre-loaded Test Solutions

### Solution 1: `SOL-TEST-001`

```javascript
{
  tenantId: "tenant-001",
  solutionId: "SOL-TEST-001",
  catalogProductId: "cat-checking-001",
  name: "Premium Checking Account",
  description: "Full-featured checking account with overdraft protection",
  category: "CHECKING",
  status: "ACTIVE",

  pricing: {
    monthlyFee: $15.00,
    minimumBalance: $1,000.00,
    currency: "USD"
  },

  availableChannels: ["WEB", "MOBILE", "BRANCH", "ATM"],

  features: {
    overdraftProtection: true,
    debitCard: true,
    onlineBanking: true,
    mobileBanking: true,
    billPay: true,
    mobileDeposit: true
  },

  terms: {
    minimumAge: 18,
    termsUrl: "https://bank.com/terms/checking"
  }
}
```

**Provisioning Readiness**: ✅ READY
- Has name, category, pricing (monthly fee + min balance), channels, terms
- Will auto-provision to: `temenos-us-east-1` (matches CHECKING product type + US region default)

**Expected Core Product ID**: `T24-{timestamp}-{random}`

---

### Solution 2: `SOL-TEST-002`

```javascript
{
  tenantId: "tenant-001",
  solutionId: "SOL-TEST-002",
  catalogProductId: "cat-savings-001",
  name: "High-Yield Savings Account",
  description: "Competitive interest rates with no monthly fees",
  category: "SAVINGS",
  status: "ACTIVE",

  pricing: {
    interestRate: 4.50,  // APY
    monthlyFee: $0.00,
    minimumBalance: $500.00,
    currency: "USD"
  },

  availableChannels: ["WEB", "MOBILE", "BRANCH"],

  features: {
    onlineBanking: true,
    mobileBanking: true,
    autoSave: true,
    roundUpSavings: true
  },

  terms: {
    minimumAge: 18,
    termsUrl: "https://bank.com/terms/savings",
    interestCompounding: "MONTHLY"
  }
}
```

**Provisioning Readiness**: ✅ READY
- Has name, category, pricing (interest rate + min balance for SAVINGS), channels, terms
- Will auto-provision to: `temenos-us-east-1` (matches SAVINGS product type)

**Expected Core Product ID**: `T24-{timestamp}-{random}`

---

### Solution 3: `SOL-ACME-001`

```javascript
{
  tenantId: "acme-bank",
  solutionId: "SOL-ACME-001",
  catalogProductId: "cat-checking-premium",
  name: "ACME Premium Checking",
  description: "Premium checking with global ATM access",
  category: "CHECKING",
  status: "ACTIVE",

  pricing: {
    monthlyFee: $25.00,
    minimumBalance: $5,000.00,
    currency: "USD"
  },

  availableChannels: ["WEB", "MOBILE", "BRANCH", "ATM"],

  features: {
    overdraftProtection: true,
    debitCard: true,
    premiumDebitCard: true,
    onlineBanking: true,
    mobileBanking: true,
    internationalAccess: true,
    conciergeService: true
  },

  terms: {
    minimumAge: 21,
    termsUrl: "https://acmebank.com/terms/premium-checking"
  },

  metadata: {
    region: "US-EAST",
    productLine: "Premium"
  }
}
```

**Provisioning Readiness**: ✅ READY
- Has name, category, pricing (monthly fee + min balance), channels, terms
- Will auto-provision to: `temenos-prod-us` (region: US-EAST matches)
- Will NOT provision to: `temenos-prod-eu` (region: EU-WEST doesn't match)

**Expected Core Product ID**: `T24-{timestamp}-{random}`

## 🧪 Test Scenarios

### Test 1: Single Core Provisioning (tenant-001)

**Trigger**: Auto-provision `SOL-TEST-001`

**Expected Flow**:
1. Change stream detects solution
2. Readiness check: ✅ PASS (has all required fields)
3. Router selects: `temenos-us-east-1` (product type CHECKING + region US-EAST)
4. Adapter calls: `POST http://localhost:9190/mock-temenos-api/products`
5. Mock API creates product with ID: `T24-{timestamp}-{random}`
6. Solution updated with:
   ```javascript
   coreProvisioningRecords: [{
     coreSystemId: "temenos-us-east-1",
     coreSystemType: "TEMENOS_T24",
     coreProductId: "T24-1705318800000-456",
     status: "PROVISIONED"
   }]
   ```

### Test 2: Multi-Core Routing (acme-bank)

**Trigger**: Auto-provision `SOL-ACME-001`

**Expected Flow**:
1. Change stream detects solution
2. Readiness check: ✅ PASS
3. Router evaluates:
   - Solution has `metadata.region = "US-EAST"`
   - `temenos-prod-us` has `region = "US-EAST"` ✅ MATCH
   - `temenos-prod-eu` has `region = "EU-WEST"` ❌ NO MATCH
4. Provisions to: `temenos-prod-us` ONLY
5. Solution updated with 1 record (not 2)

### Test 3: Readiness Failure

**Trigger**: Create solution missing pricing

```bash
curl -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "X-Tenant-ID: tenant-001" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Incomplete Checking",
    "category": "CHECKING",
    "availableChannels": ["WEB"]
  }'
```

**Expected Flow**:
1. Change stream detects solution
2. Readiness check: ❌ FAIL
   - Reason: "Pricing configuration missing"
   - Reason: "Terms are missing"
3. No provisioning triggered
4. Logs: "Solution {id} is NOT ready for provisioning"

### Test 4: Configuration Update Sync

**Trigger**: Update `SOL-TEST-001` pricing

```bash
curl -X PUT http://localhost:8082/api/v1/solutions/{id} \
  -d '{"pricing": {"monthlyFee": 20.00}}'
```

**Expected Flow**:
1. Change stream detects UPDATE
2. Listener checks: `coreProvisioningRecords` exists ✅
3. Orchestrator calls: `PUT http://localhost:9190/mock-temenos-api/products/T24-{id}`
4. Mock API updates product
5. Solution's `lastSyncedAt` updated

## 🔍 Verification Queries

### Check if solution was provisioned

```javascript
db.solutions.findOne({ solutionId: "SOL-TEST-001" }).coreProvisioningRecords
```

### Find product in mock core

```javascript
db.mock_core_products.findOne({ catalogSolutionId: "sol-123" })
```

### Count provisioned solutions per tenant

```javascript
db.solutions.aggregate([
  { $match: { "coreProvisioningRecords.0": { $exists: true } } },
  { $group: { _id: "$tenantId", count: { $sum: 1 } } }
])
```

### Get all products in Temenos cores

```javascript
db.mock_core_products.find({ coreSystemType: "TEMENOS_T24" }).count()
```

## 📊 Expected Results After Running Tests

### mock_core_products Collection

After running `test-core-provisioning.sh`:

| Core System ID | Core Type | Product Count | Solution IDs |
|----------------|-----------|---------------|--------------|
| temenos-us-east-1 | TEMENOS_T24 | 2-3 | SOL-TEST-001, SOL-TEST-002, (new test) |
| temenos-prod-us | TEMENOS_T24 | 1 | SOL-ACME-001 |
| finacle-eu-west-1 | FINACLE | 0 | (none - no matching products) |
| fis-us-west-1 | FIS_PROFILE | 0 | (tenant-002 has no pre-loaded solutions) |

### solutions Collection

| Solution ID | Tenant | Status | Provisioning Records |
|-------------|--------|--------|---------------------|
| SOL-TEST-001 | tenant-001 | ACTIVE | 1 record (temenos-us-east-1) |
| SOL-TEST-002 | tenant-001 | ACTIVE | 1 record (temenos-us-east-1) |
| SOL-ACME-001 | acme-bank | ACTIVE | 1 record (temenos-prod-us) |

## 🎯 Quick Commands

### Restart Everything

```bash
# 1. Restart mock APIs
cd infrastructure/mock-core-api && npm start

# 2. Reload test data
cd ../.. && mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
  --file infrastructure/mongodb/init-core-systems.js

# 3. Restart product service
cd backend/product-service && mvn spring-boot:run
```

### Clean Up Test Data

```bash
mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" --eval '
  db.tenant_core_mappings.deleteMany({});
  db.solutions.deleteMany({ solutionId: { $regex: "^SOL-TEST" } });
  db.mock_core_products.deleteMany({});
  print("Test data cleaned up");
'
```

### View Live Provisioning

```bash
# Terminal 1: Watch product service logs
tail -f backend/product-service/logs/application.log | grep -E "(provisioning|change stream)"

# Terminal 2: Watch mock API logs
cd infrastructure/mock-core-api && npm start

# Terminal 3: Create test solutions
curl -X POST http://localhost:8082/api/v1/solutions/configure ...
```

---

**Ready to test?** Run: `./test-core-provisioning.sh`
