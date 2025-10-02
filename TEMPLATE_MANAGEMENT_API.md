# Workflow Template Management API

Complete REST API for managing workflow templates dynamically without code changes.

## Overview

The Template Management API provides full CRUD operations, testing, versioning, and publishing capabilities for workflow templates. This enables:

- ✅ **No-code workflow configuration**: Create new approval types via API
- ✅ **Rule testing**: Validate templates before publishing
- ✅ **Version management**: Update templates with proper versioning
- ✅ **Safe publishing**: Activate templates with automatic deactivation of old versions
- ✅ **Role-based security**: Admin-only operations with method-level security

## API Endpoints

Base URL: `http://localhost:8089/api/v1/workflow-templates`

### Authentication

All endpoints require HTTP Basic Authentication:
- **Admin operations**: `admin:admin` (ROLE_ADMIN required for create/update/delete/publish)
- **Read operations**: Any authenticated user

---

## 1. Create Template

Create a new workflow template (inactive by default).

**Endpoint:** `POST /api/v1/workflow-templates`
**Auth:** Requires `ROLE_ADMIN`

**Request Body:**
```json
{
  "templateId": "LOAN_APPROVAL_V1",
  "name": "Loan Approval Workflow",
  "description": "Approval workflow for loan applications",
  "version": "1.0.0",
  "entityType": "LOAN_APPLICATION",
  "decisionTables": [
    {
      "name": "Loan Approval Rules",
      "hitPolicy": "FIRST",
      "inputs": [
        {"name": "loanAmount", "type": "number"},
        {"name": "creditScore", "type": "number"},
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
          "ruleId": "AUTO_APPROVE_LOW_AMOUNT",
          "priority": 1,
          "conditions": {
            "loanAmount": "< 10000",
            "creditScore": ">= 700",
            "riskLevel": "LOW"
          },
          "outputs": {
            "approvalRequired": false
          }
        },
        {
          "ruleId": "SINGLE_APPROVAL_MEDIUM",
          "priority": 2,
          "conditions": {
            "loanAmount": ">= 10000 && < 50000",
            "creditScore": ">= 650"
          },
          "outputs": {
            "approvalRequired": true,
            "approverRoles": ["LOAN_OFFICER"],
            "approvalCount": 1,
            "isSequential": false,
            "slaHours": 24
          }
        },
        {
          "ruleId": "DUAL_APPROVAL_HIGH_AMOUNT",
          "priority": 3,
          "conditions": {
            "loanAmount": ">= 50000"
          },
          "outputs": {
            "approvalRequired": true,
            "approverRoles": ["LOAN_OFFICER", "RISK_MANAGER"],
            "approvalCount": 2,
            "isSequential": true,
            "slaHours": 48
          }
        }
      ]
    }
  ],
  "callbackHandlers": {
    "onApprove": "LoanApprovalHandler",
    "onReject": "LoanRejectionHandler"
  },
  "createdBy": "admin@bank.com"
}
```

**Response:** `201 Created`
```json
{
  "id": "67890abcdef",
  "templateId": "LOAN_APPROVAL_V1",
  "name": "Loan Approval Workflow",
  "version": "1.0.0",
  "entityType": "LOAN_APPLICATION",
  "active": false,
  "createdAt": "2025-10-01T18:30:00",
  "createdBy": "admin@bank.com",
  ...
}
```

**cURL Example:**
```bash
curl -u admin:admin -X POST http://localhost:8089/api/v1/workflow-templates \
  -H "Content-Type: application/json" \
  -d @loan-approval-template.json
```

---

## 2. Get All Templates

Retrieve all workflow templates with optional filtering.

**Endpoint:** `GET /api/v1/workflow-templates`
**Auth:** Authenticated user

**Query Parameters:**
- `entityType` (optional): Filter by entity type (e.g., `LOAN_APPLICATION`)
- `active` (optional): Filter by active status (`true` or `false`)

**Response:** `200 OK`
```json
[
  {
    "id": "67890abcdef",
    "templateId": "LOAN_APPROVAL_V1",
    "name": "Loan Approval Workflow",
    "entityType": "LOAN_APPLICATION",
    "active": true,
    "version": "1.0.0",
    ...
  },
  {
    "templateId": "SOLUTION_CONFIG_V1",
    "name": "Solution Configuration Approval",
    "entityType": "SOLUTION_CONFIGURATION",
    "active": true,
    ...
  }
]
```

