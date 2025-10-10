# Document Validation Agent - Implementation Guide

## Overview

Document Validation Agent is the first agentic workflow capability added to the workflow service. It validates product configuration documents before human approval, catching issues early and enriching the approval decision with document completeness insights.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Workflow Submission                             │
│         POST /api/v1/solutions/configure                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│         Temporal Workflow V3 (ApprovalWorkflowImplV3)       │
│                                                              │
│  PHASE 1: Agent Execution                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Document Validation Agent                            │  │
│  │  - Check required documents                           │  │
│  │  - Validate URLs                                      │  │
│  │  - Check pricing consistency                          │  │
│  │  - Verify regulatory compliance                       │  │
│  │  - Check terms coverage                               │  │
│  └──────────────────────────────────────────────────────┘  │
│                       ↓                                      │
│  RED FLAG DETECTED?                                          │
│  ├─→ YES → Auto-reject OR Enhanced approval                 │
│  └─→ NO  → Continue                                          │
│                       ↓                                      │
│  PHASE 2: Metadata Enrichment                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Enrich entityMetadata with:                          │  │
│  │  - documentCompleteness: 0.85                         │  │
│  │  - documentValidationStatus: "PASS"                   │  │
│  │  - missingDocumentCount: 0                            │  │
│  │  - inconsistencyCount: 0                              │  │
│  │  - complianceGapCount: 0                              │  │
│  │  - documentRecommendations: [...]                     │  │
│  └──────────────────────────────────────────────────────┘  │
│                       ↓                                      │
│  PHASE 3: DMN Rule Evaluation (with enriched data)          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Rules can now use:                                   │  │
│  │  - pricingVariance (original)                         │  │
│  │  - documentCompleteness (from agent)                  │  │
│  │  - complianceGapCount (from agent)                    │  │
│  └──────────────────────────────────────────────────────┘  │
│                       ↓                                      │
│  PHASE 4: Human Approval (with AI insights)                 │
└─────────────────────────────────────────────────────────────┘
```

## Components Created

### 1. Agent Domain Models

**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/agent/model/`

- **AgentType.java** - Types of agents (MCP, GRAPH_RAG, CUSTOM)
- **AgentExecutionMode.java** - Execution modes (ASYNC_RED_FLAG, SYNC_ENRICHMENT, HYBRID)
- **RedFlagSeverity.java** - Severity levels (LOW, MEDIUM, HIGH, CRITICAL)
- **RedFlagAction.java** - Actions to take on red flags
- **DocumentAgentConfig.java** - Configuration for document validation agent
- **AgentDecision.java** - Result of agent execution with enrichment data
- **AgentReasoningStep.java** - Individual reasoning steps for explainability

### 2. Document Validation Components

**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/agent/document/`

- **DocumentValidationResult.java** - Comprehensive validation result
  - Completeness score (0.0 - 1.0)
  - Missing documents
  - Inaccessible documents
  - Pricing inconsistencies
  - Compliance gaps
  - Warnings and recommendations

- **DocumentValidator.java** - Core validation logic
  - Checks required documents (T&C, disclosures, fee schedule)
  - Validates URL accessibility
  - Verifies pricing consistency
  - Checks regulatory compliance (Reg DD, Reg E, FDIC)
  - Validates terms coverage

### 3. Agent Execution Service

**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/agent/service/`

- **AgentExecutorService.java**
  - Executes document validation
  - Builds reasoning trace
  - Detects red flags
  - Generates enrichment data
  - Calculates confidence scores

### 4. Temporal Integration

