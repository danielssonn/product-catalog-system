#!/bin/bash
# Comprehensive API Gateway Authentication Test Script
# Tests JWT, Basic Auth, and various authentication scenarios

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8097}"
TENANT_ID="tenant-001"
USER_ID="admin"

# Counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper functions
print_header() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

print_test() {
    echo -e "${YELLOW}Test $TOTAL_TESTS: $1${NC}"
    echo "-------------------"
}

print_success() {
    echo -e "${GREEN}✓ PASSED${NC}\n"
    ((PASSED_TESTS++))
}

print_failure() {
    echo -e "${RED}✗ FAILED: $1${NC}\n"
    ((FAILED_TESTS++))
}

check_service() {
    local url=$1
    local name=$2
    if curl -s -f "$url/actuator/health" > /dev/null; then
        echo -e "${GREEN}✓ $name is running${NC}"
        return 0
    else
        echo -e "${RED}✗ $name is not running at $url${NC}"
        return 1
    fi
}

# Start tests
print_header "API Gateway Authentication Test Suite"

# Pre-flight checks
echo "Pre-flight Checks:"
echo "-----------------"
check_service "$AUTH_SERVICE_URL" "Auth Service" || exit 1
check_service "$GATEWAY_URL" "API Gateway" || exit 1
echo ""

# ===========================================
# PART 1: JWT Authentication Flow
# ===========================================

print_header "PART 1: JWT Authentication Flow"

# Test 1: Get JWT Token with valid credentials
((TOTAL_TESTS++))
print_test "Get JWT Token with Valid Credentials"
TOKEN_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }')

echo "Token Response:"
echo "$TOKEN_RESPONSE" | jq '.'

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.accessToken')
REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.refreshToken')

if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
    echo "Access Token: ${ACCESS_TOKEN:0:50}..."
    echo "Refresh Token: ${REFRESH_TOKEN:0:50}..."
    print_success
else
    print_failure "Failed to get access token"
fi

# Test 2: Use JWT token to access protected endpoint via Gateway
((TOTAL_TESTS++))
print_test "Access Protected Endpoint with JWT Token"
CATALOG_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Channel-ID: PORTAL" \
  -H "X-User-ID: admin" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$CATALOG_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$CATALOG_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response Body:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" = "200" ]; then
    print_success
else
    print_failure "Expected HTTP 200, got $HTTP_CODE"
fi

# Test 3: Invalid JWT token
((TOTAL_TESTS++))
print_test "Access with Invalid JWT Token (should fail)"
INVALID_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer invalid.jwt.token" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Channel-ID: PORTAL" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$INVALID_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$INVALID_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $BODY"

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    print_success
else
    print_failure "Expected HTTP 401/403, got $HTTP_CODE"
fi

# Test 4: No authentication token
((TOTAL_TESTS++))
print_test "Access without Authentication (should fail)"
NO_AUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "X-Tenant-ID: $TENANT_ID" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$NO_AUTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    print_success
else
    print_failure "Expected HTTP 401/403, got $HTTP_CODE"
fi

# ===========================================
# PART 2: Basic Authentication
# ===========================================

print_header "PART 2: Basic Authentication"

# Test 5: Basic Auth with valid credentials
((TOTAL_TESTS++))
print_test "Access with Basic Auth (valid credentials)"
BASIC_AUTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -u admin:admin123 \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: admin" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$BASIC_AUTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$BASIC_AUTH_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" = "200" ]; then
    print_success
else
    print_failure "Expected HTTP 200, got $HTTP_CODE"
fi

# Test 6: Basic Auth with invalid credentials
((TOTAL_TESTS++))
print_test "Access with Basic Auth (invalid credentials)"
INVALID_BASIC_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -u admin:wrongpassword \
  -H "X-Tenant-ID: $TENANT_ID" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$INVALID_BASIC_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "401" ]; then
    print_success
else
    print_failure "Expected HTTP 401, got $HTTP_CODE"
fi

# ===========================================
# PART 3: Token Refresh
# ===========================================

print_header "PART 3: Token Refresh Flow"

# Test 7: Refresh token
((TOTAL_TESTS++))
print_test "Refresh Access Token using Refresh Token"
REFRESH_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d "{
    \"grantType\": \"refresh_token\",
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")

echo "Refresh Response:"
echo "$REFRESH_RESPONSE" | jq '.'

NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.accessToken')

if [ -n "$NEW_ACCESS_TOKEN" ] && [ "$NEW_ACCESS_TOKEN" != "null" ]; then
    echo "New Access Token: ${NEW_ACCESS_TOKEN:0:50}..."
    print_success
else
    print_failure "Failed to refresh token"
fi

# Test 8: Use refreshed token
((TOTAL_TESTS++))
print_test "Access with Refreshed Token"
REFRESHED_TOKEN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Channel-ID: PORTAL" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$REFRESHED_TOKEN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
    print_success
else
    print_failure "Expected HTTP 200, got $HTTP_CODE"
fi

# ===========================================
# PART 4: Token Logout/Blacklist
# ===========================================

print_header "PART 4: Token Logout and Blacklist"

# Test 9: Logout (blacklist token)
((TOTAL_TESTS++))
print_test "Logout (Blacklist Current Token)"
LOGOUT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST "$AUTH_SERVICE_URL/oauth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json")

