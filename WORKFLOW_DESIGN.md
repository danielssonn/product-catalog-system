# Workflow Foundation Design

## Overview
This document outlines the maker/checker approval workflow foundation using Temporal for the Product Catalog System. The workflow engine enables distributed approval processes for critical operations like solution configuration, pricing changes, and product catalog updates.

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                     API Layer (REST)                         │
│  POST /api/v1/workflows/submit                              │
│  POST /api/v1/workflows/{id}/approve                        │
│  POST /api/v1/workflows/{id}/reject                         │
│  GET  /api/v1/workflows/{id}                                │
│  GET  /api/v1/workflows/my-tasks                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│              Workflow Service (Spring Boot)                  │
│  - Workflow submission logic                                │
│  - Approval/Rejection handlers                              │
│  - Task assignment                                          │
│  - Notification triggers                                    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│              Temporal Workflow Engine                        │
│  - Workflow execution                                       │
│  - State management                                         │
│  - Retry logic                                              │
│  - Compensation/rollback                                    │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│              MongoDB (Workflow State)                        │
│  - Workflow instances                                       │
│  - Approval tasks                                           │
│  - Audit trail                                              │
└─────────────────────────────────────────────────────────────┘
```

## Workflow Types

### 1. Solution Configuration Workflow
**Use Case**: Create/modify tenant-specific product solution from catalog

**Maker**: Product Manager
**Checker**: Risk Manager or Senior Product Manager

**Steps**:
1. Maker submits solution configuration
2. System validates configuration against catalog rules
3. Workflow creates approval task
4. Checker reviews and approves/rejects
5. On approval: Solution is activated
6. On rejection: Solution returns to draft with feedback

### 2. Pricing Change Workflow
**Use Case**: Modify pricing for existing solution

**Maker**: Product Manager
**Checker**: Pricing Manager + Risk Manager (dual approval)

**Steps**:
1. Maker proposes pricing changes
2. System calculates impact
3. First checker (Pricing Manager) approves
4. Second checker (Risk Manager) approves
5. On dual approval: Pricing updated
6. Notification sent to affected systems

### 3. Catalog Product Workflow
**Use Case**: Create/update master catalog product

**Maker**: Product Owner
**Checker**: Chief Product Officer

**Steps**:
1. Maker creates/updates catalog product
2. Compliance check (automated)
3. Checker reviews and approves
4. On approval: Catalog product published
5. Event published to all tenants

### 4. Solution Retirement Workflow
**Use Case**: Retire a solution

**Maker**: Product Manager
**Checker**: Risk Manager + Operations Manager

**Steps**:
1. Maker initiates retirement
2. System checks for active accounts
3. Multi-level approval required
4. Grace period configured
5. Solution marked for retirement
6. Notifications sent

## Temporal Workflow Design

### Workflow Definition
```java
@WorkflowInterface
public interface MakerCheckerWorkflow {

    @WorkflowMethod
    WorkflowResult execute(WorkflowRequest request);

    @SignalMethod
    void approve(ApprovalDecision decision);

    @SignalMethod
    void reject(RejectionDecision decision);

    @QueryMethod
    WorkflowStatus getStatus();
}
```

### Workflow States
```
INITIATED → VALIDATION → PENDING_APPROVAL → APPROVED → COMPLETED
                ↓              ↓               ↓
            REJECTED       REJECTED       CANCELLED
```

### Activities
1. **ValidateRequest**: Validate the request against business rules
2. **CreateApprovalTask**: Create task in task queue
3. **NotifyChecker**: Send notification to checker
4. **ExecuteAction**: Execute the approved action
5. **PublishEvent**: Publish domain event
6. **RollbackAction**: Compensate on rejection
7. **AuditLog**: Record audit trail

## Domain Models

### WorkflowRequest
```java
- workflowId: String
- workflowType: WorkflowType (SOLUTION_CONFIG, PRICING_CHANGE, etc.)
- tenantId: String
- initiatedBy: String (maker)
- requestData: Map<String, Object>
- businessContext: BusinessContext
- approvalPolicy: ApprovalPolicy
```

### ApprovalTask
```java
- taskId: String
- workflowId: String
- assignedTo: String (checker)
- approvalLevel: int
- requiredRole: String
- dueDate: LocalDateTime
- status: TaskStatus
- priority: Priority
```

### ApprovalPolicy
```java
- requiredApprovals: int
- approverRoles: List<String>
- isSequential: boolean
- timeoutDuration: Duration
- escalationRules: List<EscalationRule>
```

### WorkflowAuditLog
```java
- workflowId: String
- timestamp: LocalDateTime
- action: WorkflowAction
- performedBy: String
- previousState: WorkflowState
- newState: WorkflowState
- comments: String
- metadata: Map<String, Object>
```

## API Design

### Submit Workflow
```
POST /api/v1/workflows/submit

Request:
{
  "workflowType": "SOLUTION_CONFIG",
  "tenantId": "tenant-001",
  "requestData": {
    "catalogProductId": "PREMIUM_CHECKING",
    "solutionName": "Elite Checking Plus",
    "customPricing": {...},
    "features": [...]
  },
  "approvalPolicy": {
    "requiredApprovals": 1,
    "approverRoles": ["RISK_MANAGER"]
  }
}

Response:
{
  "workflowId": "wf-12345",
  "status": "PENDING_APPROVAL",
  "taskId": "task-67890",
  "assignedTo": "risk.manager@bank.com",
  "estimatedCompletion": "2025-10-01T18:00:00Z"
}
```

### Approve Workflow
```
POST /api/v1/workflows/{workflowId}/approve