**cURL Examples:**
```bash
# Get all templates
curl -u admin:admin http://localhost:8089/api/v1/workflow-templates

# Get only active templates
curl -u admin:admin http://localhost:8089/api/v1/workflow-templates?active=true

# Get templates for specific entity type
curl -u admin:admin "http://localhost:8089/api/v1/workflow-templates?entityType=LOAN_APPLICATION"

# Get active template for entity type
curl -u admin:admin "http://localhost:8089/api/v1/workflow-templates?entityType=LOAN_APPLICATION&active=true"
```

---

## 3. Get Specific Template

Retrieve a single template by template ID.

**Endpoint:** `GET /api/v1/workflow-templates/{templateId}`
**Auth:** Authenticated user

**Response:** `200 OK`
```json
{
  "id": "67890abcdef",
  "templateId": "LOAN_APPROVAL_V1",
  "name": "Loan Approval Workflow",
  "description": "Approval workflow for loan applications",
  "version": "1.0.0",
  "entityType": "LOAN_APPLICATION",
  "active": true,
  "decisionTables": [...],
  "callbackHandlers": {...},
  "createdAt": "2025-10-01T18:30:00",
  "createdBy": "admin@bank.com",
  "publishedAt": "2025-10-01T18:45:00",
  "publishedBy": "admin@bank.com"
}
```

**cURL Example:**
```bash
curl -u admin:admin http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1
```

---

## 4. Update Template

Update an existing template (preserves creation metadata).

**Endpoint:** `PUT /api/v1/workflow-templates/{templateId}`
**Auth:** Requires `ROLE_ADMIN`

**Request Body:**
```json
{
  "name": "Loan Approval Workflow",
  "description": "Updated approval workflow for loan applications",
  "version": "1.1.0",
  "entityType": "LOAN_APPLICATION",
  "decisionTables": [...],
  "callbackHandlers": {...},
  "updatedBy": "admin@bank.com"
}
```

**Response:** `200 OK` (returns updated template)

**cURL Example:**
```bash
curl -u admin:admin -X PUT \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1 \
  -H "Content-Type: application/json" \
  -d @updated-template.json
```

---

## 5. Delete Template

Delete an inactive template (cannot delete active templates).

**Endpoint:** `DELETE /api/v1/workflow-templates/{templateId}`
**Auth:** Requires `ROLE_ADMIN`

**Response:** `204 No Content`

**Error:** `400 Bad Request` if template is active

**cURL Example:**
```bash
curl -u admin:admin -X DELETE \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1
```

---

## 6. Test Template (🔥 Key Feature)

Test a template with sample metadata **before publishing**. Returns matched rules and computed approval plan.

**Endpoint:** `POST /api/v1/workflow-templates/{templateId}/test`
**Auth:** Authenticated user

**Request Body:**
```json
{
  "entityMetadata": {
    "loanAmount": 75000,
    "creditScore": 720,
    "riskLevel": "MEDIUM"
  }
}
```

**Response:** `200 OK`
```json
{
  "templateId": "LOAN_APPROVAL_V1",
  "entityType": "LOAN_APPLICATION",
  "inputMetadata": {
    "loanAmount": 75000,
    "creditScore": 720,
    "riskLevel": "MEDIUM"
  },
  "matchedRules": ["DUAL_APPROVAL_HIGH_AMOUNT"],
  "approvalPlan": {
    "approvalRequired": true,
    "requiredApprovals": 2,
    "approverRoles": ["LOAN_OFFICER", "RISK_MANAGER"],
    "sequential": true,
    "sla": "PT48H"
  },
  "valid": true,
  "validationErrors": [],
  "executionTrace": [
    "Starting template evaluation for: LOAN_APPROVAL_V1",
    "Input metadata: {loanAmount=75000, creditScore=720, riskLevel=MEDIUM}",
    "Evaluation completed successfully",
    "Approval required: true",
    "Matched rules: [DUAL_APPROVAL_HIGH_AMOUNT]"
  ]
}
```

**cURL Example:**
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{
    "entityMetadata": {
      "loanAmount": 75000,
      "creditScore": 720,
      "riskLevel": "MEDIUM"
    }
  }'
```

**Test Scenarios:**
```bash
# Test auto-approval (low amount, high credit score)
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{"entityMetadata": {"loanAmount": 8000, "creditScore": 750, "riskLevel": "LOW"}}'

# Test single approval (medium amount)
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{"entityMetadata": {"loanAmount": 30000, "creditScore": 680, "riskLevel": "LOW"}}'

