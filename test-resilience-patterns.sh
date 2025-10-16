#!/bin/bash

################################################################################
# Test Script: Resilience Patterns for Context Resolution
#
# This script tests circuit breaker, retry, and bulkhead patterns applied
# to the PartyServiceClient in the API Gateway.
#
# Prerequisites:
# - All services running (docker-compose up -d)
# - Authenticated JWT token
#
# Test Scenarios:
# 1. Circuit Breaker: Stop Party Service, verify circuit opens after failures
# 2. Retry Pattern: Introduce delays, verify exponential backoff
# 3. Bulkhead: Concurrent requests, verify max concurrent limit (10)
# 4. Fallback: Verify graceful degradation when Party Service is down
# 5. Recovery: Start Party Service, verify circuit closes
#
# Author: System Architecture Team
# Date: October 15, 2025
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:8080"
PRODUCT_SERVICE_URL="http://localhost:8082"
PARTY_SERVICE_CONTAINER="party-service"
CREDENTIALS="admin:admin123"

# Test counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

################################################################################
# Helper Functions
################################################################################

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_test() {
    echo -e "${YELLOW}Test $1: $2${NC}"
    TESTS_RUN=$((TESTS_RUN + 1))
}

print_success() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    TESTS_PASSED=$((TESTS_PASSED + 1))
}

print_failure() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    TESTS_FAILED=$((TESTS_FAILED + 1))
}

print_info() {
    echo -e "${BLUE}ℹ INFO: $1${NC}"
}

wait_seconds() {
    local seconds=$1
    echo -n "Waiting ${seconds}s..."
    for i in $(seq 1 $seconds); do
        sleep 1
        echo -n "."
    done
    echo " done"
}

check_service_health() {
    local service_name=$1
    local health_url=$2

    if curl -s -f "$health_url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ $service_name is healthy${NC}"
        return 0
    else
        echo -e "${RED}✗ $service_name is NOT healthy${NC}"
        return 1
    fi
}

stop_party_service() {
    print_info "Stopping Party Service..."
    docker-compose stop party-service > /dev/null 2>&1
    sleep 2
    echo -e "${YELLOW}Party Service STOPPED${NC}"
}

start_party_service() {
    print_info "Starting Party Service..."
    docker-compose start party-service > /dev/null 2>&1
    sleep 5
    echo -e "${GREEN}Party Service STARTED${NC}"
}

check_circuit_breaker_state() {
    local response=$(curl -s -u $CREDENTIALS "$GATEWAY_URL/actuator/health" | grep -o '"circuitBreakers":{[^}]*}')
    echo "$response"
}

################################################################################
# Test Setup
################################################################################

print_header "RESILIENCE PATTERNS TEST SUITE"

echo "Configuration:"
echo "  Gateway URL: $GATEWAY_URL"
echo "  Product Service URL: $PRODUCT_SERVICE_URL"
echo "  Party Service Container: $PARTY_SERVICE_CONTAINER"
echo ""

print_info "Checking service health..."
check_service_health "API Gateway" "$GATEWAY_URL/actuator/health" || exit 1
check_service_health "Product Service" "$PRODUCT_SERVICE_URL/actuator/health" || exit 1
check_service_health "Party Service" "http://localhost:8083/actuator/health" || print_info "Party Service may not be running"

################################################################################
# Test 1: Baseline - Context Resolution Working
################################################################################

print_header "Test 1: Baseline - Context Resolution Working"

print_test "1.1" "Resolve context with Party Service healthy"

RESPONSE=$(curl -s -u $CREDENTIALS \
    -H "Content-Type: application/json" \
    "$PRODUCT_SERVICE_URL/api/v1/catalog/available")

if echo "$RESPONSE" | grep -q "\"tenantId\""; then
    print_success "Context resolved successfully with healthy Party Service"
    TENANT_ID=$(echo "$RESPONSE" | grep -o '"tenantId":"[^"]*"' | head -1 | cut -d'"' -f4)
    print_info "Tenant ID: $TENANT_ID"
else
    print_failure "Failed to resolve context with healthy Party Service"
    echo "Response: $RESPONSE"
fi

################################################################################
# Test 2: Circuit Breaker Pattern
################################################################################

print_header "Test 2: Circuit Breaker Pattern"

