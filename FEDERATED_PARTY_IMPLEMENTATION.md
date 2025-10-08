# Federated Party Model - Implementation Guide

## Quick Start

### Prerequisites
- **Java 21**
- **Neo4j 5.x** (Community or Enterprise)
- **Spring Boot 3.4.0**
- **Maven 3.9+**

### Running Neo4j

#### Using Docker
```bash
docker run -d \
  --name neo4j-party \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  -e NEO4J_PLUGINS='["graph-data-science", "apoc"]' \
  neo4j:5.14-enterprise
```

#### Access Neo4j Browser
```
http://localhost:7474
Username: neo4j
Password: password
```

### Building the Service

```bash
cd backend/party-service
mvn clean install
```

### Running the Service

```bash
mvn spring-boot:run
```

The service will start on port 8083.

## API Usage Examples

### GraphQL API

Access GraphiQL at: `http://localhost:8083/graphiql`

#### Example 1: Find Party and Traverse Hierarchy

```graphql
query {
  party(federatedId: "abc-123") {
    ... on Organization {
      federatedId
      name
      legalName
      lei
      riskRating

      # Get parent
      ultimateParent {
        name
        legalName
        jurisdiction
      }

      # Get subsidiaries
      subsidiaries(depth: 2) {
        name
        jurisdiction
        ownershipPercentage
      }

      # Provenance
      sourcedFrom {
        sourceSystem
        sourceId
        syncedAt
        qualityScore
      }

      # Computed fields
      totalSubsidiaries
      jurisdictions
      consolidatedRiskRating
    }
  }
}
```

#### Example 2: Find Ultimate Beneficial Owners

```graphql
query {
  ultimateBeneficialOwners(entityId: "entity-456") {
    owner {
      firstName
      lastName
      fullName
      nationality
      pepStatus
    }
    ownershipPercentage
    controlLevel
    ubo
    verificationDate
    ownershipPath
  }
}
```

#### Example 3: Search and Find Relationship Paths

```graphql
query {
  searchParties(name: "Goldman") {
    federatedId
    legalName
    lei
  }

  relationshipPath(org1Id: "org-1", org2Id: "org-2") {
    path {
      ... on Organization {
        name
        legalName
      }
    }
    relationshipTypes
    hops
  }
}
```

#### Example 4: Find Cross-Domain Parties

```graphql
query {
  partiesInMultipleSystems(minSystems: 2) {
    ... on Organization {
      federatedId
      name
      legalName

      sourcedFrom {
        sourceSystem
        sourceId
        syncedAt
      }
    }
  }
}
```

#### Example 5: Find Duplicate Candidates

```graphql
query {
  duplicateCandidates(threshold: 0.80) {
    party1 {
      ... on Organization {
        name
        legalName
        lei
      }
    }
    party2 {
      ... on Organization {
        name
        legalName
        lei
      }
    }
    similarityScore
    matchingFields
  }
}
```

### REST API

#### Sync Party from Source System

```bash
curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-001'
```

Response:
```json
{
  "action": "CREATED",
  "resultParty": {
    "federatedId": "uuid-here",
    "name": "ABC Corporation",
    "legalName": "ABC Corporation Inc.",
    "status": "ACTIVE",
    "confidence": 1.0
  },
  "matchedParty": null,
  "matchScore": null
}
```

#### Trigger Full Sync

```bash
curl -X POST 'http://localhost:8083/api/v1/parties/sync/full?sourceSystem=COMMERCIAL_BANKING'
```

Response:
```json
{
  "sourceSystem": "COMMERCIAL_BANKING",
  "startTime": "2025-10-07T10:00:00Z",
  "endTime": "2025-10-07T10:05:00Z",
  "totalRecords": 1000,
  "processedRecords": 1000,
  "createdRecords": 850,
  "mergedRecords": 100,
  "needsReviewRecords": 50,
  "failedRecords": 0
}
```

#### Create Cross-Domain Relationship

```bash
curl -X POST http://localhost:8083/api/v1/parties/relationships/cross-domain \
  -H 'Content-Type: application/json' \
  -d '{
    "agentId": "org-123",
    "principalId": "org-456",
    "relationshipType": "OPERATES_ON_BEHALF_OF",
    "properties": {
      "authorityLevel": "FULL",
      "scope": "TRADING"
    },
    "sourceSystems": ["COMMERCIAL_BANKING", "CAPITAL_MARKETS"]
  }'
```

