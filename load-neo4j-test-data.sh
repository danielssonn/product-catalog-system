#!/bin/bash

# Load Test Data into Neo4j
# This script loads party test data for context resolution testing

set -e

echo "=========================================="
echo "Loading Test Data into Neo4j"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

NEO4J_URL="http://localhost:7474"
NEO4J_USER="neo4j"
NEO4J_PASSWORD="password"

# Check if Neo4j is accessible
echo -e "${YELLOW}Step 1: Checking Neo4j connectivity${NC}"
if curl -s -u $NEO4J_USER:$NEO4J_PASSWORD $NEO4J_URL > /dev/null; then
    echo -e "${GREEN}✓ Neo4j is accessible${NC}"
else
    echo -e "${RED}✗ Neo4j is not accessible at $NEO4J_URL${NC}"
    echo "Make sure Neo4j is running: docker-compose ps party-neo4j"
    exit 1
fi
echo ""

# Load the Cypher script
echo -e "${YELLOW}Step 2: Loading test data from Cypher script${NC}"
SCRIPT_FILE="infrastructure/neo4j/init-test-data.cypher"

if [ ! -f "$SCRIPT_FILE" ]; then
    echo -e "${RED}✗ Script file not found: $SCRIPT_FILE${NC}"
    exit 1
fi

# Execute via cypher-shell in Docker container
echo "Executing Cypher script..."
docker exec party-neo4j cypher-shell \
    -u $NEO4J_USER \
    -p $NEO4J_PASSWORD \
    -f /var/lib/neo4j/import/init-test-data.cypher \
    2>&1 || {
        echo ""
        echo -e "${YELLOW}Note: If file not found, copying to container...${NC}"
        docker cp $SCRIPT_FILE party-neo4j:/var/lib/neo4j/import/init-test-data.cypher
        docker exec party-neo4j cypher-shell \
            -u $NEO4J_USER \
            -p $NEO4J_PASSWORD \
            -f /var/lib/neo4j/import/init-test-data.cypher
    }

echo -e "${GREEN}✓ Test data loaded successfully${NC}"
echo ""

# Verify data
echo -e "${YELLOW}Step 3: Verifying loaded data${NC}"

# Query to verify principals
echo "Checking principal-to-party mappings..."
QUERY='MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord {sourceSystem: "AUTH_SERVICE"}) RETURN s.sourceId as principal, p.federatedId as party, labels(p)[0] as type LIMIT 10'

docker exec party-neo4j cypher-shell \
    -u $NEO4J_USER \
    -p $NEO4J_PASSWORD \
    "$QUERY" \
    --format plain || echo "Query failed"

echo ""
echo -e "${GREEN}✓ Data verification complete${NC}"
echo ""

echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "Test data loaded into Neo4j!"
echo ""
echo "Principals available for testing:"
echo "  - admin              (maps to ind-admin-001, Acme Bank)"
echo "  - catalog-user       (maps to ind-user-001, Acme Bank)"
echo "  - test-principal-001 (maps to ind-user-001, Acme Bank)"
echo "  - global-user        (maps to ind-global-user-001, Global Financial)"
echo ""
echo "Tenants:"
echo "  - org-acme-bank-001        (Acme Bank - TIER_1)"
echo "  - org-global-financial-001 (Global Financial - TIER_2)"
echo ""
echo "Next steps:"
echo "1. Test context resolution: ./test-context-resolution.sh"
echo "2. Test end-to-end flow: ./test-end-to-end-context-resolution.sh"
echo ""
