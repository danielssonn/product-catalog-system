#!/bin/bash

# Test RBAC for Version Service and Workflow Service
# This script tests that:
# 1. ROLE_ADMIN can create versions and workflow templates
# 2. ROLE_USER cannot create versions or workflow templates
# 3. Both roles can query data

set -e

VERSION_SERVICE_URL="http://localhost:8090"
WORKFLOW_SERVICE_URL="http://localhost:8089"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================================="
echo "RBAC Testing for Version and Workflow Services"
echo "=================================================="
echo ""

# Test 1: Admin can query versions
echo -e "${YELLOW}Test 1: Admin querying API versions (should succeed)${NC}"
RESPONSE=$(curl -s -u admin:admin123 -w "\nHTTP_CODE:%{http_code}" \
  "${VERSION_SERVICE_URL}/api/v1/versions/product-service")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ PASS: Admin can query versions (HTTP 200)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 200, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 2: Regular user (catalog-user) can query versions
echo -e "${YELLOW}Test 2: Regular user querying API versions (should succeed)${NC}"
RESPONSE=$(curl -s -u catalog-user:catalog123 -w "\nHTTP_CODE:%{http_code}" \
  "${VERSION_SERVICE_URL}/api/v1/versions/product-service")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "404" ]; then
    echo -e "${GREEN}✓ PASS: Regular user can query versions (HTTP ${HTTP_CODE})${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 200 or 404, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 3: Admin can create new version
echo -e "${YELLOW}Test 3: Admin creating new API version (should succeed)${NC}"
RESPONSE=$(curl -s -u admin:admin123 -X POST -w "\nHTTP_CODE:%{http_code}" \
  -H "Content-Type: application/json" \
  "${VERSION_SERVICE_URL}/api/v1/versions" \
  -d '{
    "serviceId": "test-service",
    "version": "v1.0",
    "status": "STABLE",
    "description": "Test version for RBAC",
    "releaseNotes": "Initial release"
  }')

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "201" ]; then
    echo -e "${GREEN}✓ PASS: Admin can create versions (HTTP 201)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 201, got ${HTTP_CODE}${NC}"
    echo "Response: $(echo "$RESPONSE" | grep -v "HTTP_CODE")"
fi
echo ""

# Test 4: Regular user CANNOT create new version
echo -e "${YELLOW}Test 4: Regular user creating new API version (should fail with 403)${NC}"
RESPONSE=$(curl -s -u catalog-user:catalog123 -X POST -w "\nHTTP_CODE:%{http_code}" \
  -H "Content-Type: application/json" \
  "${VERSION_SERVICE_URL}/api/v1/versions" \
  -d '{
    "serviceId": "test-service",
    "version": "v1.1",
    "status": "STABLE",
    "description": "Unauthorized attempt"
  }')

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}✓ PASS: Regular user blocked from creating versions (HTTP 403)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 403, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 5: Admin can query workflow templates
echo -e "${YELLOW}Test 5: Admin querying workflow templates (should succeed)${NC}"
RESPONSE=$(curl -s -u admin:admin123 -w "\nHTTP_CODE:%{http_code}" \
  "${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ PASS: Admin can query workflow templates (HTTP 200)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 200, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 6: Regular user can query workflow templates
echo -e "${YELLOW}Test 6: Regular user querying workflow templates (should succeed)${NC}"
RESPONSE=$(curl -s -u catalog-user:catalog123 -w "\nHTTP_CODE:%{http_code}" \
  "${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ PASS: Regular user can query workflow templates (HTTP 200)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 200, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 7: Admin can create workflow template
echo -e "${YELLOW}Test 7: Admin creating workflow template (should succeed)${NC}"
RESPONSE=$(curl -s -u admin:admin123 -X POST -w "\nHTTP_CODE:%{http_code}" \
  -H "Content-Type: application/json" \
  "${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates" \
  -d '{
    "templateId": "TEST_RBAC_V1",
    "entityType": "TEST_ENTITY",
    "description": "Test template for RBAC",
    "decisionTables": [{
      "name": "Test Rules",
      "hitPolicy": "FIRST",
      "inputs": [{"name": "testField", "type": "string"}],
      "outputs": [{"name": "approvalRequired", "type": "boolean"}],
      "rules": [{
        "ruleId": "RULE_1",
        "priority": 1,
        "conditions": {"testField": "test"},
        "outputs": {"approvalRequired": true}
      }]
    }],
    "callbackHandlers": {
      "onApprove": "TestHandler",
      "onReject": "TestHandler"
    }
  }')

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "201" ]; then
    echo -e "${GREEN}✓ PASS: Admin can create workflow templates (HTTP 201)${NC}"
elif [ "$HTTP_CODE" == "409" ] || [ "$HTTP_CODE" == "400" ]; then
    echo -e "${YELLOW}⚠ PARTIAL: Template might already exist (HTTP ${HTTP_CODE})${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 201, got ${HTTP_CODE}${NC}"
    echo "Response: $(echo "$RESPONSE" | grep -v "HTTP_CODE")"
fi
echo ""

# Test 8: Regular user CANNOT create workflow template
echo -e "${YELLOW}Test 8: Regular user creating workflow template (should fail with 403)${NC}"
RESPONSE=$(curl -s -u catalog-user:catalog123 -X POST -w "\nHTTP_CODE:%{http_code}" \
  -H "Content-Type: application/json" \
  "${WORKFLOW_SERVICE_URL}/api/v1/workflow-templates" \
  -d '{
    "templateId": "UNAUTHORIZED_TEMPLATE",
    "entityType": "TEST_ENTITY",
    "description": "Unauthorized attempt"
  }')

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "403" ]; then
    echo -e "${GREEN}✓ PASS: Regular user blocked from creating templates (HTTP 403)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 403, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 9: Unauthenticated access should fail
echo -e "${YELLOW}Test 9: Unauthenticated access (should fail with 401)${NC}"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  "${VERSION_SERVICE_URL}/api/v1/versions/product-service")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "401" ]; then
    echo -e "${GREEN}✓ PASS: Unauthenticated access blocked (HTTP 401)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 401, got ${HTTP_CODE}${NC}"
fi
echo ""

# Test 10: Health endpoint should be public
echo -e "${YELLOW}Test 10: Public health endpoint (should succeed without auth)${NC}"
RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  "${VERSION_SERVICE_URL}/actuator/health")

HTTP_CODE=$(echo "$RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" == "200" ]; then
    echo -e "${GREEN}✓ PASS: Health endpoint is public (HTTP 200)${NC}"
else
    echo -e "${RED}✗ FAIL: Expected 200, got ${HTTP_CODE}${NC}"
fi
echo ""

echo "=================================================="
echo "RBAC Testing Complete"
echo "=================================================="
