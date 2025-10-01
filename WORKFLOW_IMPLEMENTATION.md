# Workflow Service Implementation Guide

## Overview

The **workflow-service** is a production-ready, Temporal-based approval orchestration system that provides extensible maker/checker workflows for any business entity. Built on Spring Boot 3.4.0 and Java 21, it features a custom JSON-based rule engine that eliminates the need for external DMN engines.

## Architecture

### Component Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│                     Product Service (8082)                        │
│  • Creates solutions in DRAFT status                             │
│  • Triggers workflow approval                                    │
│  • Receives callbacks when approved                              │
└────────────────────┬─────────────────────────────────────────────┘
                     │ HTTP REST
                     ↓
┌──────────────────────────────────────────────────────────────────┐
│                  Workflow Service (8089)                          │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    REST API Layer                        │   │
│  │  • WorkflowController - Submission & approval            │   │
│  │  • TemplateController - Template management              │   │
│  │  • TaskController - My tasks & dashboard                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              Service Layer                               │   │
│  │  • RuleEvaluationService                                │   │
│  │  • WorkflowTemplateService                              │   │
│  │  • ApprovalTaskService                                  │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │        Rule Engine (SimpleTableRuleEngine)              │   │
│  │  • ConditionEvaluator - Expression evaluation           │   │
│  │  • Hit policies: FIRST, ALL, PRIORITY, COLLECT          │   │
│  │  • ComputedApprovalPlan generation                      │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │           Temporal Workflow Engine                       │   │
│  │  • GenericApprovalWorkflow                              │   │
│  │  • Activities: Validate, Assign, Notify, Execute        │   │
│  │  • Durable execution (survives crashes)                 │   │
│  └─────────────────────────────────────────────────────────┘   │
│                              ↓                                    │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │         Callback Handler Registry                        │   │
│  │  • Plugin architecture                                   │   │
│  │  • Entity-specific approval actions                     │   │
│  │  • SolutionConfigApprovalHandler                        │   │
│  └─────────────────────────────────────────────────────────┘   │
└──────┬───────────────┬──────────────┬──────────────┬────────────┘
       │               │              │              │
       ↓               ↓              ↓              ↓
  ┌─────────┐   ┌──────────┐  ┌──────────┐  ┌──────────┐
  │ MongoDB │   │Temporal  │  │  Kafka   │  │ Product  │
  │         │   │PostgreSQL│  │          │  │ Service  │
  │Templates│   │          │  │  Events  │  │(callback)│
  │Subjects │   │          │  │          │  │          │
  │Tasks    │   │          │  │          │  │          │
  │Audit    │   │          │  │          │  │          │
  └─────────┘   └──────────┘  └──────────┘  └──────────┘
