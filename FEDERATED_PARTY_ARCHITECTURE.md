# Federated Party Model Architecture

## Executive Summary

A federated party management system that unifies multiple source party systems (Commercial Banking, Capital Markets, etc.) into a comprehensive graph-based party model using Neo4j. The system handles entity resolution, relationship synthesis, and provides a unified API for querying party hierarchies and relationships.

## Business Problem

### Current State Challenges
- **Fragmented Party Data**: Commercial and Capital Markets maintain separate party systems
- **Data Duplication**: Same legal entities exist in multiple systems with inconsistent data
- **Limited Relationship Modeling**: Each system only models relationships relevant to its domain
- **Operational Risk**: Incomplete view of client relationships creates compliance and risk management gaps
- **Inefficiency**: Manual reconciliation and duplicate onboarding processes

### Target State
- **Unified Party Graph**: Single source of truth for all party entities and relationships
- **Federated Architecture**: Source data from multiple systems without replacing them
- **Rich Relationship Model**: Model legal, operational, beneficial ownership, and custom relationships
- **Real-time Synchronization**: Keep federated graph synchronized with source systems
- **Conflict Resolution**: Intelligent merging of conflicting party data

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway Layer                            │
│  - GraphQL API (Relationship Traversal)                         │
│  - REST API (CRUD Operations)                                   │
│  - WebSocket (Real-time Updates)                                │
└───────────────────┬─────────────────────────────────────────────┘
                    │
┌───────────────────┴─────────────────────────────────────────────┐
│              Party Federation Service                            │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │  Entity      │ │ Relationship │ │  Conflict Resolution   │  │
│  │  Resolution  │ │  Synthesis   │ │  Engine                │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
│  ┌──────────────┐ ┌──────────────┐ ┌────────────────────────┐  │
│  │  Data        │ │  Lineage     │ │  Graph Builder         │  │
│  │  Enrichment  │ │  Tracker     │ │  Service               │  │
│  └──────────────┘ └──────────────┘ └────────────────────────┘  │
└───────────────────┬─────────────────────────────────────────────┘
                    │
┌───────────────────┴─────────────────────────────────────────────┐
│                    Neo4j Graph Database                          │
│  - Party Nodes (Organizations, Legal Entities, Subsidiaries)    │
│  - Relationship Edges (Legal, Operational, Beneficial)          │
│  - Provenance Metadata (Source System, Confidence Scores)       │
└───────────────────┬─────────────────────────────────────────────┘
                    │
┌───────────────────┴─────────────────────────────────────────────┐
│              Source System Integration Layer                     │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────────┐    │
│  │ Commercial  │  │   Capital   │  │    Other Source      │    │
│  │   Banking   │  │   Markets   │  │    Systems           │    │
│  │ Party API   │  │  Party API  │  │  (CRM, KYC, etc.)   │    │
│  └─────────────┘  └─────────────┘  └──────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## Neo4j Graph Model

### Node Types

#### 1. Party (Abstract/Label)
Base label for all party entities.

**Properties:**
- `federatedId`: UUID (globally unique identifier in federated system)
- `partyType`: Enum (ORGANIZATION, LEGAL_ENTITY, INDIVIDUAL, SUBSIDIARY)
- `status`: Enum (ACTIVE, INACTIVE, MERGED, DUPLICATE)
- `createdAt`: Timestamp
- `updatedAt`: Timestamp
- `confidence`: Float (0.0-1.0, confidence in entity resolution)

#### 2. Organization : Party
Top-level business entity.

**Properties:**
- `name`: String
- `legalName`: String
- `registrationNumber`: String
- `jurisdiction`: String
- `incorporationDate`: Date
- `industry`: String (NAICS code)
- `tier`: String (TIER_1, TIER_2, etc.)
- `riskRating`: String
- `amlStatus`: String

#### 3. LegalEntity : Party
Legal entity within an organizational structure.

**Properties:**
- `entityId`: String
- `entityType`: String (CORPORATION, LLC, PARTNERSHIP, TRUST)
- `taxId`: String (encrypted)
- `registeredAddress`: Map
- `mailingAddress`: Map
- `lei`: String (Legal Entity Identifier - ISO 17442)

