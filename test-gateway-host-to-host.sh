#!/bin/bash
# Test script for Host-to-Host file processing channel

set -e

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
TENANT_ID="tenant-001"
USER_ID="system@example.com"

echo "=============================================="
echo "API Gateway - Host-to-Host Channel Tests"
echo "=============================================="
echo ""

# Create test CSV file
echo "Creating test CSV file..."
cat > /tmp/test-products.csv << 'CSV_EOF'
catalogProductId,solutionName,pricingVariance,riskLevel,businessJustification
cat-savings-001,Automated Savings Product 1,3,LOW,Batch upload test
cat-checking-001,Automated Checking Product 1,5,MEDIUM,Batch upload test
cat-savings-001,Automated Savings Product 2,4,LOW,Batch upload test
CSV_EOF

echo "Test file created: /tmp/test-products.csv"
echo ""

# Test 1: Upload file
echo "Test 1: Upload File for Processing"
echo "---------------------------------"
FILE_RESPONSE=$(curl -s -u admin:admin123 -X POST \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  -H "X-File-Format: CSV" \
  -H "X-Callback-URL: https://webhook.site/test-callback" \
  -F "file=@/tmp/test-products.csv" \
  "$GATEWAY_URL/channel/host-to-host/files/upload")

echo "$FILE_RESPONSE" | jq '.'
FILE_ID=$(echo "$FILE_RESPONSE" | jq -r '.fileId')
echo ""
echo "File ID: $FILE_ID"
echo ""

# Test 2: Check file status
echo "Test 2: Check File Processing Status"
echo "-----------------------------------"
curl -s -u admin:admin123 \
  -H "X-Tenant-ID: $TENANT_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$FILE_ID/status" | jq '.'
echo ""

# Test 3: Wait a bit and check status again
echo "Test 3: Check Status After 2 Seconds"
echo "-----------------------------------"
sleep 2
curl -s -u admin:admin123 \
  -H "X-Tenant-ID: $TENANT_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$FILE_ID/status" | jq '.'
echo ""

# Test 4: Get file results
echo "Test 4: Get File Processing Results"
echo "----------------------------------"
curl -s -u admin:admin123 \
  -H "X-Tenant-ID: $TENANT_ID" \
  "$GATEWAY_URL/channel/host-to-host/files/$FILE_ID/results" | jq '.'
echo ""

# Test 5: Test different file format
echo "Test 5: Upload ISO20022 XML File"
echo "-------------------------------"
cat > /tmp/test-iso20022.xml << 'XML_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pain.001.001.03">
  <CstmrCdtTrfInitn>
    <GrpHdr>
      <MsgId>MSG123</MsgId>
      <CreDtTm>2025-01-10T10:00:00</CreDtTm>
    </GrpHdr>
  </CstmrCdtTrfInitn>
</Document>
XML_EOF

curl -s -u admin:admin123 -X POST \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-User-ID: $USER_ID" \
  -H "X-File-Format: ISO20022" \
  -F "file=@/tmp/test-iso20022.xml" \
  "$GATEWAY_URL/channel/host-to-host/files/upload" | jq '.'
echo ""

# Test 6: Cleanup - delete file
echo "Test 6: Delete File"
echo "-----------------"
curl -s -u admin:admin123 -X DELETE \
  -H "X-Tenant-ID: $TENANT_ID" \
  -w "\nHTTP_CODE:%{http_code}\n" \
  "$GATEWAY_URL/channel/host-to-host/files/$FILE_ID"
echo ""

# Cleanup
rm -f /tmp/test-products.csv /tmp/test-iso20022.xml

echo "=============================================="
echo "Host-to-Host Channel Tests Complete!"
echo "=============================================="