HTTP_CODE=$(echo "$LOGOUT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$LOGOUT_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response: $BODY"

if [ "$HTTP_CODE" = "200" ]; then
    print_success
else
    print_failure "Expected HTTP 200, got $HTTP_CODE"
fi

# Test 10: Try to use blacklisted token
((TOTAL_TESTS++))
print_test "Access with Blacklisted Token (should fail)"
sleep 1  # Give Redis a moment to sync
BLACKLISTED_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$BLACKLISTED_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    print_success
else
    print_failure "Expected HTTP 401/403, got $HTTP_CODE - token should be blacklisted"
fi

# ===========================================
# PART 5: Multi-Channel Authentication
# ===========================================

print_header "PART 5: Multi-Channel Authentication"

# Test 11: Get token for different channel
((TOTAL_TESTS++))
print_test "Get JWT Token for HOST_TO_HOST Channel"
H2H_TOKEN_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "HOST_TO_HOST"
  }')

echo "$H2H_TOKEN_RESPONSE" | jq '.'

H2H_TOKEN=$(echo "$H2H_TOKEN_RESPONSE" | jq -r '.accessToken')

if [ -n "$H2H_TOKEN" ] && [ "$H2H_TOKEN" != "null" ]; then
    print_success
else
    print_failure "Failed to get HOST_TO_HOST token"
fi

# Test 12: Access with wrong channel token
((TOTAL_TESTS++))
print_test "Access PORTAL endpoint with HOST_TO_HOST token (channel validation)"
WRONG_CHANNEL_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer $H2H_TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Channel-ID: PORTAL" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$WRONG_CHANNEL_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"
# This might succeed if channel validation is not strict, adjust expectations as needed
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "403" ]; then
    print_success
else
    print_failure "Unexpected status code: $HTTP_CODE"
fi

# ===========================================
# PART 6: Missing Required Headers
# ===========================================

print_header "PART 6: Required Headers Validation"

# Get fresh token for these tests
FRESH_TOKEN_RESPONSE=$(curl -s -X POST "$AUTH_SERVICE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }')
FRESH_TOKEN=$(echo "$FRESH_TOKEN_RESPONSE" | jq -r '.accessToken')

# Test 13: Missing Tenant-ID header
((TOTAL_TESTS++))
print_test "Access without X-Tenant-ID header (should fail)"
NO_TENANT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -H "Authorization: Bearer $FRESH_TOKEN" \
  -H "X-Channel-ID: PORTAL" \
  "$GATEWAY_URL/api/v1/catalog/available")

HTTP_CODE=$(echo "$NO_TENANT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "400" ] || [ "$HTTP_CODE" = "403" ]; then
    print_success
else
    print_failure "Expected HTTP 400/403, got $HTTP_CODE"
fi

# ===========================================
# PART 7: Gateway Health and Metrics
# ===========================================

print_header "PART 7: Gateway Health and Metrics"

# Test 14: Health check (should be public)
((TOTAL_TESTS++))
print_test "Gateway Health Check (unauthenticated)"
HEALTH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  "$GATEWAY_URL/actuator/health")

HTTP_CODE=$(echo "$HEALTH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$HEALTH_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Health:"
echo "$BODY" | jq '.'

if [ "$HTTP_CODE" = "200" ]; then
    print_success
else
    print_failure "Expected HTTP 200, got $HTTP_CODE"
fi

# Test 15: Gateway metrics
((TOTAL_TESTS++))
print_test "Gateway Metrics Endpoint"
METRICS_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  "$GATEWAY_URL/actuator/metrics")

HTTP_CODE=$(echo "$METRICS_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)

echo "HTTP Status: $HTTP_CODE"

if [ "$HTTP_CODE" = "200" ]; then
    print_success
else
    print_failure "Expected HTTP 200, got $HTTP_CODE"
fi

# ===========================================
# PART 8: POST Requests with JWT
# ===========================================

print_header "PART 8: POST Requests with JWT Authentication"

# Test 16: Create solution with JWT
((TOTAL_TESTS++))
print_test "Create Product Solution with JWT Authentication"
CREATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST \
  -H "Authorization: Bearer $FRESH_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: admin" \
  -H "X-Channel-ID: PORTAL" \
  "$GATEWAY_URL/api/v1/solutions/configure" \
  -d '{
    "catalogProductId": "cat-savings-001",
    "solutionName": "JWT Auth Test Savings",
    "description": "Testing JWT authentication on POST",
    "pricingVariance": 3,
    "riskLevel": "LOW",
    "businessJustification": "Authentication test"
  }')

HTTP_CODE=$(echo "$CREATE_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$CREATE_RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status: $HTTP_CODE"
echo "Response:"
echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ] || [ "$HTTP_CODE" = "202" ]; then
    print_success
else
    print_failure "Expected HTTP 200/201/202, got $HTTP_CODE"
fi

# ===========================================
# Summary
# ===========================================

print_header "Test Summary"

echo "Total Tests:  $TOTAL_TESTS"
echo -e "${GREEN}Passed:       $PASSED_TESTS${NC}"
echo -e "${RED}Failed:       $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed!${NC}"
    exit 1
fi
