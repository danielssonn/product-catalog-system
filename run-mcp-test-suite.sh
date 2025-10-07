#!/opt/homebrew/bin/bash

# MCP Validation Full Test Suite Runner
# Executes all MCP validation test cases and generates comprehensive report

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
RESULTS_DIR="test-results/mcp-validation"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$RESULTS_DIR/test-suite-report_${TIMESTAMP}.md"
SUMMARY_FILE="$RESULTS_DIR/test-suite-summary_${TIMESTAMP}.json"

# Test configuration
TOTAL_TESTS=8
WAIT_TIME=35

# Create results directory
mkdir -p "$RESULTS_DIR"

# Helper functions
print_header() {
    echo -e "\n${MAGENTA}╔════════════════════════════════════════════════════════════╗${NC}"
    printf "${MAGENTA}║${NC} %-58s ${MAGENTA}║${NC}\n" "$1"
    echo -e "${MAGENTA}╚════════════════════════════════════════════════════════════╝${NC}\n"
}

print_test_header() {
    echo -e "\n${CYAN}┌────────────────────────────────────────────────────────────┐${NC}"
    printf "${CYAN}│${NC} %-58s ${CYAN}│${NC}\n" "$1"
    echo -e "${CYAN}└────────────────────────────────────────────────────────────┘${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Initialize counters
PASSED=0
FAILED=0
WARNINGS=0
TOTAL_RESPONSE_TIME=0
RESPONSE_COUNT=0

# Array to store test results
declare -a TEST_RESULTS

# Start test suite
print_header "MCP Validation Test Suite"
echo "Start Time: $(date)"
echo "Total Test Cases: $TOTAL_TESTS"
echo "Results Directory: $RESULTS_DIR"
echo ""

# Initialize report file
cat > "$REPORT_FILE" <<EOF
# MCP Validation Test Suite Report

**Execution Date**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
**Model**: claude-sonnet-4-5-20250929
**Total Test Cases**: $TOTAL_TESTS

---

## Executive Summary

EOF

# Run each test case
for TEST_NUM in $(seq 1 $TOTAL_TESTS); do
    print_test_header "Test Case $TEST_NUM"

    # Run test and capture output
    TEST_OUTPUT=$(./test-mcp-validation.sh "$TEST_NUM" --save 2>&1) || TEST_FAILED=true
    TEST_EXIT_CODE=$?

    # Parse results
    RED_FLAG=$(echo "$TEST_OUTPUT" | grep "Red Flag:" | head -1 | grep -oE "(true|false)" | head -1)
    CONFIDENCE=$(echo "$TEST_OUTPUT" | grep "Confidence:" | head -1 | grep -oE "[0-9]+\.[0-9]+")
    COMPLETENESS=$(echo "$TEST_OUTPUT" | grep "Completeness:" | grep -oE "[0-9]+")
    RESPONSE_TIME=$(echo "$TEST_OUTPUT" | grep "Response Time:" | grep -oE "[0-9]+\.[0-9]+")
    WORKFLOW_ID=$(echo "$TEST_OUTPUT" | grep "Workflow submitted:" | awk '{print $NF}')

    # Update counters
    if [ $TEST_EXIT_CODE -eq 0 ]; then
        ((PASSED++))
        TEST_STATUS="PASSED"
        STATUS_ICON="✓"
        STATUS_COLOR="${GREEN}"
    else
        ((FAILED++))
        TEST_STATUS="FAILED"
        STATUS_ICON="✗"
        STATUS_COLOR="${RED}"
    fi

    # Track response times
    if [ -n "$RESPONSE_TIME" ]; then
        TOTAL_RESPONSE_TIME=$(echo "$TOTAL_RESPONSE_TIME + $RESPONSE_TIME" | bc)
        ((RESPONSE_COUNT++))
    fi

    # Display test result
    echo -e "${STATUS_COLOR}${STATUS_ICON} Test Case $TEST_NUM: $TEST_STATUS${NC}"
    echo "  Workflow ID: $WORKFLOW_ID"
    [ -n "$RED_FLAG" ] && echo "  Red Flag: $RED_FLAG"
    [ -n "$CONFIDENCE" ] && echo "  Confidence: $CONFIDENCE"
    [ -n "$COMPLETENESS" ] && echo "  Completeness: ${COMPLETENESS}%"
    [ -n "$RESPONSE_TIME" ] && echo "  Response Time: ${RESPONSE_TIME}s"

    # Store result for report
    TEST_RESULTS[$TEST_NUM]="$TEST_NUM|$TEST_STATUS|$RED_FLAG|$CONFIDENCE|$COMPLETENESS|$RESPONSE_TIME|$WORKFLOW_ID"

    # Add to report
    cat >> "$REPORT_FILE" <<EOF

### Test Case $TEST_NUM: $TEST_STATUS $STATUS_ICON

- **Workflow ID**: \`$WORKFLOW_ID\`
- **Red Flag**: $RED_FLAG
- **Confidence**: $CONFIDENCE
- **Completeness**: ${COMPLETENESS}%
- **Response Time**: ${RESPONSE_TIME}s

EOF

    # Wait between tests to avoid overwhelming the system
    if [ $TEST_NUM -lt $TOTAL_TESTS ]; then
        print_info "Waiting 10s before next test..."
        sleep 10
    fi
done

# Calculate metrics
if [ $RESPONSE_COUNT -gt 0 ]; then
    AVG_RESPONSE_TIME=$(echo "scale=2; $TOTAL_RESPONSE_TIME / $RESPONSE_COUNT" | bc)
else
    AVG_RESPONSE_TIME="N/A"
fi

ACCURACY=$(echo "scale=2; ($PASSED / $TOTAL_TESTS) * 100" | bc)

# Print summary
print_header "Test Suite Summary"

echo -e "${BLUE}Results:${NC}"
echo "  Total Tests: $TOTAL_TESTS"
print_success "Passed: $PASSED"
[ $FAILED -gt 0 ] && print_error "Failed: $FAILED" || echo "  Failed: $FAILED"
echo ""

echo -e "${BLUE}Performance:${NC}"
echo "  Avg Response Time: ${AVG_RESPONSE_TIME}s"
echo "  Total Runtime: $(( (TOTAL_TESTS * WAIT_TIME) + (TOTAL_TESTS * 10) ))s (estimated)"
echo ""

echo -e "${BLUE}Quality Metrics:${NC}"
echo "  Accuracy: ${ACCURACY}%"
echo ""

# Generate detailed results table
print_header "Detailed Results"

printf "${CYAN}%-6s %-10s %-10s %-12s %-14s %-14s${NC}\n" "Test" "Status" "Red Flag" "Confidence" "Completeness" "Time (s)"
echo "────────────────────────────────────────────────────────────────────────────"

for TEST_NUM in $(seq 1 $TOTAL_TESTS); do
    IFS='|' read -r NUM STATUS RED_FLAG CONF COMP TIME WF_ID <<< "${TEST_RESULTS[$TEST_NUM]}"

    # Color code status
    if [ "$STATUS" = "PASSED" ]; then
        STATUS_COLOR="${GREEN}"
    else
        STATUS_COLOR="${RED}"
    fi

    printf "${STATUS_COLOR}%-6s${NC} %-10s %-10s %-12s %-14s %-14s\n" \
        "$NUM" "$STATUS" "$RED_FLAG" "$CONF" "${COMP}%" "${TIME}s"
done

echo ""

# Update report with summary
sed -i.bak "s/## Executive Summary/## Executive Summary\n\n**Total Tests**: $TOTAL_TESTS\n**Passed**: $PASSED\n**Failed**: $FAILED\n**Accuracy**: ${ACCURACY}%\n**Avg Response Time**: ${AVG_RESPONSE_TIME}s\n/" "$REPORT_FILE"
rm "${REPORT_FILE}.bak"

# Add results table to report
cat >> "$REPORT_FILE" <<EOF

---

## Results Table

| Test | Status | Red Flag | Confidence | Completeness | Response Time |
|------|--------|----------|------------|--------------|---------------|
EOF

for TEST_NUM in $(seq 1 $TOTAL_TESTS); do
    IFS='|' read -r NUM STATUS RED_FLAG CONF COMP TIME WF_ID <<< "${TEST_RESULTS[$TEST_NUM]}"
    cat >> "$REPORT_FILE" <<EOF
| $NUM | $STATUS | $RED_FLAG | $CONF | ${COMP}% | ${TIME}s |
EOF
done

cat >> "$REPORT_FILE" <<EOF

---

## Performance Analysis

### Response Time Statistics
- **Average**: ${AVG_RESPONSE_TIME}s
- **Total Runtime**: ~$(( (TOTAL_TESTS * WAIT_TIME) + (TOTAL_TESTS * 10) ))s

### Quality Metrics
- **Accuracy**: ${ACCURACY}%
- **Pass Rate**: $(echo "scale=2; ($PASSED / $TOTAL_TESTS) * 100" | bc)%

---

## Recommendations

EOF

# Add recommendations based on results
if [ $FAILED -eq 0 ]; then
    cat >> "$REPORT_FILE" <<EOF
✓ All tests passed successfully
✓ MCP validator performing as expected
✓ No immediate action required

**Next Steps**:
- Continue monitoring in production
- Schedule regular regression testing
- Expand test coverage as new scenarios emerge

EOF
else
    cat >> "$REPORT_FILE" <<EOF
⚠ $FAILED test(s) failed - investigation required

**Action Items**:
1. Review failed test cases in detail
2. Check Claude response quality and reasoning
3. Validate enrichment data completeness
4. Adjust validator configuration if needed
5. Re-run failed tests after adjustments

EOF
fi

cat >> "$REPORT_FILE" <<EOF

---

## Test Environment

- **Workflow Service**: http://localhost:8089
- **Model**: claude-sonnet-4-5-20250929
- **Temperature**: 0.3
- **Max Tokens**: 4096
- **Wait Time per Test**: ${WAIT_TIME}s

---

**Report Generated**: $(date)
**Report Location**: $REPORT_FILE

EOF

# Generate JSON summary
cat > "$SUMMARY_FILE" <<EOF
{
  "executionTimestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "model": "claude-sonnet-4-5-20250929",
  "configuration": {
    "temperature": 0.3,
    "maxTokens": 4096,
    "waitTimeSeconds": $WAIT_TIME
  },
  "summary": {
    "totalTests": $TOTAL_TESTS,
    "passed": $PASSED,
    "failed": $FAILED,
    "warnings": $WARNINGS,
    "accuracy": $ACCURACY
  },
  "performance": {
    "avgResponseTimeSeconds": $AVG_RESPONSE_TIME,
    "totalRuntimeSeconds": $(( (TOTAL_TESTS * WAIT_TIME) + (TOTAL_TESTS * 10) ))
  },
  "testResults": [
EOF

# Add test results to JSON
for TEST_NUM in $(seq 1 $TOTAL_TESTS); do
    IFS='|' read -r NUM STATUS RED_FLAG CONF COMP TIME WF_ID <<< "${TEST_RESULTS[$TEST_NUM]}"

    cat >> "$SUMMARY_FILE" <<EOF
    {
      "testCase": $NUM,
      "status": "$STATUS",
      "workflowId": "$WF_ID",
      "redFlag": $RED_FLAG,
      "confidence": ${CONF:-null},
      "completeness": ${COMP:-null},
      "responseTimeSeconds": ${TIME:-null}
    }$([ $TEST_NUM -lt $TOTAL_TESTS ] && echo "," || echo "")
EOF
done

cat >> "$SUMMARY_FILE" <<EOF
  ]
}
EOF

print_success "Report saved to: $REPORT_FILE"
print_success "JSON summary saved to: $SUMMARY_FILE"

# Final status
echo ""
print_header "Test Suite Complete"

if [ $FAILED -eq 0 ]; then
    print_success "All tests passed! ✓"
    echo ""
    echo -e "${GREEN}MCP validator is functioning correctly.${NC}"
    EXIT_CODE=0
else
    print_error "$FAILED test(s) failed"
    echo ""
    echo -e "${RED}Review the report for details: $REPORT_FILE${NC}"
    EXIT_CODE=1
fi

echo ""
echo "Execution completed at: $(date)"

exit $EXIT_CODE
