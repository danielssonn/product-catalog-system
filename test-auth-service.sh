#!/bin/bash

# Auth Service Testing Script
# Tests all endpoints of the authentication service

set -e

BASE_URL="http://localhost:8097"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "  Auth Service Integration Tests"
echo "========================================"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
HEALTH=$(curl -s "$BASE_URL/actuator/health")
STATUS=$(echo "$HEALTH" | jq -r '.status')
if [ "$STATUS" = "UP" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - Service is UP"
    echo "   MongoDB: $(echo "$HEALTH" | jq -r '.components.mongo.status')"
    echo "   Redis: $(echo "$HEALTH" | jq -r '.components.redis.status')"
else
    echo -e "${RED}❌ FAILED${NC} - Service is DOWN"
    exit 1
fi
echo ""

# Test 2: Token Generation (Login)
echo -e "${YELLOW}Test 2: Token Generation (Login)${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }')

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.refreshToken')
TOKEN_TYPE=$(echo "$LOGIN_RESPONSE" | jq -r '.tokenType')
EXPIRES_IN=$(echo "$LOGIN_RESPONSE" | jq -r '.expiresIn')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - Tokens generated successfully"
    echo "   Token Type: $TOKEN_TYPE"
    echo "   Expires In: $EXPIRES_IN seconds (15 minutes)"
    echo "   Access Token: ${ACCESS_TOKEN:0:50}..."
    echo "   Refresh Token: ${REFRESH_TOKEN:0:50}..."
else
    echo -e "${RED}❌ FAILED${NC} - Failed to generate tokens"
    echo "$LOGIN_RESPONSE" | jq .
    exit 1
fi
echo ""

# Test 3: Token Validation (Decode JWT)
echo -e "${YELLOW}Test 3: Token Validation (JWT Decode)${NC}"
PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d. -f2)
# Add padding if needed
PADDING_LENGTH=$((4 - ${#PAYLOAD} % 4))
if [ $PADDING_LENGTH -ne 4 ]; then
    PAYLOAD="${PAYLOAD}$(printf '=%.0s' $(seq 1 $PADDING_LENGTH))"
fi
DECODED=$(echo "$PAYLOAD" | base64 -d 2>/dev/null | jq . 2>/dev/null || echo "")

if [ -n "$DECODED" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - Token decoded successfully"
    USERNAME=$(echo "$DECODED" | jq -r '.username')
    ROLES=$(echo "$DECODED" | jq -r '.roles | join(", ")')
    TENANT=$(echo "$DECODED" | jq -r '.tenantId')
    CHANNEL=$(echo "$DECODED" | jq -r '.channel')
    TOKEN_TYPE_CLAIM=$(echo "$DECODED" | jq -r '.tokenType')

    echo "   Username: $USERNAME"
    echo "   Roles: $ROLES"
    echo "   Tenant ID: $TENANT"
    echo "   Channel: $CHANNEL"
    echo "   Token Type: $TOKEN_TYPE_CLAIM"
else
    echo -e "${RED}❌ FAILED${NC} - Failed to decode token"
    exit 1
fi
echo ""

# Test 4: Token Refresh
echo -e "${YELLOW}Test 4: Token Refresh${NC}"
sleep 2  # Wait a bit to ensure different iat timestamp
REFRESH_RESPONSE=$(curl -s -X POST "$BASE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d "{
    \"grantType\": \"refresh_token\",
    \"refreshToken\": \"$REFRESH_TOKEN\"
  }")

NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.accessToken')
RETURNED_REFRESH_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r '.refreshToken')

if [ "$NEW_ACCESS_TOKEN" != "null" ] && [ -n "$NEW_ACCESS_TOKEN" ] && [ "$NEW_ACCESS_TOKEN" != "$ACCESS_TOKEN" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - New access token generated"
    echo "   New Access Token: ${NEW_ACCESS_TOKEN:0:50}..."
    echo "   Refresh Token matches: $([ "$RETURNED_REFRESH_TOKEN" = "$REFRESH_TOKEN" ] && echo "Yes" || echo "No")"
else
    echo -e "${RED}❌ FAILED${NC} - Failed to refresh token"
    echo "$REFRESH_RESPONSE" | jq .
    exit 1
fi
echo ""

# Test 5: Token Revocation (Logout)
echo -e "${YELLOW}Test 5: Token Revocation (Logout)${NC}"
REVOKE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/oauth/revoke" \
  -H "Authorization: Bearer $NEW_ACCESS_TOKEN")

if [ "$REVOKE_STATUS" = "200" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - Token revoked successfully"
    echo "   HTTP Status: $REVOKE_STATUS"
else
    echo -e "${RED}❌ FAILED${NC} - Failed to revoke token"
    echo "   HTTP Status: $REVOKE_STATUS"
    exit 1
fi
echo ""

# Test 6: Invalid Credentials
echo -e "${YELLOW}Test 6: Invalid Credentials (Negative Test)${NC}"
INVALID_LOGIN=$(curl -s -X POST "$BASE_URL/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "wrongpassword",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }')

ERROR=$(echo "$INVALID_LOGIN" | jq -r '.error')
if [ "$ERROR" = "invalid_grant" ]; then
    echo -e "${GREEN}✅ PASSED${NC} - Invalid credentials rejected"
    echo "   Error: $ERROR"
    echo "   Description: $(echo "$INVALID_LOGIN" | jq -r '.errorDescription')"
else
    echo -e "${RED}❌ FAILED${NC} - Invalid credentials should be rejected"
    echo "$INVALID_LOGIN" | jq .
    exit 1
fi
echo ""

echo "========================================"
echo -e "${GREEN}  All Tests Passed! ✅${NC}"
echo "========================================"
echo ""
echo "Summary:"
echo "  - Health Check: ✅"
echo "  - Token Generation: ✅"
echo "  - Token Validation: ✅"
echo "  - Token Refresh: ✅"
echo "  - Token Revocation: ✅"
echo "  - Invalid Credentials: ✅"
echo ""
