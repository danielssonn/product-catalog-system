# API Versioning Test Guide

## Overview

This guide demonstrates the v1 → v2 API evolution in product-service, specifically the breaking change where `customFees` is renamed to `customFeesFX`.

## Breaking Change Summary

| Version | Field Name | Description |
|---------|-----------|-------------|
| **v1** | `customFees` | Original field name for custom fee configuration |
| **v2** | `customFeesFX` | Renamed field for better clarity (FX = Flexible) |

## Setup

### 1. Initialize Version Data

```bash
# Connect to MongoDB
docker exec -it mongodb mongosh -u admin -p admin123 --authenticationDatabase admin

# Switch to productcatalog database
use productcatalog

# Load version initialization script
load('/path/to/init-product-versions.js')

# Or manually copy-paste the init-product-versions.js content
```

### 2. Verify Version Registration

```bash
# Check registered versions
curl -u admin:admin123 http://localhost:8090/api/v1/versions/product-service

# Expected response:
[
  {
    "version": "v2",
    "status": "BETA",
    "releasedAt": "2025-10-02T00:00:00Z",
    "breakingChanges": [...]
  },
  {
    "version": "v1",
    "status": "STABLE",
    "releasedAt": "2025-01-01T00:00:00Z"
  }
]
```

### 3. Check Transformation Rules

```bash
# Get v1 details (includes transformation to v2)
curl -u admin:admin123 http://localhost:8090/api/v1/versions/product-service/v1 | jq .transformations

# Expected:
{
  "v2": {
    "fromVersion": "v1",
    "toVersion": "v2",
    "fieldMappings": {
      "customFees": "customFeesFX"
    },
    "defaultValues": {
      "metadata": {}
    }
  }
}

# Get v2 details (includes transformation to v1)
curl -u admin:admin123 http://localhost:8090/api/v1/versions/product-service/v2 | jq .transformations

# Expected:
{
  "v1": {
    "fromVersion": "v2",
    "toVersion": "v1",
    "fieldMappings": {
      "customFeesFX": "customFees"
    },
    "fieldsToRemove": ["metadata"]
  }
}
```

## Test Scenarios

### Scenario 1: V1 Client (Existing Clients)

**Goal**: Verify existing v1 clients continue to work without changes.

```bash
# V1 Request (using customFees)
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: john.doe@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Premium Business Checking",
    "description": "Customized checking for enterprise",
    "customInterestRate": 2.5,
    "customFees": {
      "monthlyMaintenance": 15.00,
      "overdraft": 35.00,
      "wireTransfer": 25.00
    },
    "pricingVariance": 12,
    "riskLevel": "MEDIUM",
    "businessJustification": "Enterprise customer segment",
    "priority": "HIGH"
  }'

# Expected Response (V1 format):
{
  "solutionId": "550e8400-e29b-41d4-a716-446655440000",
  "solutionName": "Premium Business Checking",
  "status": "DRAFT",
  "workflowId": "workflow-123",
  "workflowStatus": "PENDING_APPROVAL",
  "approvalRequired": true,
  "requiredApprovals": 2,
  "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
  "sequential": false,
  "slaHours": 48,
  "message": "Workflow submitted for approval"
}

# Note: Response does NOT include metadata field (v1 format)
```

### Scenario 2: V2 Client (New Clients)

**Goal**: Verify new v2 clients can use the improved API with `customFeesFX`.

```bash
# V2 Request (using customFeesFX)
curl -u admin:admin123 -X POST http://localhost:8082/api/v2/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: jane.smith@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Premium Business Checking V2",
    "description": "Customized checking for enterprise",
    "customInterestRate": 2.5,
    "customFeesFX": {
      "monthlyMaintenance": 15.00,
      "overdraft": 35.00,
      "wireTransfer": 25.00
    },
    "metadata": {
      "segment": "enterprise",
      "region": "APAC",
      "accountManager": "jane.smith@bank.com"
    },
    "pricingVariance": 12,
    "riskLevel": "MEDIUM",
    "businessJustification": "Enterprise customer segment",
    "priority": "HIGH"
  }'

# Expected Response (V2 format):
{
  "solutionId": "550e8400-e29b-41d4-a716-446655440001",
  "solutionName": "Premium Business Checking V2",
  "status": "DRAFT",
  "workflowId": "workflow-124",
  "workflowStatus": "PENDING_APPROVAL",
  "approvalRequired": true,
  "requiredApprovals": 2,
  "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
  "sequential": false,
  "slaHours": 48,
  "message": "Workflow submitted for approval",
  "metadata": {
    "segment": "enterprise",
    "region": "APAC",
    "accountManager": "jane.smith@bank.com"
  }
}

# Note: Response INCLUDES metadata field (v2 format)
```

