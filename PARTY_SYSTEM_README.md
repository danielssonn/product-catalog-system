# Federated Party System

A graph-based federated party management system that unifies party data from multiple source systems (Commercial Banking, Capital Markets) using Neo4j, with automatic entity resolution and conflict management.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                 Federated Party Service (Neo4j)              │
│              http://localhost:8083                           │
│  - GraphQL API for graph traversal                          │
│  - REST API for sync operations                             │
│  - Entity resolution engine                                 │
│  - Conflict resolution based on data quality                │
└────────────┬──────────────────────────┬─────────────────────┘
             │                          │
    ┌────────┴────────┐        ┌────────┴────────┐
    │  Commercial     │        │  Capital        │
    │  Banking API    │        │  Markets API    │
    │  :8084          │        │  :8085          │
    │  (MongoDB)      │        │  (MongoDB)      │
    └─────────────────┘        └─────────────────┘
```

## 📦 Components

### 1. **Commercial Banking Party Service** (Port 8084)
- MongoDB-based party management
- 5 sample parties (Apple, Goldman Sachs, Microsoft, Tesla, JPMorgan)
- REST API for party CRUD operations

### 2. **Capital Markets Counterparty Service** (Port 8085)
- MongoDB-based counterparty management
- 5 sample counterparties (overlaps with Commercial Banking)
- REST API for counterparty operations
- LEI (Legal Entity Identifier) support

### 3. **Federated Party Service** (Port 8083)
- Neo4j graph database
- Entity resolution with LEI matching
- Automatic deduplication (>95% confidence)
- Manual review queue (75-95% confidence)
- Data lineage tracking
- GraphQL + REST APIs

### 4. **Neo4j Graph Database** (Ports 7474, 7687)
- Browser: http://localhost:7474
- Credentials: neo4j / password
- Graph Data Science library enabled
- APOC procedures enabled

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 21+
- Maven 3.9+

### Deploy

```bash
./deploy-party-system.sh
```

This script will:
1. Build all three services
2. Start MongoDB with sample data
3. Start Neo4j graph database
4. Deploy all three party services
5. Wait for health checks
6. Display access URLs

### Test

```bash
./test-party-federation.sh
```

This script tests:
- Entity resolution and auto-merge
- Cross-domain party detection
- Batch synchronization
- Duplicate detection

## 🎯 Use Cases Demonstrated

### Use Case 1: Auto-Merge Based on LEI

**Scenario**: Apple Inc. exists in both Commercial Banking (CB-001) and Capital Markets (CM-001).

**Expected**: System auto-merges based on LEI match (HWUPKR0MPOU8FGXBT394).

**Test**:
```bash
# Sync from Commercial Banking
curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-001'

# Sync from Capital Markets (auto-merges)
curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=CAPITAL_MARKETS&sourceId=CM-001'
```

**Result**: Single federated party with data from both sources.

### Use Case 2: Cross-Domain Relationship

**Scenario**: Identify entities present in multiple systems.

**Query**:
```bash
curl 'http://localhost:8083/api/v1/parties/cross-domain?minSystems=2'
```

**Expected**: Returns Apple, Goldman Sachs, Microsoft, JPMorgan (entities in both systems).

### Use Case 3: Data Quality-Based Conflict Resolution

**Scenario**: Commercial Banking has risk rating "MEDIUM", Capital Markets has "AA".

**Resolution Strategy**:
- Risk rating: Use Capital Markets (higher quality score: 0.95 vs 0.85)
- Address: Use Commercial Banking (higher quality: 0.98)
- LEI: Use Capital Markets (quality: 0.99)

**View**:
```bash
curl 'http://localhost:8083/api/v1/parties/{federatedId}'
```

## 📊 Sample Data

### Commercial Banking (5 parties)
- **CB-001**: Apple Inc. - Technology, TIER_1, LOW risk
- **CB-002**: Goldman Sachs - Banking, TIER_1, LOW risk
- **CB-003**: Microsoft - Technology, TIER_1, LOW risk
- **CB-004**: Tesla - Automotive, TIER_1, MEDIUM risk
- **CB-005**: JPMorgan - Banking, TIER_1, LOW risk

### Capital Markets (5 counterparties)
- **CM-001**: Apple Inc. (LEI: HWUPKR0MPOU8FGXBT394) - AA rating
- **CM-002**: Goldman Sachs (LEI: 784F5XWPLTWKTBV3E584) - AA rating
- **CM-003**: Microsoft (LEI: INR2EJN1ERAN0W5ZP974) - AAA rating
- **CM-004**: JPMorgan (LEI: 8I5DZWZKVSZI1NUHU748) - AA rating
- **CM-005**: Citadel (LEI: 549300JE90ZSHPBXNH35) - A rating

**Overlapping Entities** (4): Apple, Goldman Sachs, Microsoft, JPMorgan

## 🔍 API Examples

### REST API

#### Sync Single Party
```bash
curl -X POST 'http://localhost:8083/api/v1/parties/sync?sourceSystem=COMMERCIAL_BANKING&sourceId=CB-001'
```

#### Full Batch Sync
```bash
curl -X POST 'http://localhost:8083/api/v1/parties/sync/full?sourceSystem=COMMERCIAL_BANKING'
```

#### Find Duplicates
```bash
curl 'http://localhost:8083/api/v1/parties/duplicates?threshold=0.75'
```

#### Find Cross-Domain Parties
```bash
curl 'http://localhost:8083/api/v1/parties/cross-domain?minSystems=2'
```

#### Approve Merge
```bash
curl -X POST 'http://localhost:8083/api/v1/parties/merge?sourceId=party1&targetId=party2&approvedBy=admin@bank.com'
```

### GraphQL API

Access GraphiQL: http://localhost:8083/graphiql

#### Find Party by LEI
```graphql
query {
  partyByLei(lei: "HWUPKR0MPOU8FGXBT394") {
    ... on Organization {
      federatedId
      legalName
      lei
      riskRating
      sourcedFrom {
        sourceSystem
        sourceId
        syncedAt
        qualityScore
      }
    }
  }
}
```

#### Find Parties in Multiple Systems
```graphql
query {
  partiesInMultipleSystems(minSystems: 2) {
    ... on Organization {
      federatedId
      legalName
      lei
      sourcedFrom {
        sourceSystem
        sourceId
      }
    }
  }
}
```

#### Search Parties
```graphql
query {
  searchParties(name: "Goldman") {
    federatedId
    legalName
    lei
    tier
    riskRating
  }
}
```

### Neo4j Cypher Queries

Access Neo4j Browser: http://localhost:7474 (neo4j / password)

#### View All Organizations
```cypher
MATCH (o:Organization)
RETURN o
LIMIT 25
```

#### Find Parties with Multiple Sources
```cypher
MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord)
WITH p, collect(s.sourceSystem) AS systems
WHERE size(systems) >= 2
RETURN p.legalName AS party,
       systems AS sourceSystems,
       size(systems) AS sourceCount
