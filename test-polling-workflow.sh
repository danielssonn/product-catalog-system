#!/bin/bash

set -e

echo "========================================"
echo "Async Workflow Polling Test"
echo "========================================"

echo ""
echo "=== Step 1: Create solution (returns immediately with HTTP 202) ==="
RESPONSE=$(curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-polling-test" \
  -H "X-User-ID: tester@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Polling Test Solution",
    "description": "Testing async workflow polling",
    "pricingVariance": 20,
    "riskLevel": "MEDIUM",
    "businessJustification": "Testing polling pattern"
  }' -s -w "\nHTTP_CODE:%{http_code}")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$RESPONSE" | sed '/HTTP_CODE/d')

echo "HTTP Status Code: $HTTP_CODE"
echo "$BODY" | jq '.'

if [ "$HTTP_CODE" != "202" ]; then
  echo "ERROR: Expected HTTP 202 Accepted, got $HTTP_CODE"
  exit 1
fi

SOLUTION_ID=$(echo "$BODY" | jq -r '.solutionId')
POLL_URL=$(echo "$BODY" | jq -r '.pollUrl')
POLL_INTERVAL=$(echo "$BODY" | jq -r '.pollIntervalMs')

echo ""
echo "Solution ID: $SOLUTION_ID"
echo "Poll URL: $POLL_URL"
echo "Poll Interval: ${POLL_INTERVAL}ms"

echo ""
echo "=== Step 2: Poll workflow-status endpoint until SUBMITTED ==="

MAX_ATTEMPTS=10
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    echo ""
    echo "Polling attempt $ATTEMPT/$MAX_ATTEMPTS..."

    sleep $((POLL_INTERVAL / 1000))

    STATUS_RESPONSE=$(curl -u admin:admin123 -s \
        "http://localhost:8082${POLL_URL}" \
        -H "X-Tenant-ID: tenant-polling-test")

    echo "$STATUS_RESPONSE" | jq '.'

    WORKFLOW_STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.workflowSubmissionStatus')
    echo "Current status: $WORKFLOW_STATUS"

    if [ "$WORKFLOW_STATUS" = "SUBMITTED" ]; then
        WORKFLOW_ID=$(echo "$STATUS_RESPONSE" | jq -r '.workflowId')
        echo ""
        echo "✅ SUCCESS: Workflow submitted!"
        echo "Workflow ID: $WORKFLOW_ID"

        APPROVAL_REQUIRED=$(echo "$STATUS_RESPONSE" | jq -r '.approvalRequired')
        REQUIRED_APPROVALS=$(echo "$STATUS_RESPONSE" | jq -r '.requiredApprovals')
        APPROVER_ROLES=$(echo "$STATUS_RESPONSE" | jq -r '.approverRoles[]' | tr '\n' ',' | sed 's/,$//')

        echo "Approval required: $APPROVAL_REQUIRED"
        echo "Required approvals: $REQUIRED_APPROVALS"
        echo "Approver roles: $APPROVER_ROLES"
        break
    fi

    if [ "$WORKFLOW_STATUS" = "SUBMISSION_FAILED" ]; then
        ERROR_MSG=$(echo "$STATUS_RESPONSE" | jq -r '.errorMessage')
        RETRY_AT=$(echo "$STATUS_RESPONSE" | jq -r '.retryAt')
        echo ""
        echo "❌ ERROR: Workflow submission failed!"
        echo "Error: $ERROR_MSG"
        echo "Retry scheduled at: $RETRY_AT"
        exit 1
    fi

    if [ "$WORKFLOW_STATUS" != "PENDING_SUBMISSION" ]; then
        echo "⚠️  WARNING: Unexpected status: $WORKFLOW_STATUS"
    fi
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo ""
    echo "❌ ERROR: Workflow did not submit after $MAX_ATTEMPTS attempts"
    exit 1
fi

echo ""
echo "=== Step 3: Verify workflow exists in workflow-service ==="
curl -u admin:admin123 -s "http://localhost:8089/api/v1/workflows/$WORKFLOW_ID" | jq '{workflowId, state, initiatedBy}'

echo ""
echo "=== Step 4: Approve workflow (first approval) ==="
curl -u admin:admin123 -X POST "http://localhost:8089/api/v1/workflows/$WORKFLOW_ID/approve" \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "alice.pm@bank.com",
    "comments": "Approved via polling test"
  }' -s -w "HTTP %{http_code}\n"

sleep 2

echo ""
echo "=== Step 5: Approve workflow (second approval) ==="
curl -u admin:admin123 -X POST "http://localhost:8089/api/v1/workflows/$WORKFLOW_ID/approve" \
  -H "Content-Type: application/json" \
  -d '{
    "approverId": "bob.risk@bank.com",
    "comments": "Approved via polling test"
  }' -s -w "HTTP %{http_code}\n"

sleep 3

echo ""
echo "=== Step 6: Verify solution is ACTIVE ==="
curl -u admin:admin123 -s "http://localhost:8082/api/v1/solutions/$SOLUTION_ID" \
  -H "X-Tenant-ID: tenant-polling-test" | jq '{id, name, status, workflowId, workflowSubmissionStatus}'

echo ""
echo "========================================"
echo "✅ Polling Test Complete!"
echo "========================================"
