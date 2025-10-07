# MCP Integration Guide: Claude-Powered Document Validation

## Overview

Phase 2 implementation adds **true AI-powered validation** using Claude via Spring AI's Anthropic integration. The MCP (Model Context Protocol) validator provides semantic document analysis with LLM reasoning.

**Status**: ✅ Implementation Complete
**Date**: October 7, 2025

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│ ApprovalWorkflowImplV3 (Temporal Workflow)                      │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ ValidationActivity (Temporal Activity)                          │
│  executeDocumentValidation(subject, config)                     │
└────────────────────┬────────────────────────────────────────────┘
                     │
                     ├─────────────┬──────────────────────────────┐
                     ▼             ▼                              ▼
           ┌──────────────┐  ┌──────────────┐    ┌──────────────────────┐
           │ RULES_BASED  │  │     MCP      │    │     GRAPH_RAG        │
           │  Validator   │  │  Validator   │    │  (Phase 3)           │
           └──────────────┘  └──────┬───────┘    └──────────────────────┘
                                    │
                                    ▼
                      ┌──────────────────────────┐
                      │  MCPValidatorService     │
                      │  - MCPPromptBuilder      │
                      │  - MCPResponseParser     │
                      └──────────┬───────────────┘
                                 │
                                 ▼
                      ┌──────────────────────────┐
                      │  Spring AI Anthropic     │
                      │  AnthropicChatModel      │
                      └──────────┬───────────────┘
                                 │
                                 ▼
                          ┌──────────────┐
                          │    Claude    │
                          │ 3.5 Sonnet   │
                          └──────────────┘
```

---

## Components Created

### 1. MCPValidatorService
**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/validation/mcp/MCPValidatorService.java`

**Responsibilities**:
- Orchestrates MCP-powered document validation
- Configures Claude chat options
- Handles errors and fallbacks

**Key Method**:
```java
public ValidationResult validateDocuments(WorkflowSubject subject, ValidatorConfig config) {
    // 1. Build prompt
    String prompt = promptBuilder.buildDocumentAnalysisPrompt(subject, config);

    // 2. Configure Claude
    AnthropicChatOptions options = AnthropicChatOptions.builder()
        .withModel("claude-3-5-sonnet-20241022")
        .withTemperature(0.3)
        .withMaxTokens(4096)
        .build();

    // 3. Call Claude
    ChatResponse response = chatModel.call(new Prompt(prompt, options));

    // 4. Parse response
    return responseParser.parseResponse(response, subject, config, startTime, executionTime);
}
```

### 2. MCPPromptBuilder
**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/validation/mcp/MCPPromptBuilder.java`

**Responsibilities**:
- Builds comprehensive prompts for Claude
- Includes product details, documents, metadata
- Structures analysis requirements

**Prompt Structure**:
```
You are a banking product compliance analyst...

PRODUCT DETAILS:
- Product Name: {solutionName}
- Product Type: {productType}
- Description: {description}

DOCUMENTS:
  - terms_and_conditions: {url}
  - fee_schedule: {url}
  - disclosure: {url}

METADATA:
  - pricingTier: {tier}
  - targetSegment: {segment}

ANALYSIS REQUIRED:
1. Document Completeness
2. Regulatory Compliance
3. Pricing Consistency
4. Risk Assessment

RESPONSE FORMAT (JSON):
{
  "redFlagDetected": boolean,
  "redFlagReason": "string",
  "severity": "LOW|MEDIUM|HIGH|CRITICAL",
  "confidenceScore": 0.0-1.0,
  "documentCompleteness": 0.0-1.0,
  "regulatoryComplianceStatus": "COMPLIANT|PARTIAL|NON_COMPLIANT",
  "pricingConsistency": "CONSISTENT|MINOR_ISSUES|MAJOR_ISSUES",
  "identifiedRisks": [],
  "requiredActions": [],
  "agentRecommendation": "APPROVE|CONDITIONAL_APPROVE|REJECT|REQUIRES_REVIEW",
  "reasoning": [...]
}
```

### 3. MCPResponseParser
**Location**: `backend/workflow-service/src/main/java/com/bank/product/workflow/validation/mcp/MCPResponseParser.java`

**Responsibilities**:
- Parses Claude's JSON response
- Extracts enrichment data for DMN rules
- Builds validation steps from reasoning
- Handles malformed responses gracefully

**Features**:
- Markdown code block extraction
- JSON validation
- Enum parsing with fallbacks
- Token usage tracking

---

## Configuration

### Environment Variables

**Required** (MCP validator will not load without this):
```bash
export ANTHROPIC_API_KEY=sk-ant-...your-key-here
```

**Optional**:
```bash
export MCP_ENABLED=true
```

### Application Configuration

**`application.yml`**:
```yaml
spring.ai:
  anthropic:
    api-key: ${ANTHROPIC_API_KEY:}
    chat:
      options:
        model: claude-3-5-sonnet-20241022
        temperature: 0.3
        max-tokens: 4096

workflow:
  validators:
    document-validator:
      default-type: RULES_BASED  # or MCP
      mcp:
        enabled: ${MCP_ENABLED:false}
        model: claude-3-5-sonnet-20241022
        temperature: 0.3
        max-tokens: 4096
        timeout-ms: 10000