### Scenario 3: Schema Transformation (v1 → v2)

**Goal**: Test version service transformation capabilities.

```bash
# Transform v1 request to v2 format
curl -u admin:admin123 -X POST http://localhost:8090/api/v1/transformations/request \
  ?serviceId=product-service&fromVersion=v1&toVersion=v2 \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-001",
    "solutionName": "Test Solution",
    "customFees": {
      "monthlyMaintenance": 10.00,
      "overdraft": 30.00
    },
    "pricingVariance": 5
  }'

# Expected Response (transformed to v2):
{
  "catalogProductId": "cat-001",
  "solutionName": "Test Solution",
  "customFeesFX": {
    "monthlyMaintenance": 10.00,
    "overdraft": 30.00
  },
  "pricingVariance": 5,
  "metadata": {}
}

# Note: customFees → customFeesFX, metadata added
```

### Scenario 4: Schema Transformation (v2 → v1)

**Goal**: Test backward transformation for v1 clients.

```bash
# Transform v2 request to v1 format
curl -u admin:admin123 -X POST http://localhost:8090/api/v1/transformations/request \
  ?serviceId=product-service&fromVersion=v2&toVersion=v1 \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-001",
    "solutionName": "Test Solution",
    "customFeesFX": {
      "monthlyMaintenance": 10.00,
      "overdraft": 30.00
    },
    "pricingVariance": 5,
    "metadata": {
      "segment": "retail"
    }
  }'

# Expected Response (transformed to v1):
{
  "catalogProductId": "cat-001",
  "solutionName": "Test Solution",
  "customFees": {
    "monthlyMaintenance": 10.00,
    "overdraft": 30.00
  },
  "pricingVariance": 5
}

# Note: customFeesFX → customFees, metadata removed
```

### Scenario 5: Version Detection from Headers

**Goal**: Test header-based version detection.

```bash
# Request with X-API-Version header
curl -u admin:admin123 http://localhost:8082/api/solutions/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-API-Version: v2" \
  -H "X-Tenant-ID: tenant-001"

# Version interceptor detects v2 from header
# Response includes v2 fields
```

### Scenario 6: Breaking Change Documentation

**Goal**: View breaking change details for migration planning.

```bash
# Get v2 breaking changes
curl -u admin:admin123 http://localhost:8090/api/v1/versions/product-service/v2 | jq .breakingChanges

# Expected Response:
[
  {
    "type": "FIELD_RENAMED",
    "affectedEndpoint": "/api/v2/solutions/configure",
    "affectedField": "customFees",
    "description": "Field 'customFees' renamed to 'customFeesFX' for better clarity",
    "migrationStrategy": "Update all API clients to use 'customFeesFX' instead of 'customFees'",
    "exampleBefore": "{\n  \"customFees\": {...}\n}",
    "exampleAfter": "{\n  \"customFeesFX\": {...}\n}"
  },
  {
    "type": "REQUIRED_FIELD_ADDED",
    "affectedEndpoint": "/api/v2/solutions/configure",
    "affectedField": "metadata",
    "description": "New optional field 'metadata' added",
    "migrationStrategy": "Add 'metadata' object. Can be empty {}",
    "exampleBefore": "{}",
    "exampleAfter": "{\n  \"metadata\": {...}\n}"
  }
]
```

## Deprecation Workflow (Future)

When v1 needs to be deprecated:

### Step 1: Deprecate v1

```bash
curl -u admin:admin123 -X POST \
  http://localhost:8090/api/v1/versions/product-service/v1/deprecate \
  ?reason=Replaced%20by%20v2%20with%20improved%20field%20naming \
  -H "X-User-ID: admin@bank.com"

# Response:
{
  "version": "v1",
  "status": "DEPRECATED",
  "deprecatedAt": "2025-10-02T00:00:00Z",
  "sunsetAt": "2026-04-02T00:00:00Z",  // +180 days
  "eolAt": null
}
```

### Step 2: V1 Clients Receive Warning Headers

