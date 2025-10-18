# Organizational Design for Platform Execution (v2.0 - CORRECTED)
## Enterprise Product Catalog & Party Management Platform

**Version:** 2.0 **CORRECTED** - Aligned with Progressive Value Delivery
**Date:** October 2026
**Status:** Strategic Planning - Organizational Blueprint
**Prepared By:** Daniel Maly

**⚠️ CRITICAL CORRECTION in v2.0:**
- **Graph Data Engineer MUST be in Phase 1** (not Phase 4)
- Neo4j party foundation is built in Month 2, not deferred
- Context resolution is foundational architecture, not a late-stage feature
- Team sizing adjusted to reflect actual capability needs per phase

**Related Documents:**
- [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) - Technical roadmap
- [BUSINESS_VALUE_ANALYSIS.md](BUSINESS_VALUE_ANALYSIS.md) - Financial model

---

## Executive Summary - Key Changes in v2.0

### What Changed

**❌ v1.0 MISTAKE:**
- Graph Data Engineer hired in Phase 4 (Months 12-15)
- Neo4j treated as "advanced feature"
- Party management deferred until federation phase

**✅ v2.0 CORRECTION:**
- **Graph Data Engineer in Phase 1 (Month 1-2 hire)**
- **Neo4j setup in Month 1** (critical path infrastructure)
- **Party foundation in Month 2** (context resolution foundational)
- Aligns with roadmap: "Party Foundation (Month 2)" section

### Why This Matters

**Business Impact:**
- Phase 1 MVP depends on party-aware context resolution
- Without party graph, no "manages on behalf of" relationships
- Without context resolution, no fine-grained entitlements in Phase 2
- Deferring party to Phase 4 would require massive refactoring (10x cost)

**Technical Debt Avoided:**
- Building without party awareness = hardcoded user IDs everywhere
- Retrofitting context resolution = rewrite every service
- Adding Neo4j later = dual data model migration nightmare

**Progressive Value Delivery:**
- Phase 1: Party-aware audit logs (who did what)
- Phase 2: Fine-grained entitlements (resource-scoped permissions)
- Phase 3: AI workflows know WHO is approving (not just roles)
- Phase 4: Entity resolution (duplicate detection, easier with existing graph)

---

## Team Structure by Phase (CORRECTED)

### Phase 1: Foundation Squad with Party Graph (Months 1-4)
**Team Size:** 13-15 FTEs
**Budget:** $700K - $850K

#### Why Phase 1 is Larger Than Expected

**Original v1.0:** 12-14 FTEs (no Graph Engineer, smaller scope)
**Corrected v2.0:** 13-15 FTEs (includes Graph Engineer, party foundation)

