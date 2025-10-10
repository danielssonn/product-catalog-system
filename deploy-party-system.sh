#!/bin/bash

# Deployment script for Federated Party System
# This script builds and deploys the complete party federation architecture

set -e

echo "=========================================="
echo "Federated Party System Deployment"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check prerequisites
print_info "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    exit 1
fi
print_success "Docker found"

if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose is not installed"
    exit 1
fi
print_success "Docker Compose found"

if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed"
    exit 1
fi
print_success "Maven found"

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    print_error "Java 21 or higher is required (found Java $JAVA_VERSION)"
    exit 1
fi
print_success "Java 21+ found"

echo ""
print_info "Building party services..."

# Build the services
cd backend

print_info "Building Commercial Banking Party Service..."
mvn clean package -pl commercial-banking-party-service -am -DskipTests
if [ $? -eq 0 ]; then
    print_success "Commercial Banking Party Service built successfully"
else
    print_error "Failed to build Commercial Banking Party Service"
    exit 1
fi

print_info "Building Capital Markets Party Service..."
mvn clean package -pl capital-markets-party-service -am -DskipTests
if [ $? -eq 0 ]; then
    print_success "Capital Markets Party Service built successfully"
else
    print_error "Failed to build Capital Markets Party Service"
    exit 1
fi

print_info "Building Federated Party Service..."
mvn clean package -pl party-service -am -DskipTests
if [ $? -eq 0 ]; then
    print_success "Federated Party Service built successfully"
else
    print_error "Failed to build Federated Party Service"
    exit 1
fi

cd ..

echo ""
print_info "Starting Docker containers..."

# Stop existing containers
docker-compose -f docker-compose.party.yml down -v 2>/dev/null || true

# Start services
docker-compose -f docker-compose.party.yml up -d --build

echo ""
print_info "Waiting for services to be healthy..."

# Wait for MongoDB
print_info "Waiting for MongoDB..."
timeout 60 bash -c 'until docker exec party-mongodb mongosh --eval "db.adminCommand({ping: 1})" -u admin -p admin123 --quiet > /dev/null 2>&1; do sleep 2; done' || {
    print_error "MongoDB failed to start"
    docker-compose -f docker-compose.party.yml logs mongodb
    exit 1
}
print_success "MongoDB is healthy"

# Wait for Neo4j
print_info "Waiting for Neo4j..."
timeout 90 bash -c 'until curl -s http://localhost:7474 > /dev/null; do sleep 3; done' || {
    print_error "Neo4j failed to start"
    docker-compose -f docker-compose.party.yml logs neo4j
    exit 1
}
print_success "Neo4j is healthy"

# Wait for Commercial Banking Party Service
print_info "Waiting for Commercial Banking Party Service..."
timeout 120 bash -c 'until curl -s http://localhost:8084/api/commercial-banking/parties/health > /dev/null; do sleep 3; done' || {
    print_error "Commercial Banking Party Service failed to start"
    docker-compose -f docker-compose.party.yml logs commercial-banking-party-service
    exit 1
}
print_success "Commercial Banking Party Service is healthy"

# Wait for Capital Markets Party Service
print_info "Waiting for Capital Markets Party Service..."
timeout 120 bash -c 'until curl -s http://localhost:8085/api/capital-markets/counterparties/health > /dev/null; do sleep 3; done' || {
    print_error "Capital Markets Party Service failed to start"
    docker-compose -f docker-compose.party.yml logs capital-markets-party-service
    exit 1
}
print_success "Capital Markets Party Service is healthy"

# Wait for Federated Party Service
print_info "Waiting for Federated Party Service..."
timeout 120 bash -c 'until curl -s http://localhost:8083/actuator/health > /dev/null; do sleep 3; done' || {
    print_error "Federated Party Service failed to start"
    docker-compose -f docker-compose.party.yml logs party-service
    exit 1
}
print_success "Federated Party Service is healthy"

echo ""
echo "=========================================="
print_success "Deployment Complete!"
echo "=========================================="
echo ""
echo "Services:"
echo "  - MongoDB:                        http://localhost:27018"
echo "  - Neo4j Browser:                  http://localhost:7474 (neo4j/password)"
echo "  - Commercial Banking:             http://localhost:8084"
echo "  - Capital Markets:                http://localhost:8085"
echo "  - Federated Party Service:        http://localhost:8083"
echo "  - GraphiQL:                       http://localhost:8083/graphiql"
echo ""
echo "Sample Data:"
echo "  - Commercial Banking: 5 parties (CB-001 through CB-005)"
echo "  - Capital Markets: 5 counterparties (CM-001 through CM-005)"
echo "  - Overlapping entities: Apple, Goldman Sachs, Microsoft, JPMorgan"
echo ""
echo "Next Steps:"
echo "  1. Test Commercial Banking API:"
echo "     curl http://localhost:8084/api/commercial-banking/parties"
echo ""
echo "  2. Test Capital Markets API:"
echo "     curl http://localhost:8085/api/capital-markets/counterparties"
echo ""
echo "  3. Sync from Commercial Banking:"
echo "     curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-001'"
echo ""
echo "  4. Sync from Capital Markets:"
echo "     curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=CAPITAL_MARKETS&sourceId=CM-001'"
echo ""
echo "  5. View in Neo4j:"
echo "     Open http://localhost:7474 and run: MATCH (n) RETURN n LIMIT 25"
echo ""
echo "  6. Query via GraphQL:"
echo "     Open http://localhost:8083/graphiql"
echo ""
print_info "Run './test-party-federation.sh' to test end-to-end federation"
echo ""
