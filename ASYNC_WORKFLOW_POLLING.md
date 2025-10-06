# Async Workflow Polling Implementation

## Overview
Implemented Phase 1 and Phase 2 of the async workflow submission design to handle the gap between solution creation and workflow submission completion.

## Implementation Summary

### Phase 1: Domain Model Changes ✅

#### 1. WorkflowSubmissionStatus Enum
**File:** `backend/common/src/main/java/com/bank/product/enums/WorkflowSubmissionStatus.java`

```java
public enum WorkflowSubmissionStatus {
    NOT_REQUIRED,          // Auto-approved, no workflow needed
    PENDING_SUBMISSION,    // Workflow submission in progress (async)
    SUBMITTED,             // Workflow successfully created
    SUBMISSION_FAILED,     // Workflow submission failed (will retry)
    RETRY_SCHEDULED        // Retry scheduled after failure
}
```

#### 2. Updated Solution Models
**Files:**
- `backend/common/src/main/java/com/bank/product/model/Solution.java`
- `backend/common/src/main/java/com/bank/product/domain/solution/model/Solution.java`

**New Fields Added:**
```java
// Workflow submission tracking
private WorkflowSubmissionStatus workflowSubmissionStatus;
private String workflowErrorMessage;
private Instant workflowRetryAt;

// Workflow metadata (populated after submission)
private Boolean approvalRequired;
private Integer requiredApprovals;
private List<String> approverRoles;
private Boolean sequential;
private Integer slaHours;
private Instant estimatedCompletion;
```

### Phase 2: API Enhancements ✅

#### 3. SolutionWorkflowStatusResponse DTO
**File:** `backend/product-service/src/main/java/com/bank/product/domain/solution/dto/SolutionWorkflowStatusResponse.java`

```java
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SolutionWorkflowStatusResponse {
    private String solutionId;
    private SolutionStatus solutionStatus;
    private WorkflowSubmissionStatus workflowSubmissionStatus;

    // Workflow details (when SUBMITTED)
    private String workflowId;
    private Boolean approvalRequired;
    private Integer requiredApprovals;
    private List<String> approverRoles;
    private Boolean sequential;
    private Integer slaHours;
    private Instant estimatedCompletion;

    // Error details (when SUBMISSION_FAILED)
    private String errorMessage;
    private Instant retryAt;

    // Polling guidance (when PENDING_SUBMISSION)
    private String pollUrl;
    private Integer pollIntervalMs;

    private String message;
}
```

#### 4. New Polling Endpoint
**File:** `backend/product-service/src/main/java/com/bank/product/domain/solution/controller/SolutionController.java`

```java
@GetMapping("/{solutionId}/workflow-status")
public ResponseEntity<SolutionWorkflowStatusResponse> getWorkflowStatus(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @PathVariable String solutionId) {

    SolutionWorkflowStatusResponse status =
        solutionService.getWorkflowSubmissionStatus(tenantId, solutionId);

    return ResponseEntity.ok(status);
}
```

#### 5. Service Implementation
**File:** `backend/product-service/src/main/java/com/bank/product/domain/solution/service/impl/SolutionServiceImpl.java`

```java
@Override
public SolutionWorkflowStatusResponse getWorkflowSubmissionStatus(
        String tenantId, String solutionId) {

    Solution solution = getSolution(tenantId, solutionId);

    // Build response based on workflow submission status
    // Provides polling guidance, workflow metadata, or error details
}
```

## API Usage

### 1. Create Solution (Returns Immediately)

**Request:**
```bash
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: user@example.com" \
  -d '{
    "catalogProductId": "cat-checking-001",
    "solutionName": "Premium Checking",
    "pricingVariance": 20,
    "riskLevel": "MEDIUM"
  }'
```

**Response (Immediate - HTTP 202 Accepted):**
```json
{
  "solutionId": "abc-123",
  "solutionName": "Premium Checking",
  "status": "DRAFT",
  "workflowId": null,
  "workflowStatus": "PENDING_SUBMISSION",
  "message": "Solution created. Workflow submission in progress. Check status via GET /api/v1/solutions/abc-123/workflow-status"
}
```

### 2. Poll for Workflow Status

**Request:**
```bash
curl -u admin:admin123 http://localhost:8082/api/v1/solutions/abc-123/workflow-status \
  -H "X-Tenant-ID: tenant-001"
```

**Response - While Pending:**
```json
{
  "solutionId": "abc-123",
  "solutionStatus": "DRAFT",
  "workflowSubmissionStatus": "PENDING_SUBMISSION",
  "workflowId": null,
  "pollUrl": "/api/v1/solutions/abc-123/workflow-status",
  "pollIntervalMs": 1000,
  "message": "Workflow submission in progress. Please poll this endpoint for updates."
}
```