#### 4. Individual : Party
Natural person (for beneficial ownership, authorized signers, etc.).

**Properties:**
- `firstName`: String
- `lastName`: String
- `dateOfBirth`: Date (encrypted)
- `nationality`: String
- `residency`: String
- `pepStatus`: Boolean (Politically Exposed Person)

#### 5. SourceRecord
Represents raw data from source systems.

**Properties:**
- `sourceSystem`: String (COMMERCIAL_BANKING, CAPITAL_MARKETS)
- `sourceId`: String (ID in source system)
- `sourceData`: JSON (full payload from source)
- `syncedAt`: Timestamp
- `version`: Integer
- `checksum`: String

### Relationship Types

#### Legal Relationships
- **PARENT_OF / SUBSIDIARY_OF**
  - Properties: `ownership_percentage`, `effective_date`, `source_system`
- **OWNS**
  - Properties: `percentage`, `direct`, `voting_rights`
- **CONTROLLED_BY**
  - Properties: `control_type` (VOTING, ECONOMIC, OPERATIONAL)

#### Operational Relationships
- **OPERATES_ON_BEHALF_OF**
  - Properties: `authority_level`, `scope`, `valid_from`, `valid_to`, `source_systems[]`
- **PROVIDES_SERVICES_TO**
  - Properties: `service_type`, `relationship_manager`, `since`
- **COUNTERPARTY_TO**
  - Properties: `exposure_amount`, `risk_category`, `product_types[]`

#### Beneficial Ownership
- **BENEFICIAL_OWNER_OF**
  - Properties: `ownership_percentage`, `control_level`, `ubo` (Ultimate Beneficial Owner flag)
- **AUTHORIZED_SIGNER**
  - Properties: `authority_limits`, `effective_date`

#### Governance
- **BOARD_MEMBER_OF**
  - Properties: `role`, `appointed_date`
- **OFFICER_OF**
  - Properties: `title`, `start_date`

#### Provenance & Lineage
- **SOURCED_FROM**
  - Connects Party → SourceRecord
  - Properties: `field_mappings`, `transform_rules`, `master_flag` (indicates master data source)
- **MERGED_FROM**
  - Connects Party → Party (when entities are merged)
  - Properties: `merge_date`, `merge_reason`, `confidence_score`
- **DUPLICATES**
  - Connects Party → Party (candidate duplicates)
  - Properties: `similarity_score`, `matching_fields[]`, `resolution_status`

## Entity Resolution Strategy

### Matching Rules (Prioritized)

1. **Exact Match**
   - Legal Entity Identifier (LEI)
   - Tax ID + Jurisdiction
   - Registration Number + Jurisdiction

2. **Fuzzy Match**
   - Legal Name (85% similarity)
   - Address (normalized)
   - Executive names + Entity name

3. **Probabilistic Match**
   - Machine learning model using:
     - Name variations
     - Address components
     - Industry codes
     - Network relationships (connected parties)

### Resolution Workflow

```
Source Data → Candidate Generation → Scoring → Threshold Decision
                                                      ↓
                                        ┌─────────────┴──────────────┐
                                        ↓                            ↓
                                   Auto-Merge                Manual Review
                                   (>0.95 score)             (0.75-0.95)
                                        ↓                            ↓
                                   Create MERGED_FROM         Create DUPLICATES
                                   relationship               relationship
```

## Conflict Resolution

### Data Quality Scoring

Each source system has a quality score per field:

```java
sourceSystemQuality:
  COMMERCIAL_BANKING:
    legalName: 0.95
    registeredAddress: 0.98
    industry: 0.70
  CAPITAL_MARKETS:
    legalName: 0.90
    riskRating: 0.95
    counterpartyExposure: 0.99
```

### Resolution Rules

1. **Master Data Source** (MDM pattern)
   - Designated authoritative source per field
   - Example: Legal name from Legal Entity Management system

