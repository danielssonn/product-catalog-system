# Party Workflow Integration

## Overview

The federated party management system now integrates with the enterprise workflow engine to provide maker-checker approval workflows for:

1. **Relationship Creation**: New party relationships (e.g., "manages on behalf of") require approval before activation
2. **Change in Circumstance (CIC)**: Material changes to party master data from source systems trigger approval workflows

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Source Systems                               │
│  ┌──────────────────────┐    ┌────────────────────────────┐    │
│  │ Commercial Banking   │    │   Capital Markets          │    │
│  │ Party Service        │    │   Counterparty Service     │    │
│  │ (MongoDB)            │    │   (MongoDB)                │    │
│  └──────────┬───────────┘    └──────────┬─────────────────┘    │
└─────────────│───────────────────────────│──────────────────────┘
              │                           │
              │ Party Change Events       │ Party Change Events
              │ (Kafka)                   │ (Kafka)
              ▼                           ▼
    ┌─────────────────────────────────────────────────┐
    │         Workflow Service (Temporal)             │
    │                                                 │
    │  ┌─────────────────────────────────────────┐  │
    │  │   PartyChangeEventConsumer              │  │
    │  │   - commercial-banking-party-changes    │  │
    │  │   - capital-markets-party-changes       │  │
    │  └─────────────────────────────────────────┘  │
    │                    │                           │
    │                    ▼                           │
    │  ┌─────────────────────────────────────────┐  │
    │  │   Workflow Orchestration (Temporal)     │  │
    │  │   - Relationship Approval Workflow      │  │
    │  │   - CIC Approval Workflow               │  │
    │  └─────────────────────────────────────────┘  │
    │                    │                           │
    │                    ▼                           │
    │  ┌─────────────────────────────────────────┐  │
    │  │   Callback Handlers                     │  │
    │  │   - RelationshipApprovalHandler         │  │
    │  │   - RelationshipRejectionHandler        │  │
    │  │   - PartyChangeApprovalHandler          │  │
    │  │   - PartyChangeRejectionHandler         │  │
    │  └─────────────────────────────────────────┘  │
    └─────────────────────────────────────────────────┘
                       │
                       ▼
    ┌─────────────────────────────────────────────────┐
    │         Federated Party Service                 │
    │              (Neo4j)                            │
    │                                                 │
    │  - Activate approved relationships              │
    │  - Sync approved party changes                  │
    │  - Maintain workflow status                     │
    └─────────────────────────────────────────────────┘
