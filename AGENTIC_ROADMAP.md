# Agentic Workflow Roadmap: From Rules to AI

## Current State Assessment

### ❌ Misleading Naming
The current implementation uses "agent" terminology but implements **deterministic rules-based validation**:
- `AgentExecutorService` - Not an AI agent, just a rules executor
- `AgentDecision` - Not AI reasoning, just validation results
- `AgentReasoningStep` - Not LLM reasoning, just rule execution trace

### What We Actually Have
**Rules-Based Document Validator**
```java
// Current: Hardcoded logic
if (!documents.contains("terms_and_conditions.pdf")) {
    missingDocs.add("Terms & Conditions");
    redFlag = true;
}
```

**NOT:**
- AI-powered analysis
- Semantic understanding of document content
- Contextual reasoning
- Adaptive learning
- Model-based decision making

---

## High Priority Next Steps

### 1. MCP Integration for Document Validation 🔴 HIGH PRIORITY

**Goal**: Enable LLM-powered document analysis using Claude via Model Context Protocol

#### Architecture Changes Needed

```
┌─────────────────────────────────────────────────────────────┐
│ Temporal Workflow (ApprovalWorkflowImplV3)                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ AgentActivity (Temporal Activity)                           │
│  - executeDocumentValidation()                              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ MCPAgentService (NEW)                                       │
│  - Manages MCP client lifecycle                             │
│  - Builds prompts from WorkflowSubject                      │
│  - Calls Claude via MCP                                     │
│  - Parses LLM response into AgentDecision                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ MCP Client (anthropic/mcp-client-java)                     │
│  - Connects to Claude via MCP protocol                      │
│  - Supports tools/resources/prompts                         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
              ┌─────────────┐
              │   Claude    │
              │   (LLM)     │
              └─────────────┘
```

#### Implementation Requirements

**A. Add MCP Java Client Dependency**
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>mcp-client-java</artifactId>
    <version>1.0.0</version>
</dependency>
```

**B. Create MCPAgentService**
```java
@Service
public class MCPAgentService {

    private final MCPClient mcpClient;

    public AgentDecision analyzeDocuments(WorkflowSubject subject, AgentConfig config) {
        // 1. Build analysis prompt
        String prompt = buildDocumentAnalysisPrompt(subject);

        // 2. Call Claude via MCP
        MCPResponse response = mcpClient.sendRequest(
            MCPRequest.builder()
                .method("prompts/get")
                .params(Map.of(
                    "name", "document-validator",
                    "arguments", Map.of(
                        "documents", subject.getEntityData().get("documents"),
                        "metadata", subject.getEntityMetadata()
                    )
                ))
                .build()
        );

        // 3. Parse LLM reasoning
        LLMAnalysis analysis = parseLLMResponse(response);

        // 4. Convert to AgentDecision
        return AgentDecision.builder()
            .agentId(config.getAgentId())
            .agentType(AgentType.MCP)
            .redFlagDetected(analysis.hasRedFlags())
            .redFlagReason(analysis.getRedFlagExplanation())
            .severity(analysis.getSeverity())
            .enrichmentData(analysis.getEnrichmentFields())
            .reasoningSteps(analysis.getReasoningTrace())
            .confidenceScore(analysis.getConfidence())
            .build();
    }

    private String buildDocumentAnalysisPrompt(WorkflowSubject subject) {
        return """
            You are a banking product compliance analyst. Analyze the following product
            configuration documents for completeness, regulatory compliance, and risk.

            Product: %s
            Documents: %s
            Metadata: %s

            Evaluate:
            1. Are all required documents present (T&C, Disclosures, Fee Schedule)?
            2. Do documents comply with Reg DD, Reg E, FDIC requirements?
            3. Are pricing terms consistent across documents?
            4. Are there any red flags or compliance gaps?

            Provide:
            - Red flag assessment (true/false)
            - Severity (LOW/MEDIUM/HIGH/CRITICAL)
            - Detailed reasoning steps
            - Confidence score (0.0-1.0)
            - Enrichment fields: documentCompletenessScore, regulatoryComplianceStatus,
              pricingConsistency, identifiedRisks, requiredActions, agentRecommendation
            """.formatted(
                subject.getEntityData().get("solutionName"),
                subject.getEntityData().get("documents"),
                subject.getEntityMetadata()
            );
    }
}
```

**C. Update AgentConfig to Support MCP**
```java
@Data
@Builder
public class AgentConfig {
    private AgentType type; // MCP, GRAPH_RAG, RULES_BASED

    // MCP-specific configuration
    private String mcpServerUrl;
    private String mcpApiKey;
    private String modelName; // e.g., "claude-3-5-sonnet-20241022"
    private int maxTokens;
    private double temperature;