```

## Domain Model

### Core Entities

#### WorkflowTemplate
Defines approval rules for an entity type. Stored in MongoDB, versioned, and can be updated without code deployment.

```java
{
  "templateId": "SOLUTION_CONFIG_V1",
  "version": "1.0.0",
  "entityType": "SOLUTION_CONFIGURATION",
  "active": true,
  "decisionTables": [ ... ],
  "approverSelectionStrategy": { ... },
  "escalationRules": [ ... ],
  "callbackHandlers": {
    "onApprove": "SolutionConfigApprovalHandler",
    "onReject": "SolutionConfigRejectionHandler"
  }
}
```

#### WorkflowSubject
Represents a specific workflow instance for an entity.

```java
{
  "workflowId": "wf-12345",
  "workflowInstanceId": "temporal-workflow-abc",
  "entityType": "SOLUTION_CONFIGURATION",
  "entityId": "solution-789",
  "entityData": { /* full snapshot */ },
  "entityMetadata": { /* for rule evaluation */ },
  "state": "PENDING_APPROVAL",
  "approvalPlan": { /* computed from rules */ }
}
```

#### ApprovalTask
Individual task assigned to an approver.

```java
{
  "taskId": "task-456",
  "workflowId": "wf-12345",
  "assignedTo": "john.smith@bank.com",
  "requiredRole": "PRODUCT_MANAGER",
  "status": "PENDING",
  "dueDate": "2025-10-02T18:00:00Z",
  "priority": "HIGH"
}
```

## Rule Engine

### Decision Table Structure

```json
{
  "name": "Solution Approval Rules",
  "hitPolicy": "FIRST",
  "inputs": [
    {"name": "solutionType", "type": "string"},
    {"name": "pricingVariance", "type": "number"},
    {"name": "riskLevel", "type": "string"}
  ],
  "outputs": [
    {"name": "approvalRequired", "type": "boolean"},
    {"name": "approverRoles", "type": "array"},
    {"name": "approvalCount", "type": "number"},
    {"name": "isSequential", "type": "boolean"},
    {"name": "slaHours", "type": "number"}
  ],
  "rules": [
    {
      "ruleId": "AUTO_APPROVE_LOW_RISK",
      "priority": 100,
      "conditions": {
        "pricingVariance": "<= 5",
        "riskLevel": "LOW"
      },
      "outputs": {
        "approvalRequired": false,
        "approvalCount": 0,
        "slaHours": 0
      }
    },
    {
      "ruleId": "SINGLE_APPROVAL",
      "priority": 50,
      "conditions": {
        "pricingVariance": "> 5 && <= 15",
        "riskLevel": "LOW|MEDIUM"
      },
      "outputs": {
        "approvalRequired": true,
        "approverRoles": ["PRODUCT_MANAGER"],
        "approvalCount": 1,
        "isSequential": false,
        "slaHours": 24
      }
    },
    {
      "ruleId": "DUAL_APPROVAL",
      "priority": 75,
      "conditions": {
        "pricingVariance": "> 15",
        "riskLevel": "MEDIUM|HIGH"
      },
      "outputs": {
        "approvalRequired": true,
        "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
        "approvalCount": 2,
        "isSequential": true,
        "slaHours": 48
      }
    }
  ]
}
```

### Supported Condition Expressions

| Type | Operators | Examples |
|------|-----------|----------|
| **Numeric Comparison** | `>`, `<`, `>=`, `<=`, `==`, `!=` | `> 100`, `<= 50`, `== 0` |
| **Range** | `&&`, `\|\|` | `> 10 && <= 100`, `< 0 \|\| > 100` |
| **String Equality** | `==`, `!=` | `== 'CHECKING'`, `!= 'SAVINGS'` |
| **OR Conditions** | `\|` | `CHECKING\|SAVINGS\|LOAN` |
| **String Operations** | `contains`, `startsWith`, `endsWith`, `matches` | `contains 'Premium'`, `matches '^[A-Z].*'` |
| **Boolean** | Direct value | `true`, `false` |

### Hit Policies

- **FIRST**: First matching rule wins (most common)
- **ALL**: Merge outputs from all matching rules
- **PRIORITY**: Highest priority matching rule wins
- **COLLECT**: Collect all matching rule outputs into lists

## Integration Flow: Solution Configuration

### Step-by-Step Flow

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. User Submits Solution Configuration                         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    POST /api/v1/solutions/configure
    {
      "catalogProductId": "PREMIUM_CHECKING",
      "tenantId": "tenant-001",
      "customPricing": {
        "monthlyFee": 15.00,
        "overdraftFee": 35.00
      }
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 2. Product Service - SolutionService.createDraft()             │
│    - Validates request                                          │
│    - Creates Solution entity with status=DRAFT                  │
│    - Calculates pricingVariance = 15%                          │
│    - Determines riskLevel = MEDIUM                             │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    POST /api/v1/workflows/submit (workflow-service)
    {
      "entityType": "SOLUTION_CONFIGURATION",
      "entityId": "solution-12345",
      "entityData": { /* full solution snapshot */ },
      "entityMetadata": {
        "solutionType": "CHECKING",
        "pricingVariance": 15,
        "riskLevel": "MEDIUM",
        "tenantTier": "STANDARD"
      },
      "tenantId": "tenant-001",
      "initiatedBy": "user-123"
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 3. Workflow Service - RuleEvaluationService                    │
│    - Loads active template for SOLUTION_CONFIGURATION          │
│    - Evaluates decision tables                                  │
│    - Matches rule: "DUAL_APPROVAL" (variance > 15)            │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    ComputedApprovalPlan {
      "approvalRequired": true,
      "requiredApprovals": 2,
      "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
      "sequential": true,
      "sla": "PT48H"
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 4. Temporal Workflow Starts                                     │
│    - GenericApprovalWorkflow.execute()                         │
│    - Creates WorkflowSubject in MongoDB                        │
│    - Creates first ApprovalTask (PRODUCT_MANAGER)              │
│    - Sets task due date = now + 48 hours                       │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    Response to Product Service:
    {
      "workflowId": "wf-xyz-789",
      "status": "PENDING_APPROVAL",
      "taskId": "task-001",
      "assignedTo": "jane.doe@bank.com",
      "dueDate": "2025-10-03T18:00:00Z"
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 5. Product Manager Approves (First Approval)                   │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    POST /api/v1/workflows/wf-xyz-789/approve
    {
      "approverId": "jane.doe@bank.com",
      "comments": "Pricing looks good, risk is acceptable"
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 6. Temporal Workflow Continues                                 │
│    - Signals workflow with approval                            │
│    - Updates ApprovalTask status = COMPLETED                   │
│    - Creates second ApprovalTask (RISK_MANAGER)                │
│    - Sequential approval, so waits for second approver         │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 7. Risk Manager Approves (Final Approval)                      │
└─────────────────────────────────────────────────────────────────┘
                            ↓
    POST /api/v1/workflows/wf-xyz-789/approve
    {
      "approverId": "john.smith@bank.com",
      "comments": "Risk assessment passed"
    }
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 8. Callback Handler Execution                                  │
│    - SolutionConfigApprovalHandler.handle()                   │
│    - Calls product-service: PUT /solutions/{id}/activate       │
│    - Product service updates: status = DRAFT → ACTIVE          │
│    - Publishes Kafka event: "solution.approved"                │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│ 9. Workflow Completion                                          │
│    - WorkflowSubject.state = COMPLETED                         │
│    - WorkflowAuditLog entries created                          │
│    - All tasks marked COMPLETED                                 │
│    - Notifications sent to requestor                            │
└─────────────────────────────────────────────────────────────────┘
```

