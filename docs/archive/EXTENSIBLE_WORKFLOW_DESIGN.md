# Extensible Workflow Architecture

## Philosophy

The workflow system is designed to approve **ANY** business entity or action without code changes. Approval rules are externalized, version-controlled, and can be updated without redeployment.

## Core Principles

### 1. Entity Agnostic
```
WorkflowSubject {
  entityType: string       // "SOLUTION_CONFIG", "DOCUMENT", "CUSTOMER_ONBOARDING", "LOAN_APPLICATION"
  entityId: string         // Reference to the actual entity
  entityData: JSON         // Snapshot of entity at submission time
  entityMetadata: {        // Extracted attributes for rule evaluation
    amount?: number
    riskScore?: number
    customerSegment?: string
    documentType?: string
    urgency?: string
    ...                    // Any attribute needed for rules
  }
}
```

### 2. Rule-Driven Approval Routing
```
ApprovalRules {
  templateId: string       // "STANDARD_APPROVAL", "HIGH_RISK_APPROVAL"
  version: string          // "1.2.0" - for rule versioning
  conditions: DecisionTable[]
  routingStrategy: "DMN" | "DROOLS" | "SCRIPTED"
}
```

### 3. Dynamic Approver Assignment
```
ApproverSelection {
  strategy: "ROLE_BASED" | "ATTRIBUTE_BASED" | "EXTERNAL_SERVICE" | "SCRIPT"
  config: {
    // For ROLE_BASED
    roles?: ["RISK_MANAGER", "COMPLIANCE_OFFICER"]

    // For ATTRIBUTE_BASED
    attributeMapping?: {
      "customerSegment=ENTERPRISE": ["VP_SALES", "VP_RISK"]
      "amount>1000000": ["SVP_CREDIT"]
    }

    // For EXTERNAL_SERVICE
    serviceEndpoint?: "/api/approver-service/assign"

    // For SCRIPT
    script?: "groovy:getApproverForDocument(context)"
  }
}
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              Workflow Submission API                     │
│  POST /api/v1/workflows/submit                          │
│  {                                                       │
│    entityType: "CUSTOMER_ONBOARDING",                   │
│    entityId: "cust-12345",                              │
│    entityData: {...},                                   │
│    entityMetadata: {                                    │
│      customerType: "BUSINESS",                          │
│      annualRevenue: 5000000,                            │
│      riskScore: 72                                      │
│    }                                                    │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│           Rule Engine (DMN/Drools/Custom)               │
│  1. Load workflow template for entityType               │
│  2. Evaluate decision tables against entityMetadata     │
│  3. Determine:                                          │
│     - Number of approvals required                      │
│     - Approver roles/users                              │
│     - Sequential vs parallel                            │
│     - SLA/timeout                                       │
│     - Escalation rules                                  │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│              Temporal Workflow                          │
│  GenericApprovalWorkflow.execute()                      │
│  - Apply routing rules                                  │
│  - Create approval tasks                                │
│  - Wait for decisions                                   │
│  - Execute callbacks on approval/rejection              │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│            Callback Handlers (Plugin Pattern)           │
│  SolutionConfigHandler.onApprove()                      │
│  DocumentVerificationHandler.onApprove()                │
│  CustomerOnboardingHandler.onApprove()                  │
└─────────────────────────────────────────────────────────┘
```

## Workflow Templates (Database Stored)