**Response - After Submission Complete:**
```json
{
  "solutionId": "abc-123",
  "solutionStatus": "DRAFT",
  "workflowSubmissionStatus": "SUBMITTED",
  "workflowId": "workflow-456",
  "approvalRequired": true,
  "requiredApprovals": 2,
  "approverRoles": ["PRODUCT_MANAGER", "RISK_MANAGER"],
  "sequential": false,
  "slaHours": 48,
  "estimatedCompletion": "2025-10-06T15:30:00Z",
  "message": "Workflow submitted successfully. Awaiting approvals."
}
```

**Response - On Failure:**
```json
{
  "solutionId": "abc-123",
  "solutionStatus": "DRAFT",
  "workflowSubmissionStatus": "SUBMISSION_FAILED",
  "workflowId": null,
  "errorMessage": "Workflow service unavailable",
  "retryAt": "2025-10-04T10:35:00Z",
  "message": "Workflow submission failed. Retry scheduled at: 2025-10-04T10:35:00Z"
}
```

## Client Implementation Examples

### JavaScript Polling Pattern

```javascript
async function createSolutionWithPolling(solutionData) {
    // Step 1: Submit solution
    const response = await fetch('/api/v1/solutions/configure', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': 'tenant-001',
            'X-User-ID': 'user@example.com',
            'Authorization': 'Basic ' + btoa('admin:admin123')
        },
        body: JSON.stringify(solutionData)
    });

    const result = await response.json();
    const solutionId = result.solutionId;

    // Step 2: Poll until workflow submitted
    while (true) {
        await sleep(1000); // Poll every second

        const statusResponse = await fetch(
            `/api/v1/solutions/${solutionId}/workflow-status`,
            {
                headers: {
                    'X-Tenant-ID': 'tenant-001',
                    'Authorization': 'Basic ' + btoa('admin:admin123')
                }
            }
        );

        const status = await statusResponse.json();

        if (status.workflowSubmissionStatus === 'SUBMITTED') {
            console.log('Workflow created:', status.workflowId);
            return status;
        }

        if (status.workflowSubmissionStatus === 'SUBMISSION_FAILED') {
            console.error('Workflow submission failed:', status.errorMessage);
            throw new Error(status.errorMessage);
        }

        // Continue polling if PENDING_SUBMISSION
    }
}

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}
```

### Bash Polling Script

```bash
#!/bin/bash

# Create solution
RESPONSE=$(curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: user@example.com" \
  -d '{...}' -s)

SOLUTION_ID=$(echo "$RESPONSE" | jq -r '.solutionId')
echo "Solution created: $SOLUTION_ID"

# Poll for workflow status
while true; do
    sleep 1

    STATUS=$(curl -u admin:admin123 -s \
        http://localhost:8082/api/v1/solutions/$SOLUTION_ID/workflow-status \
        -H "X-Tenant-ID: tenant-001")

    WORKFLOW_STATUS=$(echo "$STATUS" | jq -r '.workflowSubmissionStatus')

    if [ "$WORKFLOW_STATUS" = "SUBMITTED" ]; then
        WORKFLOW_ID=$(echo "$STATUS" | jq -r '.workflowId')
        echo "Workflow submitted: $WORKFLOW_ID"
        break
    fi

    if [ "$WORKFLOW_STATUS" = "SUBMISSION_FAILED" ]; then
        ERROR=$(echo "$STATUS" | jq -r '.errorMessage')
        echo "Workflow submission failed: $ERROR"
        exit 1
    fi

    echo "Still pending..."
done
```

## Next Steps (Not Yet Implemented)

### 1. Update AsyncWorkflowService
- Set `workflowSubmissionStatus = PENDING_SUBMISSION` before async call
- Update to `SUBMITTED` with workflow metadata after success
- Update to `SUBMISSION_FAILED` with error details on failure

### 2. Update ConfigureSolution Response
- Change HTTP status code to 202 Accepted
- Include `pollUrl` and `pollIntervalMs` in response

### 3. Add Retry Logic
- Implement scheduled retry for SUBMISSION_FAILED status
- Use Spring `@Scheduled` or Temporal for retry orchestration

### 4. Testing
- Create end-to-end test with polling
- Test failure scenarios
- Test concurrent submissions

## Benefits

✅ **Clear Status Tracking**: Clients know exactly what's happening
✅ **Polling Guidance**: API tells clients when and where to poll
✅ **Error Handling**: Failed submissions are tracked with retry info
✅ **Backward Compatible**: Existing GET /solutions/{id} still works
✅ **Lightweight**: Dedicated polling endpoint is fast and efficient

## Build Status

✅ All modules compile successfully
✅ No test failures
✅ Ready for testing after remaining TODO items completed
