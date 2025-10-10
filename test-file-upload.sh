#!/bin/bash

set -e

echo "=========================================="
echo "FILE PROCESSING TEST"
echo "=========================================="
echo ""

# Credentials
AUTH="system:system123"
TENANT_ID="tenant-001"
USER_ID="system@example.com"
GATEWAY_URL="http://localhost:8080"

echo "Step 1: Upload CSV file"
echo "----------------------------"
CSV_FILE="test-files/product-configurations.csv"
CSV_RESPONSE=$(curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  -F "file=@$CSV_FILE" \
  -F "format=CSV" \
  "$GATEWAY_URL/channel/host-to-host/files/upload")

echo "$CSV_RESPONSE" | jq .
CSV_FILE_ID=$(echo "$CSV_RESPONSE" | jq -r '.fileId')
echo ""
echo "CSV File ID: $CSV_FILE_ID"
echo ""

echo "Step 2: Upload JSON file"
echo "----------------------------"
JSON_FILE="test-files/product-configurations.json"
JSON_RESPONSE=$(curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  -F "file=@$JSON_FILE" \
  -F "format=JSON" \
  "$GATEWAY_URL/channel/host-to-host/files/upload")

echo "$JSON_RESPONSE" | jq .
JSON_FILE_ID=$(echo "$JSON_RESPONSE" | jq -r '.fileId')
echo ""
echo "JSON File ID: $JSON_FILE_ID"
echo ""

echo "Step 3: Wait for processing (5 seconds)"
echo "----------------------------"
sleep 5
echo ""

echo "Step 4: Check CSV file status"
echo "----------------------------"
curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$CSV_FILE_ID/status" | jq .
echo ""

echo "Step 5: Check JSON file status"
echo "----------------------------"
curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$JSON_FILE_ID/status" | jq .
echo ""

echo "Step 6: Get CSV file results"
echo "----------------------------"
curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$CSV_FILE_ID/results" | jq .
echo ""

echo "Step 7: Get JSON file results"
echo "----------------------------"
curl -s -u "$AUTH" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$JSON_FILE_ID/results" | jq .
echo ""

echo "=========================================="
echo "FILE PROCESSING TEST COMPLETED"
echo "=========================================="