ORDER BY sourceCount DESC
```

#### View Data Lineage
```cypher
MATCH (p:Party {legalName: "Apple Inc."})-[:SOURCED_FROM]->(s:SourceRecord)
RETURN p.legalName AS party,
       s.sourceSystem AS source,
       s.sourceId AS externalId,
       s.syncedAt AS lastSync,
       s.qualityScore AS quality
```

#### Find Merged Parties
```cypher
MATCH (p:Party)-[m:MERGED_FROM]->(source:Party)
RETURN p.legalName AS targetParty,
       source.legalName AS sourceParty,
       m.confidenceScore AS confidence,
       m.mergeDate AS mergedAt
```

## 🛠️ Development

### Build Services
```bash
cd backend

# Build Commercial Banking
mvn clean package -pl commercial-banking-party-service -am

# Build Capital Markets
mvn clean package -pl capital-markets-party-service -am

# Build Federated Party Service
mvn clean package -pl party-service -am
```

### Run Locally (without Docker)

#### 1. Start MongoDB
```bash
docker run -d -p 27018:27017 \
  -e MONGO_INITDB_ROOT_USERNAME=admin \
  -e MONGO_INITDB_ROOT_PASSWORD=admin123 \
  -v $(pwd)/infrastructure/mongodb/init-party-data.js:/docker-entrypoint-initdb.d/init-party-data.js \
  mongo:7.0
```

#### 2. Start Neo4j
```bash
docker run -d -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:5.14
```

#### 3. Run Services
```bash
# Commercial Banking
cd backend/commercial-banking-party-service
mvn spring-boot:run

# Capital Markets
cd backend/capital-markets-party-service
mvn spring-boot:run

# Federated Party Service
cd backend/party-service
mvn spring-boot:run
```

## 📈 Monitoring

### Service Health
```bash
# Commercial Banking
curl http://localhost:8084/api/commercial-banking/parties/health

# Capital Markets
curl http://localhost:8085/api/capital-markets/counterparties/health

# Federated Party Service
curl http://localhost:8083/actuator/health
```

### Logs
```bash
docker-compose -f docker-compose.party.yml logs -f party-service
docker-compose -f docker-compose.party.yml logs -f commercial-banking-party-service
docker-compose -f docker-compose.party.yml logs -f capital-markets-party-service
```

## 🔐 Security

- **Neo4j**: Username: `neo4j`, Password: `password`
- **MongoDB**: Username: `admin`, Password: `admin123`
- **Commercial Banking DB**: Username: `commercialuser`, Password: `commercialpass`
- **Capital Markets DB**: Username: `capitaluser`, Password: `capitalpass`

**⚠️ Note**: These are development credentials. Use secure credentials in production.

## 🧹 Cleanup

```bash
# Stop and remove containers
docker-compose -f docker-compose.party.yml down -v

# Remove volumes
docker volume rm party_mongodb_data party_neo4j_data party_neo4j_logs
```

## 📚 Documentation

- [Architecture Design](FEDERATED_PARTY_ARCHITECTURE.md) - Detailed architecture and design decisions
- [Implementation Guide](FEDERATED_PARTY_IMPLEMENTATION.md) - Development guide with examples

## 🎯 Key Features

✅ **Entity Resolution**: Automatic matching using LEI, registration number, fuzzy name matching
✅ **Auto-Merge**: High-confidence matches (>95%) merged automatically
✅ **Manual Review Queue**: Medium-confidence matches (75-95%) queued for review
✅ **Data Lineage**: Track which source system contributed each field
✅ **Conflict Resolution**: Quality-scored field-level conflict resolution
✅ **Graph Traversal**: Rich relationship queries via GraphQL
✅ **Cross-Domain Relationships**: Synthesize relationships from multiple sources
✅ **Real-time Sync**: Support for event-driven and batch synchronization

## 🐛 Troubleshooting

### Services won't start
```bash
# Check logs
docker-compose -f docker-compose.party.yml logs

# Restart specific service
docker-compose -f docker-compose.party.yml restart party-service
```

### MongoDB connection issues
```bash
# Verify MongoDB is running
docker exec party-mongodb mongosh -u admin -p admin123 --eval "db.adminCommand({ping: 1})"

# Check initialization script ran
docker exec party-mongodb mongosh -u admin -p admin123 --eval "show dbs"
```

### Neo4j connection issues
```bash
# Check Neo4j status
curl http://localhost:7474

# View logs
docker logs party-neo4j
```

## 🤝 Contributing

See main project [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

See main project [LICENSE](LICENSE).
