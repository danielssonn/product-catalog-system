// MongoDB initialization script for Party Systems
// This creates Commercial Banking and Capital Markets databases with sample party data

print('========================================');
print('Initializing Party System Databases');
print('========================================');

// ============================================
// COMMERCIAL BANKING DATABASE
// ============================================

db = db.getSiblingDB('commercial_banking');
print('Creating database: commercial_banking');

// Create collection
db.createCollection('commercial_parties');

// Create indexes
db.commercial_parties.createIndex({ "partyId": 1 }, { unique: true });
db.commercial_parties.createIndex({ "registrationNumber": 1 });
db.commercial_parties.createIndex({ "tier": 1 });
db.commercial_parties.createIndex({ "riskRating": 1 });
db.commercial_parties.createIndex({ "legalName": "text", "businessName": "text" });
print('Commercial Banking indexes created');

// Insert sample commercial banking parties
db.commercial_parties.insertMany([
    {
        partyId: "CB-001",
        legalName: "Apple Inc.",
        businessName: "Apple",
        registrationNumber: "C0806592",
        jurisdiction: "California",
        incorporationDate: ISODate("1977-01-03"),
        industry: "Technology Hardware & Equipment",
        industryCode: "334220",
        tier: "TIER_1",
        riskRating: "LOW",
        amlStatus: "CLEARED",
        registeredAddress: {
            street: "One Apple Park Way",
            city: "Cupertino",
            state: "CA",
            postalCode: "95014",
            country: "United States",
            countryCode: "US"
        },
        mailingAddress: {
            street: "One Apple Park Way",
            city: "Cupertino",
            state: "CA",
            postalCode: "95014",
            country: "United States",
            countryCode: "US"
        },
        primaryContact: "Tim Cook",
        phoneNumber: "+1-408-996-1010",
        email: "corporate@apple.com",
        website: "https://www.apple.com",
        employeeCount: 164000,
        annualRevenue: 394328.0,
        accountManager: "Sarah Johnson",
        relationship: "PRIMARY",
        productTypes: ["CHECKING", "SAVINGS", "TREASURY_MGMT", "CREDIT_LINE"],
        subsidiaries: [],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        partyId: "CB-002",
        legalName: "Goldman Sachs Group, Inc.",
        businessName: "Goldman Sachs",
        registrationNumber: "2923466",
        jurisdiction: "Delaware",
        incorporationDate: ISODate("1999-05-07"),
        industry: "Investment Banking & Brokerage",
        industryCode: "523110",
        tier: "TIER_1",
        riskRating: "LOW",
        amlStatus: "CLEARED",
        registeredAddress: {
            street: "200 West Street",
            city: "New York",
            state: "NY",
            postalCode: "10282",
            country: "United States",
            countryCode: "US"
        },
        mailingAddress: {
            street: "200 West Street",
            city: "New York",
            state: "NY",
            postalCode: "10282",
            country: "United States",
            countryCode: "US"
        },
        primaryContact: "David Solomon",
        phoneNumber: "+1-212-902-1000",
        email: "corporate@gs.com",
        website: "https://www.goldmansachs.com",
        employeeCount: 45000,
        annualRevenue: 47365.0,
        accountManager: "Michael Chen",
        relationship: "PRIMARY",
        productTypes: ["CHECKING", "TREASURY_MGMT", "CREDIT_LINE", "DERIVATIVES"],
        subsidiaries: [],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        partyId: "CB-003",
        legalName: "Microsoft Corporation",
        businessName: "Microsoft",
        registrationNumber: "6004394",
        jurisdiction: "Washington",
        incorporationDate: ISODate("1981-06-25"),
        industry: "Systems Software",
        industryCode: "511210",
        tier: "TIER_1",
        riskRating: "LOW",
        amlStatus: "CLEARED",
        registeredAddress: {
            street: "One Microsoft Way",
            city: "Redmond",
            state: "WA",
            postalCode: "98052",
            country: "United States",
            countryCode: "US"
        },
        mailingAddress: {
            street: "One Microsoft Way",
            city: "Redmond",
            state: "WA",
            postalCode: "98052",
            country: "United States",
            countryCode: "US"
        },
        primaryContact: "Satya Nadella",
        phoneNumber: "+1-425-882-8080",
        email: "corporate@microsoft.com",
        website: "https://www.microsoft.com",
        employeeCount: 221000,
        annualRevenue: 211915.0,
        accountManager: "Jennifer Liu",
        relationship: "PRIMARY",
        productTypes: ["CHECKING", "SAVINGS", "TREASURY_MGMT", "CREDIT_LINE"],
        subsidiaries: [],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        partyId: "CB-004",
        legalName: "Tesla, Inc.",
        businessName: "Tesla",
        registrationNumber: "C3910393",
        jurisdiction: "Delaware",
        incorporationDate: ISODate("2003-07-01"),
        industry: "Automobile Manufacturers",
        industryCode: "336110",
        tier: "TIER_1",
        riskRating: "MEDIUM",
        amlStatus: "CLEARED",
        registeredAddress: {
            street: "13101 Harold Green Road",
            city: "Austin",
            state: "TX",
            postalCode: "78725",
            country: "United States",
            countryCode: "US"
        },
        mailingAddress: {
            street: "13101 Harold Green Road",
            city: "Austin",
            state: "TX",
            postalCode: "78725",
            country: "United States",
            countryCode: "US"
        },
        primaryContact: "Elon Musk",
        phoneNumber: "+1-512-516-8177",
        email: "ir@tesla.com",
        website: "https://www.tesla.com",
        employeeCount: 127855,
        annualRevenue: 96773.0,
        accountManager: "Robert Garcia",
        relationship: "PRIMARY",
        productTypes: ["CHECKING", "TREASURY_MGMT", "CREDIT_LINE"],
        subsidiaries: [],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        partyId: "CB-005",
        legalName: "JPMorgan Chase & Co.",
        businessName: "JP Morgan",
        registrationNumber: "2687726",
        jurisdiction: "Delaware",
        incorporationDate: ISODate("1968-10-28"),
        industry: "Diversified Banks",
        industryCode: "522110",
        tier: "TIER_1",
        riskRating: "LOW",
        amlStatus: "CLEARED",
        registeredAddress: {
            street: "383 Madison Avenue",
            city: "New York",
            state: "NY",
            postalCode: "10179",
            country: "United States",
            countryCode: "US"
        },
        mailingAddress: {
            street: "383 Madison Avenue",
            city: "New York",
            state: "NY",
            postalCode: "10179",
            country: "United States",
            countryCode: "US"
        },
        primaryContact: "Jamie Dimon",
        phoneNumber: "+1-212-270-6000",
        email: "corporate@jpmchase.com",
        website: "https://www.jpmorganchase.com",
        employeeCount: 293723,
        annualRevenue: 158101.0,
        accountManager: "Amanda Williams",
        relationship: "PRIMARY",
        productTypes: ["CHECKING", "SAVINGS", "TREASURY_MGMT", "CREDIT_LINE", "DERIVATIVES"],
        subsidiaries: [],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    }
]);