    // Prompt configuration
    private String promptTemplate;
    private Map<String, String> promptVariables;
}
```

**D. Modify AgentActivityImpl**
```java
@Component
public class AgentActivityImpl implements AgentActivity {

    private final MCPAgentService mcpAgentService;
    private final RulesBasedValidatorService rulesBasedValidator; // Renamed

    @Override
    public AgentDecision executeDocumentValidation(WorkflowSubject subject, AgentConfig config) {
        switch (config.getType()) {
            case MCP:
                return mcpAgentService.analyzeDocuments(subject, config);
            case GRAPH_RAG:
                return graphRagService.analyzeDocuments(subject, config);
            case RULES_BASED:
                return rulesBasedValidator.validate(subject, config);
            default:
                throw new IllegalArgumentException("Unknown agent type: " + config.getType());
        }
    }
}
```

#### Testing Requirements

1. **Unit Tests**: Mock MCP client, verify prompt construction
2. **Integration Tests**: Real Claude API calls with test documents
3. **Comparison Tests**: MCP vs Rules-based outputs on same data
4. **Performance Tests**: Measure latency (target: <3s per document set)

#### Configuration Example

```yaml
# application.yml
workflow:
  agents:
    document-validator:
      type: MCP
      mcp:
        server-url: https://api.anthropic.com/v1/mcp
        model-name: claude-3-5-sonnet-20241022
        max-tokens: 4096
        temperature: 0.3
        timeout-ms: 5000
      prompt-template: "document-analysis-v1"
      enrichment-outputs:
        - documentCompletenessScore
        - regulatoryComplianceStatus
        - pricingConsistency
        - identifiedRisks
        - requiredActions
        - agentRecommendation
```

#### Success Criteria

✅ Claude successfully analyzes documents via MCP
✅ LLM provides structured reasoning (not just yes/no)
✅ Confidence scores reflect uncertainty
✅ Enrichment data actionable for DMN rules
✅ Response time < 3 seconds
✅ Graceful fallback to rules-based if MCP unavailable

---

### 2. GraphRAG Integration for Regulatory Knowledge 🔴 HIGH PRIORITY

**Goal**: Enable retrieval of relevant regulatory requirements and historical precedents

#### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ MCPAgentService                                             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ GraphRAGService (NEW)                                       │
│  - Query regulatory knowledge graph                         │
│  - Retrieve relevant compliance rules                       │
│  - Find historical approval patterns                        │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ Knowledge Graph Database (Neo4j)                            │
│                                                              │
│  Nodes:                                                      │
│   - Regulation (Reg DD, Reg E, FDIC)                       │
│   - ProductType (Checking, Savings)                         │
│   - ComplianceRule                                          │
│   - ApprovalDecision (historical)                           │
│                                                              │
│  Relationships:                                              │
│   - APPLIES_TO                                              │
│   - REQUIRES                                                │
│   - PRECEDENT_FOR                                           │
└─────────────────────────────────────────────────────────────┘
```

#### Implementation Requirements

**A. Add Neo4j Dependency**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-neo4j</artifactId>
</dependency>
```

**B. Define Knowledge Graph Schema**

```java
// Regulation Node
@Node
@Data
public class Regulation {
    @Id
    private String id;
    private String name; // "Regulation DD", "Regulation E"
    private String description;
    private String authority; // "CFPB", "FDIC", "OCC"
    private LocalDate effectiveDate;

    @Relationship(type = "APPLIES_TO", direction = OUTGOING)
    private Set<ProductType> applicableProducts;

    @Relationship(type = "REQUIRES", direction = OUTGOING)
    private Set<ComplianceRule> rules;
}

// ComplianceRule Node
@Node
@Data
public class ComplianceRule {
    @Id
    private String id;
    private String ruleId; // "12 CFR 1030.4"
    private String description;
    private String requirement;
    private RuleSeverity severity;
    private List<String> requiredDocuments;
    private List<String> requiredDisclosures;
}

// Historical Approval Decision
@Node
@Data
public class ApprovalDecision {
    @Id
    private String id;
    private String workflowId;
    private LocalDateTime decidedAt;
    private String productType;
    private boolean approved;
    private String rationale;
    private Map<String, Object> productMetadata;

    @Relationship(type = "PRECEDENT_FOR", direction = OUTGOING)
    private Set<ComplianceRule> relevantRules;
}
```

**C. Create GraphRAGService**

```java
@Service
public class GraphRAGService {

    private final Neo4jClient neo4jClient;

