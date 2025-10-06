#!/bin/bash
# Verify Docker Desktop disk capacity increase

echo "==================================================================="
echo "Docker Desktop Disk Verification"
echo "==================================================================="
echo ""

echo "1. Checking Docker VM disk file size..."
DOCKER_RAW_SIZE=$(ls -lh ~/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw 2>/dev/null | awk '{print $5}')
if [ -n "$DOCKER_RAW_SIZE" ]; then
    echo "   Docker.raw size: $DOCKER_RAW_SIZE"

    # Check if size is >= 200GB
    DOCKER_RAW_BYTES=$(ls -l ~/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw 2>/dev/null | awk '{print $5}')
    MIN_BYTES=$((200 * 1024 * 1024 * 1024))  # 200GB in bytes

    if [ "$DOCKER_RAW_BYTES" -ge "$MIN_BYTES" ]; then
        echo "   ✅ PASS: Disk capacity is >= 200GB"
    else
        echo "   ❌ FAIL: Disk capacity is < 200GB"
        echo "   Please increase to 200GB in Docker Desktop settings"
        exit 1
    fi
else
    echo "   ❌ ERROR: Cannot find Docker.raw file"
    exit 1
fi

echo ""
echo "2. Checking Docker disk usage..."
docker system df
echo ""

echo "3. Checking actual disk usage..."
ACTUAL_USAGE=$(du -sh ~/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw 2>/dev/null | awk '{print $1}')
echo "   Current usage: $ACTUAL_USAGE"
echo ""

echo "4. Calculating available space..."
if [ -n "$DOCKER_RAW_BYTES" ]; then
    USAGE_BYTES=$(du -s ~/Library/Containers/com.docker.docker/Data/vms/0/data/Docker.raw 2>/dev/null | awk '{print $1}')
    USAGE_BYTES=$((USAGE_BYTES * 512))  # Convert blocks to bytes

    AVAILABLE_BYTES=$((DOCKER_RAW_BYTES - USAGE_BYTES))
    AVAILABLE_GB=$((AVAILABLE_BYTES / 1024 / 1024 / 1024))

    echo "   Available: ${AVAILABLE_GB}GB"

    if [ "$AVAILABLE_GB" -ge 50 ]; then
        echo "   ✅ PASS: Sufficient space available (>= 50GB)"
    else
        echo "   ⚠️  WARNING: Low space available (< 50GB)"
    fi
fi

echo ""
echo "==================================================================="
echo "5. Testing MongoDB startup..."
echo "==================================================================="

# Try to start MongoDB
docker-compose up -d mongodb

echo ""
echo "Waiting 15 seconds for MongoDB to initialize..."
sleep 15

# Check MongoDB status
MONGO_STATUS=$(docker ps | grep product-catalog-mongodb | grep -c "Up")
if [ "$MONGO_STATUS" -eq 1 ]; then
    echo "✅ SUCCESS: MongoDB container is running!"

    # Check for errors
    ERROR_COUNT=$(docker logs product-catalog-mongodb 2>&1 | grep -cE "(ENOSPC|no space left)")
    if [ "$ERROR_COUNT" -eq 0 ]; then
        echo "✅ SUCCESS: No disk space errors in MongoDB logs!"
    else
        echo "❌ FAIL: Still getting disk space errors"
        docker logs product-catalog-mongodb --tail 20
        exit 1
    fi

    # Check if MongoDB is accepting connections
    docker exec product-catalog-mongodb mongosh -u admin -p admin123 \
        --authenticationDatabase admin --eval "db.adminCommand('ping')" 2>/dev/null

    if [ $? -eq 0 ]; then
        echo "✅ SUCCESS: MongoDB is accepting connections!"
    else
        echo "⚠️  WARNING: MongoDB not ready yet (may need more time)"
    fi
else
    echo "❌ FAIL: MongoDB container is not running"
    echo ""
    echo "MongoDB logs:"
    docker logs product-catalog-mongodb --tail 30
    exit 1
fi

echo ""
echo "==================================================================="
echo "✅ VERIFICATION COMPLETE"
echo "==================================================================="
echo ""
echo "Summary:"
echo "  - Docker disk capacity: $DOCKER_RAW_SIZE"
echo "  - Current usage: $ACTUAL_USAGE"
echo "  - MongoDB status: RUNNING"
echo ""
echo "Next steps:"
echo "  1. Run: docker-compose up -d"
echo "  2. Monitor: docker logs product-catalog-mongodb -f"
echo "  3. Test: curl -u admin:admin123 http://localhost:8082/actuator/health"
