#!/bin/bash

# Test script for Admin Product Management API
# Tests both Product Type Management and Product Catalog Management

set -e  # Exit on error

BASE_URL="http://localhost:8082"
ADMIN_USER="admin:admin123"
REGULAR_USER="catalog-user:catalog123"

echo "========================================================================"
echo "Admin Product Management API Test Suite"
echo "========================================================================"
echo ""

# Color codes for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Function to print test header
print_test() {
    echo ""
    echo "${BLUE}========================================${NC}"
    echo "${BLUE}$1${NC}"
    echo "${BLUE}========================================${NC}"
}

# Function to check HTTP status
check_status() {
    local expected=$1
    local actual=$2
    local test_name=$3

    if [ "$actual" -eq "$expected" ]; then
        echo "${GREEN}✓ PASS${NC}: $test_name (HTTP $actual)"
        ((TESTS_PASSED++))
        return 0
    else
        echo "${RED}✗ FAIL${NC}: $test_name (Expected HTTP $expected, got $actual)"
        ((TESTS_FAILED++))
        return 1
    fi
}

# ============================================
# PART 1: PRODUCT TYPE MANAGEMENT
# ============================================

print_test "Test 1: Create New Product Type (Admin)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X POST $BASE_URL/api/v1/admin/product-types \
  -H "Content-Type: application/json" \
  -d '{
    "typeCode": "AUTO_LOAN",
    "name": "Auto Loan",
    "description": "Automobile financing loan",
    "category": "LENDING",
    "subcategory": "LOAN",
    "active": true,
    "displayOrder": 13,
    "icon": "directions_car",
    "tags": ["loan", "auto", "vehicle", "secured"],
    "metadata": {
      "regulatoryCategory": "Loan Product",
      "regulation": "Regulation Z (TILA)"
    }
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 201 $HTTP_CODE "Create product type AUTO_LOAN"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 2: Create Duplicate Product Type (Should Fail)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X POST $BASE_URL/api/v1/admin/product-types \
  -H "Content-Type: application/json" \
  -d '{
    "typeCode": "AUTO_LOAN",
    "name": "Duplicate Auto Loan",
    "description": "This should fail",
    "category": "LENDING",
    "subcategory": "LOAN",
    "active": true
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 400 $HTTP_CODE "Reject duplicate product type"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 3: Create Product Type with Invalid Code (Should Fail)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X POST $BASE_URL/api/v1/admin/product-types \
  -H "Content-Type: application/json" \
  -d '{
    "typeCode": "invalid-code",
    "name": "Invalid Code",
    "description": "This should fail due to invalid type code format",
    "category": "OTHER",
    "active": true
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 400 $HTTP_CODE "Reject invalid type code format"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 4: Get Product Type by Code"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER $BASE_URL/api/v1/admin/product-types/CHECKING_ACCOUNT)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get product type CHECKING_ACCOUNT"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 5: Get All Product Types (Paginated)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER "$BASE_URL/api/v1/admin/product-types?page=0&size=5&sort=displayOrder&sortDir=asc")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get all product types with pagination"
echo "$BODY" | jq '.content | length' 2>/dev/null || echo "$BODY"

print_test "Test 6: Get Active Product Types Only"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER $BASE_URL/api/v1/admin/product-types/active)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get all active product types"
echo "$BODY" | jq 'length' 2>/dev/null || echo "$BODY"

print_test "Test 7: Get Product Types by Category"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER "$BASE_URL/api/v1/admin/product-types/active/by-category/ACCOUNT")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get active product types in category ACCOUNT"
echo "$BODY" | jq 'length' 2>/dev/null || echo "$BODY"