    public RegulatoryContext retrieveRegulatoryContext(WorkflowSubject subject) {
        String productType = (String) subject.getEntityMetadata().get("productType");

        // Query 1: Find applicable regulations
        String query1 = """
            MATCH (r:Regulation)-[:APPLIES_TO]->(pt:ProductType {name: $productType})
            MATCH (r)-[:REQUIRES]->(cr:ComplianceRule)
            RETURN r, cr
            ORDER BY cr.severity DESC
            LIMIT 20
            """;

        Collection<Map<String, Object>> regulations = neo4jClient.query(query1)
            .bind(productType).to("productType")
            .fetch().all();

        // Query 2: Find similar historical approvals
        String query2 = """
            MATCH (ad:ApprovalDecision {productType: $productType, approved: true})
            WHERE ad.productMetadata.pricingTier = $pricingTier
            RETURN ad
            ORDER BY ad.decidedAt DESC
            LIMIT 10
            """;

        Collection<Map<String, Object>> precedents = neo4jClient.query(query2)
            .bind(productType).to("productType")
            .bind(subject.getEntityMetadata().get("pricingTier")).to("pricingTier")
            .fetch().all();

        return RegulatoryContext.builder()
            .applicableRegulations(parseRegulations(regulations))
            .requiredDocuments(extractRequiredDocuments(regulations))
            .historicalPrecedents(parsePrecedents(precedents))
            .riskFactors(identifyRiskFactors(regulations, subject))
            .build();
    }

    public AgentDecision analyzeWithRAG(WorkflowSubject subject, AgentConfig config) {
        // Step 1: Retrieve regulatory context from graph
        RegulatoryContext context = retrieveRegulatoryContext(subject);

        // Step 2: Build enriched prompt with RAG context
        String enrichedPrompt = buildRAGPrompt(subject, context);

        // Step 3: Call LLM with regulatory context
        MCPResponse response = mcpClient.sendRequest(enrichedPrompt);

        // Step 4: Parse and return decision
        return parseAgentDecision(response, context);
    }

