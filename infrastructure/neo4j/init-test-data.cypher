// ============================================================================
// Neo4j Test Data Initialization Script
// Context Resolution Architecture - Complete Test Data
// ============================================================================

// Clean up existing data (for testing only)
MATCH (n) DETACH DELETE n;

// ============================================================================
// STEP 1: Create Organizations (Tenants)
// ============================================================================

// Top-level organization: Acme Bank (Tenant)
CREATE (acmeBank:Organization:Party {
    federatedId: 'org-acme-bank-001',
    name: 'Acme Bank',
    legalName: 'Acme Banking Corporation',
    partyType: 'ORGANIZATION',
    status: 'ACTIVE',
    registrationNumber: 'REG-US-12345',
    jurisdiction: 'Delaware, USA',
    lei: 'LEI-ACME-BANK-US-001',
    industryCode: '522110',
    industry: 'Commercial Banking',
    tier: 'TIER_1',
    riskRating: 'LOW',
    amlStatus: 'COMPLIANT',
    website: 'https://acmebank.com',
    phoneNumber: '+1-212-555-0100',
    email: 'info@acmebank.com',
    employeeCount: 5000,
    annualRevenue: 2500.0,
    confidence: 1.0,
    createdAt: datetime(),
    updatedAt: datetime()
});

// Another organization: Global Financial Services
CREATE (globalFinancial:Organization:Party {
    federatedId: 'org-global-financial-001',
    name: 'Global Financial Services',
    legalName: 'Global Financial Services Inc.',
    partyType: 'ORGANIZATION',
    status: 'ACTIVE',
    registrationNumber: 'REG-US-67890',
    jurisdiction: 'New York, USA',
    lei: 'LEI-GLOBAL-FIN-US-001',
    industryCode: '523110',
    industry: 'Investment Banking',
    tier: 'TIER_2',
    riskRating: 'MEDIUM',
    amlStatus: 'COMPLIANT',
    website: 'https://globalfinancial.com',
    phoneNumber: '+1-212-555-0200',
    email: 'info@globalfinancial.com',
    employeeCount: 1500,
    annualRevenue: 850.0,
    confidence: 1.0,
    createdAt: datetime(),
    updatedAt: datetime()
});

// ============================================================================
// STEP 2: Create Individuals (Users/Principals)
// ============================================================================

// Admin user for Acme Bank
CREATE (adminUser:Individual:Party {
    federatedId: 'ind-admin-001',
    partyType: 'INDIVIDUAL',
    status: 'ACTIVE',
    firstName: 'Alice',
    lastName: 'Administrator',
    email: 'alice.admin@acmebank.com',
    phoneNumber: '+1-212-555-1001',
    dateOfBirth: date('1980-05-15'),
    nationality: 'USA',
    confidence: 1.0,
    createdAt: datetime(),
    updatedAt: datetime()
});

// Regular user for Acme Bank
CREATE (regularUser:Individual:Party {
    federatedId: 'ind-user-001',
    partyType: 'INDIVIDUAL',
    status: 'ACTIVE',
    firstName: 'Bob',
    lastName: 'User',
    email: 'bob.user@acmebank.com',
    phoneNumber: '+1-212-555-1002',
    dateOfBirth: date('1985-08-20'),
    nationality: 'USA',
    confidence: 1.0,
    createdAt: datetime(),
    updatedAt: datetime()
});

// User for Global Financial
CREATE (globalUser:Individual:Party {
    federatedId: 'ind-global-user-001',
    partyType: 'INDIVIDUAL',
    status: 'ACTIVE',
    firstName: 'Charlie',
    lastName: 'Analyst',
    email: 'charlie.analyst@globalfinancial.com',
    phoneNumber: '+1-212-555-2001',
    dateOfBirth: date('1990-03-10'),
    nationality: 'USA',
    confidence: 1.0,
    createdAt: datetime(),
    updatedAt: datetime()
});

// ============================================================================
// STEP 3: Create Source Records (Principal-to-Party Mappings)
// ============================================================================

// Map authentication principal "admin" to adminUser party
CREATE (authSourceAdmin:SourceRecord {
    id: 'src-auth-admin',
    sourceSystem: 'AUTH_SERVICE',
    sourceId: 'admin',
    sourceDataJson: '{"username":"admin","email":"alice.admin@acmebank.com"}',
    syncedAt: datetime(),
    version: 1,
    checksum: 'checksum-admin',
    masterSource: true,
    qualityScore: 1.0,
    fieldQualityScoresJson: '{}'
});

// Map authentication principal "catalog-user" to regularUser party
CREATE (authSourceUser:SourceRecord {
    id: 'src-auth-user',
    sourceSystem: 'AUTH_SERVICE',
    sourceId: 'catalog-user',
    sourceDataJson: '{"username":"catalog-user","email":"bob.user@acmebank.com"}',
    syncedAt: datetime(),
    version: 1,
    checksum: 'checksum-user',
    masterSource: true,
    qualityScore: 1.0,
    fieldQualityScoresJson: '{}'
});