2. **Most Recent** (with quality threshold)
   - If quality_score > 0.90, use most recently updated

3. **Highest Quality**
   - Select value from source with highest quality score

4. **Composite**
   - Merge complementary data (e.g., addresses from multiple sources)

5. **Manual Override**
   - Stewardship tools for data governance team

## API Design

### GraphQL Schema for Relationship Traversal

```graphql
type Organization implements Party {
  federatedId: ID!
  name: String!
  legalName: String!
  lei: String

  # Traversal
  subsidiaries(depth: Int = 1): [LegalEntity!]!
  parent: Organization
  ultimateParent: Organization

  # Relationships
  operatesOnBehalfOf: [Organization!]!
  beneficialOwners(threshold: Float = 0.25): [Individual!]!
  counterparties(productTypes: [String!]): [Organization!]!

  # Provenance
  sourcedFrom: [SourceRecord!]!
  masterSource: SourceRecord
  lastSyncedAt: DateTime!

  # Calculated
  totalSubsidiaries: Int!
  jurisdictions: [String!]!
  consolidatedRiskRating: String!
}

type LegalEntity implements Party {
  federatedId: ID!
  entityId: String!
  entityType: EntityType!
  lei: String

  parentOrganization: Organization
  subsidiaries: [LegalEntity!]!

  # Address
  registeredAddress: Address!
  operatingAddresses: [Address!]!
}

type Query {
  # Find party
  party(federatedId: ID!): Party
  partyByLEI(lei: String!): Party
  searchParties(criteria: SearchCriteria!): [Party!]!

  # Relationship queries
  organizationHierarchy(rootId: ID!, depth: Int): OrganizationTree!
  findRelationshipPath(
    fromId: ID!,
    toId: ID!,
    relationshipTypes: [RelationType!]
  ): [RelationshipPath!]!

  # Beneficial ownership
  ultimateBeneficialOwners(partyId: ID!): [BeneficialOwnership!]!
  ownershipChain(partyId: ID!): OwnershipGraph!

  # Cross-system relationships
  partiesInMultipleSystems(minSystems: Int = 2): [Party!]!

  # Data quality
  duplicateCandidates(threshold: Float = 0.75): [DuplicateSet!]!
  dataConflicts(partyId: ID!): [FieldConflict!]!
}

type Mutation {
  # Entity resolution
  mergeParties(sourceId: ID!, targetId: ID!, reason: String!): Party!
  splitParty(partyId: ID!, reason: String!): [Party!]!

  # Relationship management
  createRelationship(input: RelationshipInput!): Relationship!

  # Override
  setFieldMasterSource(
    partyId: ID!,
    field: String!,
    sourceSystem: String!
  ): Party!
}

type Subscription {
  partyUpdated(federatedId: ID!): PartyUpdate!
  relationshipChanged(partyId: ID!): RelationshipUpdate!
}
```

### REST API Endpoints

```
# Sync
POST   /api/v1/sync/trigger
GET    /api/v1/sync/status/{jobId}

# Party CRUD
GET    /api/v1/parties/{federatedId}
POST   /api/v1/parties/search
GET    /api/v1/parties/{federatedId}/relationships

# Hierarchy
GET    /api/v1/parties/{federatedId}/hierarchy
GET    /api/v1/parties/{federatedId}/ultimate-parent

# Resolution
GET    /api/v1/resolution/candidates
POST   /api/v1/resolution/merge
POST   /api/v1/resolution/review

# Lineage
GET    /api/v1/parties/{federatedId}/lineage
GET    /api/v1/parties/{federatedId}/sources
```

## Synchronization Strategy

### Real-time Sync (Event-Driven)

```
Source System → Kafka Topic → Party Federation Service → Neo4j
                   ↓
              Dead Letter Queue (failed events)
```

### Batch Sync (Scheduled)

```
Cron Job → Full/Incremental Extract → Transformation → Load → Validation
```

### Change Data Capture (CDC)

For systems supporting CDC:
```
Source DB → Debezium → Kafka → Stream Processing → Neo4j
```

