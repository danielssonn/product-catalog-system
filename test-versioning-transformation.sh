#!/bin/bash

# API Versioning Transformation Test Suite
# Tests comprehensive version transformation scenarios

set -e

BASE_URL="http://localhost:8090"
PRODUCT_SERVICE="product-service"

echo "=========================================="
echo "API Versioning Transformation Test Suite"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to print test results
print_test_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASSED${NC}: $2"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAILED${NC}: $2"
        ((TESTS_FAILED++))
    fi
}

# Helper function to print section headers
print_section() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# Test 1: Check version-service health
print_section "Test 1: Service Health Check"

HEALTH_RESPONSE=$(curl -s http://localhost:8090/actuator/health)
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    print_test_result 0 "Version service is healthy"
else
    print_test_result 1 "Version service is not healthy"
    exit 1
fi

# Test 2: Register API Version v1.0
print_section "Test 2: Register API Version v1.0"

echo "Registering version v1.0 with transformation rules..."

V1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/versions" \
    -H "Content-Type: application/json" \
    -d '{
        "serviceId": "product-service",
        "version": "v1",
        "semanticVersion": "1.0.0",
        "status": "ACTIVE",
        "releasedAt": "2024-01-01T00:00:00Z",
        "newFeatures": [
            "Initial product catalog API",
            "Basic CRUD operations"
        ],
        "contentTypes": ["application/json"],
        "defaultContentType": "application/json",
        "transformations": {
            "v2": {
                "fromVersion": "v1",
                "toVersion": "v2",
                "type": "COMPLEX",
                "fieldMappings": {
                    "productId": "id",
                    "productName": "name",
                    "productType": "type",
                    "price": "pricing.amount",
                    "category": "category.id"
                },
                "fieldTransformations": [
                    {
                        "sourceField": "description",
                        "targetField": "description",
                        "transformFunction": "trim"
                    }
                ],
                "defaultValues": {
                    "pricing.currency": "USD",
                    "pricing.billingCycle": "MONTHLY",
                    "metadata.apiVersion": "v2"
                },
                "fieldsToRemove": []
            }
        }
    }')

if echo "$V1_RESPONSE" | grep -q '"version":"v1"'; then
    print_test_result 0 "Version v1.0 registered successfully"
else
    print_test_result 1 "Failed to register version v1.0"
    echo "Response: $V1_RESPONSE"
fi

# Test 3: Register API Version v2.0
print_section "Test 3: Register API Version v2.0"

echo "Registering version v2.0 with reverse transformation rules..."

V2_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/versions" \
    -H "Content-Type: application/json" \
    -d '{
        "serviceId": "product-service",
        "version": "v2",
        "semanticVersion": "2.0.0",
        "status": "ACTIVE",
        "releasedAt": "2025-01-01T00:00:00Z",
        "newFeatures": [
            "Enhanced pricing structure",
            "Nested category object",
            "API metadata tracking"
        ],
        "breakingChanges": [
            {
                "type": "FIELD_RENAMED",
                "field": "productId",
                "description": "Renamed productId to id",
                "migrationPath": "Use id field instead of productId"
            },
            {
                "type": "FIELD_RENAMED",
                "field": "productName",
                "description": "Renamed productName to name",
                "migrationPath": "Use name field instead of productName"
            },
            {
                "type": "RESPONSE_STRUCTURE_CHANGED",
                "field": "price",
                "description": "Price changed to nested pricing object",
                "migrationPath": "Use pricing.amount for numeric value and pricing.currency for currency code"
            }
        ],
        "contentTypes": ["application/json"],
        "defaultContentType": "application/json",
        "transformations": {
            "v1": {
                "fromVersion": "v2",
                "toVersion": "v1",
                "type": "COMPLEX",
                "fieldMappings": {
                    "id": "productId",
                    "name": "productName",
                    "type": "productType",
                    "pricing.amount": "price",
                    "category.id": "category"
                },
                "fieldTransformations": [],
                "defaultValues": {},
                "fieldsToRemove": ["pricing.currency", "pricing.billingCycle", "metadata"]
            }
        }
    }')