print('Commercial Banking: Inserted 5 sample parties');

// Create user for commercial banking
db = db.getSiblingDB('admin');
db.createUser({
    user: 'commercialuser',
    pwd: 'commercialpass',
    roles: [
        { role: 'readWrite', db: 'commercial_banking' }
    ]
});
print('Commercial Banking user created');

// ============================================
// CAPITAL MARKETS DATABASE
// ============================================

db = db.getSiblingDB('capital_markets');
print('Creating database: capital_markets');

// Create collection
db.createCollection('counterparties');

// Create indexes
db.counterparties.createIndex({ "counterpartyId": 1 }, { unique: true });
db.counterparties.createIndex({ "lei": 1 }, { unique: true, sparse: true });
db.counterparties.createIndex({ "riskRating": 1 });
db.counterparties.createIndex({ "counterpartyType": 1 });
db.counterparties.createIndex({ "legalName": "text" });
print('Capital Markets indexes created');

// Insert sample capital markets counterparties
// Note: Some overlap with Commercial Banking (simulates same entities in different systems)
db.counterparties.insertMany([
    {
        counterpartyId: "CM-001",
        legalName: "Apple Inc.",
        lei: "HWUPKR0MPOU8FGXBT394",
        jurisdiction: "United States",
        jurisdictionCode: "US",
        riskRating: "AA",
        internalRating: "A1",
        exposureLimit: 5000.0,
        currentExposure: 2340.5,
        productTypes: ["FX", "DERIVATIVES", "FIXED_INCOME"],
        creditRating: "AA+",
        creditRatingAgency: "S&P",
        tradingRegions: ["AMERICAS", "EMEA", "APAC"],
        counterpartyType: "CORPORATE",
        relationshipManager: "David Kim",
        salesCoverage: "Tech Team",
        productExposures: {
            "FX": 1200.0,
            "DERIVATIVES": 890.5,
            "FIXED_INCOME": 250.0
        },
        isPrimaryDealer: false,
        isQualifiedCounterparty: true,
        settlementInstructions: "SWIFT: AAPL123",
        authorizedTraders: ["trader1@apple.com", "trader2@apple.com"],
        kycStatus: "APPROVED",
        kycExpiryDate: ISODate("2026-12-31"),
        sanctionsScreening: "CLEAR",
        lastScreeningDate: new Date(),
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        counterpartyId: "CM-002",
        legalName: "Goldman Sachs Group Inc.",
        lei: "784F5XWPLTWKTBV3E584",
        jurisdiction: "United States",
        jurisdictionCode: "US",
        riskRating: "AA",
        internalRating: "A1",
        exposureLimit: 15000.0,
        currentExposure: 8945.2,
        productTypes: ["DERIVATIVES", "FX", "FIXED_INCOME", "EQUITY"],
        creditRating: "A+",
        creditRatingAgency: "S&P",
        tradingRegions: ["AMERICAS", "EMEA", "APAC"],
        counterpartyType: "BANK",
        relationshipManager: "Laura Martinez",
        salesCoverage: "Financial Institutions",
        productExposures: {
            "DERIVATIVES": 4500.0,
            "FX": 2345.2,
            "FIXED_INCOME": 1800.0,
            "EQUITY": 300.0
        },
        isPrimaryDealer: true,
        isQualifiedCounterparty: true,
        settlementInstructions: "SWIFT: GSUS33XXX",
        authorizedTraders: ["desk1@gs.com", "desk2@gs.com", "desk3@gs.com"],
        kycStatus: "APPROVED",
        kycExpiryDate: ISODate("2026-06-30"),
        sanctionsScreening: "CLEAR",
        lastScreeningDate: new Date(),
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        counterpartyId: "CM-003",
        legalName: "Microsoft Corporation",
        lei: "INR2EJN1ERAN0W5ZP974",
        jurisdiction: "United States",
        jurisdictionCode: "US",
        riskRating: "AAA",
        internalRating: "A1",
        exposureLimit: 6000.0,
        currentExposure: 3125.8,
        productTypes: ["FX", "DERIVATIVES", "FIXED_INCOME"],
        creditRating: "AAA",
        creditRatingAgency: "Moody's",
        tradingRegions: ["AMERICAS", "EMEA", "APAC"],
        counterpartyType: "CORPORATE",
        relationshipManager: "Eric Thompson",
        salesCoverage: "Tech Team",
        productExposures: {
            "FX": 1800.0,
            "DERIVATIVES": 1025.8,
            "FIXED_INCOME": 300.0
        },
        isPrimaryDealer: false,
        isQualifiedCounterparty: true,
        settlementInstructions: "SWIFT: MSFT456",
        authorizedTraders: ["treasury1@microsoft.com", "treasury2@microsoft.com"],
        kycStatus: "APPROVED",
        kycExpiryDate: ISODate("2027-03-31"),
        sanctionsScreening: "CLEAR",
        lastScreeningDate: new Date(),
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        counterpartyId: "CM-004",
        legalName: "JPMorgan Chase & Co.",
        lei: "8I5DZWZKVSZI1NUHU748",
        jurisdiction: "United States",
        jurisdictionCode: "US",
        riskRating: "AA",
        internalRating: "A1",
        exposureLimit: 20000.0,
        currentExposure: 12456.7,
        productTypes: ["DERIVATIVES", "FX", "FIXED_INCOME", "EQUITY", "COMMODITIES"],
        creditRating: "A+",
        creditRatingAgency: "Fitch",
        tradingRegions: ["AMERICAS", "EMEA", "APAC"],
        counterpartyType: "BANK",
        relationshipManager: "Rachel Green",
        salesCoverage: "Financial Institutions",
        productExposures: {
            "DERIVATIVES": 6000.0,
            "FX": 3456.7,
            "FIXED_INCOME": 2000.0,
            "EQUITY": 800.0,
            "COMMODITIES": 200.0
        },
        isPrimaryDealer: true,
        isQualifiedCounterparty: true,
        settlementInstructions: "SWIFT: CHASUS33XXX",
        authorizedTraders: ["trading1@jpmc.com", "trading2@jpmc.com", "trading3@jpmc.com"],
        kycStatus: "APPROVED",
        kycExpiryDate: ISODate("2026-09-30"),
        sanctionsScreening: "CLEAR",
        lastScreeningDate: new Date(),
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        counterpartyId: "CM-005",
        legalName: "Citadel LLC",
        lei: "549300JE90ZSHPBXNH35",
        jurisdiction: "United States",
        jurisdictionCode: "US",
        riskRating: "A",
        internalRating: "A2",
        exposureLimit: 8000.0,
        currentExposure: 5678.3,
        productTypes: ["DERIVATIVES", "EQUITY", "FX", "FIXED_INCOME"],
        creditRating: "A",
        creditRatingAgency: "S&P",
        tradingRegions: ["AMERICAS", "EMEA", "APAC"],
        counterpartyType: "HEDGE_FUND",
        relationshipManager: "James Wilson",
        salesCoverage: "Hedge Fund Team",
        productExposures: {
            "DERIVATIVES": 3000.0,
            "EQUITY": 1678.3,
            "FX": 800.0,
            "FIXED_INCOME": 200.0
        },
        isPrimaryDealer: false,
        isQualifiedCounterparty: true,
        settlementInstructions: "SWIFT: CITAUS33XXX",
        authorizedTraders: ["trader1@citadel.com", "trader2@citadel.com"],
        kycStatus: "APPROVED",
        kycExpiryDate: ISODate("2026-11-30"),
        sanctionsScreening: "CLEAR",
        lastScreeningDate: new Date(),
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    }
]);

print('Capital Markets: Inserted 5 sample counterparties');

// Create user for capital markets
db = db.getSiblingDB('admin');
db.createUser({
    user: 'capitaluser',
    pwd: 'capitalpass',
    roles: [
        { role: 'readWrite', db: 'capital_markets' }
    ]
});
print('Capital Markets user created');

print('========================================');
print('Party System Initialization Complete!');
print('========================================');
print('');
print('Summary:');
print('- Commercial Banking: 5 parties');
print('- Capital Markets: 5 counterparties');
print('- 3 entities overlap (Apple, Goldman, Microsoft, JPMorgan)');
print('- These will be auto-merged by the federated system based on LEI');
print('');