#### Get Duplicate Candidates

```bash
curl 'http://localhost:8083/api/v1/parties/duplicates?threshold=0.75'
```

#### Approve Merge

```bash
curl -X POST 'http://localhost:8083/api/v1/parties/merge?sourceId=party-1&targetId=party-2&approvedBy=john.doe@bank.com'
```

#### Mark as Not Duplicate

```bash
curl -X POST 'http://localhost:8083/api/v1/parties/not-duplicate?partyId=party-1&candidateId=party-2&reviewedBy=jane.smith@bank.com'
```

## Cypher Queries (Direct Neo4j Access)

### Initialize Constraints and Indexes

```cypher
// Unique constraints
CREATE CONSTRAINT party_federated_id IF NOT EXISTS
FOR (p:Party) REQUIRE p.federatedId IS UNIQUE;

CREATE CONSTRAINT org_lei IF NOT EXISTS
FOR (o:Organization) REQUIRE o.lei IS UNIQUE;

CREATE CONSTRAINT source_system_id IF NOT EXISTS
FOR (s:SourceRecord) REQUIRE (s.sourceSystem, s.sourceId) IS UNIQUE;

// Indexes
CREATE INDEX party_status IF NOT EXISTS
FOR (p:Party) ON (p.status);

CREATE INDEX org_name IF NOT EXISTS
FOR (o:Organization) ON (o.name);

CREATE INDEX org_legal_name IF NOT EXISTS
FOR (o:Organization) ON (o.legalName);

CREATE INDEX source_system IF NOT EXISTS
FOR (s:SourceRecord) ON (s.sourceSystem);

// Full-text search
CREATE FULLTEXT INDEX partyNameSearch IF NOT EXISTS
FOR (p:Party) ON EACH [p.name, p.legalName];
```

### Sample Queries

#### Find Organizations with High Risk Subsidiaries

```cypher
MATCH (parent:Organization)-[:PARENT_OF*]->(subsidiary:Organization)
WHERE subsidiary.riskRating = 'HIGH'
WITH parent, collect(subsidiary) AS highRiskSubs
RETURN parent.name,
       parent.legalName,
       size(highRiskSubs) AS highRiskSubsidiaryCount,
       [sub IN highRiskSubs | sub.name] AS highRiskNames
ORDER BY highRiskSubsidiaryCount DESC
LIMIT 20;
```

#### Find Circular Ownership (if any)

```cypher
MATCH path = (org:Organization)-[:PARENT_OF*]->(org)
RETURN path, length(path) AS cycleLength
LIMIT 10;
```

#### Find Complex Ownership Chains (>3 levels)

```cypher
MATCH path = (owner)-[:OWNS*4..]->(entity:LegalEntity)
RETURN path,
       length(path) AS chainLength,
       [r IN relationships(path) | r.ownershipPercentage] AS percentages,
       reduce(total = 1.0, r IN relationships(path) | total * r.ownershipPercentage) AS effectiveOwnership
WHERE effectiveOwnership >= 25.0
ORDER BY chainLength DESC
LIMIT 50;
```

#### Find Entities with Conflicting Data from Multiple Sources

```cypher
MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord)
WITH p, collect(s) AS sources
WHERE size(sources) >= 2
RETURN p.federatedId,
       p.legalName,
       [src IN sources | {system: src.sourceSystem, quality: src.qualityScore, synced: src.syncedAt}] AS sourceInfo
ORDER BY size(sources) DESC
LIMIT 100;
```

#### Find PEPs with Significant Beneficial Ownership

```cypher
MATCH (individual:Individual {pepStatus: true})-[r:BENEFICIAL_OWNER_OF]->(entity)
WHERE r.ownershipPercentage >= 25.0
RETURN individual.fullName,
       individual.nationality,
       collect({
         entity: entity.legalName,
         ownership: r.ownershipPercentage,
         ubo: r.ubo
       }) AS ownerships
ORDER BY individual.fullName;
```

## Entity Resolution Examples

### Scenario 1: Auto-Merge (High Confidence)

**Commercial Banking Record:**
```json
{
  "id": "CB-001",
  "legalName": "Apple Inc.",
  "registrationNumber": "C0806592",
  "jurisdiction": "California",
  "lei": "HWUPKR0MPOU8FGXBT394"
}
```

**Capital Markets Record:**
```json
{
  "counterpartyId": "CM-500",
  "legalName": "Apple Inc",
  "lei": "HWUPKR0MPOU8FGXBT394",
  "riskRating": "LOW"
}
```

