# Agentic Workflow Test Cases - Document Validation Agent

## Test Strategy

### Objectives
1. Verify agent executes correctly in workflow Phase 1
2. Validate document checking logic (presence, accessibility, consistency, compliance)
3. Confirm metadata enrichment with 6 fields
4. Test red flag detection and severity classification
5. Verify DMN rules can use agent enrichment data
6. Test workflow behavior with agent failures
7. Validate reasoning trace completeness
8. Test concurrent workflow executions

### Test Levels
- **Unit Tests**: Individual validator methods
- **Integration Tests**: Agent + Temporal activity
- **End-to-End Tests**: Full workflow with agent
- **Performance Tests**: Agent execution time, concurrent workflows

---

## Test Case Categories

### Category 1: Document Presence Validation
### Category 2: Document Accessibility Validation
### Category 3: Pricing Consistency Validation
### Category 4: Regulatory Compliance Validation
### Category 5: Red Flag Detection & Severity
### Category 6: Metadata Enrichment
### Category 7: Workflow Integration
### Category 8: Error Handling & Resilience
### Category 9: Performance & Scalability

---

## Category 1: Document Presence Validation

### TC-DOC-001: All Required Documents Present
**Priority**: P0 (Critical)
**Type**: Positive Test

**Preconditions**:
- Workflow service running
- MongoDB available
- Temporal worker active

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Complete Documentation Test",
  "pricingVariance": 10,
  "termsAndConditionsUrl": "https://docs.example.com/terms.pdf",
  "disclosureUrl": "https://docs.example.com/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf",
  "documentationUrl": "https://docs.example.com/product-docs.pdf"
}
```

**Expected Results**:
- ✅ Agent executes successfully
- ✅ `missingDocumentCount` = 0
- ✅ `documentCompleteness` >= 0.9
- ✅ `documentValidationStatus` = "PASS" or "PASS_WITH_WARNINGS"
- ✅ Red flag NOT detected
- ✅ Reasoning step 1: "All required documents are present"
- ✅ Workflow proceeds to approval

**Validation Steps**:
```bash
# 1. Submit solution
RESPONSE=$(curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d @test-data/tc-doc-001.json -s)

SOLUTION_ID=$(echo "$RESPONSE" | jq -r '.solutionId')

# 2. Poll workflow status
sleep 3
WORKFLOW_STATUS=$(curl -u admin:admin123 \
  http://localhost:8082/api/v1/solutions/$SOLUTION_ID/workflow-status \
  -H "X-Tenant-ID: tenant-001" -s)

