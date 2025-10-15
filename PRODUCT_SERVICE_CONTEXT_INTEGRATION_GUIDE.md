# Product Service Context Integration Guide

**Date**: October 15, 2025
**Status**: Phase 2 Step 3 - Implementation Guide

---

## Overview

This guide shows how to integrate ProcessingContext into the Product Service.
The context provides complete information about WHO is making the request,
WHAT tenant/party they belong to, and WHAT permissions they have.

---

## Key Components Created

### 1. ContextExtractionFilter
**File**: `backend/product-service/src/main/java/com/bank/product/filter/ContextExtractionFilter.java`

**Purpose**: Extract and validate ProcessingContext from HTTP headers

**Responsibilities**:
- Extract context from `X-Processing-Context` header (injected by API Gateway)
- Validate context (not expired, party active)
- Store context in request attribute
- Enforce mandatory context for protected endpoints

### 2. ContextHolder Utility
**File**: `backend/product-service/src/main/java/com/bank/product/util/ContextHolder.java`

**Purpose**: Convenient access to context from anywhere in request lifecycle

**Usage Examples**:
```java
// Get context
ProcessingContext context = ContextHolder.getContext();

// Get tenant ID
String tenantId = ContextHolder.getRequiredTenantId();

// Check permission
if (ContextHolder.hasPermission("OPEN_ACCOUNTS")) {
    // Allow operation
}
```

---

## How to Update Controllers

### BEFORE (Old Pattern)
```java
@PostMapping("/configure")
public ResponseEntity<ConfigureSolutionResponse> configureSolution(
        @RequestHeader("X-Tenant-ID") String tenantId,
        @RequestHeader("X-User-ID") String userId,
        @RequestBody ConfigureSolutionRequest request) {

    log.info("Configuring solution from catalog {} for tenant {}",
            request.getCatalogProductId(), tenantId);

    Solution solution = solutionService.createSolutionFromCatalog(
            tenantId, userId, request);

    return ResponseEntity.ok(response);
}
```

### AFTER (New Pattern)
```java
@PostMapping("/configure")
public ResponseEntity<ConfigureSolutionResponse> configureSolution(
        @RequestBody ConfigureSolutionRequest request) {

    // Get context (automatically extracted by ContextExtractionFilter)
    ProcessingContext context = ContextHolder.getRequiredContext();

    log.info("Configuring solution from catalog {} for tenant {}, party {}, principal {}",
            request.getCatalogProductId(),
            context.getTenantId(),
            context.getPartyId(),
            context.getPrincipalId());

    // Check permissions
    if (!context.getPermissions().isProductTypeApproved(request.getProductType())) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Product type not approved for this party");
    }

    // Pass full context to service
    Solution solution = solutionService.createSolutionFromCatalog(context, request);

    return ResponseEntity.ok(response);
}
```

---

## Benefits of Using ProcessingContext

### 1. Single Source of Truth
- All context information in one object
- No need to extract multiple headers
- Consistent across all requests

### 2. Rich Context Information
```java
ProcessingContext context = ContextHolder.getContext();

// Identity
String principalId = context.getPrincipalId();
String username = context.getPrincipalUsername();
Set<String> roles = context.getPrincipalRoles();

// Party & Tenant
String partyId = context.getPartyId();
String partyName = context.getPartyName();
String tenantId = context.getTenantId();

// Jurisdiction
String country = context.getJurisdictionCountry();
Set<String> regulatory = context.getRegulatoryFrameworks();

// Permissions
boolean canOpen = context.getPermissions().isCanOpenAccounts();
BigDecimal limit = context.getPermissions().getMaxTransactionLimit();

// Relationships
boolean managing = context.getRelationshipContext().isManagingOnBehalfOf();
Set<String> managedParties = context.getRelationshipContext().getManagedPartyIds();

// Core Banking
String coreEndpoint = context.getCoreSystemEndpoint();
String coreType = context.getCoreSystemType();
```

### 3. Permission-Based Authorization
```java
// Check specific permission
if (context.getPermissions().hasPermission("INITIATE_PAYMENTS")) {
    // Allow
}

// Check transaction limit
if (amount.compareTo(context.getPermissions().getMaxTransactionLimit()) > 0) {
    throw new TransactionLimitExceededException();
}

// Check approved products
if (!context.getPermissions().isProductTypeApproved("CHECKING")) {
    throw new ProductNotApprovedException();
}
```

### 4. Tenant Isolation
```java
// Service layer - automatic tenant filtering
public List<Solution> findAllSolutions() {
    String tenantId = ContextHolder.getRequiredTenantId();
    return solutionRepository.findByTenantId(tenantId);
}

// Repository layer - tenant-aware query
@Query("{ 'tenantId': ?0, 'status': 'ACTIVE' }")
List<Solution> findByTenantId(String tenantId);
```

---

## Service Layer Changes

