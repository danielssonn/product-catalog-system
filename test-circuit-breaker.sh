#!/bin/bash

echo "==================================================================================="
echo "CIRCUIT BREAKER TEST"
echo "==================================================================================="

echo ""
echo "Step 1: Stop workflow service to trigger circuit breaker"
echo "-----------------------------------------------------------------------------------"
docker-compose stop workflow-service
sleep 2

echo ""
echo "Step 2: Submit solution (should trigger circuit breaker)"
echo "-----------------------------------------------------------------------------------"
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-cb-test" \
  -H "X-User-ID: cb@bank.com" \
  -d '{"catalogProductId":"cat-checking-001","solutionName":"Circuit Breaker Test","pricingVariance":10,"riskLevel":"MEDIUM"}' \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n" -s

echo ""
echo "Step 3: Check product service logs for circuit breaker activation"
echo "-----------------------------------------------------------------------------------"
docker logs product-service 2>&1 | grep -i "circuit\|fallback\|timeout" | tail -5

echo ""
echo "Step 4: Restart workflow service"
echo "-----------------------------------------------------------------------------------"
docker-compose up -d workflow-service
sleep 15

echo ""
echo "Step 5: Submit solution again (should work after recovery)"
echo "-----------------------------------------------------------------------------------"
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-recovery-test" \
  -H "X-User-ID: recovery@bank.com" \
  -d '{"catalogProductId":"cat-checking-001","solutionName":"Recovery Test","pricingVariance":10,"riskLevel":"MEDIUM"}' \
  -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n" -s

echo ""
echo "==================================================================================="
echo "CIRCUIT BREAKER TEST COMPLETE"
echo "==================================================================================="
