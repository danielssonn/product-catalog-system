#!/bin/bash

# Get JWT token
TOKEN_RESPONSE=$(curl -s -X POST "http://localhost:8097/oauth/token" \
  -H "Content-Type: application/json" \
  -d '{
    "grantType": "password",
    "username": "admin",
    "password": "admin123",
    "tenantId": "tenant-001",
    "channel": "PORTAL"
  }')

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.accessToken')

if [ "$ACCESS_TOKEN" != "null" ] && [ -n "$ACCESS_TOKEN" ]; then
    echo "$ACCESS_TOKEN"
else
    echo "Error getting token:" >&2
    echo "$TOKEN_RESPONSE" | jq . >&2
    exit 1
fi
