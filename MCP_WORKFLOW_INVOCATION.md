# MCP Validator - Workflow Invocation Guide

**Date**: January 7, 2025
**Purpose**: Documentation of which workflow invokes the MCP validator and how it works

---

## Summary

**The MCP validator is invoked by: `ApprovalWorkflowImplV3`**

This is the currently active workflow implementation registered in the Temporal worker. It integrates validation (rules-based, MCP, or GraphRAG) as Phase 1 of the workflow execution.

---

## Workflow Configuration

### Active Workflow

Located in: [ApprovalWorkflowImplV3.java](backend/workflow-service/src/main/java/com/bank/product/workflow/temporal/workflow/ApprovalWorkflowImplV3.java)

**Registered in**: [TemporalConfiguration.java](backend/workflow-service/src/main/java/com/bank/product/workflow/config/TemporalConfiguration.java#L122)

```java
// Line 122 in TemporalConfiguration.java
worker.registerWorkflowImplementationTypes(ApprovalWorkflowImplV3.class);
log.info("Registered workflows: ApprovalWorkflowImplV3 (with validation)");
```

### Previous Versions (Inactive)

```java
// Commented out - not active
// worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);    // V1
// worker.registerWorkflowImplementationTypes(ApprovalWorkflowImplV2.class);  // V2
```

---

## How MCP Validator is Invoked

### Workflow Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│ ApprovalWorkflowImplV3.execute()                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  PHASE 1: VALIDATION EXECUTION                             │
│  ├─ createDocumentValidatorConfig()                        │
│  │  └─ Returns ValidatorConfig with type = MCP            │
│  │                                                          │
│  ├─ validationActivity.executeDocumentValidation()         │
│  │  └─ Routes to MCPValidatorService (Claude)             │
│  │                                                          │
│  └─ Check for red flags                                    │
│     └─ Auto-reject if recommended                          │
│                                                             │
│  PHASE 2: METADATA ENRICHMENT                              │
│  └─ Merge validation enrichment data into metadata         │
│                                                             │
│  PHASE 3: DMN RULE EVALUATION                              │
│  └─ Evaluate rules with enriched metadata                  │
│                                                             │
│  PHASE 4: APPROVAL WORKFLOW                                │
│  ├─ Auto-approve if no approval required                   │
│  └─ Wait for human approvals if required                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Code Location - Validator Config Creation

**File**: `ApprovalWorkflowImplV3.java`
**Lines**: 310-332

```java
private ValidatorConfig createDocumentValidatorConfig() {
    Map<String, String> redFlagConditions = new HashMap<>();
    redFlagConditions.put("completenessScore", "< 0.5");
    redFlagConditions.put("validationStatus", "FAIL");

    return ValidatorConfig.builder()
        .validatorId("document-validator-mcp")
        .type(ValidatorType.MCP)  // ← MCP validator selected here
        .mode(AgentExecutionMode.SYNC_ENRICHMENT)
        .priority(1)
        .timeoutMs(60000)
        .redFlagConditions(redFlagConditions)
        .enrichmentOutputs(Arrays.asList(
            "documentCompleteness",
            "documentValidationStatus",
            "missingDocumentCount",
            "inconsistencyCount",
            "complianceGapCount",
            "documentRecommendations"))
        .required(false)
        .build();
}
```

### Code Location - Validation Execution

**File**: `ApprovalWorkflowImplV3.java`
**Lines**: 76-90

```java
// PHASE 1: VALIDATION EXECUTION (Document Validation)
Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
    "Phase 1: Executing document validation");

// Create document validation config
ValidatorConfig validatorConfig = createDocumentValidatorConfig();

// Execute document validation (invokes MCP)
validationResult = validationActivity.executeDocumentValidation(
    subject,
    validatorConfig
);

Workflow.getLogger(ApprovalWorkflowImplV3.class).info(
    "Document validation completed: success={}, redFlag={}, completeness={}",
    validationResult.isSuccess(),
    validationResult.isRedFlagDetected(),
    validationResult.getEnrichmentData().get("documentCompleteness"));
```

---

## Validation Activity Routing

### ValidationActivityImpl

**File**: [ValidationActivityImpl.java](backend/workflow-service/src/main/java/com/bank/product/workflow/temporal/activity/ValidationActivityImpl.java)

The validation activity routes to the appropriate validator based on the `ValidatorType`:

```java
@Override
public ValidationResult executeDocumentValidation(
    WorkflowSubject subject,
    ValidatorConfig config
) {
    ValidatorType type = config.getType() != null ?
        config.getType() : ValidatorType.RULES_BASED;

    switch (type) {
        case RULES_BASED:
            return rulesBasedValidatorService.validateDocuments(subject, config);

        case MCP:  // ← MCP routing
            if (mcpValidatorService != null) {
                log.info("Executing MCP validator (Claude-powered)");
                return mcpValidatorService.validateDocuments(subject, config);
            } else {
                log.warn("MCP validator not available, falling back to rules-based");
                return rulesBasedValidatorService.validateDocuments(subject, config);
            }

        case GRAPH_RAG:
            log.warn("GraphRAG validator not yet implemented");
            return rulesBasedValidatorService.validateDocuments(subject, config);
    }
}
```

---

## MCP Validator Service

### MCPValidatorService

**File**: [MCPValidatorService.java](backend/workflow-service/src/main/java/com/bank/product/workflow/validation/mcp/MCPValidatorService.java)

```java
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class MCPValidatorService {

    private final ChatModel chatModel;  // Spring AI Claude client
    private final MCPPromptBuilder promptBuilder;
    private final MCPResponseParser responseParser;

    public ValidationResult validateDocuments(
        WorkflowSubject subject,
        ValidatorConfig config
    ) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Build prompt for Claude
            String prompt = promptBuilder.buildDocumentAnalysisPrompt(
                subject,
                config
            );

            // 2. Configure Claude options
            AnthropicChatOptions options = AnthropicChatOptions.builder()
                .withModel("claude-sonnet-4-5-20250929")
                .withTemperature(0.3)
                .withMaxTokens(4096)
                .build();

            // 3. Call Claude API
            ChatResponse response = chatModel.call(
                new Prompt(prompt, options)
            );

            long executionTime = System.currentTimeMillis() - startTime;

            // 4. Parse Claude's response
            return responseParser.parseResponse(
                response,
                subject,
                config,
                startTime,
                executionTime
            );

        } catch (Exception e) {
            // Return failure result with error details
            return ValidationResult.builder()
                .validatorType(ValidatorType.MCP)
                .success(false)
                .redFlagDetected(true)
                .redFlagReason("MCP validation failed: " + e.getMessage())
                .build();
        }
    }
}
```

---

## How to Change Validator Type

### Option 1: Modify Workflow Code (Current)

Edit `ApprovalWorkflowImplV3.java` line 318:

```java
return ValidatorConfig.builder()
    .validatorId("document-validator-mcp")
    .type(ValidatorType.MCP)  // Change to RULES_BASED or GRAPH_RAG
```

**Available types**:
- `ValidatorType.RULES_BASED` - Deterministic rules-based validation
- `ValidatorType.MCP` - Claude AI-powered validation
- `ValidatorType.GRAPH_RAG` - GraphRAG validation (not yet implemented)

### Option 2: Configuration-Based (Future Enhancement)

Add to `application.yml`:

```yaml
workflow:
  validators:
    document-validator:
      default-type: MCP  # RULES_BASED | MCP | GRAPH_RAG
```

Then update workflow to read from configuration:

```java
private ValidatorConfig createDocumentValidatorConfig() {
    ValidatorType type = ValidatorType.valueOf(
        configurationProperties.getDefaultValidatorType()
    );

    return ValidatorConfig.builder()
        .type(type)
        // ...
        .build();
}
```

### Option 3: Template-Based (Future Enhancement)

Store validator configuration in workflow template (MongoDB):

```javascript
// In workflow_templates collection
{
  templateId: "SOLUTION_CONFIG_V1",
  validatorConfig: {
    type: "MCP",
    validatorId: "document-validator-mcp",
    timeout: 60000,
    model: "claude-sonnet-4-5-20250929"
  }
}
```

---

## Verification Commands

### Check Active Workflow

```bash
docker logs workflow-service | grep "Registered workflows"
```

**Expected output**:
```
Registered workflows: ApprovalWorkflowImplV3 (with validation)
```

### Check Validator Type in Logs

```bash
docker logs workflow-service | grep "Executing.*validator"
```

**Expected output**:
```
Executing MCP validator (Claude-powered)
Executing MCP document validation: document-validator-mcp for workflow: [UUID]
```

### Verify Claude API Calls

```bash
docker logs workflow-service | grep -E "Calling Claude|Claude response"
```

**Expected output**:
```
Calling Claude model: claude-sonnet-4-5-20250929
Claude response received in 28345ms
```

---

## Workflow Invocation Methods

### 1. Via REST API

```bash
curl -u admin:admin123 -X POST http://localhost:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -d '{
    "workflowType": "SOLUTION_CONFIGURATION",
    "entityType": "SOLUTION_CONFIGURATION",
    "entityId": "product-001",
    "entityData": {...},
    "entityMetadata": {...},
    "initiatedBy": "user@bank.com"
  }'
```

This triggers:
1. WorkflowController receives request
2. WorkflowExecutionService starts Temporal workflow
3. Temporal executes `ApprovalWorkflowImplV3`
4. Workflow invokes MCP validator in Phase 1

### 2. Via Kafka Event

Product service publishes `solution.created` event → Workflow service consumes → Starts workflow

### 3. Via Test Script

```bash
./test-mcp-validation.sh 1
```

---

## Configuration Files

### Application Configuration

**File**: `application.yml`

```yaml
# Spring AI Configuration (MCP)
spring.ai:
  anthropic:
    api-key: ${ANTHROPIC_API_KEY:}
    chat:
      options:
        model: claude-sonnet-4-5-20250929
        temperature: 0.3
        max-tokens: 4096
```

### Docker Environment

**File**: `docker-compose.yml`

```yaml
workflow-service:
  environment:
    ANTHROPIC_API_KEY: ${ANTHROPIC_API_KEY:-}
```

**File**: `.env` (project root)

```bash
ANTHROPIC_API_KEY=your-api-key-here
```

---

## Summary

### Current Setup

- ✅ **Active Workflow**: `ApprovalWorkflowImplV3`
- ✅ **Validator Type**: `ValidatorType.MCP` (hardcoded in workflow)
- ✅ **Model**: `claude-sonnet-4-5-20250929`
- ✅ **Invocation**: Phase 1 of workflow execution
- ✅ **Routing**: `ValidationActivityImpl` → `MCPValidatorService`
- ✅ **API**: Spring AI Anthropic integration

### To Switch Validators

1. **To Rules-Based**: Change line 318 to `ValidatorType.RULES_BASED`
2. **To GraphRAG**: Change line 318 to `ValidatorType.GRAPH_RAG` (when implemented)
3. **Rebuild**: `mvn package -DskipTests -pl workflow-service -am`
4. **Redeploy**: `docker-compose up -d --build workflow-service`

---

## Related Documentation

- [MCP Integration Guide](MCP_INTEGRATION_GUIDE.md) - Complete MCP implementation details
- [MCP Test Suite](MCP_TEST_SUITE.md) - Test specifications
- [MCP Test Summary](MCP_TEST_SUMMARY.md) - Test results and fixes
- [Testing Guide](TESTING.md) - How to run tests

---

**Last Updated**: January 7, 2025
**Maintained By**: Workflow Service Team