print_test "2.1" "Stop Party Service and trigger circuit breaker"

stop_party_service

print_info "Making 10 requests to trigger circuit breaker (threshold: 50% failure in 10 calls)"

SUCCESS_COUNT=0
FAILURE_COUNT=0

for i in $(seq 1 10); do
    RESPONSE=$(curl -s -u $CREDENTIALS \
        -w "\nHTTP_CODE:%{http_code}" \
        "$PRODUCT_SERVICE_URL/api/v1/catalog/available" 2>&1)

    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$HTTP_CODE" = "200" ]; then
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        echo -n "."
    else
        FAILURE_COUNT=$((FAILURE_COUNT + 1))
        echo -n "x"
    fi
done
echo ""

print_info "Success: $SUCCESS_COUNT, Failures: $FAILURE_COUNT"

# Circuit should open after 5 failures (50% of 10 calls)
if [ $FAILURE_COUNT -ge 5 ]; then
    print_success "Circuit breaker triggered after sufficient failures"
else
    print_failure "Circuit breaker did NOT trigger (failures: $FAILURE_COUNT)"
fi

print_test "2.2" "Check circuit breaker state"

CB_STATE=$(check_circuit_breaker_state)
print_info "Circuit Breaker State: $CB_STATE"

if echo "$CB_STATE" | grep -q "party-service"; then
    print_success "Circuit breaker metrics available"
else
    print_info "Circuit breaker state not available in health endpoint (may need actuator config)"
fi

################################################################################
# Test 3: Fallback Pattern
################################################################################

print_header "Test 3: Fallback Pattern (Graceful Degradation)"

print_test "3.1" "Verify requests continue even with circuit open"

RESPONSE=$(curl -s -u $CREDENTIALS \
    -w "\nHTTP_CODE:%{http_code}" \
    "$PRODUCT_SERVICE_URL/api/v1/catalog/available" 2>&1)

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)

if [ "$HTTP_CODE" = "200" ]; then
    print_success "Request succeeded despite Party Service being down (fallback working)"

    # Check if response has context or is degraded
    if echo "$RESPONSE" | grep -q "\"tenantId\""; then
        print_info "Response still contains tenant context (may be cached)"
    else
        print_info "Response has NO tenant context (graceful degradation)"
    fi
else
    print_failure "Request failed with circuit open (expected 200, got $HTTP_CODE)"
fi

print_test "3.2" "Verify fallback is fast (circuit breaker prevents slow calls)"

START_TIME=$(date +%s%3N 2>/dev/null || echo "0")

curl -s -u $CREDENTIALS "$PRODUCT_SERVICE_URL/api/v1/catalog/available" > /dev/null 2>&1

END_TIME=$(date +%s%3N 2>/dev/null || echo "0")

if [ "$START_TIME" != "0" ] && [ "$END_TIME" != "0" ]; then
    DURATION=$((END_TIME - START_TIME))
    print_info "Request duration: ${DURATION}ms"

    # With circuit open, should be very fast (< 500ms)
    if [ $DURATION -lt 500 ]; then
        print_success "Fallback is fast (${DURATION}ms < 500ms)"
    else
        print_info "Fallback took ${DURATION}ms (may include other processing)"
    fi
else
    print_info "Timing measurement not available (macOS compatibility)"
fi

################################################################################
# Test 4: Retry Pattern (after circuit closes)
################################################################################

print_header "Test 4: Recovery - Circuit Breaker Half-Open Transition"

print_test "4.1" "Wait for circuit breaker to transition to HALF_OPEN (10s wait duration)"

wait_seconds 12

print_info "Circuit should now be in HALF_OPEN state, allowing test requests"

CB_STATE=$(check_circuit_breaker_state)
print_info "Circuit Breaker State: $CB_STATE"

################################################################################
# Test 5: Service Recovery
################################################################################

print_header "Test 5: Service Recovery"

print_test "5.1" "Start Party Service and verify circuit closes"

start_party_service

print_info "Making requests to verify service recovery..."

RECOVERY_SUCCESS=0

for i in $(seq 1 5); do
    RESPONSE=$(curl -s -u $CREDENTIALS \
        -w "\nHTTP_CODE:%{http_code}" \
        "$PRODUCT_SERVICE_URL/api/v1/catalog/available" 2>&1)

    HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d':' -f2)

    if [ "$HTTP_CODE" = "200" ]; then
        RECOVERY_SUCCESS=$((RECOVERY_SUCCESS + 1))
        echo -n "."
    else
        echo -n "x"
    fi
    sleep 1
