#!/bin/bash
# MongoDB Stability Monitor
# Monitors for 15 minutes to ensure no crashes

echo "==================================================================="
echo "MongoDB Stability Monitor - 15 Minute Test"
echo "==================================================================="
echo "Started at: $(date)"
echo "Monitoring MongoDB for checkpoint operations and stability..."
echo ""

# Get container uptime
START_TIME=$(date +%s)
TARGET_TIME=$((START_TIME + 900))  # 15 minutes = 900 seconds

# Monitor loop
CHECKPOINT_COUNT=0
ERROR_COUNT=0

while [ $(date +%s) -lt $TARGET_TIME ]; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))
    REMAINING=$((TARGET_TIME - CURRENT_TIME))

    # Clear previous line
    printf "\r\033[K"

    # Check if container is running
    if ! docker ps | grep -q "product-catalog-mongodb.*Up.*healthy"; then
        echo ""
        echo "❌ FAILED: MongoDB container is not running or unhealthy!"
        docker ps -a | grep mongo
        echo ""
        echo "Last 50 lines of logs:"
        docker logs product-catalog-mongodb --tail 50
        exit 1
    fi

    # Check for checkpoint messages in last 5 seconds
    CHECKPOINTS=$(docker logs product-catalog-mongodb --since 5s 2>&1 | grep -c "checkpoint")
    if [ $CHECKPOINTS -gt 0 ]; then
        CHECKPOINT_COUNT=$((CHECKPOINT_COUNT + CHECKPOINTS))
    fi

    # Check for errors
    ERRORS=$(docker logs product-catalog-mongodb --since 5s 2>&1 | grep -cE "(FATAL|Writing to log file failed|aborting application)")
    if [ $ERRORS -gt 0 ]; then
        ERROR_COUNT=$((ERROR_COUNT + ERRORS))
        echo ""
        echo "⚠️  WARNING: Detected error in logs!"
        docker logs product-catalog-mongodb --tail 20
        exit 1
    fi

    # Display progress
    printf "⏱️  Time elapsed: %02d:%02d | Remaining: %02d:%02d | Checkpoints: %d | Status: ✅ HEALTHY" \
        $((ELAPSED / 60)) $((ELAPSED % 60)) \
        $((REMAINING / 60)) $((REMAINING % 60)) \
        $CHECKPOINT_COUNT

    sleep 5
done

echo ""
echo ""
echo "==================================================================="
echo "✅ SUCCESS: MongoDB has been stable for 15 minutes!"
echo "==================================================================="
echo "Completed at: $(date)"
echo "Total checkpoints observed: $CHECKPOINT_COUNT"
echo "Errors detected: $ERROR_COUNT"
echo ""
echo "MongoDB container status:"
docker ps | grep mongo
echo ""
echo "Recent checkpoint activity:"
docker logs product-catalog-mongodb 2>&1 | grep -i checkpoint | tail -5
echo ""
echo "🎉 MongoDB is stable and ready for production use!"