**Rationale:**
- Party foundation is NOT optional - it's **critical path**
- Context resolution is **foundational architecture**
- Neo4j requires specialist from Day 1 (not backend engineer's side project)
- Better to build foundation correctly than refactor later (10x more expensive)

#### Objectives
- ✅ Single-tenant product catalog with master templates
- ✅ Temporal-based approval workflow (DMN rule engine)
- ✅ **Party management foundation** (Organization, LegalEntity, Individual) ⭐ CRITICAL
- ✅ **Context resolution** (authentication → WHO/WHAT/WHERE) ⭐ FOUNDATIONAL
- ✅ MongoDB + **Neo4j** + Kafka integration
- ✅ "Manages on behalf of" relationship support
- ✅ Party-aware audit logs (all logs include partyId + tenantId)

#### Squad Composition

**Product & Leadership (2 FTEs)**
- **1x Product Owner** (1 FTE full-time in Phase 1)
  - Owns roadmap, prioritization, stakeholder communication
  - Bridge between business and engineering
  - **Phase 1 focus:** Define party model requirements, context resolution UX
  - Success criteria: Business value delivered, stakeholder satisfaction

- **1x Engineering Manager / Tech Lead**
  - Owns architecture, code quality, team velocity
  - **Phase 1 focus:** Architecture decisions (party graph model, context resolution pattern)
  - Removes blockers, mentors engineers
  - Success criteria: On-time delivery, technical debt < 10%, team happiness

**Engineering (9-10 FTEs)**

**Backend Team (6-7 FTEs):**
- **2x Senior Backend Engineers** (Java/Spring Boot)
  - **Engineer #1:** product-service, workflow-service, audit-service
  - **Engineer #2:** party-service (context resolution), api-gateway (context injection)
  - REST API design, MongoDB data modeling
  - Mentors mid-level engineers
  - Pair with Graph Engineer on party-service integration

- **2x Mid-Level Backend Engineers** (Java/Spring Boot)
  - Feature implementation (notification-service, auth-service helpers)
  - Unit tests, integration tests, code reviews
  - Works under guidance of senior engineers
  - Learns Neo4j integration patterns from Graph Engineer

- **1x Backend Engineer (Auth/Security focus)** ⭐ NEW IN v2.0
  - **auth-service implementation** (JWT with principalId)
  - User-to-party mapping (MongoDB + Neo4j lookups)
  - MongoDB user store with BCrypt encryption
  - Initial user setup (admin, product-manager, risk-manager)
  - Works closely with Graph Engineer on party lookups

- **1x Full-Stack Engineer** (Java + React/Angular/TypeScript)
  - Admin UI for product type management
  - Catalog browsing and solution configuration UI
  - **Party context display** (show logged-in party, delegation scenarios)
  - Integration with backend APIs

**Graph/Data Team (1 FTE) ⭐ CRITICAL:**
- **1x Graph Data Engineer** (Neo4j specialist)
  - **Month 1:**
    - Neo4j setup (3-node Causal Cluster for prod, single node for dev)
    - Initial schema design (Organization, LegalEntity, Individual, Address)
    - Relationship types (PARENT_OF, EMPLOYED_BY, AUTHORIZED_SIGNER, MANAGES_ON_BEHALF_OF)
    - Indexes and constraints
  - **Month 2:**
    - **party-service core implementation** (party CRUD, relationship management)
    - **Context resolution endpoint:** /api/v1/context/resolve
    - Principal → Party → Tenant resolution (hardcoded tenant-001 initially)
    - Permission enrichment (RBAC, later ABAC in Phase 2)
    - Caching strategy (5-minute cache, Caffeine)
    - Performance tuning (<100ms cached, <2s cold)
  - **Month 3-4:**
    - Initial data loading (100-1,000 parties for tenant-001)
    - "Manages on behalf of" relationship support
    - Delegation scenarios (Alice manages on behalf of Bob)
    - Performance testing and optimization
    - Runbook creation for Neo4j operations
  - **Why critical:** Without this engineer, party foundation cannot be built in Phase 1, delaying entire roadmap

**DevOps/Platform (1 FTE):**
- **1x DevOps/Platform Engineer**
  - Docker, Kubernetes, CI/CD pipeline (GitHub Actions)
  - Monitoring (Prometheus, Grafana), logging (ELK/Loki)
  - Infrastructure as Code (Terraform)
  - **Neo4j operational support:** Backups, monitoring, cluster health
  - Works with Graph Engineer on Neo4j production readiness

**Quality & Operations (2-3 FTEs)**
- **1x QA Engineer**
  - Test automation (JUnit, Selenium, Postman, Cypress)
  - End-to-end workflow testing (product → workflow → approval)
  - **Context resolution testing** (verify party context in all flows)
  - Performance testing (JMeter, Gatling)

- **1x Business Analyst** (0.5-1 FTE depending on requirements complexity)
  - Requirements gathering, user stories, acceptance criteria
  - **Party model requirements** (what relationships needed, what attributes)
  - Workflow rule definition (approval matrices)
  - UAT coordination

**Optional (Consultants/Part-Time):**
- **Security Consultant** (10-20 hours/month)
  - OAuth2/JWT design review
  - Neo4j security review (authentication, authorization)
  - Penetration testing
  - Compliance review (SOC 2, GDPR prep)

#### Key Activities Timeline

**Month 1: Infrastructure Setup**
- Week 1-2: MongoDB, Kafka, Redis, Temporal setup
- Week 3-4: **Neo4j setup** (Graph Engineer leads) ⭐
  - 3-node Causal Cluster (production)
  - Single node (development)
  - Initial schema design
  - Indexes and constraints

**Month 2: Party Foundation (CRITICAL MONTH)**
- Week 1-2: **party-service implementation** (Graph Engineer + 1 Senior Backend Engineer)
  - Party CRUD API
  - Basic relationship modeling
  - Neo4j integration
- Week 3-4: **Context resolution** (Graph Engineer + Auth Engineer)
  - /api/v1/context/resolve endpoint
  - Principal → Party → Tenant resolution
  - Permission enrichment
  - Caching (5-minute TTL)
  - Performance testing (<100ms cached, <2s cold)

**Month 3: Business Services**
- product-service (Senior Backend Engineer #1 + Mid-Level Engineers)
- workflow-service (Senior Backend Engineer #1)
- **Context integration:** All services use ContextHolder.getRequiredContext()
- Audit logs include partyId + tenantId

**Month 4: Integration & Testing**
- Admin UI (Full-Stack Engineer)
- End-to-end testing (QA Engineer)
- **Party-aware audit logs verification**
- Performance testing
- Production deployment preparation

#### Delivery Milestone (Month 4)
**MVP:** Single-tenant catalog + automated workflow + **party-aware context resolution**

**Business Value:**
- 5.2 FTE reduction
- $2.428M/year savings
- 35-day product launch (down from 50-day baseline, 30% improvement)

**Technical Achievements:**
- ✅ Neo4j party graph operational (100-1,000 parties)
- ✅ Context resolution <100ms (cached), <2s (cold)
- ✅ "Manages on behalf of" relationships working
- ✅ All audit logs include partyId + tenantId (foundation for compliance)
- ✅ Party-aware UI (shows logged-in party)

---

### Phase 2: Multi-Tenancy + Fine-Grained Entitlements (Months 5-7)
**Team Size:** 14-16 FTEs (+1-2 FTEs from Phase 1)
**Budget:** $750K - $950K

#### Objectives
- ✅ Multi-tenant data isolation (MongoDB tenant filtering + Neo4j namespace)
- ✅ **Fine-grained ABAC entitlements** (resource-scoped permissions, constraints) ⭐ CRITICAL
- ✅ Tenant onboarding automation
- ✅ API Gateway with rate limiting per tenant
- ✅ Zero cross-tenant data leaks (security validation)

#### Team Adjustments

**New Roles Added:**

**Security (1 FTE):**
- **1x Security Engineer**
  - Implements ABAC entitlement system (ResourceType, ResourceOperation, EntitlementConstraints)
  - Works with Graph Engineer on relationship-based entitlements (AUTHORIZED_SIGNER → permissions)
  - Tenant isolation validation (automated tests)
  - Threat modeling, security reviews
  - Secrets management (Vault, AWS Secrets Manager)

**Quality Expansion (1 FTE):**
- **+1x QA Engineer** (Total: 2 QA Engineers)
  - **Tenant isolation testing** (critical for multi-tenancy)
  - **Permission boundary testing** (ABAC validation)
  - **Cross-tenant leak testing** (attempt to access other tenant's data)
  - **Load testing per tenant** (noisy neighbor scenarios)
  - **Security regression testing**

**Graph Engineer Focus Shift:**
- Phase 1: Build party foundation
- Phase 2: **Multi-tenant party graph** (namespace isolation)
  - Tenant-scoped queries (all Cypher queries include tenantId filter)
  - Performance optimization for multi-tenant queries
  - **Relationship-based entitlements:** AUTHORIZED_SIGNER → EntitlementSource.RELATIONSHIP_BASED
  - Entity resolution preparation (address normalization, phonetic matching POCs)

#### Delivery Milestone (Month 7)
**MVP:** Multi-tenant SaaS with fine-grained entitlements

**Business Value:**
- 35 FTE reduction (cumulative)
- $18.066M/year savings
- 25-day product launch (50% improvement from baseline)
- **Zero cross-tenant data leaks** (security validation passed)

**Technical Achievements:**
- ✅ Multi-tenant Neo4j (namespace isolation)
- ✅ ABAC entitlements operational (resource-scoped permissions)
- ✅ Relationship-based entitlements (AuthorizedSigner → TRANSACT permission)
- ✅ Tenant isolation tested (100% pass rate)

---

### Phase 3: AI Automation Squad (Months 8-11) ⭐ Peak Team
**Team Size:** 18-22 FTEs (+4-6 FTEs from Phase 2)
**Budget:** $1.0M - $1.35M

#### Objectives
- ✅ Claude AI integration for document validation (W-9, incorporation certificates)
- ✅ Agentic workflows (fraud detection, credit risk, GraphRAG compliance)
- ✅ 50% faster approvals (intelligent auto-approval)
- ✅ MCP (Model Context Protocol) integration
- ✅ AI observability and explainability

#### New Roles Added

**AI/ML Team (3-4 FTEs):**
- **1x AI/ML Engineer** (Claude API, prompt engineering)
  - Claude integration for document extraction
  - MCP server development
  - AI agent orchestration (fraud, credit, compliance)
  - Works with Graph Engineer on GraphRAG (Neo4j as knowledge base)

- **1x Data Scientist**
  - Training data curation for ML models
  - A/B testing for auto-approval thresholds
  - Model performance monitoring
  - Entity resolution ML models (duplicate detection)

- **1x ML Ops Engineer** (0.5-1 FTE)
  - AI model deployment pipelines
  - Prompt version control
  - AI cost monitoring (Claude API usage)
  - Explainability tooling (trace logs, audit)

**Engineering Expansion (+1-2 FTEs):**
- **+1x Senior Backend Engineer** (async workflow specialist)
  - Temporal agentic workflow implementation
  - Background task orchestration
  - Event-driven architecture (Kafka)

- **+1x Integration Engineer** (optional)
  - Third-party integrations (credit bureaus, fraud APIs)
  - Webhook management
  - API gateway enhancements

**Graph Engineer Focus Shift:**
- Phase 2: Multi-tenant party graph
- Phase 3: **GraphRAG knowledge base**
  - Neo4j as Claude knowledge store (compliance rules, historical patterns)
  - Graph analytics for fraud detection (community detection, anomaly detection)
  - Prepare for entity resolution (Phase 4 preview)

#### Delivery Milestone (Month 11)
**MVP:** AI-powered auto-approval + document validation

**Business Value:**
- 96 FTE reduction (cumulative)
- $45.759M/year savings
- **10-day product launch** (80% improvement from baseline) ⭐ BREAKTHROUGH
- 80% auto-approval rate (no human review for low-risk products)

**Technical Achievements:**
- ✅ Claude AI document extraction (≥95% accuracy)
- ✅ Agentic fraud detection (<1% false positive rate)
- ✅ GraphRAG compliance knowledge base
- ✅ Auto-approval workflow operational

---

### Phase 4: Federated Party & Entity Resolution (Months 12-15)
**Team Size:** 16-18 FTEs (±0 from Phase 3, some AI engineers transition)
**Budget:** $950K - $1.15M

#### Objectives
- ✅ Federated party management across business lines (Commercial Banking, Capital Markets)
- ✅ Entity resolution (duplicate detection, phonetic matching, address normalization)
- ✅ UBO (Ultimate Beneficial Owner) tracking (FinCEN 25%+ compliance)
- ✅ Predictive graph construction from documents
- ✅ Cross-domain party relationships

#### Team Adjustments

**Graph Engineer Focus - Peak Complexity:**
- Phase 3: GraphRAG knowledge base
- Phase 4: **Entity resolution + federation** (most complex phase for graph work)
  - Phonetic matching algorithms (Metaphone3, Jaro-Winkler)
  - Address normalization (USPS standards)
  - Multi-strategy entity matching (LEI, registration, phonetic, address)
  - UBO calculation (25%+ ownership threshold)
  - Predictive relationship extraction from documents
  - Cross-domain party federation
  - Graph analytics at scale (1M+ parties, 10M+ relationships)

**No Additional Graph Engineers Needed:**
- Graph Engineer from Phase 1 has deep context (built foundation)
- Entity resolution algorithms implemented by existing engineer
- Data Scientist from Phase 3 helps with ML-based matching models
- Mid-level backend engineers assist with service integration

**AI Team Transition (-1 to -2 FTEs):**
- AI/ML Engineer can transition to other projects after Phase 3 stabilizes
- Retain ML Ops Engineer for ongoing AI operations
- Data Scientist remains (entity resolution ML models)

#### Delivery Milestone (Month 15)
**MVP:** Federated party + entity resolution + UBO compliance

**Business Value:**
- 146 FTE reduction (cumulative)
- $65.423M/year savings
- 7-day product launch (86% improvement from baseline)
- 75% duplicate party reduction (entity resolution working)

**Technical Achievements:**
- ✅ Entity resolution operational (precision ≥95%, recall ≥90%)
- ✅ UBO calculation (25%+ threshold, FinCEN compliant)
- ✅ Predictive graph construction (4 relationship types auto-detected)
- ✅ Cross-domain party federation

---

### Phase 5: Multi-Channel Distribution (Months 16-19)
**Team Size:** 14-16 FTEs (-2 FTEs from Phase 4)
**Budget:** $850K - $1.05M

#### Objectives
- ✅ 6-channel deployment (Web, Mobile, Branch, ATM, Call Center, Partner APIs)
- ✅ Core banking integration (deposits, loans, payments)
- ✅ Real-time product distribution
- ✅ Channel-specific UI/UX
- ✅ Multi-channel revenue enablement ($72M/year)

#### Team Adjustments

**Integration & Channels (+1-2 FTEs):**
- **+1x Integration Architect** (core banking specialist)
  - FIS, Fiserv, Temenos integration
  - ISO20022 message formats
  - Real-time account opening

- **+1x Mobile Engineer** (iOS/Android or React Native)
  - Mobile app for product catalog browsing
  - Mobile onboarding flows
  - Push notifications

**Graph Engineer Focus:**
- Phase 4: Entity resolution + federation
- Phase 5: **Operational optimization**
  - Neo4j performance tuning for production scale
  - Query optimization (millions of parties)
  - Knowledge transfer to operations team
  - Runbook finalization

#### Delivery Milestone (Month 19)
**MVP:** 6-channel distribution + core banking integration

**Business Value:**
- 196 FTE reduction (cumulative)
- $119.91M/year savings
- **2-day product launch** (96% improvement from baseline) ⭐ REVENUE ACCELERATION
- $72M multi-channel revenue (6x distribution reach)

**Technical Achievements:**
- ✅ 6 channels operational
- ✅ Core banking integration (real-time account opening)
- ✅ Neo4j at production scale (1M+ parties, 10M+ relationships)

---

### Sustaining Operations Team (Month 20+)
**Team Size:** 8-12 FTEs
**Annual Budget:** $1.8M - $2.5M

#### Core Team

**Engineering (5-7 FTEs):**
- **1x Tech Lead / Engineering Manager**
- **2-3x Senior/Mid-Level Backend Engineers**
- **1x Graph Data Engineer** (part-time, 50% FTE) ⭐ CRITICAL
  - Neo4j operational support (backups, monitoring, performance tuning)
  - Entity resolution model retraining
  - Query optimization for production scale
  - Incident response for graph-related issues
- **1x DevOps Engineer** (SRE focus)

**Quality & Product (2-3 FTEs):**
- **1x Product Owner**
- **1x QA Engineer**
- **1x Support Engineer** (L2/L3)

**Optional (On-Demand):**
- **Security Engineer** (quarterly audits)
- **AI/ML Engineer** (model retraining, prompt tuning)
- **Business Analyst** (new feature requirements)

**Why Graph Engineer in Operations:**
- Neo4j requires specialist knowledge (not general backend skill)
- Entity resolution models need periodic retraining
- Performance tuning at scale (millions of parties) requires expertise
- Graph queries can be complex (100+ line Cypher)
- Better to retain 50% FTE specialist than rely on general engineers

---

## Team Composition Matrix (CORRECTED)

| Phase | Backend | Frontend | **Graph/Data** | AI/ML | DevOps | QA | Security | Product/BA | Total FTEs |
|-------|---------|----------|----------------|-------|--------|-----|----------|-----------|------------|
| **Phase 1** | 6-7 | 1 | **1** ⭐ | 0 | 1 | 1 | 0.2 | 1.5 | **13-15** |
| **Phase 2** | 6-7 | 1 | **1** | 0 | 1 | 2 | 1 | 1.5 | **14-16** |
| **Phase 3** | 7-8 | 1 | **1** | 3-4 | 1 | 2 | 1 | 1.5 | **18-22** ⭐ |
| **Phase 4** | 7-8 | 1 | **1** | 1-2 | 1 | 2 | 1 | 1.5 | **16-18** |
| **Phase 5** | 7-8 | 2 | **1** | 1-2 | 1 | 2 | 1 | 1.5 | **14-16** |
| **Operations** | 4-5 | 1 | **0.5** | 0.5 | 1 | 1 | 0.5 | 1 | **8-12** |

**Key Differences from v1.0:**
- ✅ Graph/Data Engineer in **every phase** (not just Phase 4)
- ✅ Phase 1 slightly larger (13-15 vs 12-14 FTEs) to accommodate party foundation
- ✅ Backend team includes Auth/Security-focused engineer in Phase 1
- ✅ Operations team retains 50% FTE Graph Engineer (not 0)

---

## Critical Success Factors (UPDATED)

### 1. Hire Graph Data Engineer in Month 1-2 (Not Month 12!)

**v1.0 Mistake:** Defer graph engineer to Phase 4
**v2.0 Correction:** Hire in Phase 1, Month 1-2

**Why Critical:**
- Neo4j setup in Month 1 (infrastructure)
- Party foundation in Month 2 (core service)
- Context resolution depends on party graph
- Deferring to Phase 4 = 10x refactoring cost

**Hiring Timeline:**
- Month -2: Post job, source candidates
- Month -1 to 0: Interview, offer, accept
- Month 1: Onboard, Neo4j setup
- Month 2: party-service implementation
- Month 3-4: Performance tuning, initial data load

### 2. Product Owner Must Understand Party Model

**Why:** Party model defines "who can do what" (entitlements, relationships)

**Skills Needed:**
- Banking domain knowledge (organizational hierarchies)
- Relationship modeling (AUTHORIZED_SIGNER, MANAGES_ON_BEHALF_OF)
- UBO/FinCEN compliance requirements
- Delegation scenarios ("Alice manages on behalf of Bob")

**Training:** If Product Owner lacks Neo4j knowledge, Graph Engineer provides training

### 3. Build Party Foundation Correctly Once (Not Twice)

**Anti-Pattern:**
- Phase 1: Build with hardcoded user IDs
- Phase 4: Realize party graph needed, refactor everything
- Result: 6-8 months lost, 10x cost

**Right Pattern:**
- Phase 1: Build with party-aware context resolution from Day 1
- Phase 2: Extend with fine-grained entitlements (leverage party relationships)
- Phase 3: AI workflows know WHO is approving (party context available)
- Phase 4: Entity resolution easier (party graph already exists)

### 4. Graph Engineer is NOT a Backend Engineer's Side Project

**Why Dedicated Specialist:**
- Cypher is different from SQL (graph thinking vs relational thinking)
- Neo4j operational knowledge (clustering, backups, performance tuning)
- Graph algorithms (entity resolution, UBO calculation, community detection)
- Production support (complex queries, performance issues)

**Cost of Not Having Specialist:**
- Backend engineers spend 2-3x time learning Neo4j (opportunity cost)
- Suboptimal graph design (performance issues at scale)
- Production incidents take longer to resolve (no deep expertise)

---

## Summary: Key Organizational Changes in v2.0

### What Was Wrong in v1.0

1. **❌ Graph Data Engineer in Phase 4** (Months 12-15)
   - Too late - party foundation needed in Phase 1
   - Would require massive refactoring if deferred

2. **❌ Treated party management as "advanced feature"**
   - Party is foundational architecture, not optional
   - Context resolution depends on party graph

3. **❌ No consideration for Graph Engineer in operations**
   - Operations team had 0 Graph Engineers
   - Neo4j requires specialist knowledge

### What's Correct in v2.0

1. **✅ Graph Data Engineer in Phase 1** (Month 1-2 hire)
   - Aligns with roadmap: "Neo4j Setup (Month 1)", "Party Foundation (Month 2)"
   - Builds foundation correctly the first time

2. **✅ Party-aware from Day 1**
   - All services use ContextHolder.getRequiredContext()
   - Audit logs include partyId + tenantId
   - "Manages on behalf of" relationships from Phase 1

3. **✅ Graph Engineer through all phases**
   - Phase 1: Build foundation
   - Phase 2: Multi-tenant party graph
   - Phase 3: GraphRAG knowledge base
   - Phase 4: Entity resolution
   - Phase 5: Production optimization
   - Operations: 50% FTE for ongoing support

4. **✅ Progressive value delivery maintained**
   - Phase 1: Party-aware audit logs
   - Phase 2: Fine-grained entitlements (uses party relationships)
   - Phase 3: AI workflows with party context
   - Phase 4: Entity resolution (leverages existing graph)
   - Phase 5: Multi-channel with full party awareness

---

**END OF CORRECTED ORGANIZATIONAL DESIGN (v2.0)**

**Related Documents:**
- [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) - Technical roadmap (aligns with this v2.0)
- [BUSINESS_VALUE_ANALYSIS.md](BUSINESS_VALUE_ANALYSIS.md) - $83.75M 3-year ROI model
- [ORGANIZATIONAL_DESIGN.md](ORGANIZATIONAL_DESIGN.md) - v1.0 (DEPRECATED, do not use)

**Version History:**
- v1.0 (October 2026) - Initial organizational design (INCORRECT - Graph Engineer in Phase 4)
- v2.0 (October 2026) - Corrected organizational design (Graph Engineer in Phase 1)