print_test "Test 8: Update Product Type"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X PUT $BASE_URL/api/v1/admin/product-types/AUTO_LOAN \
  -H "Content-Type: application/json" \
  -d '{
    "typeCode": "AUTO_LOAN",
    "name": "Auto Loan - Updated",
    "description": "Updated automobile financing loan description",
    "category": "LENDING",
    "subcategory": "LOAN",
    "active": true,
    "displayOrder": 13,
    "icon": "directions_car",
    "tags": ["loan", "auto", "vehicle", "secured", "updated"],
    "metadata": {
      "regulatoryCategory": "Loan Product",
      "regulation": "Regulation Z (TILA)",
      "updated": true
    }
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Update product type AUTO_LOAN"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 9: Check Type Code Availability"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER $BASE_URL/api/v1/admin/product-types/check-availability/NEW_TYPE_CODE)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Check type code availability"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 10: Deactivate Product Type"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X PATCH $BASE_URL/api/v1/admin/product-types/AUTO_LOAN/deactivate)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Deactivate product type AUTO_LOAN"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 11: Reactivate Product Type"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X PATCH $BASE_URL/api/v1/admin/product-types/AUTO_LOAN/reactivate)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Reactivate product type AUTO_LOAN"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 12: Regular User Cannot Access Admin Product Type API"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $REGULAR_USER $BASE_URL/api/v1/admin/product-types)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
check_status 403 $HTTP_CODE "Regular user denied access to admin API"

# ============================================
# PART 2: PRODUCT CATALOG MANAGEMENT
# ============================================

print_test "Test 13: Create New Catalog Product (Admin)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X POST $BASE_URL/api/v1/admin/catalog \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "test-auto-loan-001",
    "name": "Test Auto Loan Product",
    "description": "Test automobile loan with competitive rates",
    "category": "lending",
    "type": "AUTO_LOAN",
    "status": "AVAILABLE",
    "pricingTemplate": {
      "pricingType": "FIXED",
      "currency": "USD",
      "minInterestRate": 3.5,
      "maxInterestRate": 12.0,
      "defaultInterestRate": 5.9
    },
    "availableFeatures": {
      "onlineApplication": true,
      "prequalification": true,
      "autoPayDiscount": true
    },
    "defaultTerms": {
      "termsAndConditionsUrl": "https://example.com/terms/auto-loan",
      "allowedTermMonths": [36, 48, 60, 72],
      "maxLoanAmount": 75000,
      "minLoanAmount": 5000
    },
    "configOptions": {
      "canCustomizeName": true,
      "canCustomizeDescription": true,
      "canCustomizePricing": true,
      "canCustomizeFeatures": true,
      "canCustomizeTerms": true
    },
    "supportedChannels": ["WEB", "MOBILE", "BRANCH"],
    "productTier": "STANDARD",
    "requiresApproval": true
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 201 $HTTP_CODE "Create catalog product test-auto-loan-001"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 14: Get Catalog Product by ID"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER $BASE_URL/api/v1/admin/catalog/test-auto-loan-001)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get catalog product test-auto-loan-001"
echo "$BODY" | jq '{catalogProductId, name, type, status}' 2>/dev/null || echo "$BODY"

print_test "Test 15: Get All Catalog Products (Paginated)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER "$BASE_URL/api/v1/admin/catalog?page=0&size=5&sort=createdAt&sortDir=desc")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get all catalog products with pagination"
echo "$BODY" | jq '{totalElements, size, number}' 2>/dev/null || echo "$BODY"

print_test "Test 16: Get Available Catalog Products"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER $BASE_URL/api/v1/admin/catalog/available)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get available catalog products"
echo "$BODY" | jq 'length' 2>/dev/null || echo "$BODY"

print_test "Test 17: Get Catalog Products by Type"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER "$BASE_URL/api/v1/admin/catalog/by-type/CHECKING_ACCOUNT?page=0&size=10")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get catalog products of type CHECKING_ACCOUNT"
echo "$BODY" | jq '{totalElements, content: (.content | map({catalogProductId, name}))}' 2>/dev/null || echo "$BODY"

print_test "Test 18: Get Catalog Products by Category"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER "$BASE_URL/api/v1/admin/catalog/by-category/checking?page=0&size=10")
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Get catalog products in category checking"
echo "$BODY" | jq '{totalElements}' 2>/dev/null || echo "$BODY"

