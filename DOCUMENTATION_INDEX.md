# Documentation Index

Quick reference to all documentation files in this repository.

---

## 🎯 Start Here

| Document | Purpose | Audience |
|----------|---------|----------|
| **[README.md](README.md)** | Project overview, quick start | Everyone |
| **[CLAUDE.md](CLAUDE.md)** | **⚠️ MANDATORY STANDARDS** (top section), development guide | Developers, AI |

---

## 📘 Standards & Guidelines

| Document | Description | When to Use |
|----------|-------------|-------------|
| **[CLAUDE.md](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services)** | **MANDATORY standards (top section)** | Before creating ANY new service |
| **[STANDARDS_SUMMARY.md](STANDARDS_SUMMARY.md)** | Quick reference table of all standards | Quick lookup during development |
| **[NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md)** | Step-by-step checklist for new services | When creating a new microservice |
| **[PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md)** | Implementation details, metrics, examples | When implementing performance features |
| **[SECURITY.md](SECURITY.md)** | Security guidelines, credentials | When configuring security or credentials |
| **[TENANT_ISOLATION.md](TENANT_ISOLATION.md)** | **Complete tenant isolation guide** | When implementing multi-tenancy |
| **[TENANT_ISOLATION_SUMMARY.md](TENANT_ISOLATION_SUMMARY.md)** | Tenant isolation architecture & test results | Quick reference for tenant isolation |
| **[TENANT_ISOLATION_QUICK_REF.md](TENANT_ISOLATION_QUICK_REF.md)** | One-page quick reference card | During development (keep open) |

---

## 🔧 Implementation Reference

| Location | Contents |
|----------|----------|
| **[backend/product-service/](backend/product-service/)** | Reference implementation of all standards |
| **[backend/product-service/src/main/java/com/bank/product/config/](backend/product-service/src/main/java/com/bank/product/config/)** | Configuration classes (RestTemplate, Async, Cache, Security, WebMvc) |
| **[backend/product-service/src/main/java/com/bank/product/security/](backend/product-service/src/main/java/com/bank/product/security/)** | Tenant isolation (TenantContext, TenantInterceptor) |
| **[backend/product-service/src/main/java/com/bank/product/repository/](backend/product-service/src/main/java/com/bank/product/repository/)** | TenantAwareRepository base interface |
| **[backend/product-service/src/main/java/com/bank/product/client/](backend/product-service/src/main/java/com/bank/product/client/)** | Circuit breaker implementation |
| **[backend/product-service/src/main/java/com/bank/product/domain/solution/controller/](backend/product-service/src/main/java/com/bank/product/domain/solution/controller/)** | Async + idempotency patterns |

---

## 🧪 Testing

| Test Script | Purpose |
|-------------|---------|
| **[test-optimizations.sh](test-optimizations.sh)** | Async, idempotency, connection pooling tests |
| **[test-circuit-breaker.sh](test-circuit-breaker.sh)** | Circuit breaker failure scenarios |
| **[test-idempotency.sh](test-idempotency.sh)** | Duplicate request handling |
| **[test-tenant-isolation.sh](test-tenant-isolation.sh)** | Multi-tenant isolation verification |

---

## 📋 Workflow Documents

| Document | Purpose |
|----------|---------|
| **[AGENTIC_WORKFLOW_DESIGN.md](AGENTIC_WORKFLOW_DESIGN.md)** | AI + Rules hybrid workflow architecture |
| **[TEMPLATE_MANAGEMENT_API.md](TEMPLATE_MANAGEMENT_API.md)** | Workflow template management API |
| **[WORKFLOW_APPROVAL_FLOW.md](WORKFLOW_APPROVAL_FLOW.md)** | Approval workflow details |

---

## 🚀 Deployment

| Document | Purpose |
|----------|---------|
| **[DEPLOYMENT.md](DEPLOYMENT.md)** | Docker deployment instructions |
| **[docker-compose.yml](docker-compose.yml)** | Container orchestration |
| **[QUICK_START.md](QUICK_START.md)** | Quick start guide |
| **[END_TO_END_TEST.md](END_TO_END_TEST.md)** | End-to-end testing guide |

---

## 📊 Architecture & Design