**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/temporal/`

- **AgentActivity.java** - Interface for agent Temporal activities
- **AgentActivityImpl.java** - Implementation calling AgentExecutorService
- **ApprovalWorkflowImplV3.java** - Enhanced workflow with agent execution

## Validation Checks

### 1. Required Documents Check
```java
✓ Terms and Conditions URL
✓ Disclosure URL
✓ Fee Schedule URL
✓ Product Documentation URL (recommended)
```

### 2. Document Accessibility
```java
✓ URLs are properly formatted (http:// or https://)
✓ URLs are accessible (in production: HTTP status check)
```

### 3. Pricing Consistency
```java
✓ Configured monthly fee matches documented fee
✓ Configured interest rate matches disclosed rate
✓ Additional fees match fee schedule
```

### 4. Regulatory Compliance

**For Checking/Savings Accounts:**
```java
✓ Regulation DD (Truth in Savings) disclosure
✓ FDIC insurance notice
✓ If overdraft enabled: Reg E overdraft opt-in disclosure
```

### 5. Terms Coverage
```java
✓ Terms cover all configured features
✓ Early withdrawal penalties are documented
✓ Restrictions are clearly stated
```

## Example Workflow Execution

### Scenario: Product Manager Configures New Checking Account

```json
POST /api/v1/solutions/configure
{
  "catalogProductId": "cat-checking-001",
  "solutionName": "Premium Checking",
  "pricingVariance": 15,
  "riskLevel": "MEDIUM",
  "monthlyFee": 25.00,
  "termsAndConditionsUrl": "https://docs.bank.com/terms/premium-checking-v1.pdf",
  "disclosureUrl": "https://docs.bank.com/disclosures/checking-reg-dd.pdf",
  "feeScheduleUrl": "https://docs.bank.com/fees/premium-checking.pdf",
  "hasOverdraft": true
}
```

### Agent Execution Flow

**Step 1: Check Required Documents**
```json
{
  "stepNumber": 1,
  "stepName": "Check Required Documents",
  "tool": "document_presence_checker",
  "output": {
    "allDocumentsPresent": true,
    "missingCount": 0
  },
  "reasoning": "All required documents are present"
}
```

**Step 2: Check Document Accessibility**
```json
{
  "stepNumber": 2,
  "stepName": "Check Document Accessibility",
  "tool": "url_validator",
  "output": {
    "allDocumentsAccessible": true,
    "inaccessibleCount": 0
  },
  "reasoning": "All document URLs are accessible"
}
```

**Step 3: Check Pricing Consistency**
```json
{
  "stepNumber": 3,
  "stepName": "Check Pricing Consistency",
  "tool": "consistency_checker",
  "output": {
    "hasInconsistencies": false,
    "inconsistencyCount": 0
  },
  "reasoning": "Configuration matches documented values"
}
```

**Step 4: Check Regulatory Compliance**
```json
{
  "stepNumber": 4,
  "stepName": "Check Regulatory Compliance",
  "tool": "compliance_checker",
  "output": {
    "complianceGapCount": 1,
    "complianceGaps": [
      {
        "regulation": "Regulation E",
        "requirement": "Overdraft opt-in disclosure",
        "gap": "Missing Reg E overdraft opt-in form",
        "severity": "CRITICAL"
      }
    ]
  },
  "reasoning": "Found 1 compliance gap"
}
```

### Agent Decision Output

```json
{
  "agentId": "document-validator-v1",
  "agentType": "CUSTOM",
  "executedAt": "2025-10-07T10:30:00",
  "executionTime": "PT2.5S",
  "redFlagDetected": true,
  "redFlagReason": "Document validation failed: 1 critical compliance gap; ",
  "severity": "CRITICAL",
  "enrichmentData": {
    "documentCompleteness": 0.80,
    "documentValidationStatus": "FAIL",
    "missingDocumentCount": 0,
    "inconsistencyCount": 0,
    "complianceGapCount": 1,
    "documentRecommendations": [
      "Address 1 critical compliance gap(s)"
    ]
  },
  "confidenceScore": 0.80,
  "success": true
}
```

### Workflow Outcome

**Red Flag Action: TERMINATE_REJECT**

```json
{
  "workflowId": "workflow-abc-123",
  "status": "REJECTED",
  "resultCode": "AGENT_RED_FLAG",
  "message": "Document validation failed: 1 critical compliance gap",
  "agentExecution": {
    "agentId": "document-validator-v1",
    "executionTime": "2500ms",
    "success": true,
    "redFlagDetected": true,
    "confidenceScore": 0.80
  },
  "documentValidation": {
    "documentCompleteness": 0.80,
    "documentValidationStatus": "FAIL",
    "complianceGapCount": 1
  }
}
```

**User receives immediate feedback:**
- ❌ Cannot proceed without Reg E overdraft opt-in disclosure
- Must upload required document before resubmitting

## Integration with DMN Rules

Rules can now use agent-enriched data:

```json
{
  "decisionTables": [
    {
      "name": "Product Approval with Document Validation",
      "inputs": [
        {
          "name": "pricingVariance",
          "type": "number"
        },
        {
          "name": "documentCompleteness",
          "type": "number",
          "source": "agent"
        },
        {
          "name": "complianceGapCount",
          "type": "number",
          "source": "agent"
        },
        {
          "name": "documentValidationStatus",
          "type": "string",
          "source": "agent"
        }
      ],
      "rules": [
        {
          "ruleId": "AUTO_REJECT_COMPLIANCE",
          "priority": 100,
          "conditions": {
            "complianceGapCount": "> 0"
          },
          "outputs": {
            "decision": "AUTO_REJECT",
            "reason": "Critical compliance gaps in documentation"
          }
        },
        {
          "ruleId": "ENHANCED_APPROVAL_INCOMPLETE_DOCS",
          "priority": 95,
          "conditions": {
            "documentCompleteness": "< 0.7",
            "pricingVariance": "> 15"
          },
          "outputs": {
            "approverRoles": ["PRODUCT_MANAGER", "COMPLIANCE_OFFICER", "CFO"],
            "approvalCount": 3,
            "reason": "Incomplete documentation + high pricing variance requires enhanced approval"
          }
        },
        {
          "ruleId": "SINGLE_APPROVAL_COMPLETE_DOCS",
          "priority": 80,
          "conditions": {
            "documentCompleteness": ">= 0.9",
            "pricingVariance": "<= 20"
          },
          "outputs": {
            "approverRoles": ["PRODUCT_MANAGER"],
            "approvalCount": 1,
            "reason": "Complete documentation, standard approval"
          }
        }
      ]
    }
  ]
}
```

## Configuration

### Document Agent Configuration

```java
DocumentAgentConfig config = DocumentAgentConfig.builder()
    .agentId("document-validator-v1")
    .type(AgentType.CUSTOM)
    .mode(AgentExecutionMode.SYNC_ENRICHMENT)
    .priority(1)
    .timeoutMs(60000)
    .redFlagConditions(Map.of(
        "completenessScore", "< 0.5",
        "validationStatus", "FAIL"
    ))
    .redFlagAction(RedFlagAction.builder()
        .action(RedFlagAction.ActionType.TERMINATE_REJECT)
        .autoReject(true)
        .reason("Critical documentation gaps")
        .notifyRoles(List.of("COMPLIANCE_OFFICER"))
        .build())
    .enrichmentOutputs(List.of(
        "documentCompleteness",
        "documentValidationStatus",
        "missingDocumentCount",
        "inconsistencyCount",
        "complianceGapCount",
        "documentRecommendations"
    ))
    .required(false)
    .build();
```

## Benefits

### 1. **Early Issue Detection**
- Catches missing documents before human review
- Identifies compliance gaps immediately
- Prevents incomplete submissions

### 2. **Improved Efficiency**
- Reduces back-and-forth with product managers
- Approvers receive complete, validated packages
- Faster approval cycles

### 3. **Regulatory Compliance**
- Automated compliance checks (Reg DD, Reg E, FDIC)
- Reduces compliance risk
- Audit trail of validation steps

### 4. **Explainability**
- Clear reasoning trace for all checks
- Specific recommendations for fixes
- Confidence scores for decisions

### 5. **Flexible Configuration**
- Configurable red flag thresholds
- Customizable enrichment outputs
- Adjustable validation rules

## Next Steps

### Short Term (Next Sprint)
1. **Test Coverage**
   - Unit tests for DocumentValidator
   - Integration tests for AgentExecutorService
   - End-to-end workflow tests with agent

2. **Temporal Worker Registration**
   - Register V3 workflow implementation
   - Configure agent activity workers
   - Update worker configuration

3. **Template Updates**
   - Create SOLUTION_CONFIG_V3_AGENTIC template
   - Add agent configuration to template schema
   - Migrate existing templates

### Medium Term (Next Quarter)
1. **Enhanced Document Parsing**
   - Integrate with document management system
   - Parse PDF documents for content validation
   - Extract fee schedules programmatically

2. **Additional Validators**
   - APY calculation validator
   - Marketing copy compliance checker
   - Terms & conditions parser

3. **UI Integration**
   - Display agent reasoning in approval UI
   - Show document validation results
   - Highlight compliance gaps

### Long Term (Next 6 Months)
1. **Add More Agents**
   - Profitability Analyzer Agent
   - Regulatory Compliance Agent (full version)
   - Historical Pattern GraphRAG Agent
   - Competitive Analysis Agent

2. **Agent Orchestration**
   - Parallel agent execution
   - Conditional agent workflows
   - Agent priority management

3. **Machine Learning**
   - Learn from historical validations
   - Predict approval outcomes
   - Recommend document improvements

## Testing

### Manual Testing

**1. Test Scenario: Missing Required Documents**
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Test Checking",
    "pricingVariance": 10
  }'

# Expected: RED FLAG - missing termsAndConditionsUrl, disclosureUrl
```

**2. Test Scenario: Complete Documentation**
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Test Checking",
    "pricingVariance": 10,
    "termsAndConditionsUrl": "https://docs.bank.com/terms.pdf",
    "disclosureUrl": "https://docs.bank.com/disclosure.pdf",
    "feeScheduleUrl": "https://docs.bank.com/fees.pdf",
    "documentationUrl": "https://docs.bank.com/docs.pdf"
  }'

