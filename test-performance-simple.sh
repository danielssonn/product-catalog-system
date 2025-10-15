#!/bin/bash

################################################################################
# Simple Performance Profiling: End-to-End Context Resolution
#
# This script measures performance through the API Gateway (the proper entry point)
# with complete context resolution flow.
#
# Author: System Architecture Team
# Date: October 15, 2025
################################################################################

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration - Use API Gateway as entry point
API_GATEWAY_URL="http://localhost:8080"
PRODUCT_ENDPOINT="$API_GATEWAY_URL/product/api/v1/catalog/available"
PARTY_SERVICE_URL="http://localhost:8083"
CREDENTIALS="admin:admin123"

# Check for gdate
if command -v gdate &> /dev/null; then
    DATE_CMD="gdate"
    HAS_MS=true
else
    DATE_CMD="date"
    HAS_MS=false
    echo -e "${YELLOW}⚠ Install gdate for better precision: brew install coreutils${NC}"
fi

print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

print_timing() {
    printf "%-45s ${YELLOW}%10.2f ms${NC}\n" "$1:" "$2"
}

print_metric() {
    printf "%-45s ${GREEN}%10s${NC} %s\n" "$1:" "$2" "$3"
}

################################################################################
# Test 1: Service Health
################################################################################

print_header "SERVICE HEALTH CHECK"

echo "Checking API Gateway..."
if curl -s -f "$API_GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ API Gateway is healthy${NC}"
else
    echo -e "${RED}✗ API Gateway is NOT healthy${NC}"
    exit 1
fi

echo "Checking Party Service..."
if curl -s -f "$PARTY_SERVICE_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Party Service is healthy${NC}"
else
    echo -e "${YELLOW}⚠ Party Service health check failed (may still work)${NC}"
fi

################################################################################
# Test 2: Cold Start (Cache Miss)
################################################################################

print_header "COLD START PERFORMANCE (Cache Miss)"

echo "Clearing Party Service cache..."
curl -s -X DELETE -u "$CREDENTIALS" \
    "$PARTY_SERVICE_URL/api/v1/context/cache/ind-admin-001" > /dev/null 2>&1 || true
sleep 2

echo ""
echo "Running 5 cold start tests..."
echo ""

COLD_TOTAL=0
COLD_COUNT=0

for i in {1..5}; do
    # Clear cache before each test
    curl -s -X DELETE -u "$CREDENTIALS" \
        "$PARTY_SERVICE_URL/api/v1/context/cache/ind-admin-001" > /dev/null 2>&1 || true
    sleep 1

    echo -e "${CYAN}Test $i/5:${NC}"

    # Measure with curl timing
    TIMING=$(curl -s -u "$CREDENTIALS" \
        -w "time_namelookup:%{time_namelookup}\ntime_connect:%{time_connect}\ntime_pretransfer:%{time_pretransfer}\ntime_starttransfer:%{time_starttransfer}\ntime_total:%{time_total}\nhttp_code:%{http_code}\n" \
        -o /tmp/response_cold_$i.txt \
        "$PRODUCT_ENDPOINT" 2>&1)

    TIME_DNS=$(echo "$TIMING" | grep "time_namelookup" | cut -d':' -f2)
    TIME_CONNECT=$(echo "$TIMING" | grep "time_connect" | cut -d':' -f2)
    TIME_PRETRANSFER=$(echo "$TIMING" | grep "time_pretransfer" | cut -d':' -f2)
    TIME_STARTTRANSFER=$(echo "$TIMING" | grep "time_starttransfer" | cut -d':' -f2)
    TIME_TOTAL=$(echo "$TIMING" | grep "time_total" | cut -d':' -f2)
    HTTP_CODE=$(echo "$TIMING" | grep "http_code" | cut -d':' -f2)

    # Convert to milliseconds
    TIME_DNS_MS=$(echo "scale=2; $TIME_DNS * 1000" | bc)
    TIME_CONNECT_MS=$(echo "scale=2; ($TIME_CONNECT - $TIME_DNS) * 1000" | bc)
    TIME_SSL_MS=$(echo "scale=2; ($TIME_PRETRANSFER - $TIME_CONNECT) * 1000" | bc)
    TIME_PROCESSING_MS=$(echo "scale=2; ($TIME_STARTTRANSFER - $TIME_PRETRANSFER) * 1000" | bc)
    TIME_TRANSFER_MS=$(echo "scale=2; ($TIME_TOTAL - $TIME_STARTTRANSFER) * 1000" | bc)
    TIME_TOTAL_MS=$(echo "scale=2; $TIME_TOTAL * 1000" | bc)

    echo "  DNS Lookup:           ${TIME_DNS_MS} ms"
    echo "  TCP Connect:          ${TIME_CONNECT_MS} ms"
    echo "  SSL Handshake:        ${TIME_SSL_MS} ms"
    echo "  Server Processing:    ${TIME_PROCESSING_MS} ms"
    echo "  Response Transfer:    ${TIME_TRANSFER_MS} ms"
    echo "  ─────────────────────────────────────────"
    echo "  TOTAL:                ${TIME_TOTAL_MS} ms"
    echo "  HTTP Status:          $HTTP_CODE"

    # Check if response has tenant context
    if grep -q "tenantId" /tmp/response_cold_$i.txt 2>/dev/null; then
        TENANT=$(grep -o '"tenantId":"[^"]*"' /tmp/response_cold_$i.txt | head -1 | cut -d'"' -f4)
        echo "  Context Resolved:     ✓ (tenant: $TENANT)"
    else
        echo "  Context Resolved:     ✗ (no tenant)"
    fi

    echo ""

    if [ "$HTTP_CODE" = "200" ]; then
        COLD_TOTAL=$(echo "scale=2; $COLD_TOTAL + $TIME_TOTAL_MS" | bc)
        COLD_COUNT=$((COLD_COUNT + 1))
    fi
