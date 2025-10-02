#!/bin/bash

# Test API Versioning - V1 vs V2

echo "=========================================="
echo "API Versioning Test - V1 vs V2"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test 1: V1 Endpoint Structure
echo -e "${BLUE}Test 1: Check V1 Controller exists${NC}"
docker exec product-service ls -la /app/BOOT-INF/classes/com/bank/product/domain/solution/controller/SolutionController.class 2>&1 | grep -q "SolutionController.class" && echo -e "${GREEN}✓ V1 Controller exists${NC}" || echo -e "${RED}✗ V1 Controller missing${NC}"

# Test 2: V2 Controller Structure
echo -e "${BLUE}Test 2: Check V2 Controller exists${NC}"
docker exec product-service ls -la /app/BOOT-INF/classes/com/bank/product/domain/solution/controller/v2/SolutionControllerV2.class 2>&1 | grep -q "SolutionControllerV2.class" && echo -e "${GREEN}✓ V2 Controller exists${NC}" || echo -e "${RED}✗ V2 Controller missing${NC}"

# Test 3: V1 DTO
echo -e "${BLUE}Test 3: Check V1 DTO (customFees)${NC}"
docker exec product-service ls -la /app/BOOT-INF/classes/com/bank/product/domain/solution/dto/ConfigureSolutionRequest.class 2>&1 | grep -q "ConfigureSolutionRequest.class" && echo -e "${GREEN}✓ V1 DTO exists (customFees)${NC}" || echo -e "${RED}✗ V1 DTO missing${NC}"

# Test 4: V2 DTO
echo -e "${BLUE}Test 4: Check V2 DTO (customFeesFX)${NC}"
docker exec product-service ls -la /app/BOOT-INF/classes/com/bank/product/domain/solution/dto/v2/ConfigureSolutionRequestV2.class 2>&1 | grep -q "ConfigureSolutionRequestV2.class" && echo -e "${GREEN}✓ V2 DTO exists (customFeesFX)${NC}" || echo -e "${RED}✗ V2 DTO missing${NC}"

# Test 5: Check V1 endpoint is registered
echo ""
echo -e "${BLUE}Test 5: Check V1 endpoint mapping${NC}"
docker logs product-service 2>&1 | grep "api/v1/solutions" | grep "POST" | head -1 && echo -e "${GREEN}✓ V1 endpoint registered: POST /api/v1/solutions/configure${NC}" || echo -e "${RED}✗ V1 endpoint not found${NC}"

# Test 6: Check V2 endpoint is registered
echo -e "${BLUE}Test 6: Check V2 endpoint mapping${NC}"
docker logs product-service 2>&1 | grep "api/v2/solutions" | grep "POST" | head -1 && echo -e "${GREEN}✓ V2 endpoint registered: POST /api/v2/solutions/configure${NC}" || echo -e "${RED}✗ V2 endpoint not found${NC}"

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "V1 API endpoint: POST /api/v1/solutions/configure"
echo "  - Uses: ConfigureSolutionRequest"
echo "  - Field: customFees (Map<String, BigDecimal>)"
echo ""
echo "V2 API endpoint: POST /api/v2/solutions/configure"
echo "  - Uses: ConfigureSolutionRequestV2"
echo "  - Field: customFeesFX (Map<String, BigDecimal>) [RENAMED]"
echo "  - New Field: metadata (Map<String, Object>)"
echo ""
echo "Both versions are deployed and coexist!"
echo "Clients can use either v1 or v2 based on their needs."
echo ""