```bash
# V1 client makes request
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: john@bank.com" \
  -d '{...}'

# Response Headers:
HTTP/1.1 201 Created
Warning: 299 - "Deprecated API" Deprecated on: 2025-10-02T00:00:00Z Sunset date: 2026-04-02T00:00:00Z See migration guide: https://docs.bank.com/migration-v1-to-v2
Sunset: 2026-04-02T00:00:00Z
X-API-Version: v1
X-API-Status: DEPRECATED
```

### Step 3: Promote v2 to STABLE

```bash
curl -u admin:admin123 -X PATCH \
  http://localhost:8090/api/v1/versions/product-service/v2/status?status=STABLE \
  -H "X-User-ID: admin@bank.com"

# Response:
{
  "version": "v2",
  "status": "STABLE",
  "releasedAt": "2025-10-02T00:00:00Z"
}
```

### Step 4: Sunset v1 (Read-only period)

```bash
curl -u admin:admin123 -X PATCH \
  http://localhost:8090/api/v1/versions/product-service/v1/status?status=SUNSET \
  -H "X-User-ID: admin@bank.com"

# v1 endpoints become read-only
# Clients can still read data but should migrate ASAP
```

### Step 5: EOL v1 (Requests rejected)

```bash
curl -u admin:admin123 -X PATCH \
  http://localhost:8090/api/v1/versions/product-service/v1/status?status=EOL \
  -H "X-User-ID: admin@bank.com"

# After this, v1 requests return:
HTTP/1.1 410 Gone
X-Error-Message: API version v1 has reached end-of-life
```

## Verification Checklist

- [ ] V1 endpoints work with `customFees` field
- [ ] V2 endpoints work with `customFeesFX` field
- [ ] V1 requests do NOT receive `metadata` in response
- [ ] V2 requests DO receive `metadata` in response
- [ ] Version service can transform v1 → v2 (customFees → customFeesFX)
- [ ] Version service can transform v2 → v1 (customFeesFX → customFees)
- [ ] Breaking changes are documented in version registry
- [ ] Migration guide URL is provided
- [ ] Version status transitions work (BETA → STABLE → DEPRECATED → SUNSET → EOL)
- [ ] Deprecation warnings appear in response headers

## Complete Test Script

```bash
#!/bin/bash

# Test API Versioning for Product Service

echo "=========================================="
echo "API Versioning Test Suite"
echo "=========================================="
echo ""

# Test 1: V1 Endpoint
echo "Test 1: V1 Endpoint (customFees)"
curl -s -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: v1-tester@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "V1 Test Solution",
    "customFees": {
      "monthlyMaintenance": 10.00
    },
    "pricingVariance": 5,
    "riskLevel": "LOW"
  }' | jq .

echo ""
echo "=========================================="
echo ""

# Test 2: V2 Endpoint
echo "Test 2: V2 Endpoint (customFeesFX)"
curl -s -u admin:admin123 -X POST http://localhost:8082/api/v2/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: v2-tester@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "V2 Test Solution",
    "customFeesFX": {
      "monthlyMaintenance": 10.00
    },
    "metadata": {
      "version": "v2-test"
    },
    "pricingVariance": 5,
    "riskLevel": "LOW"
  }' | jq .

echo ""
echo "=========================================="
echo ""

# Test 3: Transform v1 -> v2
echo "Test 3: Transform v1 -> v2"
curl -s -u admin:admin123 -X POST http://localhost:8090/api/v1/transformations/request \
  ?serviceId=product-service&fromVersion=v1&toVersion=v2 \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-001",
    "customFees": {
      "fee1": 10.00
    }
  }' | jq .

echo ""
echo "=========================================="
echo ""

# Test 4: Transform v2 -> v1
echo "Test 4: Transform v2 -> v1"
curl -s -u admin:admin123 -X POST http://localhost:8090/api/v1/transformations/request \
  ?serviceId=product-service&fromVersion=v2&toVersion=v1 \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-001",
    "customFeesFX": {
      "fee1": 10.00
    },
    "metadata": {
      "test": "value"
    }
  }' | jq .

echo ""
echo "=========================================="
echo "Test Suite Complete"
echo "=========================================="
```

## Summary

This test guide demonstrates:

✅ **V1 API** uses `customFees` field (backward compatible)
✅ **V2 API** uses `customFeesFX` field (breaking change)
✅ **Schema transformation** automatically converts between versions
✅ **Version registry** tracks breaking changes and migration guides
✅ **Lifecycle management** supports deprecation workflow
✅ **Zero downtime** - both versions coexist during migration period

Clients can migrate from v1 to v2 at their own pace without service disruption.
