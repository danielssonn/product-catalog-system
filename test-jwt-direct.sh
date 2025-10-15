#!/bin/bash

# Test JWT TokenController directly (without going through Gateway)
# This helps verify the controller logic works

echo "Testing JWT Token Controller..."
echo "================================"
echo ""

# Try to access the endpoint with verbose output
echo "1. Testing POST /oauth/token endpoint..."
curl -v -X POST http://localhost:8080/oauth/token \
  -H 'Content-Type: application/json' \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }' 2>&1 | grep -E "HTTP|WWW-Auth|{|}"

echo ""
echo "2. Checking if actuator shows the mapping..."
curl -s -u admin:admin123 http://localhost:8080/actuator/mappings 2>&1 | head -20

echo ""
echo "3. Trying with Basic Auth (shouldn't be needed but testing)..."
curl -v -u admin:admin123 -X POST http://localhost:8080/oauth/token \
  -H 'Content-Type: application/json' \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }' 2>&1 | grep -E "HTTP|{|}"