if echo "$V2_RESPONSE" | grep -q '"version":"v2"'; then
    print_test_result 0 "Version v2.0 registered successfully"
else
    print_test_result 1 "Failed to register version v2.0"
    echo "Response: $V2_RESPONSE"
fi

# Test 4: Get all versions
print_section "Test 4: Query Registered Versions"

VERSIONS_RESPONSE=$(curl -s "$BASE_URL/api/v1/versions?serviceId=product-service")

if echo "$VERSIONS_RESPONSE" | grep -q '"version":"v1"' && echo "$VERSIONS_RESPONSE" | grep -q '"version":"v2"'; then
    print_test_result 0 "Retrieved all versions for product-service"
    echo "Versions found: v1, v2"
else
    print_test_result 1 "Failed to retrieve all versions"
    echo "Response: $VERSIONS_RESPONSE"
fi

# Test 5: Transform v1 request to v2 format
print_section "Test 5: Transform Request v1 → v2"

echo "Input (v1 format):"
V1_REQUEST='{
  "productId": "prod-001",
  "productName": "Premium Checking Account",
  "productType": "CHECKING",
  "price": 15.00,
  "category": "deposit-accounts",
  "description": "  Premium checking with high interest  "
}'
echo "$V1_REQUEST"

echo ""
echo "Transforming..."

TRANSFORM_V1_V2=$(curl -s -X POST "$BASE_URL/api/v1/transformations/request?serviceId=product-service&fromVersion=v1&toVersion=v2" \
    -H "Content-Type: application/json" \
    -d "$V1_REQUEST")

echo ""
echo "Output (v2 format):"
echo "$TRANSFORM_V1_V2" | python3 -m json.tool

if echo "$TRANSFORM_V1_V2" | grep -q '"id":"prod-001"' && \
   echo "$TRANSFORM_V1_V2" | grep -q '"name":"Premium Checking Account"' && \
   echo "$TRANSFORM_V1_V2" | grep -q '"pricing"'; then
    print_test_result 0 "v1 → v2 transformation successful"

    # Verify specific transformations
    if echo "$TRANSFORM_V1_V2" | grep -q '"currency":"USD"'; then
        echo -e "  ${GREEN}→${NC} Default currency added: USD"
    fi
    if echo "$TRANSFORM_V1_V2" | grep -q '"billingCycle":"MONTHLY"'; then
        echo -e "  ${GREEN}→${NC} Default billing cycle added: MONTHLY"
    fi
    if echo "$TRANSFORM_V1_V2" | grep -q '"apiVersion":"v2"'; then
        echo -e "  ${GREEN}→${NC} API version metadata added: v2"
    fi
    if echo "$TRANSFORM_V1_V2" | grep -q 'Premium checking with high interest'; then
        echo -e "  ${GREEN}→${NC} Description trimmed (whitespace removed)"
    fi
else
    print_test_result 1 "v1 → v2 transformation failed"
fi

# Test 6: Transform v2 response to v1 format
print_section "Test 6: Transform Response v2 → v1"

echo "Input (v2 format):"
V2_RESPONSE_DATA='{
  "id": "prod-002",
  "name": "High-Yield Savings Account",
  "type": "SAVINGS",
  "pricing": {
    "amount": 0.00,
    "currency": "USD",
    "billingCycle": "MONTHLY"
  },
  "category": {
    "id": "deposit-accounts",
    "name": "Deposit Accounts"
  },
  "metadata": {
    "apiVersion": "v2",
    "createdAt": "2025-01-15T10:30:00Z"
  },
  "description": "High-yield savings with competitive APY"
}'
echo "$V2_RESPONSE_DATA"

echo ""
echo "Transforming..."

TRANSFORM_V2_V1=$(curl -s -X POST "$BASE_URL/api/v1/transformations/response?serviceId=product-service&fromVersion=v2&toVersion=v1" \
    -H "Content-Type: application/json" \
    -d "$V2_RESPONSE_DATA")

echo ""
echo "Output (v1 format):"
echo "$TRANSFORM_V2_V1" | python3 -m json.tool