    private String buildRAGPrompt(WorkflowSubject subject, RegulatoryContext context) {
        return """
            You are analyzing a %s product configuration.

            REGULATORY CONTEXT:
            Applicable Regulations:
            %s

            Required Documents:
            %s

            Historical Precedents:
            %s

            CURRENT PRODUCT:
            %s

            Analyze compliance against the regulatory context above. Identify:
            1. Missing required documents
            2. Compliance gaps vs. historical precedents
            3. Risk factors not addressed
            4. Recommendations for approval
            """.formatted(
                subject.getEntityMetadata().get("productType"),
                formatRegulations(context.getApplicableRegulations()),
                formatDocuments(context.getRequiredDocuments()),
                formatPrecedents(context.getHistoricalPrecedents()),
                subject.getEntityData()
            );
    }
}
```

**D. Integrate GraphRAG into Workflow**

```java
// ApprovalWorkflowImplV3.java
public WorkflowResult execute(WorkflowSubject subject) {
    // PHASE 1: AGENT EXECUTION WITH GRAPHRAG
    AgentConfig docAgentConfig = AgentConfig.builder()
        .agentId("document-validator-rag")
        .type(AgentType.GRAPH_RAG)  // Use GraphRAG
        .mode(AgentExecutionMode.HYBRID)
        .build();

    AgentDecision agentDecision = agentActivity.executeDocumentValidation(
        subject, docAgentConfig);

    // Rest of workflow...
}
```

#### Data Population

**Initial Knowledge Base**:
- 150+ compliance rules (Reg DD, Reg E, FDIC, TILA)
- 25+ product types (checking, savings, CDs, etc.)
- 500+ historical approval decisions (anonymized)

**Ongoing Updates**:
- Sync new approvals to graph after workflow completion
- Regulatory updates trigger graph updates
- Quarterly compliance rule review

#### Success Criteria

✅ Retrieves relevant regulations in < 200ms
✅ Finds historical precedents with > 80% similarity
✅ Reduces false positives by 30% vs rules-based
✅ Provides explainable reasoning with citations
✅ Improves over time as more decisions captured

---

## Immediate Action Items

### Phase 1: Rename Current Implementation (1-2 days)

**Goal**: Remove misleading "agent" terminology from rules-based code

| Current Name | New Name | Reason |
|--------------|----------|--------|
| `AgentExecutorService` | `RulesBasedValidatorService` | Clarify it's rules, not AI |
| `AgentDecision` | `ValidationResult` | More accurate terminology |
| `AgentReasoningStep` | `ValidationStep` | Not LLM reasoning |
| `executeDocumentValidation()` | `validateDocuments()` | Clearer intent |
| `agent/` package | `validation/` package | Better package structure |

**Files to Modify**:
- `backend/workflow-service/src/main/java/com/bank/product/workflow/agent/**`
- `ApprovalWorkflowImplV3.java`
- `AgentActivity.java` and `AgentActivityImpl.java`
- `DOCUMENT_VALIDATION_AGENT.md` → `DOCUMENT_VALIDATION.md`

### Phase 2: MCP Integration (2-3 weeks)

1. **Week 1**: Set up MCP client, basic connectivity
2. **Week 2**: Implement MCPAgentService, prompt engineering
3. **Week 3**: Testing, comparison with rules-based, tuning

### Phase 3: GraphRAG Integration (3-4 weeks)

1. **Week 1**: Set up Neo4j, define schema, load initial data
2. **Week 2**: Implement GraphRAGService, query optimization
3. **Week 3**: Integrate with MCP for enriched prompts
4. **Week 4**: Testing, performance tuning, validation

---

## Expected Outcomes

### After MCP Integration

**Before (Rules-Based)**:
```
Red Flag: true
Reason: Missing Terms & Conditions document
Confidence: N/A (deterministic)
```

**After (LLM-Powered)**:
```
Red Flag: true
Reason: Missing Terms & Conditions document. Additionally, the provided
        Fee Schedule lacks required FDIC disclosure language per 12 CFR
        1030.4(b). The APY calculation methodology is not clearly explained,
        which may lead to customer confusion and regulatory scrutiny.

Confidence: 0.87
Reasoning:
  1. Analyzed 3 documents against regulatory checklist
  2. Found 2 critical omissions (T&C, FDIC disclosure)
  3. Identified 1 medium-risk issue (APY explanation)
  4. Compared against 15 similar product approvals
  5. 12/15 historical approvals required remediation before final approval
```

### After GraphRAG Integration

**Enhanced Context**:
```
Applicable Regulations:
  - Regulation DD (12 CFR 1030): Truth in Savings
  - Regulation E (12 CFR 1005): Electronic Fund Transfers
  - FDIC Part 328: Advertising of Membership

Historical Precedents:
  - Workflow #12847: Similar high-yield savings, required APY clarification
  - Workflow #13201: Comparable fee structure, approved with minor edits
  - Workflow #13445: Same rate tier, rejected due to misleading advertising

Recommendation: CONDITIONAL APPROVAL
  Action Items:
    1. Add FDIC disclosure to Fee Schedule (critical)
    2. Clarify APY calculation in T&C (high priority)
    3. Review marketing materials per FDIC Part 328 (medium priority)

  Confidence: 0.91
  Basis: 15 similar precedents, 2 regulatory citations, 3 risk factors
```

---

## Technical Debt to Address

### Current Issues

1. **Misleading abstractions**: `AgentType.MCP` exists but not implemented
2. **Empty interfaces**: AgentConfig supports MCP/GraphRAG but no backend
3. **False reasoning**: `AgentReasoningStep` simulates AI reasoning
4. **Documentation gap**: DOCUMENT_VALIDATION_AGENT.md overpromises capabilities

### Resolution Plan

1. ✅ Create this roadmap document
2. 🔄 Rename current implementation (Phase 1)
3. 🔄 Implement MCP integration (Phase 2)
4. 🔄 Implement GraphRAG integration (Phase 3)
5. 🔄 Update all documentation to reflect actual capabilities
6. 🔄 Add "implementation status" badges to code comments

---

## Cost & Resource Estimates

### MCP Integration
- **Development**: 2-3 weeks (1 senior engineer)
- **LLM API Costs**: ~$0.10-0.30 per workflow (Claude Sonnet)
- **Infrastructure**: Minimal (HTTP client only)

### GraphRAG Integration
- **Development**: 3-4 weeks (1 senior engineer + 1 data engineer)
- **Infrastructure**: Neo4j instance (~$200/month hosted)
- **Data Population**: 1 week (compliance analyst + engineer)
- **Ongoing**: Quarterly updates (4 hours/quarter)

### Total Investment
- **Time**: 6-8 weeks total
- **Cost**: ~$8K-12K development + $200/month + LLM API usage
- **ROI**: 30% reduction in false positives = faster approvals = increased revenue

---

## References

- [Anthropic MCP Documentation](https://modelcontextprotocol.io/)
- [GraphRAG Paper](https://arxiv.org/abs/2404.16130)
- [Neo4j for Banking Compliance](https://neo4j.com/use-cases/compliance/)
- Spring AI MCP Integration: [Spring AI Docs](https://docs.spring.io/spring-ai/)