**Result:** Auto-merged based on LEI match (score: 1.0)

### Scenario 2: Manual Review Required

**Commercial Banking Record:**
```json
{
  "id": "CB-002",
  "legalName": "Goldman Sachs Group Inc",
  "registrationNumber": "2923466",
  "jurisdiction": "Delaware"
}
```

**Capital Markets Record:**
```json
{
  "counterpartyId": "CM-600",
  "legalName": "Goldman Sachs Group, Inc.",
  "jurisdiction": "DE"
}
```

**Result:** Needs review (score: 0.87, no LEI match but high name similarity)

## Integration Patterns

### Pattern 1: Event-Driven Sync (Real-time)

```java
@Component
public class PartyEventListener {

    @Autowired
    private PartyFederationService federationService;

    @KafkaListener(topics = "commercial-banking.party.updates")
    public void onCommercialBankingUpdate(PartyUpdateEvent event) {
        federationService.syncFromSource("COMMERCIAL_BANKING", event.getPartyId());
    }

    @KafkaListener(topics = "capital-markets.counterparty.updates")
    public void onCapitalMarketsUpdate(CounterpartyUpdateEvent event) {
        federationService.syncFromSource("CAPITAL_MARKETS", event.getCounterpartyId());
    }
}
```

### Pattern 2: Scheduled Batch Sync

```java
@Component
public class ScheduledSyncJob {

    @Autowired
    private PartyFederationService federationService;

    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    public void syncCommercialBanking() {
        SyncResult result = federationService.syncAllFromSource("COMMERCIAL_BANKING");
        log.info("Commercial Banking sync completed: {}", result);
    }

    @Scheduled(cron = "0 0 3 * * ?") // 3 AM daily
    public void syncCapitalMarkets() {
        SyncResult result = federationService.syncAllFromSource("CAPITAL_MARKETS");
        log.info("Capital Markets sync completed: {}", result);
    }
}
```

### Pattern 3: CDC with Debezium

```yaml
# Debezium connector for Commercial Banking PostgreSQL
{
  "name": "commercial-banking-party-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "commercial-banking-db",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "secret",
    "database.dbname": "commercial_banking",
    "table.include.list": "public.parties",
    "transforms": "route",
    "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
    "transforms.route.regex": "([^.]+)\\.([^.]+)\\.([^.]+)",
    "transforms.route.replacement": "party.cdc.$3"
  }
}
```

## Data Quality & Monitoring

### Quality Metrics Dashboard

```cypher
// Total parties by source
MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord)
RETURN s.sourceSystem AS source,
       count(DISTINCT p) AS partyCount
ORDER BY partyCount DESC;

// Merge rate
MATCH (p:Party {status: 'MERGED'})
RETURN count(p) AS mergedCount;

// Pending review
MATCH (p:Party {status: 'UNDER_REVIEW'})
RETURN count(p) AS pendingReview;

// Average confidence
MATCH (p:Party {status: 'ACTIVE'})
RETURN avg(p.confidence) AS avgConfidence;

// Cross-domain parties
MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord)
WITH p, collect(DISTINCT s.sourceSystem) AS systems
WHERE size(systems) >= 2
RETURN size(systems) AS systemCount,
       count(p) AS partyCount
ORDER BY systemCount DESC;
```

### Alerting Rules

1. **Low Confidence Alert**: Parties with confidence < 0.70
2. **Stale Data Alert**: Source records not synced in > 24 hours
3. **Duplicate Backlog Alert**: > 100 pending duplicate reviews
4. **Sync Failure Alert**: Sync job failed or took > 1 hour

## Performance Optimization

### Caching Strategy

```java
@Service
public class CachedPartyService {

    @Autowired
    private OrganizationRepository orgRepository;

    @Autowired
    private RedisTemplate<String, Organization> redisTemplate;

    @Cacheable(value = "party-hierarchy", key = "#rootId")
    public Organization getHierarchy(String rootId) {
        return orgRepository.findHierarchy(rootId);
    }

    @CacheEvict(value = "party-hierarchy", key = "#partyId")
    public void invalidateHierarchy(String partyId) {
        // Triggered on party update
    }
}
```

### Query Optimization

```cypher
// Use query hints for large traversals
MATCH (parent:Organization {federatedId: $parentId})
CALL apoc.path.subgraphAll(parent, {
    relationshipFilter: "PARENT_OF>",
    maxLevel: 5
})
YIELD nodes, relationships
RETURN nodes, relationships;
```