if echo "$TRANSFORM_V2_V1" | grep -q '"productId":"prod-002"' && \
   echo "$TRANSFORM_V2_V1" | grep -q '"productName":"High-Yield Savings Account"' && \
   echo "$TRANSFORM_V2_V1" | grep -q '"price":0'; then
    print_test_result 0 "v2 → v1 transformation successful"

    # Verify fields removed
    if ! echo "$TRANSFORM_V2_V1" | grep -q '"metadata"'; then
        echo -e "  ${GREEN}→${NC} Metadata removed (not in v1 schema)"
    fi
    if ! echo "$TRANSFORM_V2_V1" | grep -q '"pricing"'; then
        echo -e "  ${GREEN}→${NC} Pricing object flattened to price field"
    fi
else
    print_test_result 1 "v2 → v1 transformation failed"
fi

# Test 7: Batch transformation
print_section "Test 7: Batch Transformation (v1 → v2)"

echo "Batch transforming 3 products..."

BATCH_REQUEST='[
  {
    "productId": "prod-003",
    "productName": "Business Checking",
    "productType": "CHECKING",
    "price": 25.00,
    "category": "business-accounts",
    "description": "Full-featured business checking"
  },
  {
    "productId": "prod-004",
    "productName": "Student Savings",
    "productType": "SAVINGS",
    "price": 0.00,
    "category": "student-accounts",
    "description": "No-fee student savings"
  },
  {
    "productId": "prod-005",
    "productName": "Money Market",
    "productType": "SAVINGS",
    "price": 10.00,
    "category": "premium-accounts",
    "description": "High-interest money market"
  }
]'

BATCH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/transformations/request/batch?serviceId=product-service&fromVersion=v1&toVersion=v2" \
    -H "Content-Type: application/json" \
    -d "$BATCH_REQUEST")

BATCH_COUNT=$(echo "$BATCH_RESPONSE" | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null || echo "0")

if [ "$BATCH_COUNT" -eq 3 ]; then
    print_test_result 0 "Batch transformation processed 3 products"
    echo "$BATCH_RESPONSE" | python3 -m json.tool | head -30
else
    print_test_result 1 "Batch transformation failed (expected 3, got $BATCH_COUNT)"
fi

# Test 8: Transformation validation
print_section "Test 8: Transformation Validation"

echo "Validating transformation rules..."

VALID_TRANSFORM='{
  "fromVersion": "v1",
  "toVersion": "v2",
  "fieldMappings": {
    "productId": "id",
    "productName": "name"
  },
  "fieldTransformations": [
    {
      "sourceField": "price",
      "targetField": "pricing.amount",
      "transformFunction": "toNumber"
    }
  ]
}'

VALIDATE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/transformations/validate" \
    -H "Content-Type: application/json" \
    -d "$VALID_TRANSFORM")

if echo "$VALIDATE_RESPONSE" | grep -q '"valid":true'; then
    print_test_result 0 "Transformation validation passed"
else
    print_test_result 1 "Transformation validation failed"
    echo "Response: $VALIDATE_RESPONSE"
fi

# Test 9: Invalid transformation validation
print_section "Test 9: Invalid Transformation Detection"

echo "Testing invalid transformation (empty source field)..."

INVALID_TRANSFORM='{
  "fromVersion": "v1",
  "toVersion": "v2",
  "fieldMappings": {
    "": "id",
    "productName": ""
  }
}'

VALIDATE_INVALID=$(curl -s -X POST "$BASE_URL/api/v1/transformations/validate" \
    -H "Content-Type: application/json" \
    -d "$INVALID_TRANSFORM")

if echo "$VALIDATE_INVALID" | grep -q '"valid":false' && \
   echo "$VALIDATE_INVALID" | grep -q '"errors"'; then
    print_test_result 0 "Invalid transformation correctly detected"
    echo "Errors found:"
    echo "$VALIDATE_INVALID" | python3 -m json.tool | grep -A 5 '"errors"'
else
    print_test_result 1 "Failed to detect invalid transformation"
fi

# Test 10: Test transformation with sample data
print_section "Test 10: Transformation Testing Endpoint"

echo "Testing transformation with sample data..."

