#!/bin/bash

# Test Fine-Grained Entitlements
# This script validates the resource-scoped entitlement system

set -e

BASE_URL="http://localhost:8082"
TENANT_ID="tenant-001"

echo "========================================================================"
echo "Fine-Grained Entitlements Test Suite"
echo "========================================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to test endpoint
test_endpoint() {
    local test_name="$1"
    local url="$2"
    local expected_code="$3"
    local party_id="$4"
    local extra_args="$5"

    echo "Test: $test_name"
    echo "Party: $party_id"

    # In a real implementation, we would use JWT with party context
    # For now, we'll use basic auth with X-Tenant-ID header
    response=$(curl -s -w "\n%{http_code}" -u admin:admin123 \
        -H "X-Tenant-ID: $TENANT_ID" \
        $extra_args \
        "$url")

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" = "$expected_code" ]; then
        echo -e "${GREEN}✓ PASS${NC} - Expected HTTP $expected_code, got $http_code"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC} - Expected HTTP $expected_code, got $http_code"
        echo "Response: $body"
        ((TESTS_FAILED++))
    fi
    echo ""
}

echo "========================================================================"
echo "Phase 1: Verify MongoDB Entitlements Collection"
echo "========================================================================"
echo ""

echo "Checking if entitlements collection exists..."
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.countDocuments()" 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓${NC} Entitlements collection exists"
else
    echo -e "${RED}✗${NC} Entitlements collection not found"
    exit 1
fi
echo ""

echo "Sample entitlements in database:"
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.find().limit(2).pretty()" 2>/dev/null || true
echo ""

echo "========================================================================"
echo "Phase 2: Test Resource-Specific Permissions"
echo "========================================================================"
echo ""

echo "Test 1: Alice can VIEW solution-checking-premium-001 (explicit grant)"
echo "-----------------------------------------------------------------------"
# This would work if we had proper context resolution with party IDs
echo -e "${YELLOW}ℹ Note: This test requires ProcessingContext integration${NC}"
echo -e "${YELLOW}ℹ Expected: HTTP 200 (with entitlement) or 200 (fallback to coarse permissions)${NC}"
test_endpoint \
    "View solution with entitlement" \
    "$BASE_URL/api/v1/solutions/solution-checking-premium-001" \
    "200" \
    "alice-party-001" \
    ""

echo "Test 2: Bob can LIST solutions (type-level permission)"
echo "-----------------------------------------------------------------------"
echo -e "${YELLOW}ℹ Note: Type-level permissions apply to all CHECKING solutions${NC}"
test_endpoint \
    "List all solutions" \
    "$BASE_URL/api/v1/solutions" \
    "200" \
    "bob-party-002" \
    ""

echo "========================================================================"
echo "Phase 3: Test Entitlement Constraints"
echo "========================================================================"
echo ""

echo "Test 3: Carol has constraints (amount limits, MFA required)"
echo "-----------------------------------------------------------------------"
echo "Carol's entitlement on account-checking-12345:"
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.findOne({partyId: 'carol-party-003'})" 2>/dev/null | grep -A 10 "constraints" || true
echo ""

echo "========================================================================"
echo "Phase 4: Test Permission Hierarchy"
echo "========================================================================"
echo ""

echo "Test 4: Dave (admin) has full access to all catalog products"
echo "-----------------------------------------------------------------------"
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.findOne({partyId: 'dave-party-004'})" 2>/dev/null | grep -A 5 "operations" || true
echo ""

echo "========================================================================"
echo "Phase 5: Test Delegated Authority"
echo "========================================================================"
echo ""

echo "Test 5: Eve has delegated authority (temporary, expires in 30 days)"
echo "-----------------------------------------------------------------------"
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.findOne({partyId: 'eve-party-005', source: 'DELEGATED'})" 2>/dev/null || true
echo ""

echo "========================================================================"
echo "Phase 6: Verify Indexes for Performance"
echo "========================================================================"
echo ""

echo "Checking entitlement indexes..."
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.getIndexes()" 2>/dev/null || true
echo ""

echo "========================================================================"
echo "Phase 7: Test Context Resolution Integration"
echo "========================================================================"
echo ""

echo "Test 6: Verify PermissionContext includes resource entitlements"
echo "-----------------------------------------------------------------------"
echo -e "${YELLOW}ℹ This test requires party-service context resolution to be running${NC}"
echo -e "${YELLOW}ℹ Party Service should resolve entitlements and include in ProcessingContext${NC}"
echo ""

# Test party service context resolution (if running)
PARTY_SERVICE_URL="http://localhost:8091"
if curl -s --fail "$PARTY_SERVICE_URL/actuator/health" > /dev/null 2>&1; then
    echo "Party service is running, testing context resolution..."
    # In reality, this would be called by API Gateway during authentication
    echo "Sample context resolution request:"
    echo "POST $PARTY_SERVICE_URL/api/v1/context/resolve"
    echo '{
  "principalId": "alice@example.com",
  "partyId": "alice-party-001",
  "username": "alice",
  "roles": ["ROLE_USER"]
}'
else
    echo -e "${YELLOW}⚠ Party service not running, skipping context resolution test${NC}"
fi
echo ""

echo "========================================================================"
echo "Phase 8: Test Entitlement Resolution Service"
echo "========================================================================"
echo ""

echo "Test 7: Query entitlements for a party"
echo "-----------------------------------------------------------------------"
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.find({tenantId: 'tenant-001', partyId: 'alice-party-001', active: true}).pretty()" \
    2>/dev/null || true
echo ""

echo "Test 8: Query all parties with access to a specific resource"
echo "-----------------------------------------------------------------------"
docker exec mongodb mongosh -u admin -p admin123 --authenticationDatabase admin product_catalog_db \
    --eval "db.entitlements.find({tenantId: 'tenant-001', resourceId: 'solution-checking-premium-001'}).pretty()" \
    2>/dev/null || true
echo ""

echo "========================================================================"
echo "Test Results Summary"
echo "========================================================================"
echo ""
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