# 3. Validate enrichment data
WORKFLOW_ID=$(echo "$WORKFLOW_STATUS" | jq -r '.workflowId')
WORKFLOW_DETAILS=$(curl -u admin:admin123 \
  http://localhost:8089/api/v1/workflows/$WORKFLOW_ID -s)

# 4. Check agent execution logs
docker logs workflow-service 2>&1 | grep -A10 "Document validation completed" | tail -10
```

**Assertions**:
- [ ] missingDocumentCount == 0
- [ ] documentCompleteness >= 0.9
- [ ] documentValidationStatus == "PASS"
- [ ] workflowState == "PENDING_APPROVAL"

---

### TC-DOC-002: Missing Required Terms & Conditions
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Missing T&C Test",
  "pricingVariance": 10,
  "disclosureUrl": "https://docs.example.com/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf"
}
```

**Expected Results**:
- ✅ Agent executes successfully
- ✅ `missingDocumentCount` >= 1
- ✅ `documentCompleteness` < 0.9
- ✅ `documentValidationStatus` = "FAIL" or "PASS_WITH_WARNINGS"
- ✅ Red flag detected: severity = "HIGH" or "CRITICAL"
- ✅ Reasoning step 1: "Found X missing document(s)"
- ✅ Missing document details:
  - documentType: "TERMS_AND_CONDITIONS"
  - documentName: "Terms and Conditions"
  - required: true
  - reason: "Required for all products"

**Assertions**:
- [ ] missingDocuments array contains TERMS_AND_CONDITIONS
- [ ] missingDocuments[0].required == true
- [ ] documentCompleteness < 0.9
- [ ] redFlagDetected == true
- [ ] severity in ["HIGH", "CRITICAL"]

---

### TC-DOC-003: Missing All Required Documents
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "No Documents Test",
  "pricingVariance": 10
}
```

**Expected Results**:
- ✅ `missingDocumentCount` >= 3
- ✅ `documentCompleteness` <= 0.25
- ✅ `documentValidationStatus` = "FAIL"
- ✅ Red flag detected: severity = "CRITICAL"
- ✅ Missing documents include:
  - TERMS_AND_CONDITIONS
  - DISCLOSURE
  - FEE_SCHEDULE
- ✅ Recommendations include: "Upload X required document(s) before submission"

**Assertions**:
- [ ] missingDocumentCount >= 3
- [ ] documentCompleteness <= 0.5
- [ ] documentValidationStatus == "FAIL"
- [ ] redFlagSeverity == "CRITICAL"
- [ ] recommendations.length > 0

---

### TC-DOC-004: Optional Documentation Missing
**Priority**: P1 (High)
**Type**: Positive Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Optional Doc Missing Test",
  "pricingVariance": 10,
  "termsAndConditionsUrl": "https://docs.example.com/terms.pdf",
  "disclosureUrl": "https://docs.example.com/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf"
}
```
*Note: documentationUrl is optional*

**Expected Results**:
- ✅ `missingDocumentCount` = 1
- ✅ `documentCompleteness` >= 0.7
- ✅ `documentValidationStatus` = "PASS_WITH_WARNINGS"
- ✅ Red flag NOT detected (or severity = "LOW")
- ✅ Warning: "Optional documentation missing"

**Assertions**:
- [ ] missingDocuments contains PRODUCT_DOCUMENTATION
- [ ] missingDocuments[0].required == false
- [ ] documentValidationStatus in ["PASS", "PASS_WITH_WARNINGS"]
- [ ] redFlagDetected == false OR severity == "LOW"

---

## Category 2: Document Accessibility Validation

### TC-ACC-001: All URLs Valid and Accessible
**Priority**: P0 (Critical)
**Type**: Positive Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Valid URLs Test",
  "termsAndConditionsUrl": "https://docs.example.com/terms.pdf",
  "disclosureUrl": "https://docs.example.com/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf"
}
```

**Expected Results**:
- ✅ `inaccessibleDocuments` = []
- ✅ `allDocumentsAccessible` = true
- ✅ Reasoning step 2: "All document URLs are accessible"

**Assertions**:
- [ ] inaccessibleDocuments.length == 0
- [ ] allDocumentsAccessible == true

---

### TC-ACC-002: Invalid URL Format (No Protocol)
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Invalid URL Test",
  "termsAndConditionsUrl": "docs.example.com/terms.pdf",
  "disclosureUrl": "https://docs.example.com/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf"
}
```

**Expected Results**:
- ✅ `inaccessibleDocuments` contains 1 entry
- ✅ `allDocumentsAccessible` = false
- ✅ Inaccessible document details:
  - documentType: "TERMS_AND_CONDITIONS"
  - url: "docs.example.com/terms.pdf"
  - statusCode: 0
  - error: "Invalid URL format - must start with http:// or https://"

**Assertions**:
- [ ] inaccessibleDocuments.length == 1
- [ ] inaccessibleDocuments[0].error contains "Invalid URL format"
- [ ] allDocumentsAccessible == false

---

