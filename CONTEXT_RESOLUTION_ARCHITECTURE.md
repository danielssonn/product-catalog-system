# Context Resolution Architecture

**Last Updated**: October 15, 2025
**Status**: ✅ ACTIVE - Mandatory for all new services
**Version**: 1.0

---

## Executive Summary

The Product Catalog System implements a **Context Resolution Chain** architecture where each layer resolves critical information for downstream processing. This architecture ensures proper multi-tenancy, party relationship management, and jurisdiction-aware processing across all services.

**Key Principle**: *Authentication resolves WHO, Party Service resolves WHERE/WHAT, Business Services process in CONTEXT*

---

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [The Context Resolution Chain](#the-context-resolution-chain)
3. [Processing Context Structure](#processing-context-structure)
4. [Architecture Flow](#architecture-flow)
5. [Service Responsibilities](#service-responsibilities)
6. [Implementation Patterns](#implementation-patterns)
7. [Mandatory Standards for All Services](#mandatory-standards-for-all-services)
8. [Example: Payment Service Implementation](#example-payment-service-implementation)
9. [Context Propagation](#context-propagation)
10. [Error Handling](#error-handling)
11. [Performance Considerations](#performance-considerations)
12. [Security Implications](#security-implications)

---

## Core Concepts

### What is a Processing Context?

A **Processing Context** is a comprehensive data structure that contains ALL information needed to process a business transaction correctly:

- **Tenant Identification**: Which organization/bank is this for?
- **Party Context**: Who is the customer? What relationships do they have?
- **Jurisdiction**: What regulatory rules apply?
- **Permissions**: What can this party do?
- **Processing Region**: Where should this be processed?
- **Relationship Graph**: Who manages whom? Parent entities?

### Why Context Resolution?

#### **Problem Without Context Resolution**
```
❌ Each service independently resolves:
   - Product Service: Looks up tenant
   - Payment Service: Looks up tenant again
   - Core Banking: Looks up tenant again

Result:
   - Inconsistent tenant resolution
   - Performance overhead (3+ database calls)
   - Complex service logic
   - Potential security gaps
```

#### **Solution With Context Resolution**
```
✅ Context resolved ONCE at the gateway:
   1. Auth Service: Validates principal
   2. Party Service: Resolves complete context
   3. All downstream services: Use provided context

Result:
   - Consistent context across all services
   - Single resolution overhead
   - Simple service logic
   - Strong security boundary
```

---

## The Context Resolution Chain

### Visual Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT REQUEST                          │
│                    (with credentials/token)                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 1: AUTHENTICATION SERVICE (Port 8097)                     │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  Question: WHO is making this request?                          │
│                                                                  │
│  Input:  Credentials (username/password or JWT)                 │
│  Output: PRINCIPAL                                              │
│          {                                                       │
│            "userId": "user-123",                                │
│            "username": "john.doe@acmebank.com",                 │
│            "roles": ["ROLE_USER", "ROLE_ACCOUNT_MANAGER"],     │
│            "channelId": "PORTAL"                                │
│          }                                                       │
└────────────────────────────┬────────────────────────────────────┘
                             │ Principal extracted from JWT
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 2: PARTY SERVICE (Port 8083)                              │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  Question: WHAT is the processing context?                      │
│                                                                  │
│  Input:  Principal + Optional Party Hints                       │
│  Output: PROCESSING CONTEXT (see structure below)               │
│                                                                  │
│  Resolution Steps:                                              │
│  1. Resolve Party from Principal                                │
│     - Lookup user → party mapping in Neo4j                      │
│     - Handle "manages on behalf of" relationships               │
│     - Traverse party hierarchy                                  │
│                                                                  │
│  2. Resolve Tenant from Party                                   │
│     - Party → Legal Entity → Tenant mapping                     │
│     - Handle multi-tenant scenarios                             │
│                                                                  │
│  3. Resolve Jurisdiction                                        │
│     - Party domicile + transaction location                     │
│     - Regulatory region determination                           │
│                                                                  │
│  4. Resolve Permissions                                         │
│     - Role-based + relationship-based permissions               │
│     - Transaction limits, approval requirements                 │
│                                                                  │
│  5. Build Complete Context                                      │
│     - Assemble all resolved data                                │
│     - Add audit trail metadata                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │ Processing Context
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 3: API GATEWAY (Port 8080)                                │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  Action: Inject context into downstream requests                │
│                                                                  │
│  - Add X-Processing-Context header                              │
│  - Add individual headers for common fields:                    │
│    * X-Tenant-ID                                                │
│    * X-Party-ID                                                 │
│    * X-Jurisdiction                                             │
│  - Cache context for request duration                           │
│  - Route to appropriate service                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │ Context in headers
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 4: BUSINESS SERVICES                                      │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │ Product Service │  │ Payment Service │  │ Account Service ││
│  │   (Port 8082)   │  │  (Port 80XX)    │  │  (Port 80XX)    ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
│                                                                  │
│  ALL services:                                                   │
│  1. Extract X-Processing-Context from headers                   │
│  2. Deserialize to ProcessingContext object                     │
│  3. Use context for:                                            │
│     - Tenant isolation (filter data by tenantId)                │
│     - Party-specific logic (pricing, features, limits)          │
│     - Jurisdiction rules (compliance, regulatory)               │
│     - Permissions (authorization decisions)                     │
│  4. NO independent tenant/party resolution                      │
│                                                                  │
└────────────────────────────┬────────────────────────────────────┘
                             │ Context-aware processing
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  STEP 5: CORE BANKING INTEGRATION                               │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  Action: Execute in resolved context                            │
│                                                                  │
│  - Route to correct core system (based on tenantId)             │
│  - Apply jurisdiction-specific rules                            │
│  - Create accounts with proper party linkage                    │
│  - Maintain audit trail with full context                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Processing Context Structure

### Complete Data Model

```java
/**
 * Processing Context - Complete context for transaction processing
 *
 * This object is resolved ONCE by the Party Service and propagated
 * to all downstream services. It contains ALL information needed
 * for proper multi-tenant, party-aware, jurisdiction-compliant processing.
 *
 * @immutable - Context should not be modified after creation
 * @serializable - Must be JSON serializable for HTTP header transmission
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProcessingContext implements Serializable {

    // ═══════════════════════════════════════════════════════════════
    // PRINCIPAL INFORMATION (from Authentication)
    // ═══════════════════════════════════════════════════════════════

    /**
     * User ID of the authenticated principal
     * Source: Authentication Service
     */
    private String principalId;

    /**
     * Username/email of the principal
     */
    private String principalUsername;

    /**
     * Roles assigned to the principal
     */
    private Set<String> principalRoles;

    /**
     * Channel through which request originated
     * Values: PORTAL, MOBILE, API, BRANCH, HOST_TO_HOST
     */
    private String channelId;

    // ═══════════════════════════════════════════════════════════════
    // PARTY CONTEXT (from Party Service)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Primary party ID for this request
     * This is the party on whose behalf the transaction is executed
     */
    private String partyId;

    /**
     * Party name (for logging/audit)
     */
    private String partyName;

    /**
     * Party type
     * Values: INDIVIDUAL, CORPORATE, GOVERNMENT, FINANCIAL_INSTITUTION
     */
    private String partyType;

    /**
     * Legal Entity Identifier (LEI) if available
     * Used for entity resolution and regulatory reporting
     */
    private String legalEntityId;

    /**
     * Party status
     * Values: ACTIVE, INACTIVE, SUSPENDED, PENDING_APPROVAL
     */
    private String partyStatus;

    // ═══════════════════════════════════════════════════════════════
    // TENANT CONTEXT (from Party Service)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Tenant ID - CRITICAL for multi-tenancy
     * All data filtering MUST use this field
     */
    private String tenantId;

    /**
     * Tenant name (for logging/audit)
     */
    private String tenantName;

    /**
     * Tenant type
     * Values: COMMERCIAL_BANK, INVESTMENT_BANK, CREDIT_UNION, FINTECH
     */
    private String tenantType;

    // ═══════════════════════════════════════════════════════════════
    // JURISDICTION & REGULATORY CONTEXT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Primary jurisdiction code (ISO 3166-1 alpha-2)
     * Examples: US, GB, DE, JP
     */
    private String jurisdictionCountry;

    /**
     * Sub-jurisdiction (state/province)
     * Examples: US-NY, US-CA, CA-ON
     */
    private String jurisdictionRegion;

    /**
     * Processing region for infrastructure routing
     * Values: AMERICAS, EMEA, APAC
     */
    private String processingRegion;

    /**
     * Regulatory framework applicable
     * Examples: BASEL_III, DODD_FRANK, MiFID_II, SOX
     */
    private Set<String> regulatoryFrameworks;

    /**
     * Compliance tags for this context
     * Examples: KYC_VERIFIED, AML_CHECKED, SANCTIONS_CLEARED
     */
    private Set<String> complianceTags;

    // ═══════════════════════════════════════════════════════════════
    // RELATIONSHIP CONTEXT (from Neo4j Party Graph)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Relationship context for "Manages On Behalf Of" scenarios
     */
    private RelationshipContext relationshipContext;

    @Data
    @Builder
    public static class RelationshipContext {
        /**
         * Is this request made on behalf of another party?
         */
        private boolean isManagingOnBehalfOf;

        /**
         * List of party IDs this principal can manage
         */
        private Set<String> managedPartyIds;

        /**
         * Parent entity in the party hierarchy
         */
        private String parentEntityId;

        /**
         * Full hierarchy path from root to current party
         * Example: ["root-entity", "subsidiary-1", "current-party"]
         */
        private List<String> hierarchyPath;

        /**
         * Relationship type to parent
         * Values: SUBSIDIARY, BRANCH, AFFILIATE, CLIENT
         */
        private String relationshipType;
    }

    // ═══════════════════════════════════════════════════════════════
    // PERMISSIONS & LIMITS (resolved from Party + Roles)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Permission context for authorization decisions
     */
    private PermissionContext permissions;

    @Data
    @Builder
    public static class PermissionContext {
        /**
         * Can this party open new accounts?
         */
        private boolean canOpenAccounts;

        /**
         * Can this party initiate payments?
         */
        private boolean canInitiatePayments;

        /**
         * Does this party require approval for transactions?
         */
        private boolean requiresApproval;

        /**
         * Maximum transaction amount (in base currency)
         */
        private BigDecimal maxTransactionLimit;

        /**
         * Daily transaction limit
         */
        private BigDecimal dailyTransactionLimit;

        /**
         * Approved product types for this party
         * Example: ["CHECKING", "SAVINGS", "LOAN"]
         */
        private Set<String> approvedProductTypes;

        /**
         * Restricted operations for this party
         * Example: ["WIRE_TRANSFER", "INTERNATIONAL_PAYMENT"]
         */
        private Set<String> restrictedOperations;
    }

    // ═══════════════════════════════════════════════════════════════
    // CORE BANKING CONTEXT (for routing)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Core banking system type for this tenant
     * Values: TEMENOS_T24, FINACLE, FIS_PROFILE, CUSTOM
     */
    private String coreSystemType;

    /**
     * Core banking system URL/endpoint
     */
    private String coreSystemEndpoint;

    /**
     * Core system customer ID (if different from partyId)
     */
    private String coreSystemCustomerId;

    // ═══════════════════════════════════════════════════════════════
    // AUDIT & METADATA
    // ═══════════════════════════════════════════════════════════════

    /**
     * Unique request ID for tracing
     */
    private String requestId;

    /**
     * Timestamp when context was resolved
     */
    private Instant contextResolvedAt;

    /**
     * Context resolution source
     * Values: PARTY_SERVICE, CACHE, FALLBACK
     */
    private String resolutionSource;

    /**
     * Context version (for compatibility)
     */
    private String contextVersion = "1.0";

    /**
     * Additional metadata as key-value pairs
     * For extensibility without schema changes
     */
    private Map<String, String> metadata;

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Serialize context to JSON for HTTP header transmission
     */
    public String toJson() {
        // Implementation uses Jackson ObjectMapper
        return JsonUtils.toJson(this);
    }

    /**
     * Deserialize context from JSON
     */
    public static ProcessingContext fromJson(String json) {
        return JsonUtils.fromJson(json, ProcessingContext.class);
    }

    /**
     * Check if context is valid
     */
    public boolean isValid() {
        return principalId != null &&
               partyId != null &&
               tenantId != null &&
               contextResolvedAt != null &&
               contextResolvedAt.isAfter(Instant.now().minusSeconds(300)); // 5 min expiry
    }

    /**
     * Check if party has specific permission
     */
    public boolean hasPermission(String operation) {
        if (permissions == null) return false;
        return !permissions.getRestrictedOperations().contains(operation);
    }

    /**
     * Get full jurisdiction code
     */
    public String getFullJurisdiction() {
        if (jurisdictionRegion != null) {
            return jurisdictionCountry + "-" + jurisdictionRegion;
        }
        return jurisdictionCountry;
    }
}
```

---

## Architecture Flow

### Sequence Diagram: Account Opening Flow

```
Actor: Customer
Browser → API Gateway: POST /api/v1/accounts/open
                        Authorization: Bearer <JWT>
                        X-Party-ID: party-456 (optional)

API Gateway → Auth Service: Validate JWT
Auth Service → API Gateway: Principal {userId, roles, channel}

API Gateway → Party Service: POST /api/v1/context/resolve
                              {principalId, partyId (optional)}

Party Service → Neo4j: Query party relationships
Neo4j → Party Service: Party graph data

Party Service → MongoDB: Get tenant mapping
MongoDB → Party Service: Tenant details

Party Service → Party Service: Build ProcessingContext
                                - Resolve jurisdiction
                                - Calculate permissions
                                - Build relationship context

Party Service → API Gateway: ProcessingContext{...}

API Gateway → API Gateway: Inject context into headers
                            X-Processing-Context: <JSON>
                            X-Tenant-ID: tenant-789
                            X-Party-ID: party-456

API Gateway → Product Service: GET /api/v1/products
                                X-Processing-Context: <context>

Product Service → Product Service: Extract context from header
                                    Filter products by tenantId
                                    Apply party-specific rules
                                    Check jurisdiction compliance

Product Service → API Gateway: Available products

API Gateway → Workflow Service: POST /api/v1/workflows/submit
                                 X-Processing-Context: <context>
                                 {productId, customizations}

Workflow Service → Workflow Service: Check if approval required
                                      (based on context.permissions)

Workflow Service → Core Banking: POST /core/accounts/create
                                  X-Processing-Context: <context>
                                  Route to coreSystemEndpoint

Core Banking → Core Banking: Create account in correct tenant
                              Apply jurisdiction rules
                              Link to party in core system

Core Banking → Workflow Service: Account created

Workflow Service → API Gateway: Workflow completed

API Gateway → Browser: Account opened successfully
```

---

## Service Responsibilities

### 1. Authentication Service (Port 8097)

**Responsibility**: Identify WHO is making the request

**Does**:
- ✅ Validate credentials
- ✅ Issue JWT tokens
- ✅ Verify JWT signatures
- ✅ Extract principal information
- ✅ Enforce authentication policies

**Does NOT**:
- ❌ Resolve tenant
- ❌ Resolve party
- ❌ Make authorization decisions (beyond authentication)

### 2. Party Service (Port 8083)

**Responsibility**: Resolve COMPLETE processing context

**Does**:
- ✅ Map principal → party
- ✅ Map party → tenant
- ✅ Resolve party relationships ("manages on behalf of")
- ✅ Traverse party hierarchies
- ✅ Determine jurisdiction
- ✅ Calculate context-based permissions
- ✅ Build complete ProcessingContext object
- ✅ Maintain party graph in Neo4j
- ✅ Synchronize with source systems

**Does NOT**:
- ❌ Authenticate users
- ❌ Process business transactions
- ❌ Store product/account data

**Key Endpoints**:
```
POST /api/v1/context/resolve         # Main context resolution
GET  /api/v1/parties/{id}/context    # Get context for specific party
POST /api/v1/parties/sync            # Sync party from source systems
GET  /api/v1/parties/search          # Search parties
```

### 3. API Gateway (Port 8080)

**Responsibility**: Orchestrate context resolution and inject into requests

**Does**:
- ✅ Extract JWT from Authorization header
- ✅ Call Auth Service to validate principal
- ✅ Call Party Service to resolve context
- ✅ Cache context for request duration
- ✅ Inject context into downstream headers
- ✅ Route to appropriate services
- ✅ Handle circuit breaking
- ✅ Apply rate limiting

**Does NOT**:
- ❌ Resolve context itself (delegates to Party Service)
- ❌ Make business logic decisions

**Filter Chain**:
```
1. Authentication Filter      → Extract & validate JWT
2. Context Resolution Filter  → Call Party Service
3. Context Injection Filter   → Add X-Processing-Context header
4. Channel Routing Filter     → Route based on channel
5. Circuit Breaker Filter     → Handle failures
6. Logging Filter             → Log with context
```

### 4. Business Services (Product, Payment, Account, etc.)

**Responsibility**: Process business transactions IN provided context

**Does**:
- ✅ Extract X-Processing-Context from headers
- ✅ Validate context is present and valid
- ✅ Filter ALL data by tenantId
- ✅ Apply party-specific business rules
- ✅ Check jurisdiction compliance
- ✅ Enforce permissions from context
- ✅ Log all actions with full context

**Does NOT**:
- ❌ Resolve tenant independently
- ❌ Resolve party independently
- ❌ Make cross-tenant queries
- ❌ Bypass context validation

**Mandatory Pattern**:
```java
@RestController
public class ProductController {

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts(
            @RequestHeader("X-Processing-Context") String contextJson) {

        // STEP 1: Extract and validate context (MANDATORY)
        ProcessingContext context = ProcessingContext.fromJson(contextJson);
        if (!context.isValid()) {
            throw new InvalidContextException("Context expired or invalid");
        }

        // STEP 2: Filter by tenant (MANDATORY)
        List<Product> products = productService.findByTenantId(context.getTenantId());

        // STEP 3: Apply party-specific rules
        products = applyPartyRules(products, context);

        // STEP 4: Apply jurisdiction filters
        products = applyJurisdictionRules(products, context.getJurisdictionCountry());

        // STEP 5: Check permissions
        products = filterByPermissions(products, context.getPermissions());

        return ResponseEntity.ok(products);
    }
}
```

---

## Implementation Patterns

### Pattern 1: Context Extraction (All Services)

```java
/**
 * Context Extraction Utility
 * Used by all business services to extract processing context
 */
@Component
public class ContextExtractor {

    private static final String CONTEXT_HEADER = "X-Processing-Context";
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String PARTY_HEADER = "X-Party-ID";

    /**
     * Extract full processing context from request headers
     */
    public ProcessingContext extractContext(HttpServletRequest request) {
        String contextJson = request.getHeader(CONTEXT_HEADER);

        if (contextJson == null || contextJson.isEmpty()) {
            throw new MissingContextException(
                "X-Processing-Context header is required for all requests"
            );
        }

        ProcessingContext context = ProcessingContext.fromJson(contextJson);

        if (!context.isValid()) {
            throw new InvalidContextException(
                "Processing context is expired or invalid"
            );
        }

        return context;
    }

    /**
     * Extract tenant ID quickly (without full context deserialization)
     * Use this for performance-critical paths where only tenant is needed
     */
    public String extractTenantId(HttpServletRequest request) {
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null) {
            // Fallback to full context
            ProcessingContext context = extractContext(request);
            return context.getTenantId();
        }
        return tenantId;
    }
}
```

### Pattern 2: Context-Aware Repository (Data Access)

```java
/**
 * Base repository interface that enforces tenant isolation
 * ALL domain repositories MUST extend this
 */
public interface TenantAwareRepository<T, ID> extends MongoRepository<T, ID> {

    /**
     * Find by ID with tenant isolation
     */
    Optional<T> findByIdAndTenantId(ID id, String tenantId);

    /**
     * Find all for tenant
     */
    List<T> findByTenantId(String tenantId);

    /**
     * Find by ID - DEPRECATED, use findByIdAndTenantId
     */
    @Deprecated
    @Override
    default Optional<T> findById(ID id) {
        throw new UnsupportedOperationException(
            "Use findByIdAndTenantId to enforce tenant isolation"
        );
    }

    /**
     * Find all - DEPRECATED, use findByTenantId
     */
    @Deprecated
    @Override
    default List<T> findAll() {
        throw new UnsupportedOperationException(
            "Use findByTenantId to enforce tenant isolation"
        );
    }
}

/**
 * Example: Product Repository
 */
public interface ProductRepository extends TenantAwareRepository<Product, String> {

    List<Product> findByTenantIdAndCategory(String tenantId, String category);

    List<Product> findByTenantIdAndStatus(String tenantId, ProductStatus status);

    Optional<Product> findByTenantIdAndCatalogProductId(
        String tenantId,
        String catalogProductId
    );
}
```

### Pattern 3: Context-Aware Service Layer

```java
/**
 * Base service that provides context-aware operations
 */
@Service
public abstract class ContextAwareService {

    @Autowired
    protected ContextExtractor contextExtractor;

    /**
     * Validate that operation is allowed in this context
     */
    protected void validateContext(ProcessingContext context, String operation) {
        // Check party status
        if (!"ACTIVE".equals(context.getPartyStatus())) {
            throw new PartyInactiveException(
                "Party " + context.getPartyId() + " is not active"
            );
        }

        // Check permissions
        if (!context.hasPermission(operation)) {
            throw new PermissionDeniedException(
                "Operation " + operation + " not permitted in this context"
            );
        }

        // Check jurisdiction
        if (context.getRegulatoryFrameworks().contains("SANCTIONED")) {
            throw new ComplianceException(
                "Operations restricted due to sanctions"
            );
        }
    }

    /**
     * Apply party-specific rules to entity
     */
    protected <T> T applyPartyRules(T entity, ProcessingContext context) {
        // Override in subclasses
        return entity;
    }

    /**
     * Log operation with full context
     */
    protected void logWithContext(String operation, ProcessingContext context) {
        log.info("Operation: {} | Tenant: {} | Party: {} | Principal: {} | Request: {}",
            operation,
            context.getTenantId(),
            context.getPartyId(),
            context.getPrincipalId(),
            context.getRequestId()
        );
    }
}
```

---

## Mandatory Standards for All Services

### ⚠️ CRITICAL: Every New Service MUST Follow These Rules

#### Rule 1: Context Extraction
```java
❌ FORBIDDEN:
@GetMapping("/products")
public List<Product> getProducts() {
    return productRepository.findAll(); // NO TENANT ISOLATION!
}

✅ REQUIRED:
@GetMapping("/products")
public List<Product> getProducts(
        @RequestHeader("X-Processing-Context") String contextJson) {
    ProcessingContext context = ProcessingContext.fromJson(contextJson);
    return productRepository.findByTenantId(context.getTenantId());
}
```

#### Rule 2: Tenant Isolation
```java
❌ FORBIDDEN:
productRepository.findById(productId) // Cross-tenant leak!

✅ REQUIRED:
productRepository.findByIdAndTenantId(productId, context.getTenantId())
```

#### Rule 3: No Independent Context Resolution
```java
❌ FORBIDDEN:
String tenantId = tenantService.resolveTenantForUser(userId); // Duplicate work!

✅ REQUIRED:
String tenantId = context.getTenantId(); // Already resolved by Party Service
```

#### Rule 4: Context Validation
```java
✅ REQUIRED at start of every controller method:
ProcessingContext context = ProcessingContext.fromJson(contextJson);
if (!context.isValid()) {
    throw new InvalidContextException("Context expired");
}
```

#### Rule 5: Audit Logging
```java
✅ REQUIRED for all operations:
log.info("Operation: {} | Tenant: {} | Party: {} | User: {} | RequestID: {}",
    operation,
    context.getTenantId(),
    context.getPartyId(),
    context.getPrincipalId(),
    context.getRequestId()
);
```

---

## Example: Payment Service Implementation

### Complete Example Following Context Resolution Architecture

```java
/**
 * Payment Service Implementation
 * Demonstrates proper context resolution architecture
 */

// ═══════════════════════════════════════════════════════════════
// 1. CONTROLLER LAYER
// ═══════════════════════════════════════════════════════════════

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final ContextExtractor contextExtractor;

    /**
     * Initiate a payment
     * Context provides: tenant, party, permissions, limits
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("X-Processing-Context") String contextJson,
            @Valid @RequestBody PaymentRequest request) {

        // STEP 1: Extract context
        ProcessingContext context = ProcessingContext.fromJson(contextJson);

        // STEP 2: Validate context
        if (!context.isValid()) {
            throw new InvalidContextException("Context expired or invalid");
        }

        // STEP 3: Check permissions
        if (!context.getPermissions().isCanInitiatePayments()) {
            throw new PermissionDeniedException(
                "Party " + context.getPartyId() + " cannot initiate payments"
            );
        }

        // STEP 4: Check transaction limits
        if (request.getAmount().compareTo(
                context.getPermissions().getMaxTransactionLimit()) > 0) {
            throw new LimitExceededException(
                "Amount exceeds limit of " +
                context.getPermissions().getMaxTransactionLimit()
            );
        }

        // STEP 5: Log with context
        log.info("Initiating payment | Amount: {} | Tenant: {} | Party: {} | User: {}",
            request.getAmount(),
            context.getTenantId(),
            context.getPartyId(),
            context.getPrincipalId()
        );

        // STEP 6: Process payment
        PaymentResponse response = paymentService.initiatePayment(request, context);

        return ResponseEntity.ok(response);
    }

    /**
     * Get payment history
     * Context provides: tenant for isolation
     */
    @GetMapping("/history")
    public ResponseEntity<List<Payment>> getPaymentHistory(
            @RequestHeader("X-Processing-Context") String contextJson,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        ProcessingContext context = ProcessingContext.fromJson(contextJson);

        // Filter by tenant AND party
        List<Payment> payments = paymentService.getPaymentHistory(
            context.getTenantId(),
            context.getPartyId(),
            fromDate,
            toDate
        );

        return ResponseEntity.ok(payments);
    }
}

// ═══════════════════════════════════════════════════════════════
// 2. SERVICE LAYER
// ═══════════════════════════════════════════════════════════════

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService extends ContextAwareService {

    private final PaymentRepository paymentRepository;
    private final WorkflowService workflowService;
    private final CoreBankingAdapter coreBankingAdapter;

    /**
     * Initiate payment with full context awareness
     */
    @Transactional
    public PaymentResponse initiatePayment(
            PaymentRequest request,
            ProcessingContext context) {

        // STEP 1: Create payment entity with tenant isolation
        Payment payment = Payment.builder()
            .id(UUID.randomUUID().toString())
            .tenantId(context.getTenantId())
            .partyId(context.getPartyId())
            .fromAccount(request.getFromAccount())
            .toAccount(request.getToAccount())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .status(PaymentStatus.PENDING)
            .initiatedBy(context.getPrincipalId())
            .initiatedAt(Instant.now())
            .jurisdiction(context.getFullJurisdiction())
            .regulatoryFrameworks(context.getRegulatoryFrameworks())
            .build();

        // STEP 2: Apply party-specific rules
        applyPartyRules(payment, context);

        // STEP 3: Apply jurisdiction-specific rules
        applyJurisdictionRules(payment, context);

        // STEP 4: Check if approval required (based on context)
        if (context.getPermissions().isRequiresApproval() ||
            isHighValuePayment(payment)) {

            payment.setStatus(PaymentStatus.PENDING_APPROVAL);
            payment = paymentRepository.save(payment);

            // Submit to workflow with context
            workflowService.submitPaymentApproval(payment, context);

            return PaymentResponse.builder()
                .paymentId(payment.getId())
                .status(PaymentStatus.PENDING_APPROVAL)
                .message("Payment submitted for approval")
                .build();
        }

        // STEP 5: Process immediately if no approval required
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        // STEP 6: Send to core banking system (context determines routing)
        CoreBankingRequest coreRequest = CoreBankingRequest.builder()
            .tenantId(context.getTenantId())
            .coreSystemType(context.getCoreSystemType())
            .coreSystemEndpoint(context.getCoreSystemEndpoint())
            .payment(payment)
            .build();

        CoreBankingResponse coreResponse = coreBankingAdapter.processPayment(coreRequest);

        // STEP 7: Update payment status
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCoreSystemTransactionId(coreResponse.getTransactionId());
        payment.setCompletedAt(Instant.now());
        payment = paymentRepository.save(payment);

        return PaymentResponse.builder()
            .paymentId(payment.getId())
            .status(PaymentStatus.COMPLETED)
            .coreSystemTransactionId(coreResponse.getTransactionId())
            .message("Payment completed successfully")
            .build();
    }

    /**
     * Apply party-specific rules (override from base class)
     */
    @Override
    protected Payment applyPartyRules(Payment payment, ProcessingContext context) {
        // Example: Corporate parties get better exchange rates
        if ("CORPORATE".equals(context.getPartyType())) {
            payment.setExchangeRateType("CORPORATE");
        }

        // Example: High-value parties get faster processing
        if (context.getPermissions().getMaxTransactionLimit()
                .compareTo(new BigDecimal("1000000")) > 0) {
            payment.setPriority(PaymentPriority.HIGH);
        }

        return payment;
    }

    /**
     * Apply jurisdiction-specific rules
     */
    private void applyJurisdictionRules(Payment payment, ProcessingContext context) {
        // Example: EU requires SEPA validation
        if ("EU".equals(context.getProcessingRegion())) {
            validateSepaCompliance(payment);
        }

        // Example: US requires OFAC check
        if ("US".equals(context.getJurisdictionCountry())) {
            checkOfacSanctions(payment, context);
        }
    }

    /**
     * Get payment history with tenant and party isolation
     */
    public List<Payment> getPaymentHistory(
            String tenantId,
            String partyId,
            LocalDate fromDate,
            LocalDate toDate) {

        // ALWAYS filter by tenant AND party
        return paymentRepository.findByTenantIdAndPartyIdAndDateRange(
            tenantId,
            partyId,
            fromDate,
            toDate
        );
    }
}

// ═══════════════════════════════════════════════════════════════
// 3. REPOSITORY LAYER
// ═══════════════════════════════════════════════════════════════

/**
 * Payment Repository with mandatory tenant isolation
 */
public interface PaymentRepository extends TenantAwareRepository<Payment, String> {

    /**
     * Find payments by tenant and party
     * ALWAYS include both filters
     */
    List<Payment> findByTenantIdAndPartyIdAndDateRange(
        String tenantId,
        String partyId,
        LocalDate fromDate,
        LocalDate toDate
    );

    /**
     * Find payments by tenant and status
     */
    List<Payment> findByTenantIdAndStatus(
        String tenantId,
        PaymentStatus status
    );

    /**
     * Find payment with tenant isolation
     */
    Optional<Payment> findByIdAndTenantId(
        String id,
        String tenantId
    );
}

// ═══════════════════════════════════════════════════════════════
// 4. ENTITY/DOMAIN MODEL
// ═══════════════════════════════════════════════════════════════

@Document(collection = "payments")
@Data
@Builder
public class Payment {

    @Id
    private String id;

    /**
     * CRITICAL: Tenant ID for isolation
     * Must be indexed and included in ALL queries
     */
    @Indexed
    private String tenantId;

    /**
     * Party ID from processing context
     */
    @Indexed
    private String partyId;

    // Payment fields
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;

    // Context tracking
    private String initiatedBy;
    private Instant initiatedAt;
    private String jurisdiction;
    private Set<String> regulatoryFrameworks;

    // Core banking integration
    private String coreSystemTransactionId;
    private Instant completedAt;

    // Audit fields
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
}
```

---

## Context Propagation

### HTTP Headers Used

```
X-Processing-Context: <full JSON serialized context>
X-Tenant-ID: <tenantId>              # For quick access
X-Party-ID: <partyId>                 # For quick access
X-Jurisdiction: <jurisdictionCode>    # For quick access
X-Request-ID: <requestId>             # For tracing
```

### Inter-Service Communication

When Service A calls Service B:

```java
@Service
public class ProductService {

    @Autowired
    private RestTemplate restTemplate;

    public Payment initiatePayment(
            PaymentRequest request,
            ProcessingContext context) {

        HttpHeaders headers = new HttpHeaders();

        // MANDATORY: Propagate full context
        headers.add("X-Processing-Context", context.toJson());

        // Optional: Add individual headers for convenience
        headers.add("X-Tenant-ID", context.getTenantId());
        headers.add("X-Party-ID", context.getPartyId());
        headers.add("X-Request-ID", context.getRequestId());

        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);

        return restTemplate.postForObject(
            "http://payment-service/api/v1/payments/initiate",
            entity,
            Payment.class
        );
    }
}
```

### Feign Client Configuration

```java
@FeignClient(
    name = "payment-service",
    configuration = ContextPropagationConfiguration.class
)
public interface PaymentServiceClient {

    @PostMapping("/api/v1/payments/initiate")
    Payment initiatePayment(
        @RequestHeader("X-Processing-Context") String context,
        @RequestBody PaymentRequest request
    );
}

/**
 * Auto-inject context into all Feign calls
 */
@Configuration
public class ContextPropagationConfiguration {

    @Bean
    public RequestInterceptor contextPropagationInterceptor() {
        return requestTemplate -> {
            // Get context from thread-local or request scope
            ProcessingContext context = ContextHolder.getContext();
            if (context != null) {
                requestTemplate.header(
                    "X-Processing-Context",
                    context.toJson()
                );
            }
        };
    }
}
```

---

## Error Handling

### Context-Related Errors

```java
/**
 * Exception thrown when processing context is missing
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingContextException extends RuntimeException {
    public MissingContextException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when processing context is invalid/expired
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidContextException extends RuntimeException {
    public InvalidContextException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when party is inactive
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PartyInactiveException extends RuntimeException {
    public PartyInactiveException(String message) {
        super(message);
    }
}

/**
 * Exception thrown when operation not permitted
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}

/**
 * Global exception handler for context errors
 */
@ControllerAdvice
public class ContextExceptionHandler {

    @ExceptionHandler(MissingContextException.class)
    public ResponseEntity<ErrorResponse> handleMissingContext(
            MissingContextException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse.builder()
                .error("MISSING_CONTEXT")
                .message(ex.getMessage())
                .timestamp(Instant.now())
                .build());
    }

    @ExceptionHandler(InvalidContextException.class)
    public ResponseEntity<ErrorResponse> handleInvalidContext(
            InvalidContextException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse.builder()
                .error("INVALID_CONTEXT")
                .message(ex.getMessage())
                .hint("Please re-authenticate")
                .timestamp(Instant.now())
                .build());
    }
}
```

---

## Performance Considerations

### Context Caching Strategy

```java
/**
 * Context Cache Configuration
 * Cache resolved contexts to avoid repeated Party Service calls
 */
@Configuration
@EnableCaching
public class ContextCacheConfiguration {

    @Bean
    public CacheManager contextCacheManager() {
        return CacheManagerBuilder.newCacheManagerBuilder()
            .withCache("processing-contexts",
                CacheConfigurationBuilder.newCacheConfigurationBuilder(
                    String.class,  // principalId
                    ProcessingContext.class,
                    ResourcePoolsBuilder.heap(10000)
                )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(
                    Duration.ofMinutes(5)
                ))
            )
            .build(true);
    }
}

/**
 * Context Resolution with caching
 */
@Service
public class ContextResolutionService {

    @Cacheable(value = "processing-contexts", key = "#principalId")
    public ProcessingContext resolveContext(
            String principalId,
            String partyId) {
        // Expensive resolution only happens on cache miss
        return doResolveContext(principalId, partyId);
    }
}
```

### Performance Metrics

Target performance for context resolution chain:

- **Authentication**: < 50ms (JWT validation)
- **Context Resolution**: < 100ms (with cache hit: < 10ms)
- **Context Injection**: < 5ms
- **Total Overhead**: < 155ms (< 15ms with cache)

---

## Security Implications

### Security Benefits

1. **Single Point of Context Validation**
   - Context resolved once by Party Service
   - All downstream services trust gateway-provided context
   - Reduces attack surface

2. **Tenant Isolation Enforcement**
   - TenantId in every query
   - Repository-level enforcement
   - No cross-tenant data leaks

3. **Permission Enforcement**
   - Permissions calculated early in chain
   - Consistent across all services
   - Based on party + role combination

4. **Audit Trail**
   - Full context in every log
   - Complete traceability
   - Compliance-ready

### Security Considerations

1. **Context Tampering**
   - Solution: Sign context JSON with HMAC
   - Verify signature in each service

2. **Context Replay**
   - Solution: Include timestamp, enforce 5-minute expiry
   - Include request ID for idempotency

3. **Privilege Escalation**
   - Solution: Party Service validates principal → party mapping
   - Cannot request context for unauthorized party

---

## Migration Guide

### Migrating Existing Services

#### Before (Old Pattern)
```java
@GetMapping("/products")
public List<Product> getProducts(@RequestHeader("X-Tenant-ID") String tenantId) {
    return productRepository.findByTenantId(tenantId);
}
```

#### After (New Pattern)
```java
@GetMapping("/products")
public List<Product> getProducts(
        @RequestHeader("X-Processing-Context") String contextJson) {

    ProcessingContext context = ProcessingContext.fromJson(contextJson);

    // Use full context, not just tenant ID
    List<Product> products = productRepository.findByTenantId(context.getTenantId());
    products = applyPartyRules(products, context);
    products = applyJurisdictionRules(products, context);

    return products;
}
```

---

## Summary: Key Principles

### ✅ DO

1. **Always extract ProcessingContext from headers**
2. **Always validate context before processing**
3. **Always filter data by tenantId**
4. **Always check permissions from context**
5. **Always log with full context**
6. **Always propagate context to downstream services**
7. **Always use TenantAwareRepository**

### ❌ DON'T

1. **Never resolve tenant independently**
2. **Never query across tenants**
3. **Never bypass context validation**
4. **Never use deprecated findAll() / findById()**
5. **Never make cross-tenant relationships**
6. **Never cache data without tenant key**
7. **Never log sensitive data from context**

---

## Conclusion

The Context Resolution Architecture provides a robust, scalable foundation for multi-tenant, party-aware, jurisdiction-compliant processing. By resolving context once at the gateway and propagating it to all services, we achieve:

- **Consistency**: Same context everywhere
- **Performance**: Resolve once, use many times
- **Security**: Strong tenant isolation
- **Maintainability**: Simple service logic
- **Compliance**: Full audit trail

**ALL NEW SERVICES MUST FOLLOW THIS ARCHITECTURE.**

---

**Document Version**: 1.0
**Last Updated**: October 15, 2025
**Next Review**: Q1 2026
