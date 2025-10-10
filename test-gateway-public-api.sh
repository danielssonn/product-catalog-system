#!/bin/bash
# Test script for Public API channel

set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TENANT_ID="tenant-001"
USER_ID="admin@example.com"

echo "========================================="
echo "API Gateway - Public API Channel Tests"
echo "========================================="
echo ""

# Test 1: Health check
echo "Test 1: Health Check"
echo "-------------------"
curl -s "$GATEWAY_URL/actuator/health" | jq '.'
echo ""

# Test 2: List available products
echo "Test 2: List Available Products (via Gateway)"
echo "--------------------------------------------"
curl -s -u admin:admin123 \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  -H "X-Channel: PUBLIC_API" \
  "$GATEWAY_URL/api/v1/catalog/available" | jq '.'
echo ""

# Test 3: Configure product solution
echo "Test 3: Configure Product Solution (via Gateway)"
echo "----------------------------------------------"
curl -s -u admin:admin123 -X POST \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  -H "X-Channel: PUBLIC_API" \
  -H "Content-Type: application/json" \
  "$GATEWAY_URL/api/v1/solutions/configure" \
  -d '{
    "catalogProductId": "cat-savings-001",
    "solutionName": "Premium Savings via Gateway",
    "description": "Test via API Gateway",
    "pricingVariance": 5,
    "riskLevel": "LOW",
    "businessJustification": "Testing gateway routing"
  }' | jq '.'
echo ""

# Test 4: Test rate limiting (rapid requests)
echo "Test 4: Rate Limiting Test"
echo "-------------------------"
echo "Sending 10 rapid requests..."
for i in {1..10}; do
  response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -u admin:admin123 \
    -H "X-Tenant-ID: $TENANT_ID" \
    -H "X-User-ID: $USER_ID" \
    -H "X-Channel: PUBLIC_API" \
    "$GATEWAY_URL/api/v1/catalog/available")
  
  http_code=$(echo "$response" | grep "HTTP_CODE" | cut -d: -f2)
  echo "Request $i: HTTP $http_code"
  
  # Check rate limit headers
  if [ "$i" -eq "1" ]; then
    echo "$response" | head -n 5
  fi
done
echo ""

# Test 5: Test without required headers (should fail)
echo "Test 5: Missing Tenant ID (should fail)"
echo "-------------------------------------"
curl -s -w "\nHTTP_CODE:%{http_code}\n" -u admin:admin123 \
  -H "X-User-ID: $USER_ID" \
  "$GATEWAY_URL/api/v1/catalog/available"
echo ""

# Test 6: Gateway metrics
echo "Test 6: Gateway Metrics"
echo "---------------------"
curl -s "$GATEWAY_URL/actuator/metrics/gateway.requests" | jq '.'
echo ""

echo "========================================="
echo "Public API Channel Tests Complete!"
echo "========================================="