print_test "Test 19: Update Catalog Product"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X PUT $BASE_URL/api/v1/admin/catalog/test-auto-loan-001 \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "test-auto-loan-001",
    "name": "Test Auto Loan Product - Updated",
    "description": "Updated automobile loan description",
    "category": "lending",
    "type": "AUTO_LOAN",
    "status": "AVAILABLE",
    "pricingTemplate": {
      "pricingType": "FIXED",
      "currency": "USD",
      "minInterestRate": 3.5,
      "maxInterestRate": 12.0,
      "defaultInterestRate": 5.5
    },
    "availableFeatures": {
      "onlineApplication": true,
      "prequalification": true,
      "autoPayDiscount": true,
      "flexiblePayment": true
    },
    "defaultTerms": {
      "termsAndConditionsUrl": "https://example.com/terms/auto-loan",
      "allowedTermMonths": [36, 48, 60, 72, 84],
      "maxLoanAmount": 100000,
      "minLoanAmount": 5000
    },
    "configOptions": {
      "canCustomizeName": true,
      "canCustomizeDescription": true,
      "canCustomizePricing": true,
      "canCustomizeFeatures": true,
      "canCustomizeTerms": true
    },
    "supportedChannels": ["WEB", "MOBILE", "BRANCH", "API"],
    "productTier": "PREMIUM",
    "requiresApproval": true
  }')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Update catalog product test-auto-loan-001"
echo "$BODY" | jq '{catalogProductId, name, productTier}' 2>/dev/null || echo "$BODY"

print_test "Test 20: Bulk Create Catalog Products"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X POST $BASE_URL/api/v1/admin/catalog/bulk \
  -H "Content-Type: application/json" \
  -d '[
    {
      "catalogProductId": "bulk-test-001",
      "name": "Bulk Test Product 1",
      "description": "First bulk test product",
      "category": "test",
      "type": "OTHER",
      "status": "PREVIEW",
      "pricingTemplate": {"pricingType": "FIXED", "currency": "USD"},
      "supportedChannels": ["WEB"],
      "requiresApproval": false
    },
    {
      "catalogProductId": "bulk-test-002",
      "name": "Bulk Test Product 2",
      "description": "Second bulk test product",
      "category": "test",
      "type": "OTHER",
      "status": "PREVIEW",
      "pricingTemplate": {"pricingType": "FIXED", "currency": "USD"},
      "supportedChannels": ["WEB"],
      "requiresApproval": false
    }
  ]')
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Bulk create catalog products"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 21: Regular User Cannot Access Admin Catalog API"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $REGULAR_USER $BASE_URL/api/v1/admin/catalog)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
check_status 403 $HTTP_CODE "Regular user denied access to admin catalog API"

print_test "Test 22: Delete Product Type (Should Fail - Referenced by Catalog)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X DELETE $BASE_URL/api/v1/admin/product-types/AUTO_LOAN)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 409 $HTTP_CODE "Reject delete of product type referenced by catalog"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

print_test "Test 23: Delete Catalog Products (Cleanup)"
curl -s -u $ADMIN_USER -X DELETE $BASE_URL/api/v1/admin/catalog/test-auto-loan-001 > /dev/null 2>&1
curl -s -u $ADMIN_USER -X DELETE $BASE_URL/api/v1/admin/catalog/bulk-test-001 > /dev/null 2>&1
curl -s -u $ADMIN_USER -X DELETE $BASE_URL/api/v1/admin/catalog/bulk-test-002 > /dev/null 2>&1
echo "${GREEN}✓${NC} Cleanup: Deleted test catalog products"

print_test "Test 24: Delete Product Type (Now Should Succeed)"
RESPONSE=$(curl -s -w "\n%{http_code}" -u $ADMIN_USER -X DELETE $BASE_URL/api/v1/admin/product-types/AUTO_LOAN)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')
check_status 200 $HTTP_CODE "Delete product type AUTO_LOAN"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

# ============================================
# TEST SUMMARY
# ============================================

echo ""
echo "========================================================================"
echo "                          TEST SUMMARY"
echo "========================================================================"
echo "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo "${RED}Tests Failed: $TESTS_FAILED${NC}"
echo "Total Tests:  $((TESTS_PASSED + TESTS_FAILED))"
echo "========================================================================"

if [ $TESTS_FAILED -eq 0 ]; then
    echo "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo "${RED}Some tests failed! ✗${NC}"
    exit 1
fi
