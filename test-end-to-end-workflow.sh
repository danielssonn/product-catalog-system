#!/bin/bash

set -e

echo "========================================"
echo "End-to-End Workflow Test"
echo "========================================"

echo ""
echo "=== Step 1: Create solution with high variance (requires dual approval) ==="
RESPONSE=$(curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-e2e" \
  -H "X-User-ID: john@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Enterprise Premium Checking E2E Test",
    "description": "End-to-end test with dual approval",
    "pricingVariance": 25,
    "riskLevel": "MEDIUM",
    "businessJustification": "Competitive enterprise pricing required"
  }' -s)

echo "$RESPONSE" | jq '.'
SOLUTION_ID=$(echo "$RESPONSE" | jq -r '.solutionId')
echo ""
echo "Solution ID: $SOLUTION_ID"

echo ""
echo "=== Step 2: Wait for async workflow submission (3 seconds) ==="
sleep 3

echo ""
echo "=== Step 3: Get solution details (should have workflowId populated) ==="
SOLUTION=$(curl -u admin:admin123 -s http://localhost:8082/api/v1/solutions/$SOLUTION_ID \
  -H "X-Tenant-ID: tenant-e2e")
echo "$SOLUTION" | jq '{id, workflowId, status}'

WORKFLOW_ID=$(echo "$SOLUTION" | jq -r '.workflowId')
echo ""
echo "Workflow ID: $WORKFLOW_ID"

if [ "$WORKFLOW_ID" = "null" ]; then
  echo "ERROR: Workflow ID is still null after 3 seconds!"
  exit 1
fi

echo ""
echo "=== Step 4: Check workflow status ==="
WORKFLOW=$(curl -u admin:admin123 -s http://localhost:8089/api/v1/workflows/$WORKFLOW_ID)
echo "$WORKFLOW" | jq '{workflowId, state, initiatedBy, entityId}'

echo ""
echo "=== Step 5: First approval (Product Manager) ==="
curl -u admin:admin123 -X POST http://localhost:8089/api/v1/workflows/$WORKFLOW_ID/approve \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "alice.pm@bank.com",
    "comments": "Pricing is competitive. Approved."
  }' -s -w "HTTP %{http_code}\n"

sleep 2

echo ""
echo "=== Step 6: Second approval (Risk Manager) ==="
curl -u admin:admin123 -X POST http://localhost:8089/api/v1/workflows/$WORKFLOW_ID/approve \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "bob.risk@bank.com",
    "comments": "Risk acceptable. Approved."
  }' -s -w "HTTP %{http_code}\n"

sleep 2

echo ""
echo "=== Step 7: Check final solution status (should be ACTIVE) ==="
curl -u admin:admin123 -s http://localhost:8082/api/v1/solutions/$SOLUTION_ID \
  -H "X-Tenant-ID: tenant-e2e" | jq '{id, name, status, updatedBy, updatedAt}'

echo ""
echo "========================================"
echo "End-to-End Test Complete!"
echo "========================================"
