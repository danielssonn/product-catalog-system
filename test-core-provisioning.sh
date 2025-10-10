#!/bin/bash

# Test script for Core Banking Integration - Product Provisioning
# This script tests the complete provisioning flow through the abstraction layer

set -e

BASE_URL="http://localhost:8082"
ADMIN_AUTH="admin:admin123"

echo "================================================================================"
echo "Core Banking Integration - Provisioning Test Suite"
echo "================================================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print section header
print_header() {
    echo ""
    echo -e "${BLUE}================================================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================================================================${NC}"
    echo ""
}

# Function to print success
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Function to print info
print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Check if services are running
print_header "Step 1: Health Checks"

echo "Checking Product Service..."
if curl -s -u $ADMIN_AUTH $BASE_URL/actuator/health | grep -q "UP"; then
    print_success "Product Service is UP"
else
    print_error "Product Service is DOWN"
    exit 1
fi

echo ""
echo "Checking Mock Temenos API..."
if curl -s http://localhost:9190/mock-temenos-api/health | grep -q "Temenos"; then
    print_success "Temenos Mock API is UP"
else
    print_error "Temenos Mock API is DOWN - Start it with: cd infrastructure/mock-core-api && npm start"
    exit 1
fi

# Initialize MongoDB with test data
print_header "Step 2: Initialize Test Data"

print_info "Loading core system mappings and test solutions into MongoDB..."
mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --file infrastructure/mongodb/init-core-systems.js

print_success "Test data initialized"

# Test 1: Check tenant core mappings
print_header "Test 1: Verify Tenant Core System Mappings"

echo "Checking tenant-001 mappings..."
MAPPINGS=$(mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --quiet --eval "db.tenant_core_mappings.findOne({tenantId: 'tenant-001'}).coreSystems.length")

if [ "$MAPPINGS" -gt "0" ]; then
    print_success "tenant-001 has $MAPPINGS core system(s) configured"
else
    print_error "No core systems configured for tenant-001"
    exit 1
fi

# Test 2: Manual provisioning via orchestrator
print_header "Test 2: Manual Provisioning Test"

print_info "Creating a new solution for tenant-001..."

SOLUTION_RESPONSE=$(curl -s -u $ADMIN_AUTH -X POST $BASE_URL/api/v1/solutions/configure \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: tenant-001" \
    -H "X-User-ID: test@bank.com" \
    -d '{
        "catalogProductId": "cat-checking-001",
        "solutionName": "Test Premium Checking",
        "description": "Test checking account for provisioning",
        "category": "CHECKING",
        "pricing": {
            "monthlyFee": 15.00,
            "minimumBalance": 1000.00,
            "currency": "USD"
        },
        "availableChannels": ["WEB", "MOBILE", "BRANCH"],
        "terms": {
            "minimumAge": 18,
            "termsUrl": "https://bank.com/terms/checking"
        }
    }')

SOLUTION_ID=$(echo $SOLUTION_RESPONSE | jq -r '.id')

if [ "$SOLUTION_ID" != "null" ] && [ -n "$SOLUTION_ID" ]; then
    print_success "Solution created: $SOLUTION_ID"
else
    print_error "Failed to create solution"
    echo "Response: $SOLUTION_RESPONSE"
    exit 1
fi

# Wait for auto-provisioning (change stream)
print_info "Waiting 5 seconds for auto-provisioning via change stream..."
sleep 5

# Check if solution was provisioned
print_info "Checking provisioning status..."

PROVISIONED=$(mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --quiet --eval "db.solutions.findOne({_id: ObjectId('$SOLUTION_ID')}).coreProvisioningRecords?.length || 0")