# Expected: PASS - all documents present
```

**3. Test Scenario: Compliance Gap (Overdraft)**
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: test@bank.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Test Checking",
    "pricingVariance": 10,
    "hasOverdraft": true,
    "termsAndConditionsUrl": "https://docs.bank.com/terms.pdf",
    "disclosureUrl": "https://docs.bank.com/disclosure.pdf",
    "feeScheduleUrl": "https://docs.bank.com/fees.pdf"
  }'

# Expected: RED FLAG - missing Reg E overdraft opt-in disclosure
```

## Files Created

```
backend/workflow-service/src/main/java/com/bank/product/workflow/
├── agent/
│   ├── model/
│   │   ├── AgentType.java
│   │   ├── AgentExecutionMode.java
│   │   ├── RedFlagSeverity.java
│   │   ├── RedFlagAction.java
│   │   ├── DocumentAgentConfig.java
│   │   ├── AgentDecision.java
│   │   └── AgentReasoningStep.java
│   ├── document/
│   │   ├── DocumentValidationResult.java
│   │   └── DocumentValidator.java
│   └── service/
│       └── AgentExecutorService.java
└── temporal/
    ├── activity/
    │   ├── AgentActivity.java
    │   └── AgentActivityImpl.java
    └── workflow/
        └── ApprovalWorkflowImplV3.java
```

## Summary

The Document Validation Agent is the foundation for agentic workflows in the product catalog system. It demonstrates:

✅ **Agent Integration** - Seamless integration with Temporal workflows
✅ **Red Flag Detection** - Early termination on critical issues
✅ **Metadata Enrichment** - Enhanced DMN rules with AI insights
✅ **Explainability** - Clear reasoning trace for all decisions
✅ **Regulatory Focus** - Automated compliance checks
✅ **Extensibility** - Framework ready for additional agents

This implementation provides a blueprint for adding future agents like Profitability Analyzer, Competitive Analysis, and Historical Pattern Recognition.