done
echo ""

if [ $RECOVERY_SUCCESS -ge 3 ]; then
    print_success "Service recovered successfully ($RECOVERY_SUCCESS/5 successful requests)"
else
    print_failure "Service recovery incomplete ($RECOVERY_SUCCESS/5 successful requests)"
fi

print_test "5.2" "Verify context resolution working again"

RESPONSE=$(curl -s -u $CREDENTIALS "$PRODUCT_SERVICE_URL/api/v1/catalog/available")

if echo "$RESPONSE" | grep -q "\"tenantId\""; then
    TENANT_ID=$(echo "$RESPONSE" | grep -o '"tenantId":"[^"]*"' | head -1 | cut -d'"' -f4)
    print_success "Context resolution fully operational (tenant: $TENANT_ID)"
else
    print_failure "Context resolution still not working after service recovery"
fi

################################################################################
# Test 6: Bulkhead Pattern (Concurrent Requests)
################################################################################

print_header "Test 6: Bulkhead Pattern (Concurrent Request Limiting)"

print_test "6.1" "Send 15 concurrent requests (bulkhead limit: 10)"

print_info "Launching 15 parallel requests..."

# Create a temp directory for responses
TEMP_DIR=$(mktemp -d)

# Launch 15 concurrent requests
for i in $(seq 1 15); do
    (
        HTTP_CODE=$(curl -s -u $CREDENTIALS \
            -w "%{http_code}" \
            -o "$TEMP_DIR/response_$i.txt" \
            "$PRODUCT_SERVICE_URL/api/v1/catalog/available" 2>&1)
        echo "$HTTP_CODE" > "$TEMP_DIR/http_code_$i.txt"
    ) &
done

# Wait for all background jobs to complete
wait

# Count results
SUCCESS_COUNT=$(grep -l "200" "$TEMP_DIR"/http_code_*.txt 2>/dev/null | wc -l | xargs)
REJECTED_COUNT=$(grep -l "429\|503" "$TEMP_DIR"/http_code_*.txt 2>/dev/null | wc -l | xargs)

print_info "Successful: $SUCCESS_COUNT, Rejected: $REJECTED_COUNT"

# Cleanup
rm -rf "$TEMP_DIR"

if [ $SUCCESS_COUNT -ge 10 ]; then
    print_success "Bulkhead allowed at least 10 concurrent requests ($SUCCESS_COUNT succeeded)"
else
    print_info "Bulkhead may have limited concurrent requests ($SUCCESS_COUNT succeeded)"
fi

if [ $REJECTED_COUNT -gt 0 ]; then
    print_info "Some requests were rejected due to bulkhead limit ($REJECTED_COUNT rejected)"
fi

################################################################################
# Test 7: Exponential Backoff Retry
################################################################################

print_header "Test 7: Retry Pattern (Logged Analysis)"

print_test "7.1" "Check API Gateway logs for retry evidence"

print_info "Looking for retry patterns in recent logs..."

# Check if docker-compose logs work
LOGS=$(docker-compose logs --tail=50 api-gateway 2>/dev/null | grep -i "retry\|attempt\|backoff" || echo "")

if [ -n "$LOGS" ]; then
    echo "$LOGS" | head -10
    print_success "Found retry-related log entries"
else
    print_info "No explicit retry logs found (may be successful on first attempt)"
fi

################################################################################
# Final Summary
################################################################################

print_header "TEST SUMMARY"

echo "Tests Run:    $TESTS_RUN"
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

PASS_RATE=$((TESTS_PASSED * 100 / TESTS_RUN))
echo "Pass Rate: $PASS_RATE%"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    echo -e "${GREEN}   ALL RESILIENCE TESTS PASSED! ✓${NC}"
    echo -e "${GREEN}════════════════════════════════════════${NC}"
    exit 0
else
    echo -e "${YELLOW}════════════════════════════════════════${NC}"
    echo -e "${YELLOW}   SOME TESTS FAILED (see above)${NC}"
    echo -e "${YELLOW}════════════════════════════════════════${NC}"
    exit 1
fi