// Map "test-principal-001" to regularUser for testing
CREATE (authSourceTest:SourceRecord {
    id: 'src-auth-test',
    sourceSystem: 'AUTH_SERVICE',
    sourceId: 'test-principal-001',
    sourceDataJson: '{"username":"test-principal-001","email":"test@acmebank.com"}',
    syncedAt: datetime(),
    version: 1,
    checksum: 'checksum-test',
    masterSource: true,
    qualityScore: 1.0,
    fieldQualityScoresJson: '{}'
});

// Map Global Financial user
CREATE (authSourceGlobal:SourceRecord {
    id: 'src-auth-global',
    sourceSystem: 'AUTH_SERVICE',
    sourceId: 'global-user',
    sourceDataJson: '{"username":"global-user","email":"charlie.analyst@globalfinancial.com"}',
    syncedAt: datetime(),
    version: 1,
    checksum: 'checksum-global',
    masterSource: true,
    qualityScore: 1.0,
    fieldQualityScoresJson: '{}'
});

// ============================================================================
// STEP 4: Create Relationships
// ============================================================================

// Link individuals to their organizations (EMPLOYED_BY)
MATCH (admin:Individual {federatedId: 'ind-admin-001'}),
      (acme:Organization {federatedId: 'org-acme-bank-001'})
CREATE (admin)-[:EMPLOYED_BY {
    position: 'System Administrator',
    department: 'IT',
    startDate: date('2020-01-01'),
    employmentType: 'FULL_TIME',
    status: 'ACTIVE'
}]->(acme);

MATCH (user:Individual {federatedId: 'ind-user-001'}),
      (acme:Organization {federatedId: 'org-acme-bank-001'})
CREATE (user)-[:EMPLOYED_BY {
    position: 'Product Manager',
    department: 'Product',
    startDate: date('2021-06-15'),
    employmentType: 'FULL_TIME',
    status: 'ACTIVE'
}]->(acme);

MATCH (globalUser:Individual {federatedId: 'ind-global-user-001'}),
      (global:Organization {federatedId: 'org-global-financial-001'})
CREATE (globalUser)-[:EMPLOYED_BY {
    position: 'Financial Analyst',
    department: 'Research',
    startDate: date('2022-03-01'),
    employmentType: 'FULL_TIME',
    status: 'ACTIVE'
}]->(global);

// Link source records to parties (SOURCED_FROM)
MATCH (admin:Individual {federatedId: 'ind-admin-001'}),
      (src:SourceRecord {id: 'src-auth-admin'})
CREATE (admin)-[:SOURCED_FROM]->(src);

MATCH (user:Individual {federatedId: 'ind-user-001'}),
      (src:SourceRecord {id: 'src-auth-user'})
CREATE (user)-[:SOURCED_FROM]->(src);

MATCH (user:Individual {federatedId: 'ind-user-001'}),
      (src:SourceRecord {id: 'src-auth-test'})
CREATE (user)-[:SOURCED_FROM]->(src);

MATCH (globalUser:Individual {federatedId: 'ind-global-user-001'}),
      (src:SourceRecord {id: 'src-auth-global'})
CREATE (globalUser)-[:SOURCED_FROM]->(src);

// ============================================================================
// STEP 5: Create Indexes for Performance
// ============================================================================

CREATE INDEX party_federated_id IF NOT EXISTS FOR (p:Party) ON (p.federatedId);
CREATE INDEX party_type IF NOT EXISTS FOR (p:Party) ON (p.partyType);
CREATE INDEX party_status IF NOT EXISTS FOR (p:Party) ON (p.status);
CREATE INDEX source_system_id IF NOT EXISTS FOR (s:SourceRecord) ON (s.sourceSystem, s.sourceId);
CREATE INDEX organization_lei IF NOT EXISTS FOR (o:Organization) ON (o.lei);
CREATE INDEX individual_email IF NOT EXISTS FOR (i:Individual) ON (i.email);

// ============================================================================
// STEP 6: Verification Queries
// ============================================================================

// Return summary of created data
MATCH (p:Party)
RETURN 'Total Parties' as metric, count(p) as count
UNION ALL
MATCH (o:Organization)
RETURN 'Organizations' as metric, count(o) as count
UNION ALL
MATCH (i:Individual)
RETURN 'Individuals' as metric, count(i) as count
UNION ALL
MATCH (s:SourceRecord)
RETURN 'Source Records' as metric, count(s) as count
UNION ALL
MATCH ()-[r]->()
RETURN 'Total Relationships' as metric, count(r) as count;

// Test principal-to-party resolution
MATCH (p:Party)-[:SOURCED_FROM]->(s:SourceRecord {sourceSystem: 'AUTH_SERVICE'})
RETURN s.sourceId as principalId, p.federatedId as partyId,
       p.partyType as partyType, labels(p) as labels;

// ============================================================================
// DONE: Test Data Loaded Successfully
// ============================================================================
//
// Principals created:
// - admin          → ind-admin-001       (Acme Bank Admin)
// - catalog-user   → ind-user-001        (Acme Bank User)
// - test-principal-001 → ind-user-001    (Test User)
// - global-user    → ind-global-user-001 (Global Financial User)
//
// Tenants (Organizations):
// - org-acme-bank-001        (Acme Bank - TIER_1)
// - org-global-financial-001 (Global Financial - TIER_2)
//
// Ready for context resolution testing!
// ============================================================================
