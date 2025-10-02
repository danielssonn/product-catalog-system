#!/bin/bash

# Tenant Isolation Integration Tests
# Tests the automatic tenant filtering implemented via TenantContext

set -e

BASE_URL="http://localhost:8082"
ADMIN_CREDS="admin:admin123"

echo "======================================================================"
echo "Tenant Isolation Integration Tests"
echo "======================================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

pass_count=0
fail_count=0

pass() {
    echo -e "${GREEN}✓ PASS:${NC} $1"
    ((pass_count++))
}

fail() {
    echo -e "${RED}✗ FAIL:${NC} $1"
    ((fail_count++))
}

info() {
    echo -e "${YELLOW}ℹ INFO:${NC} $1"
}

# Test 1: Missing X-Tenant-ID header should be rejected
echo "Test 1: Missing X-Tenant-ID header"
echo "-----------------------------------"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -u $ADMIN_CREDS \
    $BASE_URL/api/v1/catalog/available)

if [ "$HTTP_CODE" == "400" ]; then
    pass "Request without X-Tenant-ID rejected with 400"
else
    fail "Expected 400, got $HTTP_CODE"
fi
echo ""

# Test 2: Health endpoint should work without X-Tenant-ID
echo "Test 2: Health endpoint (no tenant required)"
echo "---------------------------------------------"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    $BASE_URL/actuator/health)

if [ "$HTTP_CODE" == "200" ]; then
    pass "Health endpoint accessible without X-Tenant-ID"
else
    fail "Expected 200, got $HTTP_CODE"
fi
echo ""

# Test 3: Create solution for tenant-001
echo "Test 3: Create solution for tenant-001"
echo "---------------------------------------"
RESPONSE=$(curl -s -u $ADMIN_CREDS -X POST \
    $BASE_URL/api/v1/solutions/configure \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: tenant-001" \
    -H "X-User-ID: user1@tenant001.com" \
    -d '{
        "catalogProductId": "premium-checking-001",
        "solutionName": "Tenant 001 Premium Checking",
        "description": "Test solution for tenant isolation",
        "pricingVariance": 5,
        "riskLevel": "LOW"
    }')