done

if [ $COLD_COUNT -gt 0 ]; then
    COLD_AVG=$(echo "scale=2; $COLD_TOTAL / $COLD_COUNT" | bc)
    echo ""
    print_timing "Average Cold Start Time" "$COLD_AVG"
    print_metric "Successful Tests" "$COLD_COUNT" "/ 5"

    if (( $(echo "$COLD_AVG < 2000" | bc -l) )); then
        echo -e "${GREEN}✓ PASS: Cold start < 2000ms target${NC}"
    else
        echo -e "${RED}✗ FAIL: Cold start exceeds 2000ms target${NC}"
    fi
fi

################################################################################
# Test 3: Warm Cache (Cache Hit)
################################################################################

print_header "WARM CACHE PERFORMANCE (Cache Hit)"

echo "Warming up cache with 5 requests..."
for i in {1..5}; do
    curl -s -u "$CREDENTIALS" "$PRODUCT_ENDPOINT" > /dev/null 2>&1
done

sleep 2

echo ""
echo "Running 10 warm cache tests..."
echo ""

WARM_TOTAL=0
WARM_COUNT=0
WARM_MIN=999999
WARM_MAX=0

for i in {1..10}; do
    TIMING=$(curl -s -u "$CREDENTIALS" \
        -w "%{time_total}" \
        -o /tmp/response_warm_$i.txt \
        "$PRODUCT_ENDPOINT" 2>&1)

    TIME_TOTAL_MS=$(echo "scale=2; $TIMING * 1000" | bc)

    echo "  Test $i: ${TIME_TOTAL_MS} ms"

    # Track min/max
    if (( $(echo "$TIME_TOTAL_MS < $WARM_MIN" | bc -l) )); then
        WARM_MIN=$TIME_TOTAL_MS
    fi
    if (( $(echo "$TIME_TOTAL_MS > $WARM_MAX" | bc -l) )); then
        WARM_MAX=$TIME_TOTAL_MS
    fi

    WARM_TOTAL=$(echo "scale=2; $WARM_TOTAL + $TIME_TOTAL_MS" | bc)
    WARM_COUNT=$((WARM_COUNT + 1))
done

if [ $WARM_COUNT -gt 0 ]; then
    WARM_AVG=$(echo "scale=2; $WARM_TOTAL / $WARM_COUNT" | bc)
    echo ""
    print_timing "Average Warm Cache Time" "$WARM_AVG"
    print_timing "Min Response Time" "$WARM_MIN"
    print_timing "Max Response Time" "$WARM_MAX"

    if (( $(echo "$WARM_AVG < 100" | bc -l) )); then
        echo -e "${GREEN}✓ PASS: Warm cache < 100ms target${NC}"
    else
        echo -e "${YELLOW}⚠ WARNING: Warm cache exceeds 100ms target${NC}"
    fi
fi

################################################################################
# Test 4: Component Breakdown
################################################################################

print_header "COMPONENT BREAKDOWN ANALYSIS"

echo ""
echo "End-to-End Flow:"
echo ""
echo "  1. Client → API Gateway"
echo "     • DNS Lookup: ~${TIME_DNS_MS} ms"
echo "     • TCP Connect: ~${TIME_CONNECT_MS} ms"
echo "     • SSL Handshake: ~${TIME_SSL_MS} ms"
echo ""
echo "  2. API Gateway Processing"
echo "     • JWT Validation: ~5-10 ms (estimated)"
echo "     • Context Resolution Filter: ~2-5 ms (estimated)"
echo ""
echo "  3. Party Service Call (Context Resolution)"
echo "     • Cold (Neo4j query): ~100-300 ms"
echo "     • Cached: ~5-20 ms"
echo ""
echo "  4. API Gateway → Product Service"
echo "     • Context Injection: ~1-3 ms (estimated)"
echo "     • Routing: ~5-10 ms (estimated)"
echo ""
echo "  5. Product Service Processing"
echo "     • Context Extraction: ~2-5 ms (estimated)"
echo "     • Business Logic: ~20-50 ms"
echo "     • MongoDB Query: ~20-50 ms"
echo ""

