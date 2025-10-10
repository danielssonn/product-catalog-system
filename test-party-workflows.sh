#!/bin/bash

echo "============================================================================="
echo "Party Workflow Integration Demo"
echo "Relationship Approval + Change in Circumstance (CIC) Workflows"
echo "============================================================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}=== Part 1: Upload Workflow Templates ===${NC}"
echo ""

echo "Uploading Party Relationship Approval template..."
curl -s -u admin:admin123 -X POST 'http://localhost:8089/api/v1/templates' \
  -H 'Content-Type: application/json' \
  -d @infrastructure/workflow-templates/party-relationship-approval-template.json | python3 -m json.tool

echo ""
echo "Uploading Party CIC Approval template..."
curl -s -u admin:admin123 -X POST 'http://localhost:8089/api/v1/templates' \
  -H 'Content-Type: application/json' \
  -d @infrastructure/workflow-templates/party-cic-approval-template.json | python3 -m json.tool

echo ""
echo -e "${GREEN}✓ Templates uploaded${NC}"
echo ""

echo -e "${BLUE}=== Part 2: Create Relationship (Triggers Workflow) ===${NC}"
echo "Scenario: Goldman Sachs wants to manage $2B for Microsoft"
echo ""

# First sync the parties
echo "Step 1: Sync Goldman Sachs..."
GOLDMAN_RESULT=$(curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-002')
GOLDMAN_ID=$(echo $GOLDMAN_RESULT | python3 -c "import sys, json; d=json.load(sys.stdin); print(d['resultParty']['federatedId'])" 2>/dev/null)

echo "Step 2: Sync Microsoft..."
MSFT_RESULT=$(curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-003')
MSFT_ID=$(echo $MSFT_RESULT | python3 -c "import sys, json; d=json.load(sys.stdin); print(d['resultParty']['federatedId'])" 2>/dev/null)

echo -e "  Goldman ID: $GOLDMAN_ID"
echo -e "  Microsoft ID: $MSFT_ID"
echo ""

echo "Step 3: Create management relationship (will trigger workflow)..."
RELATIONSHIP_RESULT=$(curl -s -X POST 'http://localhost:8083/api/v1/relationships/manages-on-behalf-of' \
  -H 'Content-Type: application/json' \
  -d @- << EOF
{
  "managerId": "$GOLDMAN_ID",
  "principalId": "$MSFT_ID",
  "managementType": "ASSET_MANAGEMENT",
  "scope": "Discretionary asset management for Microsoft corporate treasury",
  "authorityLevel": "DISCRETIONARY",
  "startDate": "2025-02-01",
  "endDate": "2029-01-31",
  "status": "PENDING",
  "servicesProvided": ["Portfolio Management", "Cash Management"],
  "assetsUnderManagement": 2000000000.00,
  "aumCurrency": "USD",
  "feeStructure": "0.20% annual management fee",
  "relationshipManager": "Jane Smith, Goldman Sachs",
  "principalContact": "Amy Hood, CFO, Microsoft",
  "managerContact": "Jane Smith, VP, Goldman Sachs",
  "notificationRequirements": "Monthly reports",
  "reportingFrequency": "Monthly",
  "reviewDate": "2026-02-01",
  "notes": "Microsoft treasury management mandate",
  "createdBy": "trader1@bank.com",
  "documentType": "MANAGEMENT_AGREEMENT",
  "documentReference": "GS-MSFT-AM-2025-001",
  "documentTitle": "Asset Management Agreement - Goldman Sachs / Microsoft",
  "documentDescription": "Discretionary asset management agreement",
  "executionDate": "2025-01-15",
  "effectiveDate": "2025-02-01",
  "expirationDate": "2029-01-31",
  "documentStatus": "EXECUTED",
  "documentUrl": "https://docs.example.com/GS-MSFT-AM-2025-001.pdf",
  "jurisdiction": "Delaware",
  "governingLaw": "Delaware State Law",
  "principalSignatory": "Amy Hood, CFO, Microsoft Corporation",
  "agentSignatory": "David Solomon, CEO, Goldman Sachs",
  "scopeOfAuthority": "Discretionary authority for fixed income and money market investments",
  "specialTerms": "Investment grade only, maximum 15% single issuer concentration"
}
EOF
)

WORKFLOW_ID=$(echo $RELATIONSHIP_RESULT | python3 -c "import sys, json; d=json.load(sys.stdin); print(d.get('workflowId', 'N/A'))" 2>/dev/null)

echo -e "${GREEN}✓ Relationship created - Workflow ID: $WORKFLOW_ID${NC}"
echo ""

if [ "$WORKFLOW_ID" != "N/A" ]; then
  echo "Step 4: Check workflow status..."
  sleep 2
  curl -s -u admin:admin123 "http://localhost:8089/api/v1/workflows/$WORKFLOW_ID" | python3 -m json.tool
  echo ""
  
  echo "Step 5: Approve the relationship..."
  curl -s -u admin:admin123 -X POST "http://localhost:8089/api/v1/workflows/$WORKFLOW_ID/approve" \
    -H 'Content-Type: application/json' \
    -d '{"approverId":"risk.manager@bank.com","comments":"Approved - within risk limits"}' | python3 -m json.tool
  
  echo ""
  echo -e "${GREEN}✓ Relationship approved and activated${NC}"
fi

echo ""
echo -e "${BLUE}=== Part 3: Simulate Change in Circumstance (CIC) Event ===${NC}"
echo "Scenario: Goldman Sachs risk rating changes in Commercial Banking system"
echo ""

echo "Simulating party change event via Kafka..."
echo "(In production, this would come from source system)"

# Simulate Kafka event
CIC_EVENT='{
  "eventType": "PARTY_RISK_RATING_CHANGED",
  "partyId": "CB-002",
  "timestamp": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
  "changes": {
    "field": "riskRating",
    "oldValue": "LOW",
    "newValue": "MEDIUM",
    "reason": "Increased trading activity and exposure"
  },
  "sourceSystem": "COMMERCIAL_BANKING"
}'

echo "$CIC_EVENT" | docker exec -i product-catalog-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic commercial-banking-party-changes 2>/dev/null

echo -e "${GREEN}✓ CIC event published to Kafka${NC}"
echo ""

echo "Waiting for workflow-service to process event..."
sleep 5

echo ""
echo -e "${BLUE}=== Summary ===${NC}"
echo ""
echo "✓ Workflow templates uploaded for:"
echo "  - Party Relationship Approval"
echo "  - Party Change in Circumstance (CIC)"
echo ""
echo "✓ Created management relationship (Goldman → Microsoft)"
echo "  - Relationship created with status: PENDING"
echo "  - Workflow triggered automatically"
echo "  - Required approval from: Risk Manager"
echo "  - Workflow approved"
echo "  - Relationship activated"
echo ""
echo "✓ CIC workflow triggered by party change event"
echo "  - Event: Risk rating change (LOW → MEDIUM)"
echo "  - Source: Commercial Banking system"
echo "  - Workflow created for approval"
echo "  - Awaiting Risk Manager approval"
echo ""
echo -e "${YELLOW}View Workflows:${NC}"
echo "  Workflow Service: http://localhost:8089/api/v1/workflows"
echo "  Temporal UI: http://localhost:8088"
echo ""
echo -e "${YELLOW}View in Neo4j:${NC}"
echo "  http://localhost:7474"
echo "  Query: MATCH (m)-[r:MANAGES_ON_BEHALF_OF]->(p) RETURN m, r, p"
echo ""