## Neo4j Cypher Queries (Examples)

### Find Ultimate Parent

```cypher
MATCH path = (child:Organization {federatedId: $childId})-[:SUBSIDIARY_OF*]->(parent:Organization)
WHERE NOT (parent)-[:SUBSIDIARY_OF]->()
RETURN parent AS ultimateParent, length(path) AS depth
ORDER BY depth DESC
LIMIT 1
```

### Find All Subsidiaries (Any Depth)

```cypher
MATCH (parent:Organization {federatedId: $parentId})-[:PARENT_OF*]->(subsidiary)
RETURN subsidiary,
       labels(subsidiary) AS types,
       size((parent)-[:PARENT_OF*]->(subsidiary)) AS depth
ORDER BY depth
```

### Operates On Behalf Of (Cross-System Relationship)

```cypher
MATCH (agent:Organization)-[r:OPERATES_ON_BEHALF_OF]->(principal:Organization)
WHERE $sourceSystem IN r.source_systems
  AND r.valid_from <= date() <= r.valid_to
RETURN agent, r, principal
```

### Find Beneficial Owners (>25% threshold)

```cypher
MATCH path = (owner:Individual)-[:BENEFICIAL_OWNER_OF*]->(entity:LegalEntity {federatedId: $entityId})
WHERE ALL(r IN relationships(path) WHERE r.ownership_percentage >= 0.25)
WITH owner,
     reduce(ownership = 1.0, r IN relationships(path) | ownership * r.ownership_percentage) AS effectiveOwnership
WHERE effectiveOwnership >= 0.25
RETURN owner, effectiveOwnership
ORDER BY effectiveOwnership DESC
```

### Find Duplicate Candidates

```cypher
MATCH (p1:Party)-[:DUPLICATES]->(p2:Party)
WHERE p1.status = 'ACTIVE' AND p2.status = 'ACTIVE'
WITH p1, p2,
     [(p1)-[d:DUPLICATES]->(p2) | d.similarity_score][0] AS score
WHERE score >= $threshold
RETURN p1, p2, score
ORDER BY score DESC
```

### Data Lineage

```cypher
MATCH (party:Party {federatedId: $partyId})-[:SOURCED_FROM]->(source:SourceRecord)
RETURN party.name AS partyName,
       source.sourceSystem AS system,
       source.sourceId AS externalId,
       source.syncedAt AS lastSync,
       source.sourceData AS rawData
```

### Find Relationship Paths

```cypher
MATCH path = shortestPath(
  (org1:Organization {federatedId: $org1Id})-[*1..5]-(org2:Organization {federatedId: $org2Id})
)
RETURN path,
       [r in relationships(path) | type(r)] AS relationshipTypes,
       length(path) AS hops
```

### Aggregate Risk Across Hierarchy

```cypher
MATCH (parent:Organization {federatedId: $parentId})-[:PARENT_OF*]->(subsidiary)
WITH parent,
     collect(subsidiary.riskRating) AS riskRatings,
     count(subsidiary) AS totalSubsidiaries
RETURN parent.name,
       totalSubsidiaries,
       apoc.coll.frequencies(riskRatings) AS riskDistribution,
       CASE
         WHEN 'HIGH' IN riskRatings THEN 'HIGH'
         WHEN 'MEDIUM' IN riskRatings THEN 'MEDIUM'
         ELSE 'LOW'
       END AS consolidatedRisk
```

## Technology Stack

### Core Components
- **Graph Database**: Neo4j 5.x Enterprise (with Graph Data Science library)
- **Application Framework**: Spring Boot 3.4.0
- **Graph Access**: Spring Data Neo4j, Neo4j Java Driver
- **API Layer**: Spring GraphQL, Spring Web
- **Messaging**: Apache Kafka
- **Caching**: Redis (for frequent graph traversals)

### Entity Resolution
- **ML Pipeline**: Python (scikit-learn, dedupe library)
- **Feature Store**: Feast or custom solution
- **Model Serving**: MLflow or TensorFlow Serving