### Template Structure
```json
{
  "templateId": "DOCUMENT_APPROVAL_V1",
  "version": "1.0.0",
  "name": "Document Approval Workflow",
  "description": "Approves documents submitted during customer onboarding",
  "entityType": "DOCUMENT_VERIFICATION",
  "active": true,
  "decisionTables": [
    {
      "name": "Document Approval Rules",
      "inputs": [
        {"name": "documentType", "type": "string"},
        {"name": "customerType", "type": "string"},
        {"name": "verificationScore", "type": "number"}
      ],
      "outputs": [
        {"name": "approvalRequired", "type": "boolean"},
        {"name": "approverRoles", "type": "string[]"},
        {"name": "approvalCount", "type": "number"},
        {"name": "slaHours", "type": "number"}
      ],
      "rules": [
        {
          "conditions": {
            "documentType": "PASSPORT",
            "customerType": "INDIVIDUAL",
            "verificationScore": ">= 80"
          },
          "outputs": {
            "approvalRequired": false,
            "approverRoles": [],
            "approvalCount": 0,
            "slaHours": 0
          }
        },
        {
          "conditions": {
            "documentType": "PASSPORT",
            "customerType": "INDIVIDUAL",
            "verificationScore": "< 80"
          },
          "outputs": {
            "approvalRequired": true,
            "approverRoles": ["COMPLIANCE_OFFICER"],
            "approvalCount": 1,
            "slaHours": 24
          }
        },
        {
          "conditions": {
            "documentType": "ARTICLES_OF_INCORPORATION",
            "customerType": "BUSINESS"
          },
          "outputs": {
            "approvalRequired": true,
            "approverRoles": ["COMPLIANCE_OFFICER", "LEGAL_COUNSEL"],
            "approvalCount": 2,
            "slaHours": 48
          }
        }
      ]
    }
  ],
  "approverSelectionStrategy": {
    "type": "ROLE_BASED_WITH_LOAD_BALANCING",
    "config": {
      "loadBalancing": "ROUND_ROBIN",
      "fallbackStrategy": "ESCALATE_TO_MANAGER"
    }
  },
  "escalationRules": [
    {
      "condition": "task.age > slaHours * 0.8",
      "action": "SEND_REMINDER"
    },
    {
      "condition": "task.age > slaHours",
      "action": "ESCALATE_TO_ROLE",
      "escalateToRole": "SENIOR_COMPLIANCE_OFFICER"
    }
  ],
  "callbackHandlers": {
    "onApprove": "com.bank.product.workflow.handlers.DocumentApprovalHandler",
    "onReject": "com.bank.product.workflow.handlers.DocumentRejectionHandler",
    "onTimeout": "com.bank.product.workflow.handlers.DocumentTimeoutHandler"
  }
}
```

### Solution Configuration Template Example
```json
{
  "templateId": "SOLUTION_CONFIG_V1",
  "version": "1.0.0",
  "entityType": "SOLUTION_CONFIGURATION",
  "decisionTables": [
    {
      "name": "Solution Approval Rules",
      "inputs": [
        {"name": "solutionType", "type": "string"},
        {"name": "pricingVariance", "type": "number"},
        {"name": "riskLevel", "type": "string"},
        {"name": "tenantTier", "type": "string"}
      ],
      "outputs": [
        {"name": "approverRoles", "type": "string[]"},
        {"name": "isSequential", "type": "boolean"},
        {"name": "slaHours", "type": "number"}
      ],
      "rules": [
        {
          "conditions": {
            "solutionType": "CHECKING",
            "pricingVariance": "<= 10",
            "riskLevel": "LOW"
          },
          "outputs": {
            "approverRoles": ["PRODUCT_MANAGER"],
            "isSequential": false,
            "slaHours": 24
          }
        },
        {
          "conditions": {
            "solutionType": "CHECKING",
            "pricingVariance": "> 10",
            "riskLevel": "MEDIUM|HIGH"
          },
          "outputs": {
            "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
            "isSequential": true,
            "slaHours": 48
          }
        },
        {
          "conditions": {
            "tenantTier": "ENTERPRISE",
            "pricingVariance": "> 20"
          },
          "outputs": {
            "approverRoles": ["VP_PRODUCT", "VP_RISK", "CFO"],
            "isSequential": true,
            "slaHours": 72
          }
        }
      ]
    }
  ]
}
```

## Generic Domain Models

### WorkflowSubject (Replaces WorkflowRequest)
```java
@Document(collection = "workflow_subjects")
public class WorkflowSubject {
    private String id;
    private String workflowId;
    private String workflowInstanceId;  // Temporal workflow ID

    // Generic entity reference
    private String entityType;          // "SOLUTION_CONFIG", "DOCUMENT", etc.
    private String entityId;            // ID of the entity being approved
    private Map<String, Object> entityData;  // Full snapshot
    private Map<String, Object> entityMetadata;  // Extracted for rules

    // Template reference
    private String templateId;
    private String templateVersion;

    // Workflow state
    private WorkflowState state;
    private String tenantId;
    private String initiatedBy;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;

    // Computed from rules
    private ComputedApprovalPlan approvalPlan;

    // Results
    private WorkflowResult result;
    private String errorMessage;
}
```

### WorkflowTemplate
```java
@Document(collection = "workflow_templates")
public class WorkflowTemplate {
    private String id;
    private String templateId;
    private String version;
    private String name;
    private String description;
    private String entityType;
    private boolean active;

    // Decision logic
    private List<DecisionTable> decisionTables;
    private ApproverSelectionStrategy approverSelectionStrategy;
    private List<EscalationRule> escalationRules;

    // Callback configuration
    private CallbackHandlers callbackHandlers;

    // Validation rules
    private ValidationRules validationRules;

    // Metadata
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime publishedAt;
}
```