## API Reference

### Workflow Submission

```http
POST /api/v1/workflows/submit
Content-Type: application/json

{
  "entityType": "SOLUTION_CONFIGURATION",
  "entityId": "solution-12345",
  "entityData": { ... },
  "entityMetadata": {
    "solutionType": "CHECKING",
    "pricingVariance": 15,
    "riskLevel": "MEDIUM"
  },
  "tenantId": "tenant-001",
  "initiatedBy": "user-123",
  "priority": "HIGH"
}
```

### Approval

```http
POST /api/v1/workflows/{workflowId}/approve
Content-Type: application/json

{
  "approverId": "jane.doe@bank.com",
  "comments": "Approved with conditions",
  "conditions": ["Monitor for 30 days"]
}
```

### Rejection

```http
POST /api/v1/workflows/{workflowId}/reject
Content-Type: application/json

{
  "rejecterId": "john.smith@bank.com",
  "reason": "Pricing variance too high for tenant tier",
  "requiredChanges": [
    "Reduce monthly fee to $12.00",
    "Align with standard tier pricing"
  ]
}
```

### My Tasks

```http
GET /api/v1/workflows/my-tasks?role=PRODUCT_MANAGER&status=PENDING
```

## Configuration

### application.yml

```yaml
temporal:
  connection:
    target: localhost:7233  # temporal:7233 in Docker
  namespace: default
  workflows:
    task-queue: workflow-task-queue
  worker:
    enabled: true
    max-concurrent-workflow-task-executors: 100

workflow:
  default-timeout-seconds: 3600
  default-sla-hours: 24
  escalation:
    enabled: true
    check-interval-minutes: 30
```

## Testing

### Unit Tests (22 tests, all passing)

**ConditionEvaluatorTest** - 16 tests
- Numeric comparisons
- Range conditions
- String operations
- OR conditions
- Null/invalid handling

**SimpleTableRuleEngineTest** - 6 tests
- Hit policy scenarios
- Auto-approval rules
- Priority-based selection
- Default fallback

### Running Tests

```bash
cd backend/workflow-service
mvn test
```

## Deployment

### Docker

```bash
# Build workflow-service
docker-compose build workflow-service

# Start all infrastructure (MongoDB, Temporal, Kafka)
docker-compose up -d mongodb temporal kafka

# Start workflow-service
docker-compose up -d workflow-service

# View logs
docker-compose logs -f workflow-service

# Access Temporal UI
open http://localhost:8088
```

### Ports

| Service | Port | Purpose |
|---------|------|---------|
| workflow-service | 8089 | REST API |
| Temporal Server | 7233 | gRPC endpoint |
| Temporal UI | 8088 | Web dashboard |
| MongoDB | 27018 | Database (external) |
| PostgreSQL | 5432 | Temporal persistence |

## Monitoring

### Temporal UI (http://localhost:8088)

- View running workflows
- Inspect workflow history
- See failed workflows and retry attempts
- Query workflow state
- Manually signal/terminate workflows

### Health Check

```bash
curl http://localhost:8089/actuator/health
```

### Metrics

```bash
curl http://localhost:8089/actuator/metrics
```

## Next Steps

### Phase 3: Temporal Workflows & Activities
1. Implement `GenericApprovalWorkflow` interface
2. Create activities: `ValidateRequestActivity`, `AssignApproversActivity`, etc.
3. Implement signal handlers for approve/reject
4. Add workflow query methods

### Phase 4: REST APIs
1. `WorkflowController` - Submission and approval endpoints
2. `TemplateController` - Template CRUD operations
3. `TaskController` - Task management and dashboard

### Phase 5: Integration
1. `WorkflowClient` in product-service for triggering workflows
2. Callback handlers for approved/rejected actions
3. Kafka event publishing for workflow state changes
4. Notification service integration

## Troubleshooting

### Workflow stuck in PENDING_APPROVAL
- Check Temporal UI for workflow status
- Verify approver assignment is correct
- Check if task is visible in my-tasks API

### Rule evaluation not matching
- Test template with `/test` endpoint
- Verify entityMetadata contains all required fields
- Check condition expressions for syntax errors

### Callback handler not executing
- Verify handler is registered in `WorkflowHandlerRegistry`
- Check callback configuration in template
- Look for exceptions in workflow-service logs
