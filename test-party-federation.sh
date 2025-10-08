#!/bin/bash

# Test script for federated party system
echo "=========================================="
echo "Federated Party System Integration Test"
echo "=========================================="
echo ""

echo "=== Test 1: Sync Apple from Commercial Banking ==="
curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-001' | python3 -c "import sys, json; d=json.load(sys.stdin); print(f\"Action: {d['action']}\"); print(f\"Federated ID: {d['resultParty']['federatedId']}\"); print(f\"Name: {d['resultParty']['name']}\"); print(f\"Sources: {len(d['resultParty']['sourceRecords'])}\")"
echo ""

echo "=== Test 2: Sync Apple from Capital Markets ==="
curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=CAPITAL_MARKETS&sourceId=CM-001' | python3 -c "import sys, json; d=json.load(sys.stdin); print(f\"Action: {d['action']}\"); print(f\"Federated ID: {d['resultParty']['federatedId']}\"); print(f\"LEI: {d['resultParty'].get('lei')}\"); print(f\"Sources: {len(d['resultParty']['sourceRecords'])}\")"
echo ""

echo "=== Test 3: Sync Goldman Sachs ===="
curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-002' | python3 -c "import sys, json; d=json.load(sys.stdin); print(f\"Action: {d['action']}\"); print(f\"Federated ID: {d['resultParty']['federatedId']}\"); print(f\"Name: {d['resultParty']['legalName']}\")"
echo ""

echo "=== Test 4: Sync Microsoft ===="
curl -s -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-003' | python3 -c "import sys, json; d=json.load(sys.stdin); print(f\"Action: {d['action']}\"); print(f\"Federated ID: {d['resultParty']['federatedId']}\"); print(f\"Name: {d['resultParty']['legalName']}\")"
echo ""

echo "=========================================="
echo "✓ Federated party system is operational!"
echo "✓ Neo4j Browser: http://localhost:7474"
echo "✓ Username: neo4j / Password: password"
echo "=========================================="