### Batching

```java
// Batch process for large syncs
@Transactional
public void syncBatch(List<String> partyIds) {
    List<Party> parties = new ArrayList<>();

    for (String partyId : partyIds) {
        Party party = fetchAndTransform(partyId);
        parties.add(party);

        if (parties.size() >= 100) {
            partyRepository.saveAll(parties);
            parties.clear();
        }
    }

    if (!parties.isEmpty()) {
        partyRepository.saveAll(parties);
    }
}
```

## Security Considerations

### Field-Level Encryption

```java
@Component
public class EncryptionService {

    @Autowired
    private KeyManager keyManager;

    public String encrypt(String plaintext) {
        // Encrypt PII fields (SSN, DOB, Tax ID)
        Key key = keyManager.getEncryptionKey();
        // ... encryption logic
    }

    public String decrypt(String ciphertext) {
        // Decrypt for authorized users only
    }
}
```

### Row-Level Security

```java
@PreAuthorize("hasRole('ADMIN') or hasBusinessUnit(#partyId)")
public Party getParty(String partyId) {
    return partyRepository.findByFederatedId(partyId).orElseThrow();
}
```

### Audit Trail

```cypher
// Track all changes
CREATE (audit:AuditLog {
  timestamp: datetime(),
  user: $userId,
  action: $action,
  partyId: $partyId,
  changes: $changes
})
```

## Testing

### Unit Test Example

```java
@SpringBootTest
class EntityMatcherTest {

    @Autowired
    private EntityMatcher matcher;

    @Test
    void testLeiMatch() {
        Organization org1 = createOrg("Apple Inc.", "HWUPKR0MPOU8FGXBT394");
        Organization org2 = createOrg("Apple Inc", "HWUPKR0MPOU8FGXBT394");

        List<MatchCandidate> candidates = matcher.findCandidates(org1, List.of(org2));

        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getScore()).isGreaterThan(0.95);
        assertThat(candidates.get(0).getRecommendedAction()).isEqualTo(MatchAction.AUTO_MERGE);
    }
}
```

### Integration Test

```java
@SpringBootTest
@Testcontainers
class PartyFederationServiceIT {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5.14");

    @Autowired
    private PartyFederationService federationService;

    @Test
    void testSyncAndMerge() {
        // Sync from Commercial Banking
        ResolutionResult result1 = federationService.syncFromSource("COMMERCIAL_BANKING", "CB-001");
        assertThat(result1.getAction()).isEqualTo(ResolutionAction.CREATED);

        // Sync same entity from Capital Markets
        ResolutionResult result2 = federationService.syncFromSource("CAPITAL_MARKETS", "CM-500");
        assertThat(result2.getAction()).isEqualTo(ResolutionAction.MERGED);

        // Verify merged party has both sources
        Party merged = result2.getResultParty();
        assertThat(merged.getSourceRecords()).hasSize(2);
    }
}
```

## Troubleshooting

### Common Issues

**Issue: Duplicate Detection Not Working**
```cypher
// Check if DUPLICATES relationships exist
MATCH (p1:Party)-[d:DUPLICATES]->(p2:Party)
RETURN p1, d, p2 LIMIT 10;

// Manually create duplicate candidate
MATCH (p1:Party {federatedId: $id1}), (p2:Party {federatedId: $id2})
CREATE (p1)-[:DUPLICATES {similarityScore: 0.85, matchingFields: ['legalName']}]->(p2);
```

**Issue: Slow Hierarchy Queries**
```cypher
// Check if indexes exist
SHOW INDEXES;

// Profile query
PROFILE MATCH (parent:Organization {federatedId: $parentId})-[:PARENT_OF*]->(subsidiary)
RETURN subsidiary;
```

**Issue: Source Sync Failing**
```bash
# Check source system connectivity
curl http://localhost:8080/commercial-banking/health

# Check logs
tail -f logs/party-service.log | grep ERROR
```

## Next Steps

1. **ML-Based Entity Resolution**: Implement machine learning model for better duplicate detection
2. **Graph Algorithms**: Use Neo4j GDS for centrality, community detection
3. **Real-time Dashboards**: Build operational dashboards with Grafana
4. **Data Catalog Integration**: Integrate with data catalog (Collibra, Alation)
5. **API Rate Limiting**: Implement rate limiting for source system APIs