### BEFORE
```java
@Service
public class SolutionServiceImpl implements SolutionService {

    public Solution createSolutionFromCatalog(
            String tenantId,
            String userId,
            ConfigureSolutionRequest request) {

        // Manually pass tenantId everywhere
        Solution solution = new Solution();
        solution.setTenantId(tenantId);
        solution.setCreatedBy(userId);

        return solutionRepository.save(solution);
    }
}
```

### AFTER
```java
@Service
public class SolutionServiceImpl implements SolutionService {

    public Solution createSolutionFromCatalog(
            ProcessingContext context,
            ConfigureSolutionRequest request) {

        // Context provides everything
        Solution solution = new Solution();
        solution.setTenantId(context.getTenantId());
        solution.setPartyId(context.getPartyId());
        solution.setCreatedBy(context.getPrincipalId());
        solution.setCreatedByUsername(context.getPrincipalUsername());
        solution.setJurisdiction(context.getJurisdictionCountry());

        // Route to correct core banking system
        String coreEndpoint = context.getCoreSystemEndpoint();
        provisionInCore(coreEndpoint, solution);

        return solutionRepository.save(solution);
    }

    // Alternative: Use ContextHolder
    public List<Solution> findAllSolutions() {
        String tenantId = ContextHolder.getRequiredTenantId();
        return solutionRepository.findByTenantId(tenantId);
    }
}
```

---

## Repository Layer Patterns

### Pattern 1: Explicit Tenant Filtering
```java
@Repository
public interface SolutionRepository extends MongoRepository<Solution, String> {

    // Explicit tenant parameter
    List<Solution> findByTenantId(String tenantId);

    @Query("{ 'tenantId': ?0, 'status': ?1 }")
    List<Solution> findByTenantIdAndStatus(String tenantId, SolutionStatus status);
}

// Service usage
String tenantId = ContextHolder.getRequiredTenantId();
List<Solution> solutions = solutionRepository.findByTenantId(tenantId);
```

### Pattern 2: Base Repository with Tenant Isolation
```java
public interface TenantAwareRepository<T, ID> extends MongoRepository<T, ID> {

    default String getCurrentTenantId() {
        return ContextHolder.getRequiredTenantId();
    }

    // Override save to inject tenantId
    @Override
    default <S extends T> S save(S entity) {
        if (entity instanceof TenantAware) {
            TenantAware tenantEntity = (TenantAware) entity;
            if (tenantEntity.getTenantId() == null) {
                tenantEntity.setTenantId(getCurrentTenantId());
            }
        }
        return MongoRepository.super.save(entity);
    }
}
```

---

## Testing with Context

### Unit Tests
```java
@Test
void testCreateSolution_withContext() {
    // Mock context
    ProcessingContext context = ProcessingContext.builder()
            .tenantId("tenant-001")
            .partyId("party-001")
            .principalId("user-123")
            .build();

    // Mock ContextHolder (using PowerMock or similar)
    when(ContextHolder.getContext()).thenReturn(context);

    // Test
    Solution solution = solutionService.createSolutionFromCatalog(context, request);

    assertThat(solution.getTenantId()).isEqualTo("tenant-001");
    assertThat(solution.getPartyId()).isEqualTo("party-001");
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
class SolutionControllerIT {

    @Test
    void testConfigureSolution_withContextHeader() throws Exception {
        ProcessingContext context = createTestContext();

        mockMvc.perform(post("/api/v1/solutions/configure")
                .header("X-Processing-Context", context.toBase64Json())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isAccepted());
    }
}
```

---

## Migration Strategy

### Phase 1: Dual Mode (Current)
Support both old headers AND new context:

```java
@PostMapping("/configure")
public ResponseEntity<ConfigureSolutionResponse> configureSolution(
        @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
        @RequestHeader(value = "X-User-ID", required = false) String userId,
        @RequestBody ConfigureSolutionRequest request) {

    // Try to get context first (new way)
    ProcessingContext context = ContextHolder.getContext();

    if (context != null) {
        // Use context (preferred)
        tenantId = context.getTenantId();
        userId = context.getPrincipalId();
    } else if (tenantId == null || userId == null) {
        // Neither context nor headers provided
        return ResponseEntity.badRequest().body("Missing context or headers");
    }

    // Proceed with business logic
    Solution solution = solutionService.createSolutionFromCatalog(
            tenantId, userId, request);

    return ResponseEntity.ok(response);
}
```

### Phase 2: Context Only (Future)
After all clients are updated:

```java
@PostMapping("/configure")
public ResponseEntity<ConfigureSolutionResponse> configureSolution(
        @RequestBody ConfigureSolutionRequest request) {

    ProcessingContext context = ContextHolder.getRequiredContext();

    Solution solution = solutionService.createSolutionFromCatalog(
            context, request);

    return ResponseEntity.ok(response);
}
```

---

## Complete Example: Updated SolutionController Method