### TC-ACC-003: Multiple Invalid URLs
**Priority**: P1 (High)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Multiple Invalid URLs Test",
  "termsAndConditionsUrl": "ftp://docs.example.com/terms.pdf",
  "disclosureUrl": "file:///local/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf"
}
```

**Expected Results**:
- ✅ `inaccessibleDocuments` contains 2 entries
- ✅ Red flag severity = "HIGH"

**Assertions**:
- [ ] inaccessibleDocuments.length == 2
- [ ] redFlagDetected == true

---

## Category 3: Pricing Consistency Validation

### TC-PRICE-001: Pricing Matches Documentation
**Priority**: P0 (Critical)
**Type**: Positive Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Consistent Pricing Test",
  "pricingVariance": 10,
  "monthlyFee": 25.00,
  "interestRate": 2.5,
  "termsAndConditionsUrl": "https://docs.example.com/terms.pdf",
  "disclosureUrl": "https://docs.example.com/disclosure.pdf",
  "feeScheduleUrl": "https://docs.example.com/fees.pdf",
  "metadata": {
    "documentedMonthlyFee": 25.00,
    "documentedInterestRate": 2.5
  }
}
```

**Expected Results**:
- ✅ `inconsistencyCount` = 0
- ✅ `hasInconsistencies` = false
- ✅ Reasoning step 3: "Configuration matches documented values"

**Assertions**:
- [ ] inconsistencies.length == 0
- [ ] hasInconsistencies == false

---

### TC-PRICE-002: Monthly Fee Mismatch
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Fee Mismatch Test",
  "monthlyFee": 25.00,
  "metadata": {
    "documentedMonthlyFee": 30.00
  }
}
```

**Expected Results**:
- ✅ `inconsistencyCount` >= 1
- ✅ `hasInconsistencies` = true
- ✅ Inconsistency details:
  - field: "monthlyFee"
  - configuredValue: "$25"
  - documentedValue: "$30"
  - documentType: "FEE_SCHEDULE"
  - severity: "HIGH"
- ✅ Red flag severity = "HIGH"

**Assertions**:
- [ ] inconsistencies.length >= 1
- [ ] inconsistencies[0].field == "monthlyFee"
- [ ] inconsistencies[0].severity in ["HIGH", "CRITICAL"]

---

### TC-PRICE-003: Interest Rate Mismatch (Critical)
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-savings-001",
  "solutionName": "Interest Rate Mismatch Test",
  "interestRate": 3.5,
  "metadata": {
    "documentedInterestRate": 2.5
  }
}
```

**Expected Results**:
- ✅ Inconsistency severity = "CRITICAL"
- ✅ Red flag severity = "CRITICAL"
- ✅ Recommendations: "Resolve X inconsistency/inconsistencies between configuration and documents"

**Assertions**:
- [ ] inconsistencies[0].severity == "CRITICAL"
- [ ] redFlagSeverity == "CRITICAL"

---

## Category 4: Regulatory Compliance Validation

### TC-COMP-001: Checking Account with Full Compliance
**Priority**: P0 (Critical)
**Type**: Positive Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Compliant Checking Test",
  "productType": "CHECKING",
  "hasOverdraft": false,
  "metadata": {
    "disclosures": ["REG_DD", "FDIC_NOTICE"]
  }
}
```

**Expected Results**:
- ✅ `complianceGapCount` = 0
- ✅ Reasoning step 4: "All regulatory requirements met"

**Assertions**:
- [ ] complianceGaps.length == 0

---

### TC-COMP-002: Checking Account Missing Reg DD
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Missing Reg DD Test",
  "productType": "CHECKING",
  "metadata": {
    "disclosures": ["FDIC_NOTICE"]
  }
}
```

**Expected Results**:
- ✅ `complianceGapCount` >= 1
- ✅ Compliance gap details:
  - regulation: "Regulation DD (Truth in Savings)"
  - requirement: "Account disclosure statement"
  - gap: "Missing Reg DD disclosure in documentation"
  - severity: "CRITICAL"
- ✅ Red flag severity = "CRITICAL"
- ✅ Recommendations: "Address X critical compliance gap(s)"