# Test dual approval (high amount)
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{"entityMetadata": {"loanAmount": 100000, "creditScore": 720, "riskLevel": "MEDIUM"}}'
```

---

## 7. Publish Template

Activate a template for use. Automatically deactivates other templates for the same entity type.

**Endpoint:** `POST /api/v1/workflow-templates/{templateId}/publish`
**Auth:** Requires `ROLE_ADMIN`

**Request Body:**
```json
{
  "publishedBy": "admin@bank.com",
  "confirmed": true
}
```

**Response:** `200 OK`
```json
{
  "id": "67890abcdef",
  "templateId": "LOAN_APPROVAL_V1",
  "active": true,
  "publishedAt": "2025-10-01T18:45:00",
  "publishedBy": "admin@bank.com",
  ...
}
```

**cURL Example:**
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/publish \
  -H "Content-Type: application/json" \
  -d '{"publishedBy": "admin@bank.com", "confirmed": true}'
```

---

## 8. Deactivate Template

Deactivate an active template (useful for maintenance or rollback).

**Endpoint:** `POST /api/v1/workflow-templates/{templateId}/deactivate`
**Auth:** Requires `ROLE_ADMIN`

**Response:** `200 OK` (returns deactivated template)

**cURL Example:**
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/deactivate
```

---

## 9. Get Active Template for Entity Type

Retrieve the currently active template for a specific entity type.

**Endpoint:** `GET /api/v1/workflow-templates/active/{entityType}`
**Auth:** Authenticated user

**Response:** `200 OK` or `404 Not Found`
```json
{
  "templateId": "LOAN_APPROVAL_V1",
  "name": "Loan Approval Workflow",
  "entityType": "LOAN_APPLICATION",
  "active": true,
  ...
}
```

**cURL Example:**
```bash
curl -u admin:admin \
  http://localhost:8089/api/v1/workflow-templates/active/LOAN_APPLICATION
```

---

## 10. Validate Template Configuration

Validate a template configuration **before creating it** (useful for UI pre-validation).

**Endpoint:** `POST /api/v1/workflow-templates/validate`
**Auth:** Authenticated user

**Request Body:** Same as create template request

**Response:** `200 OK`
```json
{
  "valid": true,
  "errors": [],
  "warnings": []
}
```

**Error Response:**
```json
{
  "valid": false,
  "errors": [
    "Template with ID 'LOAN_APPROVAL_V1' already exists",
    "At least one decision table is required"
  ],
  "warnings": []
}
```

**cURL Example:**
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/validate \
  -H "Content-Type: application/json" \
  -d @new-template.json
```

---

## Complete Workflow: Create and Publish a New Template

### Step 1: Validate Configuration
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/validate \
  -H "Content-Type: application/json" \
  -d @loan-approval-template.json
```

### Step 2: Create Template
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates \
  -H "Content-Type: application/json" \
  -d @loan-approval-template.json
```

### Step 3: Test Template with Various Scenarios
```bash
# Low amount - should auto-approve
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{"entityMetadata": {"loanAmount": 5000, "creditScore": 750, "riskLevel": "LOW"}}'

# Medium amount - should require single approval
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{"entityMetadata": {"loanAmount": 30000, "creditScore": 680, "riskLevel": "LOW"}}'

# High amount - should require dual approval
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/test \
  -H "Content-Type: application/json" \
  -d '{"entityMetadata": {"loanAmount": 100000, "creditScore": 720, "riskLevel": "HIGH"}}'
```

### Step 4: Publish Template
```bash
curl -u admin:admin -X POST \
  http://localhost:8089/api/v1/workflow-templates/LOAN_APPROVAL_V1/publish \
  -H "Content-Type: application/json" \
  -d '{"publishedBy": "admin@bank.com", "confirmed": true}'
```

### Step 5: Verify Active Template
```bash
curl -u admin:admin \
  http://localhost:8089/api/v1/workflow-templates/active/LOAN_APPLICATION
```

---

## Decision Table Rule Syntax

### Condition Expressions

**Numeric comparisons:**
```
"< 10000"              // Less than
"<= 10000"             // Less than or equal
"> 10000"              // Greater than
">= 10000"             // Greater than or equal
"== 10000"             // Equal
"!= 10000"             // Not equal
">= 10000 && < 50000"  // Range
```

**String matching:**
```
"LOW"                  // Exact match
"LOW|MEDIUM|HIGH"      // OR condition (any of these)
"contains:premium"     // Contains substring
"startsWith:LOAN"      // Starts with
"endsWith:_APP"        // Ends with
"matches:LOAN_.*"      // Regex match
```