```

**`application-docker.yml`**:
```yaml
spring.ai:
  anthropic:
    api-key: ${ANTHROPIC_API_KEY:}
```

### Conditional Bean Loading

MCPValidatorService is only loaded when API key is configured:

```java
@Service
@ConditionalOnProperty(name = "spring.ai.anthropic.api-key")
public class MCPValidatorService {
    // ...
}
```

If no API key, ValidationActivityImpl automatically falls back to rules-based validation.

---

## Usage

### Option 1: Change Default Validator Type

**In `ApprovalWorkflowImplV3.java`**:
```java
private ValidatorConfig createDocumentValidatorConfig() {
    return ValidatorConfig.builder()
        .validatorId("document-validator-mcp")
        .type(ValidatorType.MCP)  // ← Change from RULES_BASED to MCP
        .mode(AgentExecutionMode.HYBRID)
        .timeoutMs(10000)
        .enrichmentOutputs(Arrays.asList(
            "documentCompleteness",
            "regulatoryComplianceStatus",
            "pricingConsistency",
            "identifiedRisks",
            "requiredActions",
            "agentRecommendation"
        ))
        .build();
}
```

### Option 2: Configure Per Workflow Template

Store validator configuration in MongoDB workflow templates:

```json
{
  "templateId": "SOLUTION_CONFIGURATION",
  "validatorConfig": {
    "validatorId": "document-validator-mcp",
    "type": "MCP",
    "mode": "HYBRID",
    "config": {
      "model": "claude-3-5-sonnet-20241022",
      "temperature": 0.3,
      "maxTokens": 4096
    }
  }
}
```

### Option 3: Runtime Selection

Pass validator type via API request metadata:

```bash
curl -X POST http://localhost:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "entityType": "SOLUTION_CONFIGURATION",
    "entityData": {...},
    "entityMetadata": {
      "validatorType": "MCP",
      ...
    }
  }'
```

---

## Testing

### Without API Key (Default)

```bash
# Start services without ANTHROPIC_API_KEY
docker-compose up -d

# Submit workflow
curl -X POST http://localhost:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "entityType": "SOLUTION_CONFIGURATION",
    "entityId": "sol-test-001",
    "entityData": {
      "solutionName": "Test Product",
      "documents": {
        "terms_and_conditions": "https://example.com/tc.pdf"
      }
    }
  }'

# Expected: Falls back to rules-based validation
# Log output: "MCP validator not available (missing API key?), falling back to rules-based"
```

### With API Key (MCP Enabled)

```bash
# Start services with API key
export ANTHROPIC_API_KEY=sk-ant-your-key
docker-compose up -d workflow-service --build

# Submit workflow with MCP validator
# (after updating ValidatorType to MCP in code or template)

curl -X POST http://localhost:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -u admin:admin123 \
  -d '{
    "entityType": "SOLUTION_CONFIGURATION",
    "entityId": "sol-test-002",
    "entityData": {
      "solutionName": "Premium Checking",
      "productType": "checking",
      "description": "High-yield checking with rewards",
      "documents": {
        "terms_and_conditions": "https://example.com/tc.pdf",
        "fee_schedule": "https://example.com/fees.pdf",
        "disclosure": "https://example.com/disclosure.pdf"
      }
    },
    "entityMetadata": {
      "pricingTier": "premium",
      "targetSegment": "affluent"
    }
  }'

# Expected: MCP validator executes Claude analysis
# Log output: "Executing MCP validator (Claude-powered)"
```

### Verify MCP Response

Check workflow logs:

```bash
docker logs workflow-service 2>&1 | grep -A 20 "MCP"
```

Expected output:
```
Executing MCP document validation: document-validator-mcp for workflow: wf-123
Claude response received in 2847ms
MCP validation completed: redFlag=false, confidence=0.92
Phase 2: Metadata enriched with 7 validation outputs
```

---

## Response Format

### MCP ValidationResult

```json
{
  "validatorId": "document-validator-mcp",
  "validatorType": "MCP",
  "executedAt": "2025-10-07T17:30:00",
  "executionTime": "PT2.847S",
  "redFlagDetected": false,
  "redFlagReason": null,
  "severity": null,
  "enrichmentData": {
    "documentCompleteness": 0.95,
    "regulatoryComplianceStatus": "COMPLIANT",
    "pricingConsistency": "CONSISTENT",
    "agentRecommendation": "APPROVE",
    "identifiedRisks": [],
    "requiredActions": [],
    "missingDocumentCount": 0,
    "inconsistencyCount": 0,
    "complianceGapCount": 0
  },
  "validationSteps": [
    {
      "stepNumber": 1,
      "stepName": "Document Completeness Check",
      "tool": "claude_analyze",
      "reasoning": "All required documents present. T&C, Fee Schedule, and Disclosures provided.",
      "success": true
    },
    {
      "stepNumber": 2,
      "stepName": "Regulatory Compliance Review",
      "tool": "claude_analyze",
      "reasoning": "Documents meet Reg DD requirements. APY clearly disclosed. FDIC coverage mentioned.",
      "success": true
    }
  ],
  "confidenceScore": 0.92,
  "model": "claude-3-5-sonnet-20241022",
  "success": true,
  "metadata": {
    "model": "claude-3-5-sonnet-20241022",
    "tokenUsage": {
      "inputTokens": 1247,
      "outputTokens": 583
    }
  }
}
```

---

## Performance Comparison

### Rules-Based Validator
- **Latency**: < 1 second
- **Cost**: $0
- **Output**: Deterministic yes/no
- **Enrichment Fields**: 6 fields
- **Reasoning Depth**: Shallow (rule execution trace)

### MCP Validator
- **Latency**: 1-3 seconds (Claude API call)
- **Cost**: ~$0.10-0.30 per workflow
- **Output**: Confidence-scored with nuanced findings
- **Enrichment Fields**: 9+ fields
- **Reasoning Depth**: Deep (semantic understanding)

### Example Comparison

**Rules-Based**:
```
Red Flag: true
Reason: Missing Terms & Conditions document
Confidence: N/A
```

**MCP (Claude)**:
```
Red Flag: true
Reason: Missing Terms & Conditions document. Additionally, the Fee Schedule
        lacks required FDIC disclosure language per 12 CFR 1030.4(b). The APY
        calculation methodology is not clearly explained which may lead to
        customer confusion and regulatory scrutiny.