**Assertions**:
- [ ] complianceGaps.length >= 1
- [ ] complianceGaps[0].regulation contains "Regulation DD"
- [ ] complianceGaps[0].severity == "CRITICAL"

---

### TC-COMP-003: Savings Account Missing FDIC Notice
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-savings-001",
  "solutionName": "Missing FDIC Test",
  "productType": "SAVINGS",
  "metadata": {
    "disclosures": ["REG_DD"]
  }
}
```

**Expected Results**:
- ✅ Compliance gap:
  - regulation: "FDIC Insurance"
  - requirement: "FDIC insurance notice"
  - gap: "Missing FDIC insurance disclosure"
  - severity: "ERROR"

**Assertions**:
- [ ] complianceGaps contains FDIC_NOTICE gap
- [ ] complianceGaps[0].severity in ["ERROR", "CRITICAL"]

---

### TC-COMP-004: Overdraft Feature Missing Reg E Disclosure
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Overdraft Compliance Test",
  "productType": "CHECKING",
  "hasOverdraft": true,
  "metadata": {
    "disclosures": ["REG_DD", "FDIC_NOTICE"]
  }
}
```

**Expected Results**:
- ✅ Compliance gap:
  - regulation: "Regulation E"
  - requirement: "Overdraft opt-in disclosure"
  - gap: "Missing Reg E overdraft opt-in form"
  - severity: "CRITICAL"
- ✅ Red flag severity = "CRITICAL"

**Assertions**:
- [ ] complianceGaps contains REG_E_OVERDRAFT gap
- [ ] redFlagSeverity == "CRITICAL"

---

### TC-COMP-005: Multiple Compliance Gaps
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**:
```json
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Multiple Compliance Gaps Test",
  "productType": "CHECKING",
  "hasOverdraft": true,
  "metadata": {
    "disclosures": []
  }
}
```

**Expected Results**:
- ✅ `complianceGapCount` >= 3 (REG_DD, FDIC_NOTICE, REG_E_OVERDRAFT)
- ✅ Red flag severity = "CRITICAL"
- ✅ documentCompleteness significantly reduced

**Assertions**:
- [ ] complianceGaps.length >= 3
- [ ] complianceGaps contains all 3 critical gaps
- [ ] documentCompleteness < 0.5

---

## Category 5: Red Flag Detection & Severity

### TC-FLAG-001: No Red Flags (Clean Submission)
**Priority**: P0 (Critical)
**Type**: Positive Test

**Test Data**: Perfect configuration (all documents, compliant, consistent)

**Expected Results**:
- ✅ `redFlagDetected` = false
- ✅ `severity` = null or "LOW"
- ✅ Workflow proceeds normally to approval

**Assertions**:
- [ ] redFlagDetected == false
- [ ] workflowState == "PENDING_APPROVAL"

---

### TC-FLAG-002: Low Severity Red Flag
**Priority**: P1 (High)
**Type**: Edge Case

**Test Data**: Optional documentation missing only

**Expected Results**:
- ✅ `redFlagDetected` = true
- ✅ `severity` = "LOW"
- ✅ Workflow proceeds to approval (not blocked)

**Assertions**:
- [ ] redFlagDetected == true
- [ ] severity == "LOW"
- [ ] workflowState == "PENDING_APPROVAL"

---

### TC-FLAG-003: Medium Severity Red Flag
**Priority**: P1 (High)
**Type**: Negative Test

**Test Data**: Minor inconsistencies, no critical issues

**Expected Results**:
- ✅ `severity` = "MEDIUM"
- ✅ Workflow proceeds to approval (enhanced review recommended)

**Assertions**:
- [ ] severity == "MEDIUM"
- [ ] workflowState == "PENDING_APPROVAL"

---

### TC-FLAG-004: High Severity Red Flag
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**: Required documents missing

**Expected Results**:
- ✅ `severity` = "HIGH"
- ✅ `redFlagReason` contains "required document(s) missing"
- ✅ Workflow continues but flagged for enhanced approval

