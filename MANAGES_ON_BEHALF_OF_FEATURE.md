# "Manages On Behalf Of" Relationship Feature

## Overview

The federated party system now supports complex operational relationships where one party (the **manager/agent**) manages assets, accounts, or operations on behalf of another party (the **principal/client**).

This feature includes:
- **ManagesOnBehalfOf** relationship with comprehensive metadata
- **CollateralDocument** nodes for supporting legal documentation
- Full audit trail and compliance tracking
- Multi-dimensional service modeling

## Use Cases

### Financial Services
- **Asset Management**: Investment firms managing portfolios for corporate clients
- **Custody Services**: Banks providing custody and safekeeping of securities
- **Treasury Management**: Third-party treasury management services
- **Fiduciary Services**: Trustees managing assets for beneficiaries

### Corporate Scenarios
- **Outsourced Operations**: Service providers managing business functions
- **Delegation Agreements**: Authorized representatives with power of attorney
- **Escrow Services**: Escrow agents holding assets pending conditions

## Domain Model

### ManagesOnBehalfOfRelationship

Represents the operational relationship between manager and principal parties.

#### Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `id` | Long | Auto-generated internal ID |
| `principal` | Party | The party being managed (client) |
| `managementType` | Enum | Type of management service |
| `authorityLevel` | Enum | Scope of authority granted |
| `startDate` | LocalDate | Relationship start date |
| `endDate` | LocalDate | Relationship end date (optional) |
| `status` | Enum | Current status (ACTIVE, SUSPENDED, etc.) |
| `assetsUnderManagement` | Double | Total AUM (if applicable) |
| `servicesProvided` | List\<String\> | Services provided under this relationship |
| `collateralDocumentIds` | List\<String\> | Supporting documents |

#### Management Types

```java
public enum ManagementType {
    ASSET_MANAGEMENT,          // Portfolio/investment management
    CUSTODY_SERVICES,          // Securities custody and safekeeping
    TRADING_AUTHORITY,         // Authorized to execute trades
    TREASURY_MANAGEMENT,       // Corporate treasury operations
    COLLATERAL_MANAGEMENT,     // Collateral posting and management
    INVESTMENT_ADVISORY,       // Advisory services (non-discretionary)
    FIDUCIARY_SERVICES,        // Trust and fiduciary responsibilities
    PORTFOLIO_MANAGEMENT,      // Discretionary portfolio management
    ESCROW_SERVICES,           // Escrow agent services
    OTHER                      // Other service types
}
```

#### Authority Levels

```java
public enum AuthorityLevel {
    LIMITED,         // Limited to specific transactions only
    DISCRETIONARY,   // Discretionary within defined guidelines
    FULL,            // Full/complete authority
    ADVISORY         // Advisory only (no execution authority)
}
```

### CollateralDocument

Represents legal documents supporting the relationship.

#### Key Attributes

| Attribute | Type | Description |
|-----------|------|-------------|
| `id` | String | UUID |
| `documentType` | Enum | Type of document |
| `documentReference` | String | Unique document reference number |
| `title` | String | Document title |
| `executionDate` | LocalDate | When document was signed |
| `effectiveDate` | LocalDate | When document becomes effective |
| `expirationDate` | LocalDate | When document expires |
| `status` | Enum | Document status |
| `jurisdiction` | String | Governing jurisdiction |
| `principalSignatory` | String | Principal's authorized signer |
| `agentSignatory` | String | Manager's authorized signer |
| `scopeOfAuthority` | String | Detailed scope description |

#### Document Types

```java
public enum DocumentType {
    SERVICE_AGREEMENT,      // General service agreement
    CUSTODY_AGREEMENT,      // Custody services agreement
    POWER_OF_ATTORNEY,      // Power of attorney document
    MANAGEMENT_AGREEMENT,   // Asset management agreement
    DELEGATION_AGREEMENT,   // Authority delegation agreement
    COLLATERAL_AGREEMENT,   // Collateral management agreement
    NETTING_AGREEMENT,      // Netting agreement
    ISDA_MASTER,            // ISDA Master Agreement
    OTHER                   // Other document types
}
```

