#!/bin/bash

# Startup script for Mock Core Banking APIs
# Ensures environment variables are set before starting

set -e

echo "=========================================="
echo "Mock Core Banking API Server"
echo "=========================================="
echo ""

# Check if .env file exists
if [ -f .env ]; then
    echo "✅ Found .env file"
    source .env
else
    echo "⚠️  No .env file found"
    echo ""
    echo "Creating .env from .env.example..."

    if [ ! -f .env.example ]; then
        echo "❌ Error: .env.example not found"
        exit 1
    fi

    cp .env.example .env
    echo "✅ Created .env file"
    echo ""
    echo "📝 Please edit .env and set your MongoDB credentials:"
    echo "   MONGODB_USERNAME=admin"
    echo "   MONGODB_PASSWORD=your-password"
    echo ""
    echo "Then run this script again."
    exit 1
fi

# Check required environment variables
if [ -z "$MONGODB_USERNAME" ] && [ -z "$MONGODB_URI" ]; then
    echo "❌ ERROR: MongoDB credentials not configured"
    echo ""
    echo "Please set one of:"
    echo "  1. MONGODB_USERNAME and MONGODB_PASSWORD in .env"
    echo "  2. MONGODB_URI in .env"
    echo ""
    exit 1
fi

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
    echo "✅ Dependencies installed"
    echo ""
fi

# Display configuration
echo "Configuration:"
echo "  MongoDB Host: ${MONGODB_HOST:-localhost}"
echo "  MongoDB Port: ${MONGODB_PORT:-27018}"
echo "  MongoDB Database: ${MONGODB_DATABASE:-product_catalog_db}"
echo "  Temenos Port: ${TEMENOS_PORT:-9190}"
echo "  Finacle Port: ${FINACLE_PORT:-9191}"
echo "  FIS Port: ${FIS_PORT:-9192}"
echo ""

# Test MongoDB connection
echo "Testing MongoDB connection..."
export MONGODB_URI="${MONGODB_URI:-mongodb://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@${MONGODB_HOST:-localhost}:${MONGODB_PORT:-27018}/${MONGODB_DATABASE:-product_catalog_db}?authSource=${MONGODB_AUTH_SOURCE:-admin}}"

if node test-connection.js; then
    echo ""
    echo "✅ MongoDB connection successful"
    echo ""
else
    echo ""
    echo "❌ MongoDB connection failed"
    echo ""
    echo "Please check:"
    echo "  1. MongoDB is running: docker-compose ps mongodb"
    echo "  2. Credentials in .env are correct"
    echo "  3. Port is correct (usually 27018)"
    echo ""
    exit 1
fi

# Start the mock APIs
echo "=========================================="
echo "Starting Mock Core Banking APIs..."
echo "=========================================="
echo ""

npm start