### Data Quality
- **Great Expectations**: Data validation framework
- **Apache Griffin**: Data quality platform

## Performance Considerations

### Indexing Strategy

```cypher
-- Unique constraints
CREATE CONSTRAINT party_federated_id IF NOT EXISTS
FOR (p:Party) REQUIRE p.federatedId IS UNIQUE;

CREATE CONSTRAINT org_lei IF NOT EXISTS
FOR (o:Organization) REQUIRE o.lei IS UNIQUE;

-- Indexes
CREATE INDEX party_status IF NOT EXISTS
FOR (p:Party) ON (p.status);

CREATE INDEX org_name IF NOT EXISTS
FOR (o:Organization) ON (o.name);

CREATE INDEX source_system IF NOT EXISTS
FOR (s:SourceRecord) ON (s.sourceSystem, s.sourceId);

-- Full-text search
CREATE FULLTEXT INDEX partyNameSearch IF NOT EXISTS
FOR (p:Party) ON EACH [p.name, p.legalName];
```

### Query Optimization
- Use query parameters to enable query caching
- Limit traversal depth with max hops
- Implement pagination for large result sets
- Use `PROFILE` and `EXPLAIN` for query tuning

### Caching Strategy
- Cache frequently accessed party hierarchies in Redis
- TTL-based invalidation (5-15 minutes)
- Event-driven cache invalidation on updates

### Scaling
- Neo4j Causal Cluster (read replicas for queries)
- Shard by region or business unit if needed
- Read-write separation: writes to leader, reads from followers

## Security & Compliance

### Data Protection
- **PII Encryption**: Encrypt sensitive fields (SSN, DOB) at rest
- **Field-level Access Control**: Use Neo4j RBAC for sensitive data
- **Audit Trail**: Log all data access and modifications

### API Security
- OAuth 2.0 / JWT authentication
- Role-based access control (RBAC)
- Row-level security based on user's business unit

### Compliance
- **GDPR**: Right to erasure, data portability
- **KYC/AML**: Maintain audit trail of party data changes
- **SOX**: Segregation of duties for data stewardship

## Monitoring & Observability

### Metrics
- Graph size (nodes, relationships)
- Query performance (p50, p95, p99)
- Sync latency per source system
- Entity resolution accuracy
- Duplicate detection rate

### Alerting
- Sync failures
- Data quality degradation
- Duplicate threshold breaches
- Query performance degradation

### Dashboards
- Party hierarchy visualization
- Source system health
- Data lineage explorer
- Conflict resolution queue

## Migration & Rollout Strategy

### Phase 1: Foundation (Months 1-3)
- Neo4j infrastructure setup
- Core domain model implementation
- Source system connectors (read-only)
- Basic entity resolution

### Phase 2: Integration (Months 4-6)
- Batch sync from all source systems
- Entity resolution tuning
- GraphQL API development
- Data stewardship tools

### Phase 3: Advanced Features (Months 7-9)
- Real-time sync (CDC/event-driven)
- Custom relationship synthesis
- ML-based entity resolution
- Advanced graph analytics

### Phase 4: Production (Months 10-12)
- Gradual rollout to consuming applications
- Performance optimization
- Full observability stack
- Runbook and operational procedures

## Success Metrics

### Quantitative
- **Deduplication Rate**: % reduction in duplicate party records
- **Data Completeness**: % of parties with full attribute coverage
- **Query Performance**: <100ms for simple lookups, <500ms for complex traversals
- **Sync Latency**: <5 minutes from source update to graph update

### Qualitative
- Improved risk assessment accuracy
- Faster client onboarding
- Enhanced regulatory reporting
- Better cross-sell opportunities

## References

### Neo4j Patterns
- Master Data Management with Neo4j
- Financial Services graph models
- Entity Resolution in graphs

### Standards
- ISO 17442 (Legal Entity Identifier)
- GLEIF (Global LEI Foundation)
- FinCEN beneficial ownership rules

### Tools
- Neo4j Bloom (visualization)
- Neo4j Graph Data Science (algorithms)
- Apache Kafka Connect (source integration)