TEST_DATA='{
  "productId": "test-001",
  "productName": "Test Product",
  "productType": "CHECKING",
  "price": 99.99,
  "category": "test-category",
  "description": "Test description"
}'

TEST_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/transformations/test?serviceId=product-service&fromVersion=v1&toVersion=v2" \
    -H "Content-Type: application/json" \
    -d "$TEST_DATA")

if echo "$TEST_RESPONSE" | grep -q '"success":true'; then
    print_test_result 0 "Transformation test successful"
    echo "Test result:"
    echo "$TEST_RESPONSE" | python3 -m json.tool | grep -A 10 '"transformedData"'
else
    print_test_result 1 "Transformation test failed"
    echo "Response: $TEST_RESPONSE"
fi

# Test 11: Get available transformations
print_section "Test 11: Query Available Transformations"

AVAILABLE_TRANSFORMS=$(curl -s "$BASE_URL/api/v1/transformations/available?serviceId=product-service")

if echo "$AVAILABLE_TRANSFORMS" | grep -q '"fromVersion":"v1"' && \
   echo "$AVAILABLE_TRANSFORMS" | grep -q '"toVersion":"v2"'; then
    print_test_result 0 "Retrieved available transformations"
    echo "Available transformations:"
    echo "$AVAILABLE_TRANSFORMS" | python3 -m json.tool
else
    print_test_result 1 "Failed to retrieve available transformations"
fi

# Test 12: Get transformation details
print_section "Test 12: Get Transformation Details"

TRANSFORM_DETAILS=$(curl -s "$BASE_URL/api/v1/transformations/details?serviceId=product-service&fromVersion=v1&toVersion=v2")

if echo "$TRANSFORM_DETAILS" | grep -q '"fieldMappings"' && \
   echo "$TRANSFORM_DETAILS" | grep -q '"fromVersion":"v1"'; then
    print_test_result 0 "Retrieved transformation details"
    echo "Transformation details:"
    echo "$TRANSFORM_DETAILS" | python3 -m json.tool | head -20
else
    print_test_result 1 "Failed to retrieve transformation details"
fi

# Test 13: Round-trip transformation (v1 → v2 → v1)
print_section "Test 13: Round-Trip Transformation (v1 → v2 → v1)"

echo "Testing data integrity through round-trip transformation..."

ORIGINAL_V1='{
  "productId": "prod-999",
  "productName": "Round-Trip Test Product",
  "productType": "SAVINGS",
  "price": 123.45,
  "category": "test-category",
  "description": "Testing round-trip"
}'

echo "Original v1 data:"
echo "$ORIGINAL_V1"

# Transform to v2
INTERMEDIATE_V2=$(curl -s -X POST "$BASE_URL/api/v1/transformations/request?serviceId=product-service&fromVersion=v1&toVersion=v2" \
    -H "Content-Type: application/json" \
    -d "$ORIGINAL_V1")

echo ""
echo "Intermediate v2 data:"
echo "$INTERMEDIATE_V2" | python3 -m json.tool

# Transform back to v1
FINAL_V1=$(curl -s -X POST "$BASE_URL/api/v1/transformations/response?serviceId=product-service&fromVersion=v2&toVersion=v1" \
    -H "Content-Type: application/json" \
    -d "$INTERMEDIATE_V2")

echo ""
echo "Final v1 data (after round-trip):"
echo "$FINAL_V1" | python3 -m json.tool

# Verify core fields match
if echo "$FINAL_V1" | grep -q '"productId":"prod-999"' && \
   echo "$FINAL_V1" | grep -q '"productName":"Round-Trip Test Product"' && \
   echo "$FINAL_V1" | grep -q '"price":123.45'; then
    print_test_result 0 "Round-trip transformation preserved data integrity"
else
    print_test_result 1 "Round-trip transformation data mismatch"
    echo "Expected: productId=prod-999, productName=Round-Trip Test Product, price=123.45"
fi

# Test 14: Nested field transformation
print_section "Test 14: Nested Field Transformation"

echo "Testing nested field support..."

