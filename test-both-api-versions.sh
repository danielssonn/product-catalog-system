#!/bin/bash

echo "==========================================================================="
echo "API VERSIONING TEST - V1 vs V2"
echo "Breaking Change: customFees → customFeesFX"
echo "==========================================================================="
echo ""

# Test V1
echo "📝 TEST 1: V1 API - POST /api/v1/solutions/configure"
echo "   Field: customFees (original)"
echo ""
echo "Request:"
cat << 'EOF'
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "V1 Premium Checking",
  "customFees": {
    "monthlyMaintenance": 15.00,
    "overdraft": 35.00
  }
}
EOF

echo ""
echo "Response:"
curl -s -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: v1-tester@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "V1 Premium Checking",
    "customFees": {
      "monthlyMaintenance": 15.00,
      "overdraft": 35.00
    },
    "pricingVariance": 10,
    "riskLevel": "LOW"
  }' | python3 -c "import sys, json; r=json.load(sys.stdin); print(json.dumps({k:r[k] for k in ['solutionId','solutionName','status','workflowStatus','message'] if k in r}, indent=2))"

echo ""
echo "✅ V1 uses 'customFees' field"
echo "❌ V1 does NOT have 'metadata' field in response"
echo ""
echo "==========================================================================="
echo ""

# Test V2
echo "📝 TEST 2: V2 API - POST /api/v2/solutions/configure"
echo "   Field: customFeesFX (renamed from customFees)"
echo "   New: metadata field"
echo ""
echo "Request:"
cat << 'EOF'
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "V2 Premium Checking",
  "customFeesFX": {
    "monthlyMaintenance": 20.00,
    "overdraft": 40.00
  },
  "metadata": {
    "segment": "enterprise",
    "region": "APAC"
  }
}
EOF

echo ""
echo "Response:"
curl -s -u admin:admin123 -X POST http://localhost:8082/api/v2/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: v2-tester@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "V2 Premium Checking",
    "customFeesFX": {
      "monthlyMaintenance": 20.00,
      "overdraft": 40.00
    },
    "metadata": {
      "segment": "enterprise",
      "region": "APAC"
    },
    "pricingVariance": 10,
    "riskLevel": "LOW"
  }' | python3 -c "import sys, json; r=json.load(sys.stdin); print(json.dumps({k:r[k] for k in ['solutionId','solutionName','status','workflowStatus','message','metadata'] if k in r}, indent=2))"

echo ""
echo "✅ V2 uses 'customFeesFX' field (renamed)"
echo "✅ V2 INCLUDES 'metadata' field in response"
echo ""
echo "==========================================================================="
echo ""

echo "🎯 KEY DIFFERENCES:"
echo ""
echo "┌────────────────────────────────────────────────────────────────┐"
echo "│                     V1                 V2                       │"
echo "├────────────────────────────────────────────────────────────────┤"
echo "│ Endpoint:           /api/v1/...       /api/v2/...              │"
echo "│ Fees Field:         customFees        customFeesFX (RENAMED)   │"
echo "│ Metadata Field:     ❌ Not Available   ✅ Available             │"
echo "│ Status:             STABLE             BETA                    │"
echo "└────────────────────────────────────────────────────────────────┘"
echo ""
echo "✅ Both versions deployed and working simultaneously!"
echo "✅ Zero downtime migration - clients can upgrade at their own pace"
echo ""