### DecisionTable
```java
public class DecisionTable {
    private String name;
    private List<DecisionInput> inputs;
    private List<DecisionOutput> outputs;
    private List<DecisionRule> rules;
    private String defaultRuleId;  // Fallback if no rules match
    private HitPolicy hitPolicy;   // FIRST, ALL, PRIORITY, etc.
}

public class DecisionInput {
    private String name;
    private String type;  // string, number, boolean, date
    private String expression;  // JSONPath or SpEL
}

public class DecisionOutput {
    private String name;
    private String type;
    private Object defaultValue;
}

public class DecisionRule {
    private String ruleId;
    private int priority;
    private Map<String, String> conditions;  // inputName -> condition expression
    private Map<String, Object> outputs;
    private String description;
}

public enum HitPolicy {
    FIRST,      // First matching rule wins
    ALL,        // All matching rules apply
    PRIORITY,   // Highest priority rule wins
    COLLECT     // Collect all outputs
}
```

### ComputedApprovalPlan
```java
public class ComputedApprovalPlan {
    private boolean approvalRequired;
    private int requiredApprovals;
    private List<String> approverRoles;
    private List<String> specificApprovers;
    private boolean isSequential;
    private Duration sla;
    private List<EscalationRule> escalationRules;
    private Map<String, Object> additionalConfig;
}
```

### CallbackHandlers
```java
public class CallbackHandlers {
    private String onApprove;      // Fully qualified class name or bean name
    private String onReject;
    private String onTimeout;
    private String onCancel;
    private String onValidate;     // Pre-submission validation
}

// Handler interface
public interface WorkflowCallbackHandler {
    void handle(WorkflowContext context) throws WorkflowException;
}

public class WorkflowContext {
    private WorkflowSubject subject;
    private Map<String, Object> entityData;
    private List<ApprovalDecision> decisions;
    private WorkflowResult previousResult;  // For retry scenarios
}
```

## Rule Engine Integration

### Evaluation Flow
```java
public interface RuleEngine {
    ComputedApprovalPlan evaluate(
        WorkflowTemplate template,
        Map<String, Object> entityMetadata
    );
}

// Implementation options
public class DmnRuleEngine implements RuleEngine {
    // Uses Camunda DMN engine
}

public class DroolsRuleEngine implements RuleEngine {
    // Uses Drools rule engine
}

public class ScriptedRuleEngine implements RuleEngine {
    // Uses JavaScript/Groovy for rules
}

public class SimpleTableRuleEngine implements RuleEngine {
    // JSON-based decision tables (no external engine)
}
```

### Example Evaluation
```java
// Entity being submitted
Map<String, Object> metadata = Map.of(
    "documentType", "PASSPORT",
    "customerType", "INDIVIDUAL",
    "verificationScore", 65,
    "urgency", "HIGH"
);

// Load template
WorkflowTemplate template = templateRepo.findByEntityTypeAndActive("DOCUMENT_VERIFICATION");

// Evaluate rules
ComputedApprovalPlan plan = ruleEngine.evaluate(template, metadata);

// Results:
// plan.approvalRequired = true
// plan.approverRoles = ["COMPLIANCE_OFFICER"]
// plan.requiredApprovals = 1
// plan.sla = Duration.ofHours(24)
```

## Dynamic Approver Assignment

### Strategy Interface
```java
public interface ApproverAssignmentStrategy {
    List<String> assignApprovers(
        ComputedApprovalPlan plan,
        WorkflowSubject subject
    );
}

// Implementations
public class RoleBasedAssignment implements ApproverAssignmentStrategy {
    // Queries user service for users with required roles
    // Applies load balancing
}

public class AttributeBasedAssignment implements ApproverAssignmentStrategy {
    // Uses entity attributes to select approvers
    // e.g., customerSegment=ENTERPRISE -> dedicated account managers
}

public class ExternalServiceAssignment implements ApproverAssignmentStrategy {
    // Calls external service for assignment
    // POST /api/approver-service/assign
}

public class ScriptedAssignment implements ApproverAssignmentStrategy {
    // Executes Groovy/JavaScript for custom logic
}
```

## Template Management API

### Template CRUD
```
# Create new template
POST /api/v1/workflow-templates
{
  "templateId": "LOAN_APPROVAL_V1",
  "entityType": "LOAN_APPLICATION",
  "decisionTables": [...],
  ...
}

# Update template (creates new version)
PUT /api/v1/workflow-templates/LOAN_APPROVAL_V1
{
  "version": "1.1.0",
  "decisionTables": [...]
}

# Publish template
POST /api/v1/workflow-templates/LOAN_APPROVAL_V1/versions/1.1.0/publish

# List all templates for entity type
GET /api/v1/workflow-templates?entityType=LOAN_APPLICATION

# Test template against sample data
POST /api/v1/workflow-templates/LOAN_APPROVAL_V1/test
{
  "entityMetadata": {
    "loanAmount": 500000,
    "creditScore": 720,
    "loanType": "MORTGAGE"
  }
}

Response:
{
  "approvalRequired": true,
  "approverRoles": ["LOAN_OFFICER", "UNDERWRITER"],
  "requiredApprovals": 2,
  "isSequential": true,
  "slaHours": 72
}
```