| Document | Purpose |
|----------|---------|
| **[CLAUDE.md - Architecture](CLAUDE.md#architecture)** | System architecture overview |
| **[CLAUDE.md - Workflow Foundation](CLAUDE.md#workflow-foundation-extensible-makerchecke)** | Temporal workflow system |
| **[CLAUDE.md - Integration](CLAUDE.md#product-service--workflow-service-integration)** | Service integration details |

---

## 🔍 Quick Lookup

### "I need to..."

| Task | Document |
|------|----------|
| Create a new service | [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md) |
| Understand mandatory standards | [CLAUDE.md (top section)](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services) |
| Quick reference standards | [STANDARDS_SUMMARY.md](STANDARDS_SUMMARY.md) |
| See implementation examples | [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) |
| Configure security/credentials | [SECURITY.md](SECURITY.md) |
| Implement tenant isolation | [TENANT_ISOLATION_QUICK_REF.md](TENANT_ISOLATION_QUICK_REF.md) → [TENANT_ISOLATION.md](TENANT_ISOLATION.md) |
| Deploy with Docker | [DEPLOYMENT.md](DEPLOYMENT.md) |
| Run tests | Test scripts (test-*.sh) |
| Review code | [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md) |

---

## 📁 File Organization

```
product-catalog-system/
├── README.md                          ← Start here
├── CLAUDE.md                          ← ⚠️ MANDATORY STANDARDS (top section)
├── DOCUMENTATION_INDEX.md             ← This file
│
├── Standards & Guidelines
│   ├── STANDARDS_SUMMARY.md           ← Quick reference
│   ├── NEW_SERVICE_CHECKLIST.md       ← Step-by-step checklist
│   ├── PERFORMANCE_OPTIMIZATIONS.md   ← Implementation details
│   ├── SECURITY.md                    ← Security guidelines
│   ├── TENANT_ISOLATION.md            ← Complete tenant isolation guide
│   ├── TENANT_ISOLATION_SUMMARY.md    ← Architecture & test results
│   └── TENANT_ISOLATION_QUICK_REF.md  ← One-page quick reference
│
├── Test Scripts
│   ├── test-optimizations.sh
│   ├── test-circuit-breaker.sh
│   ├── test-idempotency.sh
│   └── test-tenant-isolation.sh
│
├── Deployment
│   ├── DEPLOYMENT.md
│   ├── QUICK_START.md
│   ├── END_TO_END_TEST.md
│   └── docker-compose.yml
│
├── Workflow
│   ├── AGENTIC_WORKFLOW_DESIGN.md
│   ├── TEMPLATE_MANAGEMENT_API.md
│   └── WORKFLOW_APPROVAL_FLOW.md
│
└── backend/
    └── product-service/               ← Reference implementation
        ├── src/main/java/com/bank/product/
        │   ├── config/                ← RestTemplate, Async, Cache, Security, WebMvc
        │   ├── security/              ← TenantContext, TenantInterceptor
        │   ├── repository/            ← TenantAwareRepository
        │   ├── client/                ← Circuit breaker
        │   └── domain/solution/
        │       ├── controller/        ← Async + idempotency
        │       └── service/           ← Business logic
        └── src/main/resources/
            ├── application.yml
            └── application-resilience4j.yml
```

---

## 🎓 Learning Path

### For New Developers
1. Read [README.md](README.md) - Project overview
2. Review [CLAUDE.md (top section)](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services) - Mandatory standards
3. Study [backend/product-service/](backend/product-service/) - Reference implementation
4. Run test scripts to see standards in action

### For Creating a New Service
1. Read [MANDATORY STANDARDS](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services)
2. Use [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md) - Complete all items
3. Reference [PERFORMANCE_OPTIMIZATIONS.md](PERFORMANCE_OPTIMIZATIONS.md) - Copy patterns
4. Run test scripts and verify results

### For Code Review
1. Check [MANDATORY STANDARDS](CLAUDE.md#⚠️-important-mandatory-standards-for-all-new-services) compliance
2. Verify [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md) complete
3. Run test scripts
4. Verify documentation updated

---

## 📅 Version

- **Last Updated**: October 2, 2025
- **Version**: 1.0
- **Status**: Production Ready

---

**Need help?** Start with [CLAUDE.md](CLAUDE.md) and follow the references.