```

## Features

### 1. Relationship Approval Workflow

**Trigger**: When a new "manages on behalf of" relationship is created

**Process**:
1. User creates relationship via party-service API
2. Relationship created with status `PENDING`
3. Workflow automatically submitted to workflow-service
4. Approvers determined based on:
   - Authority level (LIMITED, DISCRETIONARY, FULL)
   - Assets under management
   - Relationship type
5. Approvers review and approve/reject
6. On approval: Relationship activated in Neo4j
7. On rejection: Relationship marked as rejected

**Approval Rules**:

| Condition | Required Approvers | Strategy |
|-----------|-------------------|----------|
| FULL authority + AUM > $1B | Compliance Officer + Senior Management | ALL must approve |
| FULL authority | Compliance Officer | ALL must approve |
| DISCRETIONARY + AUM > $500M | Risk Manager | ALL must approve |
| Default | Operations Manager | ALL must approve |

### 2. Change in Circumstance (CIC) Workflow

**Trigger**: Material change detected in source system (Commercial Banking or Capital Markets)

**Change Events**:
- `PARTY_RISK_RATING_CHANGED` - Risk rating modification
- `PARTY_STATUS_CHANGED` - Status change (esp. SUSPENDED/TERMINATED)
- `PARTY_LEI_CHANGED` - LEI modification
- `PARTY_JURISDICTION_CHANGED` - Jurisdiction change
- `PARTY_CONTROL_CHANGE` - Ownership/control structure change

**Process**:
1. Source system publishes change event to Kafka
2. Workflow-service consumes event
3. Event evaluated for materiality
4. If material: CIC workflow triggered
5. Approvers review change
6. On approval: Changes synced to federated party system
7. On rejection: Changes NOT synced, audit logged

**Approval Rules**:

| Change Type | Required Approvers | Strategy |
|-------------|-------------------|----------|
| PARTY_CONTROL_CHANGE | Compliance Officer + Risk Manager | ALL must approve |
| PARTY_RISK_RATING_CHANGED | Risk Manager | ALL must approve |
| PARTY_STATUS_CHANGED | Compliance Officer | ALL must approve |
| PARTY_LEI_CHANGED, PARTY_JURISDICTION_CHANGED | Operations Manager | ALL must approve |
| Default | Operations Manager | ALL must approve |

## Implementation

### Workflow Handlers

**Created in workflow-service:**

1. **RelationshipApprovalHandler.java**
   - Activates relationship in party-service after approval
   - Updates workflow status
   - Calls: `POST /api/v1/relationships/{type}/{id}/activate`

2. **RelationshipRejectionHandler.java**
   - Marks relationship as rejected
   - Records rejection reason
   - Calls: `POST /api/v1/relationships/{type}/{id}/reject`

3. **PartyChangeApprovalHandler.java**
   - Triggers sync to federated system after CIC approval
   - Force re-sync with change event context
   - Calls: `POST /api/v1/parties/sync`

4. **PartyChangeRejectionHandler.java**
   - Logs rejection
   - Does NOT sync changes to federated system
   - Could trigger alerts/notifications

### Kafka Event Consumer

**PartyChangeEventConsumer.java**

Listens to two Kafka topics:
- `commercial-banking-party-changes`
- `capital-markets-party-changes`

**Event Structure**:
```json
{
  "eventType": "PARTY_RISK_RATING_CHANGED",
  "partyId": "CB-002",
  "timestamp": "2025-10-07T23:00:00Z",
  "changes": {
    "field": "riskRating",
    "oldValue": "LOW",
    "newValue": "MEDIUM",
    "reason": "Increased trading activity"
  },
  "sourceSystem": "COMMERCIAL_BANKING"
}
```

**Materiality Rules**:
- Risk rating changes: **Always material**
- Status changes to SUSPENDED/TERMINATED: **Material**
- LEI changes: **Material**
- Jurisdiction changes: **Material**
- Control changes (>25% ownership): **Material**
- Other changes: **Not material** (auto-sync)

### Workflow Templates

**1. party-relationship-approval-template.json**
```json
{
  "templateId": "party-relationship-approval",
  "entityType": "PARTY_RELATIONSHIP",
  "approvalDecisionTable": {
    "inputs": ["relationshipType", "assetsUnderManagement", "authorityLevel"],
    "outputs": ["requiredApprovers", "approvalStrategy"],
    "rules": [...]
  },
  "callbackHandlers": {
    "onApproved": "relationshipApprovalHandler",
    "onRejected": "relationshipRejectionHandler"
  },
  "timeoutSeconds": 172800,
  "escalationRule": {
    "escalateAfterSeconds": 86400,
    "escalateTo": "SENIOR_MANAGEMENT"
  }
}
```

**2. party-cic-approval-template.json**
```json
{
  "templateId": "party-cic-approval",
  "entityType": "PARTY_CHANGE",
  "approvalDecisionTable": {
    "inputs": ["changeType", "sourceSystem"],
    "outputs": ["requiredApprovers", "approvalStrategy"],
    "rules": [...]
  },
  "callbackHandlers": {
    "onApproved": "partyChangeApprovalHandler",
    "onRejected": "partyChangeRejectionHandler"
  },
  "timeoutSeconds": 86400
}
```

## API Integration

### Party Service Enhancements

**ManagesOnBehalfOfRelationship** added fields:
- `workflowId` - Reference to workflow instance
- `workflowStatus` - PENDING_APPROVAL, APPROVED, REJECTED

**New Endpoints**:
```bash
# Activate relationship after approval
POST /api/v1/relationships/manages-on-behalf-of/{id}/activate
{
  "workflowId": "uuid",
  "approvalTimestamp": 1704672000000
}

# Reject relationship
POST /api/v1/relationships/manages-on-behalf-of/{id}/reject
{
  "workflowId": "uuid",
  "rejectionReason": "Insufficient documentation",
  "rejectionTimestamp": 1704672000000
}

# Sync with workflow context
POST /api/v1/parties/sync
{
  "sourceSystem": "COMMERCIAL_BANKING",
  "sourceId": "CB-002",
  "workflowId": "uuid",
  "forceSync": true,
  "changeEvent": "PARTY_RISK_RATING_CHANGED"
}
```

## Testing

### Demo Script

Run the complete demonstration:
```bash
./test-party-workflows.sh
```

This script demonstrates:
1. Uploading workflow templates
2. Creating a relationship (Goldman Sachs → Microsoft)
3. Automatic workflow trigger
4. Approval process
5. Relationship activation
6. Simulating CIC event via Kafka
7. CIC workflow trigger

### Manual Testing

#### Test 1: Create Relationship with Workflow

```bash
# 1. Upload templates
curl -u admin:admin123 -X POST 'http://localhost:8089/api/v1/templates' \
  -H 'Content-Type: application/json' \
  -d @infrastructure/workflow-templates/party-relationship-approval-template.json