### Hit Policies

- **FIRST**: Return first matching rule (default)
- **ALL**: Merge outputs from all matching rules
- **PRIORITY**: Return highest priority matching rule
- **COLLECT**: Collect all outputs into lists

---

## Security Model

### Method-Level Security

Enabled via `@EnableMethodSecurity` in [SecurityConfig.java](backend/workflow-service/src/main/java/com/bank/product/workflow/config/SecurityConfig.java:17)

### Protected Operations

**Admin-only operations** (`@PreAuthorize("hasRole('ADMIN')")`):
- Create template
- Update template
- Delete template
- Publish template
- Deactivate template

**Public operations** (authenticated users):
- List templates
- Get template details
- Test template
- Validate template configuration

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2025-10-01T18:50:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Template with ID 'LOAN_APPROVAL_V1' already exists",
  "path": "/api/v1/workflow-templates"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2025-10-01T18:50:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied",
  "path": "/api/v1/workflow-templates"
}
```

### 404 Not Found
```json
{
  "timestamp": "2025-10-01T18:50:00",
  "status": 404,
  "error": "Not Found",
  "message": "Template not found: LOAN_APPROVAL_V999",
  "path": "/api/v1/workflow-templates/LOAN_APPROVAL_V999"
}
```

---

## Example Templates

### Solution Configuration Template
```json
{
  "templateId": "SOLUTION_CONFIG_V1",
  "name": "Solution Configuration Approval",
  "entityType": "SOLUTION_CONFIGURATION",
  "version": "1.0.0",
  "decisionTables": [{
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
      {"name": "approvalCount", "type": "number"}
    ],
    "rules": [
      {
        "ruleId": "AUTO_APPROVE_LOW_RISK",
        "conditions": {
          "pricingVariance": "< 5",
          "riskLevel": "LOW"
        },
        "outputs": {
          "approvalRequired": false
        }
      }
    ]
  }],
  "callbackHandlers": {
    "onApprove": "SolutionConfigApprovalHandler",
    "onReject": "SolutionConfigRejectionHandler"
  }
}
```

### Document Verification Template
```json
{
  "templateId": "DOCUMENT_VERIFICATION_V1",
  "name": "Document Verification Workflow",
  "entityType": "DOCUMENT_VERIFICATION",
  "version": "1.0.0",
  "decisionTables": [{
    "name": "Document Verification Rules",
    "hitPolicy": "FIRST",
    "inputs": [
      {"name": "documentType", "type": "string"},
      {"name": "riskScore", "type": "number"},
      {"name": "customerTier", "type": "string"}
    ],
    "outputs": [
      {"name": "approvalRequired", "type": "boolean"},
      {"name": "approverRoles", "type": "array"}
    ],
    "rules": [
      {
        "ruleId": "AUTO_APPROVE_STANDARD_DOCS",
        "conditions": {
          "documentType": "ID_CARD|PASSPORT",
          "riskScore": "< 30",
          "customerTier": "PREMIUM|GOLD"
        },
        "outputs": {
          "approvalRequired": false
        }
      }
    ]
  }],
  "callbackHandlers": {
    "onApprove": "DocumentVerificationHandler"
  }
}
```

---

## Integration with Workflow Service

Once a template is published, it's automatically used when workflows are submitted:

```bash
# Submit workflow - uses active template for LOAN_APPLICATION
curl -u admin:admin -X POST http://localhost:8089/api/v1/workflows/submit \
  -H "Content-Type: application/json" \
  -d '{
    "entityType": "LOAN_APPLICATION",
    "entityId": "loan-12345",
    "entityMetadata": {
      "loanAmount": 75000,
      "creditScore": 720,
      "riskLevel": "MEDIUM"
    },
    "initiatedBy": "loan.officer@bank.com"
  }'
```

The workflow service will:
1. Load the active `LOAN_APPROVAL_V1` template
2. Evaluate the decision rules
3. Compute the approval plan (2 approvers, sequential)
4. Start the Temporal workflow

---

## Summary

The Template Management API provides:

✅ **Dynamic Configuration**: Add new workflow types via API
✅ **Safe Testing**: Validate rules before activation
✅ **Version Control**: Track template versions and changes
✅ **Zero Downtime**: Hot-swap templates without redeployment
✅ **Role-Based Security**: Admin-only mutations, public reads
✅ **Audit Trail**: Track who created/updated/published templates

**Key Achievement**: Business users can configure approval workflows without developer involvement!