# Test direct Party Service performance
echo "Direct Party Service Timing:"
echo ""

# Cold
curl -s -X DELETE -u "$CREDENTIALS" \
    "$PARTY_SERVICE_URL/api/v1/context/cache/ind-admin-001" > /dev/null 2>&1 || true
sleep 1

PARTY_COLD=$(curl -s -u "$CREDENTIALS" \
    -w "%{time_total}" \
    -o /dev/null \
    "$PARTY_SERVICE_URL/api/v1/context/resolve" 2>&1)
PARTY_COLD_MS=$(echo "scale=2; $PARTY_COLD * 1000" | bc)

# Warm
PARTY_WARM=$(curl -s -u "$CREDENTIALS" \
    -w "%{time_total}" \
    -o /dev/null \
    "$PARTY_SERVICE_URL/api/v1/context/resolve" 2>&1)
PARTY_WARM_MS=$(echo "scale=2; $PARTY_WARM * 1000" | bc)

print_timing "Party Service (Cold)" "$PARTY_COLD_MS"
print_timing "Party Service (Cached)" "$PARTY_WARM_MS"

################################################################################
# Test 5: Concurrent Load
################################################################################

print_header "CONCURRENT LOAD TESTING"

for CONCURRENT in 10 25 50; do
    echo ""
    echo -e "${CYAN}Testing with $CONCURRENT concurrent requests...${NC}"

    # Warm up
    for i in {1..3}; do
        curl -s -u "$CREDENTIALS" "$PRODUCT_ENDPOINT" > /dev/null 2>&1
    done

    # Create temp directory
    TEMP_DIR=$(mktemp -d)

    # Launch concurrent requests
    for i in $(seq 1 $CONCURRENT); do
        (
            TIME_RESULT=$(curl -s -u "$CREDENTIALS" \
                -w "%{time_total}|%{http_code}" \
                -o "$TEMP_DIR/response_$i.txt" \
                "$PRODUCT_ENDPOINT" 2>&1)

            echo "$TIME_RESULT" > "$TEMP_DIR/timing_$i.txt"
        ) &
    done

    # Wait for completion
    wait

    # Analyze results
    SUCCESS_COUNT=0
    TOTAL_TIME=0

    for i in $(seq 1 $CONCURRENT); do
        if [ -f "$TEMP_DIR/timing_$i.txt" ]; then
            TIMING_DATA=$(cat "$TEMP_DIR/timing_$i.txt")
            TIME_TOTAL=$(echo "$TIMING_DATA" | cut -d'|' -f1)
            HTTP_CODE=$(echo "$TIMING_DATA" | cut -d'|' -f2)

            if [ "$HTTP_CODE" = "200" ]; then
                SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
                TIME_MS=$(echo "scale=2; $TIME_TOTAL * 1000" | bc)
                TOTAL_TIME=$(echo "scale=2; $TOTAL_TIME + $TIME_MS" | bc)
            fi
        fi
    done

    if [ $SUCCESS_COUNT -gt 0 ]; then
        AVG_TIME=$(echo "scale=2; $TOTAL_TIME / $SUCCESS_COUNT" | bc)
        print_metric "Concurrent Requests" "$CONCURRENT" ""
        print_metric "Successful" "$SUCCESS_COUNT" "/ $CONCURRENT"
        print_timing "Average Response Time" "$AVG_TIME"
    fi

    # Cleanup
    rm -rf "$TEMP_DIR"
done

################################################################################
# Summary
################################################################################

print_header "PERFORMANCE SUMMARY"

echo ""
echo "End-to-End Performance:"
echo ""
print_timing "Cold Start (Cache Miss)" "$COLD_AVG"
print_timing "Warm Cache (Cache Hit)" "$WARM_AVG"
echo ""

echo "Component Performance:"
echo ""
print_timing "Party Service (Cold)" "$PARTY_COLD_MS"
print_timing "Party Service (Cached)" "$PARTY_WARM_MS"
echo ""

echo "Performance Targets:"
echo ""
if (( $(echo "$COLD_AVG < 2000" | bc -l) )); then
    echo -e "  Cold Start < 2000ms:     ${GREEN}✓ PASS${NC} (${COLD_AVG} ms)"
else
    echo -e "  Cold Start < 2000ms:     ${RED}✗ FAIL${NC} (${COLD_AVG} ms)"
fi

if (( $(echo "$WARM_AVG < 100" | bc -l) )); then
    echo -e "  Warm Cache < 100ms:      ${GREEN}✓ PASS${NC} (${WARM_AVG} ms)"
else
    echo -e "  Warm Cache < 100ms:      ${YELLOW}⚠ WARN${NC} (${WARM_AVG} ms)"
fi

echo ""
echo -e "${GREEN}Performance profiling complete!${NC}"
