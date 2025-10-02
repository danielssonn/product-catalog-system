#!/bin/bash

echo "==================================================================================="
echo "IDEMPOTENCY PROTECTION TEST"
echo "==================================================================================="

echo ""
echo "Step 1: Create a solution"
echo "-----------------------------------------------------------------------------------"
RESPONSE=$(curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-idemp-test" \
  -H "X-User-ID: idemp@bank.com" \
  -d '{"catalogProductId":"cat-checking-001","solutionName":"Idempotency Test","pricingVariance":10,"riskLevel":"MEDIUM"}' -s)

SOLUTION_ID=$(echo $RESPONSE | grep -o '"solutionId":"[^"]*"' | cut -d'"' -f4)
echo "Created solution: $SOLUTION_ID"

echo ""
echo "Step 2: Wait for async workflow submission to complete"
echo "-----------------------------------------------------------------------------------"
sleep 3

echo ""
echo "Step 3: Activate solution with idempotency key (first call)"
echo "-----------------------------------------------------------------------------------"
curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID/activate" \
  -H "X-Idempotency-Key: test-key-$(date +%s)" \
  -w "\nHTTP Status: %{http_code}\n" -s

echo ""
echo "Step 4: Activate same solution with SAME idempotency key (duplicate - should succeed)"
echo "-----------------------------------------------------------------------------------"
IDEMP_KEY="duplicate-test-$(date +%s)"
curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID/activate" \
  -H "X-Idempotency-Key: $IDEMP_KEY" \
  -w "\nFirst call - HTTP: %{http_code}\n" -s

curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID/activate" \
  -H "X-Idempotency-Key: $IDEMP_KEY" \
  -w "\nSecond call (duplicate) - HTTP: %{http_code}\n" -s

echo ""
echo "Step 5: Verify solution status"
echo "-----------------------------------------------------------------------------------"
curl -u admin:admin123 "http://localhost:8082/api/v1/solutions/$SOLUTION_ID" \
  -H "X-Tenant-ID: tenant-idemp-test" -s | grep -o '"status":"[^"]*"'

echo ""
echo "Step 6: Test rejection idempotency"
echo "-----------------------------------------------------------------------------------"
# Create another solution
RESPONSE2=$(curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-idemp-test-2" \
  -H "X-User-ID: idemp2@bank.com" \
  -d '{"catalogProductId":"cat-savings-001","solutionName":"Reject Test","pricingVariance":5,"riskLevel":"LOW"}' -s)

SOLUTION_ID_2=$(echo $RESPONSE2 | grep -o '"solutionId":"[^"]*"' | cut -d'"' -f4)
echo "Created second solution: $SOLUTION_ID_2"
sleep 2

REJECT_KEY="reject-test-$(date +%s)"
curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID_2/reject" \
  -H "X-Idempotency-Key: $REJECT_KEY" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Testing idempotency"}' \
  -w "\nFirst reject - HTTP: %{http_code}\n" -s

curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID_2/reject" \
  -H "X-Idempotency-Key: $REJECT_KEY" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Testing idempotency"}' \
  -w "\nSecond reject (duplicate) - HTTP: %{http_code}\n" -s

echo ""
echo "==================================================================================="
echo "IDEMPOTENCY TEST COMPLETE"
echo "==================================================================================="