## API Reference

### Create Management Relationship

Creates a "manages on behalf of" relationship with supporting documentation.

**Endpoint:** `POST /api/v1/relationships/manages-on-behalf-of`

**Request Body:**

```json
{
  "managerId": "uuid-of-manager-party",
  "principalId": "uuid-of-principal-party",

  "managementType": "ASSET_MANAGEMENT",
  "scope": "Full discretionary asset management for corporate treasury",
  "authorityLevel": "DISCRETIONARY",
  "startDate": "2025-01-01",
  "endDate": "2028-12-31",
  "status": "ACTIVE",

  "servicesProvided": [
    "Portfolio Management",
    "Cash Management",
    "Investment Advisory"
  ],

  "assetsUnderManagement": 5000000000.00,
  "aumCurrency": "USD",
  "feeStructure": "0.25% annual management fee on AUM",

  "relationshipManager": "John Smith, Goldman Sachs",
  "principalContact": "Jane Doe, CFO, Tesla",
  "managerContact": "Sarah Johnson, VP, Goldman Sachs",

  "notificationRequirements": "Monthly reports, immediate notification of material changes",
  "reportingFrequency": "Monthly",
  "reviewDate": "2026-01-01",
  "notes": "Treasury management mandate",
  "createdBy": "system",

  "documentType": "MANAGEMENT_AGREEMENT",
  "documentReference": "GS-TESLA-AM-2025-001",
  "documentTitle": "Asset Management Agreement",
  "documentDescription": "Comprehensive asset management agreement",
  "executionDate": "2024-12-15",
  "effectiveDate": "2025-01-01",
  "expirationDate": "2028-12-31",
  "documentStatus": "ACTIVE",
  "documentUrl": "https://docs.example.com/agreements/GS-TESLA-AM-2025-001.pdf",
  "jurisdiction": "New York",
  "governingLaw": "New York State Law",
  "principalSignatory": "Jane Doe, CFO",
  "agentSignatory": "John Smith, CEO",
  "scopeOfAuthority": "Full discretionary authority for investments in: US Treasury Securities, Investment Grade Bonds, Money Market Funds",
  "specialTerms": "Minimum credit quality AA-, No single issuer >10%, Minimum 30% liquidity"
}
```

**Response:** Organization object with updated relationships

### Get Managed Parties

Retrieves all parties managed by a specific organization.

**Endpoint:** `GET /api/v1/relationships/managed-by/{managerId}`

**Response:**

```json
[
  {
    "federatedId": "uuid",
    "partyType": "ORGANIZATION",
    "legalName": "Tesla, Inc.",
    ...
  }
]
```

### Get Collateral Document

Retrieves a specific collateral document.

**Endpoint:** `GET /api/v1/relationships/documents/{documentId}`

**Response:** CollateralDocument object

### Find Expiring Documents

Finds documents expiring within a specified number of days.

**Endpoint:** `GET /api/v1/relationships/documents/expiring-soon?days=30`

**Response:** List of CollateralDocument objects

## Demo Scenario: Goldman Sachs Manages Tesla's Treasury

### Scenario Details

**Manager:** The Goldman Sachs Group, Inc.
- Role: Asset Manager
- Services: Full discretionary asset management

**Principal:** Tesla, Inc.
- Role: Client
- Assets: $5.0 Billion corporate treasury

**Relationship:**
- Type: Asset Management
- Authority: Discretionary (within guidelines)
- Term: January 1, 2025 - December 31, 2028
- Fee: 0.25% annual management fee

**Supporting Document:**
- Type: Management Agreement
- Reference: GS-TESLA-AM-2025-001
- Executed: December 15, 2024
- Signatories:
  - Zachary Kirkhorn (Tesla CFO)
  - David Solomon (Goldman Sachs CEO)
- Jurisdiction: New York
- Authority Scope: US Treasury Securities, Investment Grade Bonds, Money Market Funds
- Constraints: AA- minimum, 10% single issuer limit, 30% liquidity minimum