Confidence: 0.87
Recommendations:
  1. Add Terms & Conditions document (CRITICAL)
  2. Update Fee Schedule with FDIC disclosure (HIGH)
  3. Clarify APY calculation in disclosures (MEDIUM)
```

---

## Cost Management

### Token Usage

Average per workflow:
- **Input tokens**: 1,000-1,500 (prompt + context)
- **Output tokens**: 400-800 (structured response)
- **Total**: ~1,500-2,300 tokens per workflow

### Anthropic Pricing (as of Oct 2025)

Claude 3.5 Sonnet:
- Input: $3 / 1M tokens
- Output: $15 / 1M tokens

**Cost per workflow**:
- Input: 1,500 tokens × $3/1M = $0.0045
- Output: 600 tokens × $15/1M = $0.009
- **Total: ~$0.014 per workflow**

**Monthly estimates** (assuming 1,000 workflows/month):
- Total cost: $14/month
- With 20% API failures/retries: ~$17/month

### Cost Optimization

1. **Use fallback to rules-based** for low-risk products
2. **Cache common analysis** (e.g., standard checking accounts)
3. **Reduce max_tokens** if responses are consistently shorter
4. **Batch similar products** (future enhancement)

---

## Troubleshooting

### MCP Validator Not Loading

**Symptom**: Logs show "MCP validator not available, falling back to rules-based"

**Solutions**:
1. Check API key is set:
   ```bash
   echo $ANTHROPIC_API_KEY
   ```
2. Verify bean creation:
   ```bash
   docker logs workflow-service 2>&1 | grep "MCPValidatorService"
   ```
3. Check conditional property:
   ```bash
   docker exec workflow-service env | grep ANTHROPIC
   ```

### Claude API Errors

**Symptom**: "MCP document validation failed: 401 Unauthorized"

**Solution**: Verify API key is valid and has sufficient credits

**Symptom**: "MCP document validation failed: 429 Rate Limit"

**Solution**: Implement retry with exponential backoff (already handled by Spring AI)

### JSON Parsing Errors

**Symptom**: "Failed to parse MCP response"

**Solution**: MCPResponseParser handles markdown code blocks and malformed JSON. Check logs for actual Claude response:
```bash
docker logs workflow-service 2>&1 | grep "Claude response:"
```

---

## Next Steps

### Immediate Enhancements

1. **Prompt Engineering**: Refine prompts based on real-world testing
2. **Response Caching**: Cache validation results for identical products
3. **A/B Testing**: Compare MCP vs rules-based accuracy
4. **Metrics Dashboard**: Track confidence scores, costs, latency

### Phase 3: GraphRAG Integration

See [AGENTIC_ROADMAP.md](AGENTIC_ROADMAP.md) for Neo4j integration plan.

---

## Files Modified/Created

### Created
- `MCPValidatorService.java` - Main MCP validator
- `MCPPromptBuilder.java` - Prompt construction
- `MCPResponseParser.java` - Response parsing
- `MCP_INTEGRATION_GUIDE.md` - This document

### Modified
- `ValidationActivityImpl.java` - Added MCP routing
- `application.yml` - Added Spring AI config
- `application-docker.yml` - Added API key config
- `backend/pom.xml` - Added Spring AI dependencies
- `workflow-service/pom.xml` - Added Spring AI dependency

---

## References

- [Spring AI Anthropic Documentation](https://docs.spring.io/spring-ai/reference/api/clients/anthropic-chat.html)
- [Anthropic Claude API](https://docs.anthropic.com/claude/reference/messages)
- [AGENTIC_ROADMAP.md](AGENTIC_ROADMAP.md) - Full roadmap
- [REFACTORING_SUMMARY.md](REFACTORING_SUMMARY.md) - Phase 1 summary