**Assertions**:
- [ ] severity == "HIGH"
- [ ] redFlagReason contains "required document"

---

### TC-FLAG-005: Critical Severity Red Flag
**Priority**: P0 (Critical)
**Type**: Negative Test

**Test Data**: Critical compliance gaps + inconsistencies

**Expected Results**:
- ✅ `severity` = "CRITICAL"
- ✅ Multiple critical issues detected
- ✅ Workflow continues (current behavior) or auto-rejects (future enhancement)

**Assertions**:
- [ ] severity == "CRITICAL"
- [ ] complianceGapCount > 0 OR criticalInconsistencies > 0

---

## Category 6: Metadata Enrichment

### TC-ENRICH-001: All 6 Fields Enriched
**Priority**: P0 (Critical)
**Type**: Integration Test

**Verification**:
```bash
# Check workflow logs for enrichment
docker logs workflow-service 2>&1 | grep "Metadata enriched with"
# Should see: "Phase 2: Metadata enriched with 6 agent outputs"
```

**Expected Enrichment Fields**:
1. `documentCompleteness` (number, 0.0-1.0)
2. `documentValidationStatus` (string: PASS/FAIL/PASS_WITH_WARNINGS)
3. `missingDocumentCount` (number)
4. `inconsistencyCount` (number)
5. `complianceGapCount` (number)
6. `documentRecommendations` (array)

**Assertions**:
- [ ] All 6 fields present in enriched metadata
- [ ] Field types match expected
- [ ] Values are within valid ranges

---

### TC-ENRICH-002: Enriched Data Available to DMN Rules
**Priority**: P0 (Critical)
**Type**: Integration Test

**Test Setup**:
Create workflow template with rules using agent fields:
```json
{
  "rules": [
    {
      "conditions": {
        "documentCompleteness": "< 0.7",
        "complianceGapCount": "> 0"
      },
      "outputs": {
        "approverRoles": ["PRODUCT_MANAGER", "COMPLIANCE_OFFICER"],
        "approvalCount": 2
      }
    }
  ]
}
```

**Expected Results**:
- ✅ DMN rules can access `documentCompleteness`
- ✅ DMN rules can access `complianceGapCount`
- ✅ Enhanced approval triggered when conditions met

**Assertions**:
- [ ] requiredApprovals increased based on agent data
- [ ] Additional approver roles added

---

### TC-ENRICH-003: Enrichment with Agent Failure
**Priority**: P1 (High)
**Type**: Error Handling

**Test Setup**: Simulate agent failure

**Expected Results**:
- ✅ Enrichment fields set to safe defaults or null
- ✅ Workflow continues (agent is non-blocking)
- ✅ Error logged

**Assertions**:
- [ ] workflowState != "FAILED"
- [ ] Agent error logged but workflow proceeds

---

## Category 7: Workflow Integration

### TC-WF-001: Agent Execution in Phase 1
**Priority**: P0 (Critical)
**Type**: Integration Test

**Verification**:
```bash
# Check logs for phase execution order
docker logs workflow-service 2>&1 | grep "Phase [1-4]"
```

**Expected Log Sequence**:
```
Phase 1: Executing document validation agent
Phase 2: Metadata enriched with 6 agent outputs
Phase 3: Rules evaluated: approvalRequired=true
Phase 4: Waiting for N approvals
```

**Assertions**:
- [ ] Phase 1 executes first
- [ ] Agent completes before Phase 2
- [ ] Enrichment happens before Phase 3
- [ ] Rules evaluate after enrichment

---

### TC-WF-002: Reasoning Trace Completeness
**Priority**: P1 (High)
**Type**: Integration Test

**Expected Reasoning Steps**:
1. "Check Required Documents"
2. "Check Document Accessibility"
3. "Check Pricing Consistency"
4. "Check Regulatory Compliance"