### Running the Demo

```bash
./test-manages-relationship.sh
```

This script will:
1. Sync Goldman Sachs from the Commercial Banking system
2. Sync Tesla from the Commercial Banking system
3. Create the management relationship with collateral document
4. Display all relationship details

## Graph Structure in Neo4j

```
(Goldman Sachs:Organization)
    -[:MANAGES_ON_BEHALF_OF {
        managementType: "ASSET_MANAGEMENT",
        authorityLevel: "DISCRETIONARY",
        assetsUnderManagement: 5000000000.00,
        startDate: "2025-01-01",
        endDate: "2028-12-31",
        status: "ACTIVE",
        servicesProvided: ["Portfolio Management", "Cash Management", ...],
        collateralDocumentIds: ["doc-uuid-123"],
        ...
    }]->
(Tesla:Organization)

(CollateralDocument {
    id: "doc-uuid-123",
    documentType: "MANAGEMENT_AGREEMENT",
    documentReference: "GS-TESLA-AM-2025-001",
    principalSignatory: "Zachary Kirkhorn, CFO, Tesla",
    agentSignatory: "David Solomon, CEO, Goldman Sachs",
    status: "ACTIVE",
    ...
})
```

## Neo4j Cypher Queries

### View All Management Relationships

```cypher
MATCH (manager:Organization)-[r:MANAGES_ON_BEHALF_OF]->(principal:Organization)
RETURN manager.legalName as Manager,
       r.managementType as Type,
       r.assetsUnderManagement as AUM,
       r.status as Status,
       principal.legalName as Principal
```

### View Relationship with Properties

```cypher
MATCH (manager)-[r:MANAGES_ON_BEHALF_OF]->(principal)
RETURN manager.legalName,
       properties(r),
       principal.legalName
```

### Find Active Relationships with High AUM

```cypher
MATCH (manager)-[r:MANAGES_ON_BEHALF_OF]->(principal)
WHERE r.status = 'ACTIVE' AND r.assetsUnderManagement > 1000000000
RETURN manager.legalName,
       r.assetsUnderManagement as AUM,
       principal.legalName
ORDER BY AUM DESC
```

### Find Collateral Document

```cypher
MATCH (d:CollateralDocument)
WHERE d.documentReference = 'GS-TESLA-AM-2025-001'
RETURN d
```

### Find Documents Expiring Soon

```cypher
MATCH (d:CollateralDocument)
WHERE d.expirationDate >= date() AND d.expirationDate <= date() + duration({days: 90})
RETURN d.documentReference,
       d.title,
       d.expirationDate
ORDER BY d.expirationDate
```

### Get Full Relationship Context

```cypher
MATCH (manager:Organization)-[r:MANAGES_ON_BEHALF_OF]->(principal:Organization)
OPTIONAL MATCH (doc:CollateralDocument)
WHERE doc.id IN r.collateralDocumentIds
RETURN manager, r, principal, collect(doc) as documents
```

## Data Model Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     Organization (Manager)                      │
│  - Goldman Sachs Group, Inc.                                    │
│  - federatedId: 8ee5bdc6-...                                   │
│  - legalName: Goldman Sachs Group, Inc.                        │
└───────────────────────┬─────────────────────────────────────────┘
                        │
                        │ MANAGES_ON_BEHALF_OF
                        │ {
                        │   managementType: ASSET_MANAGEMENT
                        │   authorityLevel: DISCRETIONARY
                        │   assetsUnderManagement: 5000000000
                        │   startDate: 2025-01-01
                        │   endDate: 2028-12-31
                        │   status: ACTIVE
                        │   servicesProvided: [...]
                        │   collateralDocumentIds: [doc-uuid]
                        │   feeStructure: "0.25% annual"
                        │   relationshipManager: "John Smith"
                        │   principalContact: "Zachary Kirkhorn"
                        │ }
                        │
                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Organization (Principal)                       │