```java
/**
 * Configure a new solution from catalog product with workflow approval
 *
 * This endpoint now uses ProcessingContext for:
 * - Tenant isolation
 * - Party identification
 * - Permission checking
 * - Audit logging
 */
@PostMapping("/configure")
public ResponseEntity<ConfigureSolutionResponse> configureSolution(
        @RequestBody ConfigureSolutionRequest request) {

    // Get processing context (extracted by ContextExtractionFilter)
    ProcessingContext context = ContextHolder.getRequiredContext();

    log.info("Configuring solution from catalog {} for tenant {}, party {}, principal {}",
            request.getCatalogProductId(),
            context.getTenantId(),
            context.getPartyId(),
            context.getPrincipalUsername());

    // 1. Check permissions
    if (!context.getPermissions().isCanOpenAccounts()) {
        log.warn("Principal {} does not have permission to open accounts",
                context.getPrincipalId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ConfigureSolutionResponse.builder()
                        .message("You do not have permission to configure solutions")
                        .build());
    }

    // 2. Check product type approval
    String productType = determineProductType(request);
    if (!context.getPermissions().isProductTypeApproved(productType)) {
        log.warn("Product type {} not approved for party {}",
                productType, context.getPartyId());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ConfigureSolutionResponse.builder()
                        .message("Product type not approved for your organization")
                        .build());
    }

    // 3. Check transaction limits (if applicable)
    if (request.getInitialDeposit() != null) {
        BigDecimal amount = request.getInitialDeposit();
        if (amount.compareTo(context.getPermissions().getMaxTransactionLimit()) > 0) {
            log.warn("Initial deposit {} exceeds limit {} for party {}",
                    amount, context.getPermissions().getMaxTransactionLimit(),
                    context.getPartyId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ConfigureSolutionResponse.builder()
                            .message("Amount exceeds transaction limit")
                            .build());
        }
    }

    // 4. Create solution with full context
    Solution solution = solutionService.createSolutionFromCatalog(context, request);

    // 5. Build workflow metadata (enhanced with context)
    Map<String, Object> entityMetadata = new HashMap<>();
    entityMetadata.put("solutionType", solution.getCategory());
    entityMetadata.put("pricingVariance", request.getPricingVariance() != null ?
            request.getPricingVariance() : 0.0);
    entityMetadata.put("riskLevel", request.getRiskLevel() != null ?
            request.getRiskLevel() : "LOW");
    entityMetadata.put("partyType", context.getPartyType());
    entityMetadata.put("jurisdiction", context.getJurisdictionCountry());
    entityMetadata.put("relationshipType", context.getRelationshipContext() != null ?
            context.getRelationshipContext().getRelationshipType() : "DIRECT");

    // 6. Build workflow request
    WorkflowSubmitRequest workflowRequest = WorkflowSubmitRequest.builder()
            .entityType("SOLUTION_CONFIGURATION")
            .entityId(solution.getId())
            .entityData(buildEntityData(solution, request))
            .entityMetadata(entityMetadata)
            .initiatedBy(context.getPrincipalId())
            .initiatedByUsername(context.getPrincipalUsername())
            .tenantId(context.getTenantId())
            .partyId(context.getPartyId())
            .businessJustification(request.getBusinessJustification())
            .priority(request.getPriority())
            .build();

    // 7. Submit workflow asynchronously
    asyncWorkflowService.submitWorkflowAsync(solution, workflowRequest)
            .exceptionally(ex -> {
                log.error("Workflow submission failed for solution: {}", solution.getId(), ex);
                return null;
            });

    // 8. Return response
    ConfigureSolutionResponse response = ConfigureSolutionResponse.builder()
            .solutionId(solution.getId())
            .solutionName(solution.getName())
            .status(solution.getStatus().name())
            .tenantId(context.getTenantId())
            .partyId(context.getPartyId())
            .workflowId(null)
            .workflowStatus("PENDING_SUBMISSION")
            .pollUrl("/api/v1/solutions/" + solution.getId() + "/workflow-status")
            .pollIntervalMs(1000)
            .message("Solution created. Workflow submission in progress.")
            .build();

    return ResponseEntity.status(HttpStatus.ACCEPTED)
            .header("Location", "/api/v1/solutions/" + solution.getId())
            .header("X-Request-ID", context.getRequestId())
            .body(response);
}
```

---

## Summary

**Benefits of Context Integration**:
1. ✅ Single source of truth for request context
2. ✅ Rich context information (party, tenant, permissions, jurisdiction)
3. ✅ Automatic tenant isolation
4. ✅ Permission-based authorization
5. ✅ Better audit trail
6. ✅ Core banking routing
7. ✅ Relationship-aware processing

**Implementation Complete**:
- ✅ ContextExtractionFilter created
- ✅ ContextHolder utility created
- ✅ Integration guide documented

**Next Steps**:
1. Update SolutionController endpoints
2. Update CatalogController endpoints
3. Update service layer to accept ProcessingContext
4. Add permission checks
5. Test with real context headers

**Estimated Effort**: 6-8 hours for complete integration