# 2. Sync parties
curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-002'
curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-003'

# 3. Create relationship (triggers workflow)
curl -X POST 'http://localhost:8083/api/v1/relationships/manages-on-behalf-of' \
  -H 'Content-Type: application/json' \
  -d '{
    "managerId": "goldman-federated-id",
    "principalId": "microsoft-federated-id",
    "managementType": "ASSET_MANAGEMENT",
    "authorityLevel": "DISCRETIONARY",
    "assetsUnderManagement": 2000000000.00,
    ...
  }'

# 4. Check workflow status
curl -u admin:admin123 'http://localhost:8089/api/v1/workflows/{workflowId}'

# 5. Approve workflow
curl -u admin:admin123 -X POST 'http://localhost:8089/api/v1/workflows/{workflowId}/approve' \
  -H 'Content-Type: application/json' \
  -d '{
    "approverId": "risk.manager@bank.com",
    "comments": "Approved - within limits"
  }'
```

#### Test 2: Trigger CIC Workflow

```bash
# Publish party change event to Kafka
echo '{
  "eventType": "PARTY_RISK_RATING_CHANGED",
  "partyId": "CB-002",
  "timestamp": "2025-10-07T23:00:00Z",
  "changes": {
    "field": "riskRating",
    "oldValue": "LOW",
    "newValue": "MEDIUM"
  },
  "sourceSystem": "COMMERCIAL_BANKING"
}' | docker exec -i product-catalog-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic commercial-banking-party-changes

# Check workflow was created
curl -u admin:admin123 'http://localhost:8089/api/v1/workflows?entityType=PARTY_CHANGE'
```

## Monitoring

### Workflow Status

```bash
# List all party relationship workflows
curl -u admin:admin123 'http://localhost:8089/api/v1/workflows?entityType=PARTY_RELATIONSHIP'

# List all CIC workflows
curl -u admin:admin123 'http://localhost:8089/api/v1/workflows?entityType=PARTY_CHANGE'

# Get specific workflow
curl -u admin:admin123 'http://localhost:8089/api/v1/workflows/{workflowId}'
```

### Temporal UI

Access: http://localhost:8088

View:
- Workflow execution history
- Approval task status
- Escalations
- Timeouts

### Neo4j Queries

```cypher
# Find relationships pending approval
MATCH (m)-[r:MANAGES_ON_BEHALF_OF]->(p)
WHERE r.workflowStatus = 'PENDING_APPROVAL'
RETURN m.legalName, r.workflowId, p.legalName

# Find approved relationships
MATCH (m)-[r:MANAGES_ON_BEHALF_OF]->(p)
WHERE r.workflowStatus = 'APPROVED'
RETURN m.legalName, r.assetsUnderManagement, p.legalName

# Find rejected relationships
MATCH (m)-[r:MANAGES_ON_BEHALF_OF]->(p)
WHERE r.workflowStatus = 'REJECTED'
RETURN m.legalName, r.workflowId, p.legalName
```

## Business Value

### Risk Management
- All material relationships require approval before activation
- Changes to party master data reviewed before federation
- Audit trail of all approvals/rejections
- Prevents unauthorized relationship creation

### Compliance
- Maker-checker segregation of duties
- Material changes flagged for review
- Complete workflow history
- Regulatory compliance for party management

### Operational Control
- Automated routing based on business rules
- Escalation for delayed approvals
- Consistent approval process across all relationships
- Integration with existing workflow infrastructure

## Future Enhancements

### Planned Features
- [ ] Bulk relationship creation with batch workflow
- [ ] Advanced CIC rules (e.g., threshold-based approval)
- [ ] Integration with compliance screening systems
- [ ] Automated document validation
- [ ] SLA monitoring and reporting
- [ ] Mobile approval interface
- [ ] Delegation and out-of-office handling

---

**Status**: ✅ Production Ready
**Integration**: Complete with existing workflow infrastructure
**Test Coverage**: Demonstration scripts provided
**Documentation**: Complete