**Assertions**:
- [ ] 4 reasoning steps present
- [ ] Each step has input/output/reasoning
- [ ] Steps are in correct order
- [ ] Timestamps are sequential

---

### TC-WF-003: Agent Execution Time
**Priority**: P2 (Medium)
**Type**: Performance Test

**Expected Results**:
- ✅ Agent execution < 5 seconds
- ✅ Total workflow submission < 10 seconds

**Assertions**:
- [ ] executionTime < 5000ms
- [ ] Workflow submission doesn't timeout

---

### TC-WF-004: Concurrent Workflow Executions
**Priority**: P1 (High)
**Type**: Load Test

**Test Setup**: Submit 10 workflows simultaneously

**Expected Results**:
- ✅ All agents execute successfully
- ✅ No race conditions
- ✅ Each workflow gets unique agent decision
- ✅ No cross-contamination of enrichment data

**Assertions**:
- [ ] 10 successful agent executions
- [ ] 10 workflows reach PENDING_APPROVAL
- [ ] Enrichment data is workflow-specific

---

### TC-WF-005: Workflow Status Query During Agent Execution
**Priority**: P2 (Medium)
**Type**: Integration Test

**Test Steps**:
1. Submit workflow
2. Immediately query status (while agent executing)
3. Query again after completion

**Expected Results**:
- ✅ Initial query: workflowState = "INITIATED" or "VALIDATION"
- ✅ Final query: workflowState = "PENDING_APPROVAL"
- ✅ No errors during intermediate queries

**Assertions**:
- [ ] Status queries don't fail during agent execution
- [ ] State transitions are valid

---

## Category 8: Error Handling & Resilience

### TC-ERR-001: Agent Activity Timeout
**Priority**: P1 (High)
**Type**: Error Handling

**Test Setup**: Configure very short timeout (1ms)

**Expected Results**:
- ✅ Activity times out
- ✅ Temporal retries (up to 2 times)
- ✅ Workflow fails gracefully or continues with defaults

**Assertions**:
- [ ] Retry attempts logged
- [ ] Workflow doesn't hang indefinitely

---

### TC-ERR-002: Agent Throws Exception
**Priority**: P1 (High)
**Type**: Error Handling

**Test Setup**: Force agent to throw exception (e.g., null solution data)

**Expected Results**:
- ✅ Exception caught in AgentExecutorService
- ✅ AgentDecision.success = false
- ✅ AgentDecision.errorMessage populated
- ✅ Workflow continues (non-blocking)

**Assertions**:
- [ ] agentDecision.success == false
- [ ] agentDecision.errorMessage != null
- [ ] workflowState != "FAILED"

---

### TC-ERR-003: MongoDB Connection Lost During Validation
**Priority**: P1 (High)
**Type**: Resilience

**Test Setup**: Stop MongoDB mid-validation

**Expected Results**:
- ✅ Connection error caught
- ✅ Agent returns error state
- ✅ Workflow can retry or fail gracefully

---

### TC-ERR-004: Invalid Entity Data Structure
**Priority**: P1 (High)
**Type**: Error Handling

**Test Data**: Malformed entityData (missing required fields)

**Expected Results**:
- ✅ Agent handles missing fields gracefully
- ✅ Validation continues with available data
- ✅ Warnings logged for missing fields

**Assertions**:
- [ ] No NullPointerException
- [ ] Agent completes execution
- [ ] Warnings in logs

---

### TC-ERR-005: Temporal Worker Restart During Workflow
**Priority**: P2 (Medium)
**Type**: Resilience

**Test Steps**:
1. Start workflow
2. Restart workflow-service during Phase 1
3. Verify workflow resumes

**Expected Results**:
- ✅ Workflow resumes after restart
- ✅ Agent re-executes (idempotent)
- ✅ No data loss

**Assertions**:
- [ ] Workflow completes successfully
- [ ] Same workflowId maintained

---

## Category 9: Performance & Scalability

