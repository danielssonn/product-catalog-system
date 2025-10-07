# MCP Validation Testing Guide

Quick reference for testing the MCP (Model Context Protocol) validator integration with Claude AI.

---

## Prerequisites

- Bash 5+ installed: `brew install bash`
- Workflow service running: `docker-compose up -d workflow-service`
- ANTHROPIC_API_KEY configured in `.env` file
- MongoDB running on port 27018

---

## Quick Start

### Test Individual Scenario

```bash
./test-mcp-validation.sh <test-number>
```

**Test Cases:**
1. Premium Checking Account
2. High-Yield Savings Account
3. Mortgage Loan Disclosure
4. Credit Card Fee Schedule
5. Incomplete Student Account Draft
6. Problematic Business Checking Pricing
7. Non-Compliant CD Marketing
8. Missing Documentation

### Run Full Test Suite

```bash
./run-mcp-test-suite.sh
```

---

## Test Options

### Verbose Mode

Get detailed output including request payload and full responses:

```bash
./test-mcp-validation.sh 1 --verbose
```

### Save Results

Save test results to JSON file:

```bash
./test-mcp-validation.sh 1 --save
```

Results saved to: `test-results/mcp-validation/test-case-X_[timestamp].json`

### Custom Wait Time

Adjust wait time for Claude validation (default: 35 seconds):

```bash
./test-mcp-validation.sh 1 --wait 45
```

---

## Understanding Test Results

### Successful Test Output

```
╔════════════════════════════════════════════════════════════╗
║ MCP Validation Test Case 1                                ║
╚════════════════════════════════════════════════════════════╝

Product: Premium Checking Account
Type: SAVINGS
Pricing Variance: 12.5%
Expected Red Flag: true

✓ Workflow submitted: [UUID]
ℹ Waiting 35s for Claude validation...

MCP Validation Results:
├─ Red Flag: true ✓ (Expected: true)
├─ Confidence: 1.0 ✓ (Expected: 0.9+)
├─ Completeness: 0%
├─ Response Time: 25.8s ✓ (< 60s)
└─ Enrichment Fields: 11 ✓

Claude Reasoning:
"Critical compliance failure: No required documents provided..."

✓ TEST CASE 1: PASSED ✓
```

### Key Metrics

| Metric | Acceptable | Explanation |
|--------|-----------|-------------|
| Red Flag | true | Documents missing (expected in tests) |
| Confidence | ≥ 0.85 | Claude's certainty in assessment |
| Response Time | < 60s | API call performance |
| Enrichment Fields | ≥ 8 | Metadata for DMN rules |

---

## Interpreting Results

### Why All Tests Show Red Flags

**This is expected behavior!** The tests use document descriptions only, not actual document files. Claude correctly identifies that no actual documentation is attached.

**What this validates:**
- ✅ Claude properly detects missing documentation
- ✅ API integration working
- ✅ Response parsing functional
- ✅ Enrichment data flowing to DMN rules

**For testing actual document content:**
See "Next Steps" section in [MCP_TEST_SUMMARY.md](MCP_TEST_SUMMARY.md)

### Claude's Reasoning

Each test includes Claude's reasoning. Example:

```
"Critical compliance documentation is completely missing. No
Terms & Conditions, Fee Schedule, or Disclosure Statement
provided. Product cannot be offered to consumers without
required regulatory disclosures per Regulation DD and FDIC
requirements."
```

This demonstrates:
- ✅ Regulatory knowledge (Reg DD, FDIC)
- ✅ Document requirement understanding
- ✅ Professional quality assessment
- ✅ Actionable feedback

---

## Common Issues

### Issue: Test Fails with "bad interpreter"

```bash
./test-mcp-validation.sh: bad interpreter: /opt/homebrew/bin/bash: no such file
```

**Solution:** Install bash 5:
```bash
brew install bash
```

### Issue: "500 - Overloaded" Error

```
org.springframework.ai.retry.TransientAiException: 500 - Overloaded
```

**Solution:** This is a transient Claude API error. The system will retry automatically.
- Wait a few minutes
- Run test again
- Check Anthropic status: https://status.anthropic.com

### Issue: Workflow Not Found

```
✗ Failed to submit workflow
```

**Solution:** Check workflow service is running:
```bash
docker-compose ps workflow-service
docker logs workflow-service
```

### Issue: API Key Not Configured

```
401 - x-api-key header is required
```

**Solution:** Create `.env` file in project root:
```bash
echo "ANTHROPIC_API_KEY=your-api-key-here" > .env
docker-compose down
docker-compose up -d
```

---

## Test Results Location

### Individual Tests

```
test-results/mcp-validation/
├── test-case-1_20250107_143000.json
├── test-case-2_20250107_143100.json
└── ...
```