## Generic Workflow Submission

### Submission API (Works for ANY Entity)
```java
POST /api/v1/workflows/submit

{
  "entityType": "CUSTOMER_ONBOARDING",
  "entityId": "customer-789",
  "entityData": {
    "customerId": "customer-789",
    "customerType": "BUSINESS",
    "businessDetails": {
      "annualRevenue": 5000000,
      "industry": "TECHNOLOGY",
      "yearsInBusiness": 3
    },
    "documents": [...]
  },
  "entityMetadata": {
    "customerType": "BUSINESS",
    "annualRevenue": 5000000,
    "riskScore": 72,
    "requiresEnhancedDueDiligence": true,
    "country": "US"
  },
  "templateOverride": "ENHANCED_ONBOARDING_V2",  // Optional
  "priority": "HIGH",
  "requestedBy": "user-123",
  "businessJustification": "High-value enterprise customer"
}
```

The system will:
1. Load template for `CUSTOMER_ONBOARDING`
2. Evaluate decision tables against `entityMetadata`
3. Compute approval plan
4. Assign approvers
5. Create Temporal workflow
6. Create approval tasks

### Universal Approval API (Same for ANY Entity)
```java
POST /api/v1/workflows/{workflowId}/approve
{
  "approverId": "user-456",
  "comments": "Customer verified, all documentation complete",
  "conditions": []
}

POST /api/v1/workflows/{workflowId}/reject
{
  "rejecterId": "user-456",
  "reason": "Missing proof of business registration",
  "requiredChanges": ["Upload business registration certificate"]
}
```

## Plugin Architecture for Handlers

### Handler Registration
```java
@Component
public class WorkflowHandlerRegistry {
    private Map<String, WorkflowCallbackHandler> handlers = new ConcurrentHashMap<>();

    public void register(String handlerName, WorkflowCallbackHandler handler) {
        handlers.put(handlerName, handler);
    }

    public WorkflowCallbackHandler get(String handlerName) {
        return handlers.get(handlerName);
    }
}

// Auto-register handlers
@Component
public class SolutionConfigApprovalHandler implements WorkflowCallbackHandler {

    @PostConstruct
    public void init() {
        registry.register("SolutionConfigApprovalHandler", this);
    }

    @Override
    public void handle(WorkflowContext context) {
        // Extract solution config from context
        Map<String, Object> solutionData = context.getEntityData();

        // Activate the solution
        solutionService.activate(
            (String) solutionData.get("solutionId"),
            context.getSubject().getInitiatedBy()
        );

        // Publish event
        eventPublisher.publish("solution.approved", solutionData);
    }
}

@Component
public class DocumentApprovalHandler implements WorkflowCallbackHandler {

    @PostConstruct
    public void init() {
        registry.register("DocumentApprovalHandler", this);
    }

    @Override
    public void handle(WorkflowContext context) {
        Map<String, Object> docData = context.getEntityData();

        // Mark document as verified
        documentService.markAsVerified(
            (String) docData.get("documentId"),
            context.getDecisions().get(0).getApproverId()
        );
    }
}
```

## Benefits

### 1. **Zero Code Deployment for New Approval Types**
   - Add new workflow template via API
   - Register callback handler (one-time per entity type)
   - No redeployment needed

### 2. **Business User Empowerment**
   - Decision tables editable via UI
   - Test changes before publishing
   - Version control for compliance

### 3. **Extreme Flexibility**
   - Conditional routing based on any attribute
   - Dynamic approver assignment
   - Complex escalation logic

### 4. **Compliance & Audit**
   - All rule changes versioned
   - Template changes require approval
   - Complete audit trail

### 5. **Reusability**
   - Same workflow engine for all approval types
   - Shared approver assignment strategies
   - Common escalation patterns

## Migration Path

### From Current Design
```sql
-- Existing SOLUTION_CONFIG workflows
INSERT INTO workflow_templates (
  template_id,
  entity_type,
  decision_tables,
  ...
) VALUES (
  'SOLUTION_CONFIG_V1',
  'SOLUTION_CONFIGURATION',
  [decision_table_json],
  ...
);

-- New document approval
INSERT INTO workflow_templates (
  template_id,
  entity_type,
  decision_tables,
  ...
) VALUES (
  'DOCUMENT_APPROVAL_V1',
  'DOCUMENT_VERIFICATION',
  [decision_table_json],
  ...
);
```

All existing workflows continue to work, new types added via configuration.
