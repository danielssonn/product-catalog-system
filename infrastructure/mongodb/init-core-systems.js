// MongoDB initialization script for core banking system test data
// This script sets up mock core system mappings for testing provisioning

db = db.getSiblingDB('product_catalog_db');

print('Initializing core banking system test data...');

// 1. Create tenant_core_mappings collection with test data
print('Creating tenant_core_mappings collection...');

db.tenant_core_mappings.deleteMany({});

db.tenant_core_mappings.insertMany([
  {
    _id: ObjectId(),
    tenantId: "tenant-001",
    coreSystems: [
      {
        coreSystemId: "temenos-us-east-1",
        coreSystemType: "TEMENOS_T24",
        active: true,
        priority: 100,
        region: "US-EAST",
        supportedProductTypes: ["CHECKING", "SAVINGS"],
        config: {
          apiEndpoint: "http://localhost:9190/mock-temenos-api",
          apiKey: "test-api-key-temenos-001",
          connectionTimeoutMs: 30000,
          readTimeoutMs: 60000,
          useSsl: false,
          environment: "TEST"
        },
        addedAt: new Date()
      },
      {
        coreSystemId: "finacle-eu-west-1",
        coreSystemType: "FINACLE",
        active: true,
        priority: 90,
        region: "EU-WEST",
        supportedProductTypes: [],
        config: {
          apiEndpoint: "http://localhost:9191/mock-finacle-api",
          username: "catalog-service",
          password: "test-password",
          connectionTimeoutMs: 30000,
          readTimeoutMs: 60000,
          useSsl: false,
          environment: "TEST"
        },
        addedAt: new Date()
      }
    ],
    defaultCoreSystemId: "temenos-us-east-1",
    createdAt: new Date(),
    updatedAt: new Date(),
    updatedBy: "system"
  },
  {
    _id: ObjectId(),
    tenantId: "tenant-002",
    coreSystems: [
      {
        coreSystemId: "fis-us-west-1",
        coreSystemType: "FIS_PROFILE",
        active: true,
        priority: 100,
        region: "US-WEST",
        supportedProductTypes: ["CHECKING", "SAVINGS", "LOAN"],
        config: {
          apiEndpoint: "http://localhost:9192/mock-fis-api",
          apiKey: "test-api-key-fis-001",
          connectionTimeoutMs: 30000,
          readTimeoutMs: 60000,
          useSsl: false,
          environment: "TEST"
        },
        addedAt: new Date()
      }
    ],
    defaultCoreSystemId: "fis-us-west-1",
    createdAt: new Date(),
    updatedAt: new Date(),
    updatedBy: "system"
  },
  {
    _id: ObjectId(),
    tenantId: "acme-bank",
    coreSystems: [
      {
        coreSystemId: "temenos-prod-us",
        coreSystemType: "TEMENOS_T24",
        active: true,
        priority: 100,
        region: "US-EAST",
        supportedProductTypes: ["CHECKING", "SAVINGS", "CREDIT_CARD"],
        config: {
          apiEndpoint: "http://localhost:9190/mock-temenos-api",
          apiKey: "acme-temenos-key-123",
          connectionTimeoutMs: 30000,
          readTimeoutMs: 60000,
          useSsl: false,
          environment: "TEST",
          additionalConfig: {
            institutionId: "ACME001",
            branchCode: "001"
          }
        },
        addedAt: new Date()
      },
      {
        coreSystemId: "temenos-prod-eu",
        coreSystemType: "TEMENOS_T24",
        active: true,
        priority: 90,
        region: "EU-WEST",
        supportedProductTypes: ["CHECKING", "SAVINGS"],
        config: {
          apiEndpoint: "http://localhost:9190/mock-temenos-api",
          apiKey: "acme-temenos-eu-key-456",
          connectionTimeoutMs: 30000,
          readTimeoutMs: 60000,
          useSsl: false,
          environment: "TEST",
          additionalConfig: {
            institutionId: "ACME002",
            branchCode: "002"
          }
        },
        addedAt: new Date()
      }
    ],
    defaultCoreSystemId: "temenos-prod-us",
    createdAt: new Date(),
    updatedAt: new Date(),
    updatedBy: "system"
  }
]);

print('Created ' + db.tenant_core_mappings.countDocuments() + ' tenant core mappings');

// 2. Create mock_core_products collection to simulate core banking system responses
print('Creating mock_core_products collection...');

db.mock_core_products.deleteMany({});

// Create indexes
db.mock_core_products.createIndex({ coreSystemId: 1, coreProductId: 1 }, { unique: true });
db.mock_core_products.createIndex({ catalogSolutionId: 1 });
db.mock_core_products.createIndex({ tenantId: 1 });

print('Created mock_core_products collection with indexes');

// 3. Add sample test solutions ready for provisioning
print('Adding test solutions...');

