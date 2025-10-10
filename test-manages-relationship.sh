#!/bin/bash

echo "========================================================================="
echo "Federated Party System: 'Manages On Behalf Of' Relationship Demonstration"
echo "========================================================================="
echo ""

# First, sync the parties we need
echo "=== Step 1: Sync Goldman Sachs (Manager) ==="
GOLDMAN_RESULT=$(curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-002')
GOLDMAN_ID=$(echo $GOLDMAN_RESULT | python3 -c "import sys, json; d=json.load(sys.stdin); print(d['resultParty']['federatedId'])" 2>/dev/null)
echo "✓ Synced Goldman Sachs"
echo "  Federated ID: $GOLDMAN_ID"
echo ""

echo "=== Step 2: Sync Tesla (Principal/Client) ==="
TESLA_RESULT=$(curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-004')
TESLA_ID=$(echo $TESLA_RESULT | python3 -c "import sys, json; d=json.load(sys.stdin); print(d['resultParty']['federatedId'])" 2>/dev/null)
echo "✓ Synced Tesla"
echo "  Federated ID: $TESLA_ID"
echo ""

echo "=== Step 3: Create 'Manages On Behalf Of' Relationship ==="
echo "Scenario: Goldman Sachs provides asset management services for Tesla"
echo ""

# Create the management relationship with collateral document
RELATIONSHIP_RESULT=$(curl -s -X POST 'http://localhost:8083/api/v1/relationships/manages-on-behalf-of' \
  -H 'Content-Type: application/json' \
  -d @- << EOF
{
  "managerId": "$GOLDMAN_ID",
  "principalId": "$TESLA_ID",
  "managementType": "ASSET_MANAGEMENT",
  "scope": "Full discretionary asset management for Tesla corporate treasury",
  "authorityLevel": "DISCRETIONARY",
  "startDate": "2025-01-01",
  "endDate": "2028-12-31",
  "status": "ACTIVE",
  "servicesProvided": [
    "Portfolio Management",
    "Cash Management",
    "Investment Advisory",
    "Risk Management"
  ],
  "assetsUnderManagement": 5000000000.00,
  "aumCurrency": "USD",
  "feeStructure": "0.25% annual management fee on AUM",
  "relationshipManager": "John Smith, Goldman Sachs",
  "principalContact": "Zachary Kirkhorn, CFO, Tesla",
  "managerContact": "Sarah Johnson, VP Asset Management, Goldman Sachs",
  "notificationRequirements": "Monthly portfolio reports, immediate notification of material changes",
  "reportingFrequency": "Monthly",
  "reviewDate": "2026-01-01",
  "notes": "Tesla treasury management mandate effective Q1 2025",
  "createdBy": "system",
  "documentType": "MANAGEMENT_AGREEMENT",
  "documentReference": "GS-TESLA-AM-2025-001",
  "documentTitle": "Asset Management Agreement between Goldman Sachs and Tesla, Inc.",
  "documentDescription": "Comprehensive asset management agreement granting Goldman Sachs discretionary authority to manage Tesla's corporate treasury assets",
  "executionDate": "2024-12-15",
  "effectiveDate": "2025-01-01",
  "expirationDate": "2028-12-31",
  "documentStatus": "ACTIVE",
  "documentUrl": "https://docs.example.com/agreements/GS-TESLA-AM-2025-001.pdf",
  "jurisdiction": "New York",
  "governingLaw": "New York State Law",
  "principalSignatory": "Zachary Kirkhorn, Chief Financial Officer, Tesla, Inc.",
  "agentSignatory": "David Solomon, CEO, The Goldman Sachs Group, Inc.",
  "scopeOfAuthority": "Full discretionary authority for investments in: (1) US Treasury Securities, (2) Investment Grade Corporate Bonds, (3) Money Market Funds, (4) Commercial Paper rated A-1/P-1 or better",
  "specialTerms": "Goldman Sachs shall maintain minimum credit quality of AA- for all investments. No single issuer concentration above 10% of portfolio. Minimum 30% liquidity in T+1 instruments."
}
EOF
)

RELATIONSHIP_STATUS=$(echo $RELATIONSHIP_RESULT | python3 -c "import sys, json; d=json.load(sys.stdin); print('SUCCESS' if 'federatedId' in d else 'ERROR')" 2>/dev/null)

if [ "$RELATIONSHIP_STATUS" == "SUCCESS" ]; then
    echo "✓ Successfully created management relationship!"
    echo ""
    echo "Relationship Details:"
    echo "  Manager: Goldman Sachs Group, Inc."
    echo "  Principal: Tesla, Inc."
    echo "  Type: Asset Management"
    echo "  Authority: Discretionary"
    echo "  AUM: \$5.0 Billion USD"
    echo "  Term: 2025-01-01 to 2028-12-31"
    echo "  Status: ACTIVE"
    echo ""
    echo "Supporting Document:"
    echo "  Type: Management Agreement"
    echo "  Reference: GS-TESLA-AM-2025-001"
    echo "  Executed: 2024-12-15"
    echo "  Effective: 2025-01-01"
    echo "  Expires: 2028-12-31"
    echo "  Jurisdiction: New York"
    echo "  Signatories:"
    echo "    - Zachary Kirkhorn (Tesla CFO)"
    echo "    - David Solomon (Goldman Sachs CEO)"
    echo ""
else
    echo "⚠ Error creating relationship"
    echo "$RELATIONSHIP_RESULT" | python3 -m json.tool 2>/dev/null || echo "$RELATIONSHIP_RESULT"
fi

echo "========================================================================="
echo "✓ Demonstration Complete"
echo "========================================================================="
echo ""
echo "What was created:"
echo "  1. Two federated party entities in Neo4j (Goldman Sachs & Tesla)"
echo "  2. A MANAGES_ON_BEHALF_OF relationship between them"
echo "  3. A CollateralDocument node supporting the relationship"
echo "  4. Full data lineage and provenance tracking"
echo ""
echo "View in Neo4j Browser (http://localhost:7474):"
echo "  # See the management relationship"
echo "  MATCH (manager:Organization)-[r:MANAGES_ON_BEHALF_OF]->(principal:Organization)"
echo "  RETURN manager.legalName, r, principal.legalName"
echo ""
echo "  # See all relationship properties"
echo "  MATCH (manager)-[r:MANAGES_ON_BEHALF_OF]->(principal)"
echo "  RETURN manager.legalName, properties(r), principal.legalName"
echo ""
echo "  # Find the collateral document"
echo "  MATCH (d:CollateralDocument {documentReference: 'GS-TESLA-AM-2025-001'})"
echo "  RETURN d"
echo ""
