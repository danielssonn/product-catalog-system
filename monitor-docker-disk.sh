#!/bin/bash

################################################################################
# Docker Disk Space Monitoring Script
#
# Purpose: Monitor Docker VM disk usage and container log sizes
# Usage: ./monitor-docker-disk.sh
#
# Alerts when:
# - Docker VM disk usage > 80%
# - Any container log > 1GB
# - Total container logs > 10GB
################################################################################

set -e

# Color codes for output
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Thresholds
DISK_WARNING_THRESHOLD=80
DISK_CRITICAL_THRESHOLD=90
SINGLE_LOG_WARNING_MB=1024  # 1GB
TOTAL_LOGS_WARNING_MB=10240 # 10GB

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  Docker Disk Space Monitor${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

################################################################################
# 1. Docker System Summary
################################################################################
echo -e "${BLUE}📊 Docker System Summary${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker system df
echo ""

################################################################################
# 2. Docker VM Disk Usage
################################################################################
echo -e "${BLUE}💾 Docker VM Disk Usage${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Get disk usage via Docker VM
VM_DISK_INFO=$(docker run --rm --privileged --pid=host alpine:latest \
    nsenter -t 1 -m -u -n -i df -h | grep '/var/lib$' || echo "")

if [ -n "$VM_DISK_INFO" ]; then
    echo "$VM_DISK_INFO"

    # Extract usage percentage
    USAGE_PCT=$(echo "$VM_DISK_INFO" | awk '{print $5}' | sed 's/%//')

    if [ "$USAGE_PCT" -ge "$DISK_CRITICAL_THRESHOLD" ]; then
        echo -e "${RED}🚨 CRITICAL: Docker VM disk usage at ${USAGE_PCT}%!${NC}"
    elif [ "$USAGE_PCT" -ge "$DISK_WARNING_THRESHOLD" ]; then
        echo -e "${YELLOW}⚠️  WARNING: Docker VM disk usage at ${USAGE_PCT}%${NC}"
    else
        echo -e "${GREEN}✅ Docker VM disk usage healthy (${USAGE_PCT}%)${NC}"
    fi
else
    echo -e "${YELLOW}⚠️  Could not retrieve Docker VM disk usage${NC}"
fi
echo ""

################################################################################
# 3. Container Log Sizes
################################################################################
echo -e "${BLUE}📝 Container Log Sizes (Top 10)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Get container log sizes
LOG_SIZES=$(docker run --rm --privileged --pid=host alpine:latest \
    nsenter -t 1 -m -u -n -i sh -c 'du -sh /var/lib/docker/containers/*/*.log 2>/dev/null | sort -hr | head -10' || echo "")

if [ -n "$LOG_SIZES" ]; then
    # Map container IDs to names
    echo "$LOG_SIZES" | while read size path; do
        container_id=$(basename $(dirname "$path"))
        container_name=$(docker ps -a --format "{{.ID}} {{.Names}}" | grep "^${container_id:0:12}" | awk '{print $2}' || echo "unknown")

        # Convert size to MB for comparison
        size_mb=$(echo "$size" | sed 's/G/*1024/;s/M//;s/K\/1024/' | bc 2>/dev/null || echo "0")

        # Color code based on size
        if (( $(echo "$size_mb > $SINGLE_LOG_WARNING_MB" | bc -l 2>/dev/null || echo 0) )); then
            echo -e "  ${RED}🔴 $size\t$container_name${NC}"
        elif (( $(echo "$size_mb > 100" | bc -l 2>/dev/null || echo 0) )); then
            echo -e "  ${YELLOW}🟡 $size\t$container_name${NC}"
        else
            echo -e "  ${GREEN}🟢 $size\t$container_name${NC}"
        fi
    done
else
    echo -e "${YELLOW}⚠️  Could not retrieve container log sizes${NC}"
fi
echo ""

################################################################################
# 4. Total Container Logs Size
################################################################################
echo -e "${BLUE}📦 Total Container Logs${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

TOTAL_LOGS=$(docker run --rm --privileged --pid=host alpine:latest \
    nsenter -t 1 -m -u -n -i du -sh /var/lib/docker/containers 2>/dev/null | awk '{print $1}' || echo "unknown")

echo "Total size: $TOTAL_LOGS"
echo ""

################################################################################
# 5. Volume Usage
################################################################################
echo -e "${BLUE}💿 Volume Usage${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
docker volume ls -q | while read vol; do
    size=$(docker run --rm -v "$vol":/data alpine:latest du -sh /data 2>/dev/null | awk '{print $1}' || echo "unknown")
    echo "  $vol: $size"
done
echo ""

################################################################################
# 6. Recommendations
################################################################################
echo -e "${BLUE}💡 Recommendations${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ -n "$VM_DISK_INFO" ] && [ "$USAGE_PCT" -ge "$DISK_WARNING_THRESHOLD" ]; then
    echo -e "${YELLOW}1. Docker VM disk usage is high. Consider:${NC}"
    echo "   - Truncating container logs: docker-compose down && docker run --rm --privileged --pid=host alpine:latest nsenter -t 1 -m -u -n -i sh -c 'truncate -s 0 /var/lib/docker/containers/*/*-json.log'"
    echo "   - Pruning unused images: docker image prune -a"
    echo "   - Pruning unused volumes: docker volume prune"
    echo "   - Cleaning build cache: docker builder prune"
fi

# Check if any container logs are large
LARGE_LOGS=$(echo "$LOG_SIZES" | head -1 | awk '{print $1}')
if [[ "$LARGE_LOGS" =~ G$ ]]; then
    echo -e "${YELLOW}2. Large container logs detected. Verify:${NC}"
    echo "   - Log rotation is configured in docker-compose.yml"
    echo "   - Application logging levels are appropriate (INFO in production, not DEBUG)"
fi

echo -e "${GREEN}3. To clean up disk space:${NC}"
echo "   docker system prune -a --volumes  # CAUTION: Removes all unused resources"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Monitoring complete - $(date)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