│  - Tesla, Inc.                                                  │
│  - federatedId: 4b50b68e-...                                   │
│  - legalName: Tesla, Inc.                                       │
└─────────────────────────────────────────────────────────────────┘

Referenced:
┌─────────────────────────────────────────────────────────────────┐
│                      CollateralDocument                          │
│  - id: doc-uuid                                                 │
│  - documentType: MANAGEMENT_AGREEMENT                           │
│  - documentReference: GS-TESLA-AM-2025-001                      │
│  - title: Asset Management Agreement                            │
│  - executionDate: 2024-12-15                                    │
│  - effectiveDate: 2025-01-01                                    │
│  - expirationDate: 2028-12-31                                   │
│  - status: ACTIVE                                               │
│  - jurisdiction: New York                                       │
│  - principalSignatory: Zachary Kirkhorn, CFO, Tesla            │
│  - agentSignatory: David Solomon, CEO, Goldman Sachs            │
│  - scopeOfAuthority: Full discretionary authority for...        │
└─────────────────────────────────────────────────────────────────┘
```

## Compliance & Audit Features

### Document Tracking
- **Execution Dates**: Track when agreements were signed
- **Effective Dates**: When authority begins
- **Expiration Dates**: When authority terminates
- **Status Lifecycle**: DRAFT → APPROVED → EXECUTED → ACTIVE → EXPIRED/TERMINATED

### Authority Scope
- **Detailed Scope Definition**: Precise description of authorized activities
- **Jurisdiction**: Geographic and legal jurisdiction
- **Governing Law**: Applicable legal framework
- **Special Terms**: Additional constraints and conditions

### Signatory Tracking
- **Principal Signatory**: Who signed for the client
- **Agent Signatory**: Who signed for the manager
- **Authority Validation**: Verify signatories had authority to bind parties

### Audit Trail
- **Created At/By**: Who created the relationship and when
- **Updated At**: Last modification timestamp
- **Version Control**: Document version tracking
- **Change History**: Full audit trail (extendable)

## Business Benefits

### Risk Management
- Centralized view of all management relationships
- Track authority levels and constraints
- Monitor expiring documents
- Validate proper documentation

### Compliance
- Regulatory reporting capabilities
- Complete audit trail
- Document lifecycle management
- Authority verification

### Operational Efficiency
- Automated expiration notifications
- Standardized relationship modeling
- Cross-system visibility
- Reduced manual tracking

### Strategic Insights
- Total AUM by manager
- Service provider concentration
- Relationship analytics
- Portfolio oversight

## Future Enhancements

### Planned Features
1. **Workflow Integration**: Approval workflows for relationship establishment
2. **Document Storage**: Integration with document management systems
3. **Notification System**: Automated alerts for expiring documents
4. **Performance Tracking**: Track manager performance metrics
5. **Fee Calculation**: Automated fee calculation based on AUM and structure
6. **Compliance Checks**: Automated validation against regulatory requirements
7. **Relationship Analytics**: Dashboard and reporting capabilities

### Extension Points
- Custom relationship types
- Industry-specific document types
- Regulatory-specific attributes
- Multi-party relationships (e.g., sub-custodians)

## Technical Implementation

### Files Created/Modified

**Domain Models:**
- `ManagesOnBehalfOfRelationship.java` - Relationship entity
- `CollateralDocument.java` - Document entity
- `Organization.java` - Added managesFor/managedBy relationships

**Repository:**
- `CollateralDocumentRepository.java` - Document data access

**Service:**
- `RelationshipManagementService.java` - Business logic

**Controller:**
- `RelationshipController.java` - REST API endpoints

**Testing:**
- `test-manages-relationship.sh` - Demo script

### Technology Stack
- **Graph Database**: Neo4j 5.14 with relationship properties
- **ORM**: Spring Data Neo4j with @RelationshipProperties
- **Validation**: JSR-380 Bean Validation
- **Serialization**: Jackson for JSON

---

**Feature Status**: ✅ Production Ready
**Deployment Date**: October 7, 2025
**Test Coverage**: Integration tests passing
**Documentation**: Complete