### Full Suite Reports

```
test-results/mcp-validation/
├── test-suite-report_20250107_143000.md   # Detailed report
└── test-suite-summary_20250107_143000.json # Machine-readable summary
```

---

## Sample Documents

Reference documents created for MCP testing:

```
test-data/documents/
├── premium-checking-terms.md      # Complete checking T&C
├── high-yield-savings-terms.md    # Complete savings docs
├── mortgage-loan-disclosure.md    # Complete mortgage disclosure
├── credit-card-fee-schedule.md    # Complete credit card fees
├── incomplete-product-draft.md    # Draft with [TBD] items
├── problematic-pricing.md         # Pricing violations
└── non-compliant-marketing.md     # Marketing violations
```

See [test-data/README.md](test-data/README.md) for document descriptions.

---

## Workflow Service API

### Manual Testing

Submit workflow directly:

```bash
curl -u admin:admin123 -X POST http://localhost:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -d '{
    "workflowType": "SOLUTION_CONFIGURATION",
    "entityType": "SOLUTION_CONFIGURATION",
    "entityId": "test-001",
    "entityData": {
      "solutionName": "Test Product",
      "description": "Product description",
      "productType": "SAVINGS"
    },
    "entityMetadata": {
      "pricingVariance": 10.0,
      "riskLevel": "MEDIUM",
      "tenantId": "test-tenant"
    },
    "initiatedBy": "test@bank.com",
    "priority": "NORMAL"
  }'
```

### Check Workflow Status

```bash
curl -u admin:admin123 http://localhost:8089/api/v1/workflows/[WORKFLOW_ID]
```

### Check Logs

```bash
# Watch validation logs live
docker logs -f workflow-service | grep -E "MCP|Claude|Phase|RED FLAG"

# Search for specific workflow
docker logs workflow-service | grep [WORKFLOW_ID]

# Check for errors
docker logs workflow-service 2>&1 | grep -i error | tail -20
```

---

## Performance Benchmarks

Based on test results:

| Metric | Value | Target |
|--------|-------|--------|
| Avg Response Time | 28.5s | < 60s ✓ |
| Min Response Time | 23.2s | N/A |
| Max Response Time | 35.4s | < 60s ✓ |
| Success Rate | 100% | > 95% ✓ |
| Enrichment Fields | 11 | ≥ 8 ✓ |
| Confidence Score | 0.9-1.0 | ≥ 0.85 ✓ |

---

## Troubleshooting Checklist

- [ ] Bash 5 installed: `/opt/homebrew/bin/bash --version`
- [ ] Docker containers running: `docker-compose ps`
- [ ] MongoDB accessible: `mongosh --host localhost --port 27018`
- [ ] Workflow service healthy: `curl http://localhost:8089/actuator/health`
- [ ] API key configured: `docker logs workflow-service | grep ANTHROPIC_API_KEY`
- [ ] Temporal worker running: `docker logs workflow-service | grep "Worker started"`
- [ ] Test scripts executable: `ls -l *.sh`

---

## Next Steps

### Phase 1: Validate Integration Health

```bash
# Run single test to validate setup
./test-mcp-validation.sh 1

# If successful, run full suite
./run-mcp-test-suite.sh
```

### Phase 2: Review Results

```bash
# Review detailed report
cat test-results/mcp-validation/test-suite-report_*.md | less

# Check JSON summary
cat test-results/mcp-validation/test-suite-summary_*.json | jq '.'
```

### Phase 3: Document Integration

See [MCP_TEST_SUMMARY.md](MCP_TEST_SUMMARY.md) for recommendations on:
- Adding document attachment support
- Testing with real PDF/markdown files
- Production deployment considerations

---

## Additional Resources

- **MCP Integration Guide**: [MCP_INTEGRATION_GUIDE.md](MCP_INTEGRATION_GUIDE.md)
- **Test Suite Specification**: [MCP_TEST_SUITE.md](MCP_TEST_SUITE.md)
- **Test Summary & Fixes**: [MCP_TEST_SUMMARY.md](MCP_TEST_SUMMARY.md)
- **Sample Documents**: [test-data/README.md](test-data/README.md)

---

## Quick Commands

```bash
# Status check
docker-compose ps

# Run one test
./test-mcp-validation.sh 1

# Run all tests
./run-mcp-test-suite.sh

# Check logs
docker logs workflow-service | tail -50

# Restart services
docker-compose restart workflow-service

# View latest test report
ls -t test-results/mcp-validation/*.md | head -1 | xargs cat
```

---

**Questions?** See [MCP_TEST_SUMMARY.md](MCP_TEST_SUMMARY.md) or check workflow service logs.

**Last Updated**: January 7, 2025