NESTED_V2='{
  "id": "prod-nested",
  "name": "Nested Test",
  "type": "CHECKING",
  "pricing": {
    "amount": 50.00,
    "currency": "USD",
    "billingCycle": "MONTHLY"
  },
  "category": {
    "id": "nested-category",
    "name": "Nested Category",
    "parent": {
      "id": "parent-category",
      "name": "Parent Category"
    }
  }
}'

NESTED_V1=$(curl -s -X POST "$BASE_URL/api/v1/transformations/response?serviceId=product-service&fromVersion=v2&toVersion=v1" \
    -H "Content-Type: application/json" \
    -d "$NESTED_V2")

if echo "$NESTED_V1" | grep -q '"price":50' && \
   echo "$NESTED_V1" | grep -q '"category":"nested-category"'; then
    print_test_result 0 "Nested field transformation successful"
    echo "Nested pricing.amount → price: ✓"
    echo "Nested category.id → category: ✓"
else
    print_test_result 1 "Nested field transformation failed"
fi

# Test 15: Register version v1.5 for chain testing
print_section "Test 15: Register Intermediate Version v1.5"

echo "Registering v1.5 for multi-hop chain testing..."

V15_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/versions" \
    -H "Content-Type: application/json" \
    -d '{
        "serviceId": "product-service",
        "version": "v1.5",
        "semanticVersion": "1.5.0",
        "status": "ACTIVE",
        "releasedAt": "2024-07-01T00:00:00Z",
        "newFeatures": [
            "Added displayName field",
            "Enhanced description formatting"
        ],
        "transformations": {
            "v2": {
                "fromVersion": "v1.5",
                "toVersion": "v2",
                "type": "SIMPLE",
                "fieldMappings": {
                    "productId": "id",
                    "displayName": "name",
                    "productType": "type",
                    "price": "pricing.amount"
                },
                "defaultValues": {
                    "pricing.currency": "USD",
                    "metadata.apiVersion": "v2"
                }
            },
            "v1": {
                "fromVersion": "v1.5",
                "toVersion": "v1",
                "type": "SIMPLE",
                "fieldMappings": {
                    "displayName": "productName"
                },
                "fieldsToRemove": ["enhancedDescription"]
            }
        }
    }')

# Also add v1 → v1.5 transformation
V1_UPDATE=$(curl -s -X PATCH "$BASE_URL/api/v1/versions/product-service/v1" \
    -H "Content-Type: application/json" \
    -d '{
        "transformations": {
            "v2": {
                "fromVersion": "v1",
                "toVersion": "v2",
                "type": "COMPLEX",
                "fieldMappings": {
                    "productId": "id",
                    "productName": "name",
                    "productType": "type",
                    "price": "pricing.amount",
                    "category": "category.id"
                },
                "defaultValues": {
                    "pricing.currency": "USD",
                    "pricing.billingCycle": "MONTHLY",
                    "metadata.apiVersion": "v2"
                }
            },
            "v1.5": {
                "fromVersion": "v1",
                "toVersion": "v1.5",
                "type": "SIMPLE",
                "fieldMappings": {
                    "productName": "displayName"
                },
                "defaultValues": {
                    "enhancedDescription": "Standard product"
                }
            }
        }
    }' 2>/dev/null)

if echo "$V15_RESPONSE" | grep -q '"version":"v1.5"'; then
    print_test_result 0 "Version v1.5 registered successfully"
else
    print_test_result 1 "Failed to register version v1.5"
fi

# Summary
print_section "Test Summary"

TOTAL_TESTS=$((TESTS_PASSED + TESTS_FAILED))
PASS_RATE=$((TESTS_PASSED * 100 / TOTAL_TESTS))

echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
echo -e "Pass Rate: ${YELLOW}${PASS_RATE}%${NC}"

echo ""
if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}╔═══════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   ALL TESTS PASSED SUCCESSFULLY! ✓   ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════╝${NC}"
    exit 0
else
    echo -e "${RED}╔═══════════════════════════════════════╗${NC}"
    echo -e "${RED}║      SOME TESTS FAILED! ✗             ║${NC}"
    echo -e "${RED}╚═══════════════════════════════════════╝${NC}"
    exit 1
fi