### TC-PERF-001: Agent Execution Baseline
**Priority**: P2 (Medium)
**Type**: Performance

**Measurement**: Single workflow execution time

**Expected Results**:
- ✅ Agent execution: < 3 seconds
- ✅ Workflow submission: < 10 seconds
- ✅ Metadata enrichment: < 100ms

**Assertions**:
- [ ] p50 < 3s
- [ ] p95 < 5s
- [ ] p99 < 8s

---

### TC-PERF-002: Concurrent Workflows (10 simultaneous)
**Priority**: P1 (High)
**Type**: Load Test

**Test Setup**: Submit 10 workflows at once

**Expected Results**:
- ✅ All complete within 15 seconds
- ✅ No degradation > 2x baseline
- ✅ No failures

**Assertions**:
- [ ] Success rate = 100%
- [ ] Max execution time < 15s

---

### TC-PERF-003: Concurrent Workflows (50 simultaneous)
**Priority**: P2 (Medium)
**Type**: Stress Test

**Test Setup**: Submit 50 workflows at once

**Expected Results**:
- ✅ All complete within 60 seconds
- ✅ Worker pool handles load
- ✅ No OOM errors

**Assertions**:
- [ ] Success rate >= 95%
- [ ] Max execution time < 60s
- [ ] Memory usage < 2GB

---

### TC-PERF-004: Sustained Load (100 workflows/minute)
**Priority**: P2 (Medium)
**Type**: Endurance Test

**Test Duration**: 10 minutes

**Expected Results**:
- ✅ 1000 workflows processed
- ✅ No memory leaks
- ✅ Consistent performance throughout

**Assertions**:
- [ ] Total processed >= 1000
- [ ] Memory usage stable
- [ ] Last minute performance == first minute

---

## Test Execution Plan

### Phase 1: Critical Path (P0 Tests)
**Duration**: 2-3 hours
**Tests**: All P0 tests (30+ test cases)
**Goal**: Verify core functionality

### Phase 2: Extended Coverage (P1 Tests)
**Duration**: 4-6 hours
**Tests**: All P1 tests (20+ test cases)
**Goal**: Error handling and edge cases

### Phase 3: Performance & Load (P2 Tests)
**Duration**: 2-4 hours
**Tests**: All P2 tests (10+ test cases)
**Goal**: Validate scalability

---

## Test Data Setup

### Catalog Products Required
```bash
# Ensure these exist in MongoDB
- cat-checking-001 (Checking account)
- cat-savings-001 (Savings account)
```

### Users Required
```bash
# Ensure these users exist
- admin:admin123 (ROLE_ADMIN, ROLE_USER)
- catalog-user:catalog123 (ROLE_USER)
```

### Workflow Templates Required
```bash
# Ensure template exists
- SOLUTION_CONFIG_V1 (or V3 with agent config)
```

---

## Success Criteria

### Functional Requirements
- ✅ 100% of P0 tests pass
- ✅ 95% of P1 tests pass
- ✅ 90% of P2 tests pass

### Performance Requirements
- ✅ Agent execution < 5s (p95)
- ✅ Workflow submission < 10s (p95)
- ✅ 50 concurrent workflows supported

### Reliability Requirements
- ✅ Agent failure doesn't block workflow
- ✅ Retries work correctly
- ✅ No data corruption under load

---

## Test Reporting Template

### Test Case: TC-XXX-NNN
**Status**: ✅ PASS / ❌ FAIL / ⚠️ BLOCKED
**Execution Date**: YYYY-MM-DD
**Executed By**: [Name]
**Duration**: Xs

**Results**:
- Assertion 1: ✅ PASS
- Assertion 2: ❌ FAIL - Expected X, Got Y
- Assertion 3: ✅ PASS

**Evidence**:
- Logs: [Link to logs]
- Screenshots: [Attach if applicable]
- Data: [Attach test data/results]

**Defects**:
- BUG-001: [Description if failed]

**Notes**:
[Any additional observations]