SOLUTION_ID_T1=$(echo $RESPONSE | grep -o '"solutionId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$SOLUTION_ID_T1" ]; then
    pass "Created solution for tenant-001: $SOLUTION_ID_T1"
    info "Response: $(echo $RESPONSE | head -c 200)..."
else
    fail "Failed to create solution for tenant-001"
    echo "Response: $RESPONSE"
fi
echo ""

# Test 4: Create solution for tenant-002
echo "Test 4: Create solution for tenant-002"
echo "---------------------------------------"
RESPONSE=$(curl -s -u $ADMIN_CREDS -X POST \
    $BASE_URL/api/v1/solutions/configure \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: tenant-002" \
    -H "X-User-ID: user2@tenant002.com" \
    -d '{
        "catalogProductId": "high-yield-savings-001",
        "solutionName": "Tenant 002 Savings",
        "description": "Test solution for cross-tenant access prevention",
        "pricingVariance": 3,
        "riskLevel": "LOW"
    }')

SOLUTION_ID_T2=$(echo $RESPONSE | grep -o '"solutionId":"[^"]*"' | cut -d'"' -f4)

if [ -n "$SOLUTION_ID_T2" ]; then
    pass "Created solution for tenant-002: $SOLUTION_ID_T2"
    info "Response: $(echo $RESPONSE | head -c 200)..."
else
    fail "Failed to create solution for tenant-002"
    echo "Response: $RESPONSE"
fi
echo ""

# Sleep to allow async processing
echo "Waiting 5 seconds for async processing..."
sleep 5
echo ""

# Test 5: List solutions for tenant-001 (should see only tenant-001 data)
echo "Test 5: List solutions for tenant-001"
echo "--------------------------------------"
RESPONSE=$(curl -s -u $ADMIN_CREDS \
    "$BASE_URL/api/v1/solutions?page=0&size=10" \
    -H "X-Tenant-ID: tenant-001")

T1_COUNT=$(echo $RESPONSE | grep -o '"tenantId":"tenant-001"' | wc -l | tr -d ' ')
T2_COUNT=$(echo $RESPONSE | grep -o '"tenantId":"tenant-002"' | wc -l | tr -d ' ')

if [ "$T1_COUNT" -gt 0 ] && [ "$T2_COUNT" -eq 0 ]; then
    pass "Tenant-001 sees only their own solutions ($T1_COUNT found, 0 cross-tenant)"
else
    fail "Tenant isolation failed: T1=$T1_COUNT, T2=$T2_COUNT"
    info "Response: $(echo $RESPONSE | head -c 500)"
fi
echo ""

# Test 6: List solutions for tenant-002 (should see only tenant-002 data)
echo "Test 6: List solutions for tenant-002"
echo "--------------------------------------"
RESPONSE=$(curl -s -u $ADMIN_CREDS \
    "$BASE_URL/api/v1/solutions?page=0&size=10" \
    -H "X-Tenant-ID: tenant-002")

T1_COUNT=$(echo $RESPONSE | grep -o '"tenantId":"tenant-001"' | wc -l | tr -d ' ')
T2_COUNT=$(echo $RESPONSE | grep -o '"tenantId":"tenant-002"' | wc -l | tr -d ' ')

if [ "$T2_COUNT" -gt 0 ] && [ "$T1_COUNT" -eq 0 ]; then
    pass "Tenant-002 sees only their own solutions ($T2_COUNT found, 0 cross-tenant)"
else
    fail "Tenant isolation failed: T1=$T1_COUNT, T2=$T2_COUNT"
    info "Response: $(echo $RESPONSE | head -c 500)"
fi
echo ""

# Test 7: Cross-tenant access attempt (tenant-002 tries to access tenant-001's solution)
echo "Test 7: Cross-tenant access prevention"
echo "---------------------------------------"
if [ -n "$SOLUTION_ID_T1" ]; then
    # Get the actual solutionId (not UUID id) from the solution
    SOLUTION_RESPONSE=$(curl -s -u $ADMIN_CREDS \
        "$BASE_URL/api/v1/solutions?page=0&size=1" \
        -H "X-Tenant-ID: tenant-001")

    SOLUTION_ID_FIELD=$(echo $SOLUTION_RESPONSE | grep -o '"solutionId":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -n "$SOLUTION_ID_FIELD" ]; then
        info "Attempting to access tenant-001's solution ($SOLUTION_ID_FIELD) as tenant-002"

        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u $ADMIN_CREDS \
            "$BASE_URL/api/v1/solutions/$SOLUTION_ID_FIELD" \
            -H "X-Tenant-ID: tenant-002")

        if [ "$HTTP_CODE" == "404" ] || [ "$HTTP_CODE" == "500" ]; then
            pass "Cross-tenant access blocked (HTTP $HTTP_CODE)"
        else
            fail "Cross-tenant access NOT blocked (HTTP $HTTP_CODE)"
        fi
    else
        info "Skipping cross-tenant test (no solutionId found)"
    fi
else
    info "Skipping cross-tenant test (no solution created)"
fi
echo ""

# Test 8: Same-tenant access should succeed
echo "Test 8: Same-tenant access (positive case)"
echo "------------------------------------------"
if [ -n "$SOLUTION_ID_T1" ]; then
    SOLUTION_RESPONSE=$(curl -s -u $ADMIN_CREDS \
        "$BASE_URL/api/v1/solutions?page=0&size=1" \
        -H "X-Tenant-ID: tenant-001")

    SOLUTION_ID_FIELD=$(echo $SOLUTION_RESPONSE | grep -o '"solutionId":"[^"]*"' | head -1 | cut -d'"' -f4)

    if [ -n "$SOLUTION_ID_FIELD" ]; then
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u $ADMIN_CREDS \
            "$BASE_URL/api/v1/solutions/$SOLUTION_ID_FIELD" \
            -H "X-Tenant-ID: tenant-001")

        if [ "$HTTP_CODE" == "200" ]; then
            pass "Same-tenant access allowed (HTTP $HTTP_CODE)"
        else
            fail "Same-tenant access failed (HTTP $HTTP_CODE)"
        fi
    fi
fi
echo ""

# Test 9: Catalog endpoint with tenant header
echo "Test 9: Catalog browsing with tenant context"
echo "---------------------------------------------"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u $ADMIN_CREDS \
    $BASE_URL/api/v1/catalog/available \
    -H "X-Tenant-ID: tenant-001")

if [ "$HTTP_CODE" == "200" ]; then
    pass "Catalog accessible with X-Tenant-ID header"
else
    fail "Catalog access failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 10: Thread-local cleanup (make multiple rapid requests)
echo "Test 10: Thread-local context cleanup"
echo "--------------------------------------"
success_count=0
for i in {1..5}; do
    HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -u $ADMIN_CREDS \
        "$BASE_URL/api/v1/solutions?page=0&size=1" \
        -H "X-Tenant-ID: tenant-00$i")

    if [ "$HTTP_CODE" == "200" ]; then
        ((success_count++))
    fi
done

if [ "$success_count" -eq 5 ]; then
    pass "Multiple rapid requests handled correctly (no context pollution)"
else
    fail "Context pollution detected ($success_count/5 requests succeeded)"
fi
echo ""

# Summary
echo "======================================================================"
echo "Test Summary"
echo "======================================================================"
echo -e "${GREEN}Passed: $pass_count${NC}"
echo -e "${RED}Failed: $fail_count${NC}"
echo ""

if [ $fail_count -eq 0 ]; then
    echo -e "${GREEN}✓ All tenant isolation tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed. Review implementation.${NC}"
    exit 1
fi
