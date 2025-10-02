#!/bin/bash

echo "==================================================================================="
echo "PERFORMANCE & OPTIMIZATION TEST SUITE"
echo "==================================================================================="

echo ""
echo "Test 1: Async Workflow Submission"
echo "-----------------------------------------------------------------------------------"
time curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-test-1" \
  -H "X-User-ID: test@bank.com" \
  -d '{"catalogProductId":"cat-checking-001","solutionName":"Test 1","pricingVariance":10,"riskLevel":"MEDIUM"}' \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n" -s

echo ""
echo "Test 2: Idempotency Protection"
echo "-----------------------------------------------------------------------------------"
SOLUTION_ID="test-solution-123"
echo "Activating solution twice with same idempotency key..."
curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID/activate" \
  -H "X-Idempotency-Key: test-key-456" -w "\nFirst call - HTTP: %{http_code}\n" -s
curl -u admin:admin123 -X PUT "http://localhost:8082/api/v1/solutions/$SOLUTION_ID/activate" \
  -H "X-Idempotency-Key: test-key-456" -w "\nSecond call (duplicate) - HTTP: %{http_code}\n" -s

echo ""
echo "Test 3: Connection Pooling (10 concurrent requests)"
echo "-----------------------------------------------------------------------------------"
for i in {1..10}; do
  curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: tenant-pool-$i" \
    -H "X-User-ID: pool@bank.com" \
    -d "{\"catalogProductId\":\"cat-checking-001\",\"solutionName\":\"Pool Test $i\",\"pricingVariance\":5,\"riskLevel\":\"LOW\"}" \
    -w "Request $i - Time: %{time_total}s\n" -o /dev/null -s &
done
wait
echo "All concurrent requests completed"

echo ""
echo "==================================================================================="
echo "TEST SUITE COMPLETE"
echo "==================================================================================="