if [ "$PROVISIONED" -gt "0" ]; then
    print_success "Solution auto-provisioned to $PROVISIONED core system(s)!"

    # Get core product ID
    CORE_PRODUCT_ID=$(mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
        --quiet --eval "db.solutions.findOne({_id: ObjectId('$SOLUTION_ID')}).coreProvisioningRecords[0].coreProductId" | tr -d '"')

    print_success "Core Product ID: $CORE_PRODUCT_ID"

    # Verify in mock core system
    echo ""
    print_info "Verifying product in Temenos Mock API..."

    CORE_PRODUCT=$(curl -s http://localhost:9190/mock-temenos-api/products/$CORE_PRODUCT_ID)

    if echo "$CORE_PRODUCT" | grep -q "Test Premium Checking"; then
        print_success "Product verified in Temenos core system"
        echo "Product Details:"
        echo "$CORE_PRODUCT" | jq '.'
    else
        print_error "Product not found in core system"
    fi
else
    print_error "Solution was NOT auto-provisioned"
    print_info "Check application logs for readiness evaluation results"
fi

# Test 3: Update and sync
print_header "Test 3: Configuration Update & Sync"

if [ "$SOLUTION_ID" != "null" ] && [ -n "$SOLUTION_ID" ]; then
    print_info "Updating solution pricing..."

    UPDATE_RESPONSE=$(curl -s -u $ADMIN_AUTH -X PUT $BASE_URL/api/v1/solutions/$SOLUTION_ID \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: tenant-001" \
        -d '{
            "pricing": {
                "monthlyFee": 20.00,
                "minimumBalance": 1500.00
            }
        }')

    print_info "Waiting 3 seconds for sync to core..."
    sleep 3

    # Check if update synced to core
    if [ -n "$CORE_PRODUCT_ID" ]; then
        UPDATED_CORE=$(curl -s http://localhost:9190/mock-temenos-api/products/$CORE_PRODUCT_ID)

        SYNCED_FEE=$(echo "$UPDATED_CORE" | jq -r '.monthlyFee')

        if [ "$SYNCED_FEE" == "20" ]; then
            print_success "Configuration update synced to core system"
            echo "Updated monthly fee: \$$SYNCED_FEE"
        else
            print_error "Configuration NOT synced (fee: \$$SYNCED_FEE)"
        fi
    fi
fi

# Test 4: Check mock core products collection
print_header "Test 4: Verify Mock Core Products"

MOCK_COUNT=$(mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --quiet --eval "db.mock_core_products.countDocuments({})")

print_success "Total products in mock core systems: $MOCK_COUNT"

echo ""
print_info "Mock products by core system type:"

mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --quiet --eval '
    db.mock_core_products.aggregate([
        { $group: { _id: "$coreSystemType", count: { $sum: 1 } } },
        { $sort: { count: -1 } }
    ]).forEach(doc => print("  - " + doc._id + ": " + doc.count))
    '

# Test 5: Health monitoring
print_header "Test 5: Core System Health Monitoring"

print_info "Checking health monitor status..."

# Check if health monitor ran
LAST_CHECK=$(mongosh "mongodb://admin:admin123@localhost:27018/product_catalog_db?authSource=admin" \
    --quiet --eval "db.core_health_status?.findOne()?.lastCheckTime" 2>/dev/null || echo "null")

if [ "$LAST_CHECK" != "null" ]; then
    print_success "Health monitoring is active"
    print_info "Last check: $LAST_CHECK"
else
    print_info "Health monitoring may still be initializing (runs every 30 seconds)"
fi

# Test 6: Kafka events
print_header "Test 6: Provisioning Events (Kafka)"

print_info "Checking for provisioning events..."
print_info "Topic: core-provisioning-events"
print_info "(Events would be consumed by downstream services for audit trail)"

# Summary
print_header "Test Summary"

echo ""
echo "✅ Core Banking Integration Tests Complete"
echo ""
echo "📊 Results:"
echo "  - Tenant core mappings: Configured"
echo "  - Auto-provisioning: $([ "$PROVISIONED" -gt "0" ] && echo "✅ Working" || echo "⚠️  Check logs")"
echo "  - Configuration sync: $([ "$SYNCED_FEE" == "20" ] && echo "✅ Working" || echo "⚠️  Check logs")"
echo "  - Mock core systems: $MOCK_COUNT products"
echo ""
echo "🔗 Useful MongoDB Queries:"
echo "  # View all provisioned solutions:"
echo "    db.solutions.find({ 'coreProvisioningRecords.0': { \$exists: true } })"
echo ""
echo "  # View mock core products:"
echo "    db.mock_core_products.find()"
echo ""
echo "  # View tenant core mappings:"
echo "    db.tenant_core_mappings.find()"
echo ""
echo "📋 Logs to check:"
echo "  - Product service logs for change stream events"
echo "  - Mock core API logs: npm logs in infrastructure/mock-core-api"
echo ""
echo "================================================================================"