Request:
{
  "approverId": "user-456",
  "comments": "Approved with minor concern on fee structure",
  "conditions": []
}

Response:
{
  "workflowId": "wf-12345",
  "status": "APPROVED",
  "completedAt": "2025-09-30T15:30:00Z",
  "result": {
    "solutionId": "sol-789",
    "effectiveDate": "2025-10-01T00:00:00Z"
  }
}
```

### Reject Workflow
```
POST /api/v1/workflows/{workflowId}/reject

Request:
{
  "rejecterId": "user-456",
  "reason": "Pricing structure doesn't align with risk appetite",
  "requiredChanges": [
    "Reduce overdraft limit to $500",
    "Increase minimum balance to $2500"
  ]
}
```

### Get My Tasks
```
GET /api/v1/workflows/my-tasks?role=RISK_MANAGER&status=PENDING

Response:
{
  "tasks": [
    {
      "taskId": "task-67890",
      "workflowId": "wf-12345",
      "workflowType": "SOLUTION_CONFIG",
      "tenantId": "tenant-001",
      "submittedBy": "product.manager@bank.com",
      "submittedAt": "2025-09-30T14:00:00Z",
      "priority": "HIGH",
      "dueDate": "2025-10-01T18:00:00Z",
      "summary": "New Elite Checking Plus solution configuration"
    }
  ],
  "total": 1
}
```

## Integration Points

### 1. Product Service
- Trigger workflow on solution creation/update
- Receive workflow completion events
- Update solution status based on workflow result

### 2. Notification Service
- Send notifications on task assignment
- Send reminders for pending tasks
- Alert on workflow timeouts

### 3. Audit Service
- Record all workflow state changes
- Track maker/checker actions
- Maintain compliance trail

### 4. Event Publisher
- Publish workflow events to Kafka
- Enable event-driven integrations
- Support external system notifications

## Temporal Configuration

### Docker Compose Addition
```yaml
temporal:
  image: temporalio/auto-setup:1.22.4
  ports:
    - "7233:7233"
  environment:
    - DB=postgresql
    - POSTGRES_SEEDS=postgres
    - DYNAMIC_CONFIG_FILE_PATH=config/dynamicconfig/development.yaml
  depends_on:
    - postgres

temporal-ui:
  image: temporalio/ui:2.21.3
  ports:
    - "8088:8080"
  environment:
    - TEMPORAL_ADDRESS=temporal:7233
  depends_on:
    - temporal

postgres:
  image: postgres:15-alpine
  environment:
    POSTGRES_PASSWORD: temporal
    POSTGRES_USER: temporal
    POSTGRES_DB: temporal
  volumes:
    - postgres_data:/var/lib/postgresql/data
```

### Maven Dependencies
```xml
<dependency>
    <groupId>io.temporal</groupId>
    <artifactId>temporal-sdk</artifactId>
    <version>1.22.4</version>
</dependency>
<dependency>
    <groupId>io.temporal</groupId>
    <artifactId>temporal-spring-boot-starter-alpha</artifactId>
    <version>1.22.4</version>
</dependency>
```

## Security Considerations

1. **Authorization**
   - Role-based access for workflow submission
   - Approval authority verification
   - Segregation of duties enforcement

2. **Audit Trail**
   - Immutable workflow history
   - All decisions logged with user context
   - Tamper-proof audit records

3. **Data Privacy**
   - Sensitive data encryption in workflow state
   - PII handling compliance
   - Data retention policies

## Workflow Patterns

### Sequential Approval
```
Maker → Checker 1 → Checker 2 → Completion
```

### Parallel Approval
```
         ┌→ Checker 1 ┐
Maker →  |             | → Completion (all approve)
         └→ Checker 2 ┘
```

### Escalation
```
Maker → Checker → [Timeout] → Escalation to Senior → Completion
```

### Conditional Approval
```
Maker → Risk Assessment →
  if (high risk) → Dual Approval
  if (low risk) → Single Approval
```

## Implementation Phases

### Phase 1: Foundation (Current Sprint)
- [ ] Add Temporal to infrastructure
- [ ] Create workflow domain models
- [ ] Implement basic workflow service
- [ ] Solution configuration workflow

### Phase 2: Advanced Workflows (Next Sprint)
- [ ] Pricing change workflow
- [ ] Catalog product workflow
- [ ] Multi-level approvals
- [ ] Escalation logic

### Phase 3: Enhancement (Future)
- [ ] SLA monitoring
- [ ] Advanced analytics
- [ ] Workflow templates
- [ ] External system integration

## Monitoring & Observability

1. **Workflow Metrics**
   - Average approval time
   - Rejection rate by workflow type
   - SLA compliance
   - Bottleneck identification

2. **Temporal UI Dashboard**
   - Workflow execution history
   - Failed workflows and retries
   - Task queue monitoring
   - Worker health

3. **Business Metrics**
   - Time to market (workflow → production)
   - Approval velocity by checker
   - Exception handling effectiveness

## Benefits

1. **Compliance**: Built-in maker/checker controls
2. **Auditability**: Complete workflow history
3. **Reliability**: Temporal handles retries and failures
4. **Scalability**: Distributed workflow execution
5. **Flexibility**: Easy to modify approval rules
6. **Visibility**: Real-time workflow monitoring