db.solutions.insertMany([
  {
    _id: ObjectId(),
    tenantId: "tenant-001",
    solutionId: "SOL-TEST-001",
    catalogProductId: "cat-checking-001",
    configurationId: "config-001",
    name: "Premium Checking Account",
    description: "Full-featured checking account with overdraft protection",
    category: "CHECKING",
    status: "ACTIVE",
    pricing: {
      pricingType: "MONTHLY",
      basePrice: NumberDecimal("0.00"),
      currency: "USD",
      monthlyFee: NumberDecimal("15.00"),
      minimumBalance: NumberDecimal("1000.00"),
      transactionFee: NumberDecimal("0.00"),
      additionalFees: []
    },
    availableChannels: ["WEB", "MOBILE", "BRANCH", "ATM"],
    features: {
      overdraftProtection: true,
      debitCard: true,
      onlineBanking: true,
      mobileBanking: true,
      billPay: true,
      mobileDeposit: true
    },
    eligibilityCriteria: ["Age >= 18", "US Resident", "Valid SSN"],
    terms: {
      minimumAge: 18,
      termsUrl: "https://bank.com/terms/checking",
      disclosureUrl: "https://bank.com/disclosures/checking"
    },
    version: "1.0",
    versionNumber: 1,
    effectiveDate: new Date(),
    createdAt: new Date(),
    updatedAt: new Date(),
    createdBy: "system",
    updatedBy: "system",
    approvalRequired: false,
    coreProvisioningRecords: []
  },
  {
    _id: ObjectId(),
    tenantId: "tenant-001",
    solutionId: "SOL-TEST-002",
    catalogProductId: "cat-savings-001",
    configurationId: "config-002",
    name: "High-Yield Savings Account",
    description: "Competitive interest rates with no monthly fees",
    category: "SAVINGS",
    status: "ACTIVE",
    pricing: {
      pricingType: "INTEREST_BEARING",
      currency: "USD",
      interestRate: NumberDecimal("4.50"),
      interestType: "APY",
      monthlyFee: NumberDecimal("0.00"),
      minimumBalance: NumberDecimal("500.00")
    },
    availableChannels: ["WEB", "MOBILE", "BRANCH"],
    features: {
      onlineBanking: true,
      mobileBanking: true,
      autoSave: true,
      roundUpSavings: true
    },
    eligibilityCriteria: ["Age >= 18"],
    terms: {
      minimumAge: 18,
      termsUrl: "https://bank.com/terms/savings",
      interestCompounding: "MONTHLY"
    },
    version: "1.0",
    versionNumber: 1,
    effectiveDate: new Date(),
    createdAt: new Date(),
    updatedAt: new Date(),
    createdBy: "system",
    updatedBy: "system",
    approvalRequired: false,
    coreProvisioningRecords: []
  },
  {
    _id: ObjectId(),
    tenantId: "acme-bank",
    solutionId: "SOL-ACME-001",
    catalogProductId: "cat-checking-premium",
    configurationId: "config-acme-001",
    name: "ACME Premium Checking",
    description: "Premium checking with global ATM access",
    category: "CHECKING",
    status: "ACTIVE",
    pricing: {
      pricingType: "MONTHLY",
      currency: "USD",
      monthlyFee: NumberDecimal("25.00"),
      minimumBalance: NumberDecimal("5000.00"),
      transactionFee: NumberDecimal("0.00")
    },
    availableChannels: ["WEB", "MOBILE", "BRANCH", "ATM"],
    features: {
      overdraftProtection: true,
      debitCard: true,
      premiumDebitCard: true,
      onlineBanking: true,
      mobileBanking: true,
      internationalAccess: true,
      conciergeService: true
    },
    eligibilityCriteria: ["Age >= 21", "Annual Income >= $75000"],
    terms: {
      minimumAge: 21,
      termsUrl: "https://acmebank.com/terms/premium-checking"
    },
    version: "1.0",
    versionNumber: 1,
    effectiveDate: new Date(),
    createdAt: new Date(),
    updatedAt: new Date(),
    createdBy: "admin",
    updatedBy: "admin",
    approvalRequired: false,
    metadata: {
      region: "US-EAST",
      productLine: "Premium"
    },
    coreProvisioningRecords: []
  }
]);

print('Created ' + db.solutions.countDocuments({ coreProvisioningRecords: { $size: 0 } }) + ' test solutions ready for provisioning');

// 4. Create indexes on tenant_core_mappings
print('Creating indexes on tenant_core_mappings...');
db.tenant_core_mappings.createIndex({ tenantId: 1 }, { unique: true });
db.tenant_core_mappings.createIndex({ "coreSystems.coreSystemId": 1 });
db.tenant_core_mappings.createIndex({ "coreSystems.coreSystemType": 1 });

print('');
print('===================================================================');
print('Core Banking System Test Data Initialized Successfully!');
print('===================================================================');
print('');
print('Tenant Core Mappings:');
print('  - tenant-001: 2 cores (Temenos US-EAST, Finacle EU-WEST)');
print('  - tenant-002: 1 core (FIS US-WEST)');
print('  - acme-bank: 2 cores (Temenos US + EU)');
print('');
print('Test Solutions (Ready for Provisioning):');
print('  - SOL-TEST-001: Premium Checking (tenant-001)');
print('  - SOL-TEST-002: High-Yield Savings (tenant-001)');
print('  - SOL-ACME-001: ACME Premium Checking (acme-bank, region: US-EAST)');
print('');
print('Mock Core API Endpoints:');
print('  - Temenos T24: http://localhost:9190/mock-temenos-api');
print('  - Finacle:     http://localhost:9191/mock-finacle-api');
print('  - FIS Profile: http://localhost:9192/mock-fis-api');
print('');
print('Next Steps:');
print('  1. Start mock core API servers (or use WireMock)');
print('  2. Restart product-service to enable change stream listener');
print('  3. Test auto-provisioning or call orchestrator manually');
print('===================================================================');
