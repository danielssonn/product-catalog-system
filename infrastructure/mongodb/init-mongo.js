// MongoDB initialization script
// This script creates the database, collections, and indexes

db = db.getSiblingDB('product_catalog_db');

print('Creating database: product_catalog_db');

// Create collections
db.createCollection('product_catalog');
db.createCollection('catalog_categories');
db.createCollection('products');
db.createCollection('tenant_product_configurations');
db.createCollection('product_versions');
db.createCollection('bundles');
db.createCollection('cross_sell_rules');
db.createCollection('consumers');
db.createCollection('audit_logs');
db.createCollection('api_versions');
db.createCollection('schema_versions');
db.createCollection('users');

print('Collections created successfully');

// ============================================
// CATALOG DOMAIN INDEXES
// ============================================

// Product Catalog indexes
db.product_catalog.createIndex({ "catalogProductId": 1 }, { unique: true });
db.product_catalog.createIndex({ "status": 1 });
db.product_catalog.createIndex({ "category": 1 });
db.product_catalog.createIndex({ "type": 1 });
db.product_catalog.createIndex({ "productTier": 1 });
db.product_catalog.createIndex({ "createdAt": -1 });
print('Product catalog indexes created');

// Catalog Categories indexes
db.catalog_categories.createIndex({ "categoryId": 1 }, { unique: true });
db.catalog_categories.createIndex({ "parentCategoryId": 1 });
db.catalog_categories.createIndex({ "active": 1 });
print('Catalog categories indexes created');

// ============================================
// PRODUCT DOMAIN INDEXES
// ============================================

// Products indexes (Tenant active products)
db.products.createIndex({ "tenantId": 1, "productId": 1 }, { unique: true });
db.products.createIndex({ "tenantId": 1, "status": 1 });
db.products.createIndex({ "tenantId": 1, "catalogProductId": 1 });
db.products.createIndex({ "tenantId": 1, "configurationId": 1 });
db.products.createIndex({ "tenantId": 1, "category": 1 });
db.products.createIndex({ "catalogProductId": 1 });
db.products.createIndex({ "effectiveDate": 1 });
db.products.createIndex({ "expirationDate": 1 });
print('Products indexes created');

// Tenant Product Configurations indexes
db.tenant_product_configurations.createIndex({ "tenantId": 1, "configurationId": 1 }, { unique: true });
db.tenant_product_configurations.createIndex({ "tenantId": 1, "catalogProductId": 1 });
db.tenant_product_configurations.createIndex({ "tenantId": 1, "status": 1 });
db.tenant_product_configurations.createIndex({ "catalogProductId": 1 });
db.tenant_product_configurations.createIndex({ "status": 1 });
print('Tenant product configurations indexes created');

// Product Versions indexes
db.product_versions.createIndex({ "tenantId": 1, "productId": 1, "versionNumber": 1 }, { unique: true });
db.product_versions.createIndex({ "tenantId": 1, "productId": 1 });
db.product_versions.createIndex({ "createdAt": -1 });
print('Product versions indexes created');

// ============================================
// BUNDLE DOMAIN INDEXES
// ============================================

// Bundles indexes
db.bundles.createIndex({ "tenantId": 1, "bundleId": 1 }, { unique: true });
db.bundles.createIndex({ "tenantId": 1, "status": 1 });
db.bundles.createIndex({ "tenantId": 1 });
print('Bundles indexes created');

// ============================================
// CROSS-SELL DOMAIN INDEXES
// ============================================

// Cross-sell rules indexes
db.cross_sell_rules.createIndex({ "tenantId": 1, "ruleId": 1 }, { unique: true });
db.cross_sell_rules.createIndex({ "tenantId": 1, "status": 1 });
db.cross_sell_rules.createIndex({ "tenantId": 1, "sourceProductId": 1 });
print('Cross-sell rules indexes created');

// ============================================
// AUDIT AND VERSIONING INDEXES
// ============================================

// Audit logs indexes
db.audit_logs.createIndex({ "tenantId": 1, "createdAt": -1 });
db.audit_logs.createIndex({ "entityType": 1, "entityId": 1 });
db.audit_logs.createIndex({ "userId": 1 });
db.audit_logs.createIndex({ "action": 1 });
db.audit_logs.createIndex({ "createdAt": -1 });
print('Audit logs indexes created');

// Consumers indexes
db.consumers.createIndex({ "tenantId": 1, "consumerId": 1 }, { unique: true });
db.consumers.createIndex({ "apiKey": 1 }, { unique: true });
print('Consumers indexes created');

// API Versions indexes
db.api_versions.createIndex({ "version": 1 }, { unique: true });
db.api_versions.createIndex({ "status": 1 });
print('API versions indexes created');

// Schema Versions indexes
db.schema_versions.createIndex({ "entityType": 1, "version": 1 }, { unique: true });
print('Schema versions indexes created');

print('All indexes created successfully');

// ============================================
// SAMPLE DATA (Optional)
// ============================================

// Insert sample catalog categories
db.catalog_categories.insertMany([
    {
        categoryId: "deposit-accounts",
        name: "Deposit Accounts",
        description: "Checking, Savings, and Money Market Accounts",
        parentCategoryId: null,
        subCategories: ["checking", "savings", "money-market"],
        displayOrder: 1,
        active: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        categoryId: "checking",
        name: "Checking Accounts",
        description: "Personal and Business Checking Accounts",
        parentCategoryId: "deposit-accounts",
        subCategories: [],
        displayOrder: 1,
        active: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        categoryId: "savings",
        name: "Savings Accounts",
        description: "High-Yield Savings Accounts",
        parentCategoryId: "deposit-accounts",
        subCategories: [],
        displayOrder: 2,
        active: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        categoryId: "lending",
        name: "Lending Products",
        description: "Loans, Mortgages, and Credit Cards",
        parentCategoryId: null,
        subCategories: ["personal-loans", "mortgages", "credit-cards"],
        displayOrder: 2,
        active: true,
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        categoryId: "cash-management",
        name: "Cash Management",
        description: "Treasury and Cash Management Services",
        parentCategoryId: null,
        subCategories: [],
        displayOrder: 3,
        active: true,
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

print('Sample categories inserted');

// Insert sample catalog products
db.product_catalog.insertMany([
    {
        catalogProductId: "premium-checking-001",
        name: "Premium Checking Account",
        description: "Feature-rich checking account with premium benefits",
        category: "checking",
        type: "CHECKING_ACCOUNT",
        status: "AVAILABLE",
        pricingTemplate: {
            pricingType: "TIERED",
            currency: "USD",
            minMonthlyFee: 0,
            maxMonthlyFee: 25.00,
            defaultMonthlyFee: 15.00,
            minBalance: 1000.00,
            recommendedMinBalance: 2500.00,
            allowCustomPricing: true,
            availableFees: [
                {
                    feeType: "OVERDRAFT",
                    description: "Overdraft fee",
                    minAmount: 25.00,
                    maxAmount: 35.00,
                    defaultAmount: 30.00,
                    frequency: "PER_OCCURRENCE",
                    waivable: true,
                    waiverConditions: "Fee waived with minimum balance of $5000",
                    required: false
                }
            ]
        },
        availableFeatures: {
            "atmAccess": true,
            "onlineBanking": true,
            "mobileBanking": true,
            "billPay": true,
            "overdraftProtection": true,
            "checkWriting": true
        },
        defaultTerms: {
            termsAndConditionsUrl: "https://example.com/terms/premium-checking",
            disclosureUrl: "https://example.com/disclosures/premium-checking",
            allowEarlyWithdrawal: true,
            restrictions: ["Minimum age 18", "Valid ID required"],
            benefits: ["No foreign transaction fees", "ATM fee reimbursement", "Free checks"]
        },
        configOptions: {
            canCustomizeName: true,
            canCustomizeDescription: true,
            canCustomizePricing: true,
            canCustomizeFeatures: true,
            canCustomizeTerms: false,
            canCustomizeEligibility: true,
            canSelectChannels: true,
            configurableFields: ["monthlyFee", "minimumBalance", "features"]
        },
        supportedChannels: ["WEB", "MOBILE", "BRANCH", "API"],
        defaultEligibilityCriteria: ["Age 18 or older", "Valid government ID", "US resident"],
        complianceTags: ["FDIC_INSURED", "REGULATION_D", "REGULATION_E"],
        productTier: "PREMIUM",
        requiresApproval: false,
        documentationUrl: "https://example.com/docs/premium-checking",
        relatedProducts: ["premium-savings-001", "overdraft-line-001"],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        catalogProductId: "high-yield-savings-001",
        name: "High-Yield Savings Account",
        description: "Competitive interest rates with no monthly fees",
        category: "savings",
        type: "SAVINGS_ACCOUNT",
        status: "AVAILABLE",
        pricingTemplate: {
            pricingType: "TIERED",
            currency: "USD",
            minInterestRate: 0.50,
            maxInterestRate: 4.50,
            defaultInterestRate: 3.00,
            interestType: "APY",
            minMonthlyFee: 0,
            maxMonthlyFee: 0,
            defaultMonthlyFee: 0,
            minBalance: 100.00,
            recommendedMinBalance: 1000.00,
            allowCustomPricing: true,
            rateTierTemplates: [
                {
                    tierName: "Tier 1",
                    suggestedMinBalance: 0,
                    suggestedMaxBalance: 10000,
                    minInterestRate: 0.50,
                    maxInterestRate: 2.00,
                    defaultInterestRate: 1.00
                },
                {
                    tierName: "Tier 2",
                    suggestedMinBalance: 10000,
                    suggestedMaxBalance: 50000,
                    minInterestRate: 2.00,
                    maxInterestRate: 3.50,
                    defaultInterestRate: 2.50
                },
                {
                    tierName: "Tier 3",
                    suggestedMinBalance: 50000,
                    suggestedMaxBalance: null,
                    minInterestRate: 3.00,
                    maxInterestRate: 4.50,
                    defaultInterestRate: 3.50
                }
            ]
        },
        availableFeatures: {
            "onlineBanking": true,
            "mobileBanking": true,
            "atmAccess": true,
            "autoSave": true,
            "savingsGoals": true
        },
        defaultTerms: {
            termsAndConditionsUrl: "https://example.com/terms/high-yield-savings",
            disclosureUrl: "https://example.com/disclosures/high-yield-savings",
            allowEarlyWithdrawal: true,
            restrictions: ["Maximum 6 withdrawals per month per Regulation D"],
            benefits: ["Competitive APY", "No monthly maintenance fees", "FDIC insured"]
        },
        configOptions: {
            canCustomizeName: true,
            canCustomizeDescription: true,
            canCustomizePricing: true,
            canCustomizeFeatures: true,
            canCustomizeTerms: false,
            canCustomizeEligibility: true,
            canSelectChannels: true,
            configurableFields: ["interestRate", "rateTiers", "minimumBalance"]
        },
        supportedChannels: ["WEB", "MOBILE", "BRANCH", "API"],
        defaultEligibilityCriteria: ["Age 18 or older", "Valid government ID"],
        complianceTags: ["FDIC_INSURED", "REGULATION_D"],
        productTier: "STANDARD",
        requiresApproval: false,
        documentationUrl: "https://example.com/docs/high-yield-savings",
        relatedProducts: ["premium-checking-001", "cd-12month-001"],
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    }
]);

print('Sample catalog products inserted');

// ============================================
// USERS COLLECTION
// ============================================

// Users indexes
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 });
db.users.createIndex({ "enabled": 1 });
print('Users indexes created');

// Insert sample users
// Passwords are BCrypt encrypted:
// admin123 -> $2b$12$oOqJDPOcNgzRiC7k1mZhDOLtc1fmYyN6/A5jteJUunr2J5MoETG3C
// catalog123 -> $2b$12$TsbLQqnpJTYu3noHg.rA2um4jrhEoMzt3NmCReOpxgn7rrd2m5rtG
db.users.insertMany([
    {
        username: "admin",
        password: "$2b$12$oOqJDPOcNgzRiC7k1mZhDOLtc1fmYyN6/A5jteJUunr2J5MoETG3C",
        email: "admin@example.com",
        fullName: "System Administrator",
        roles: ["ROLE_ADMIN", "ROLE_USER"],
        enabled: true,
        accountNonExpired: true,
        accountNonLocked: true,
        credentialsNonExpired: true,
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        username: "catalog-user",
        password: "$2b$12$TsbLQqnpJTYu3noHg.rA2um4jrhEoMzt3NmCReOpxgn7rrd2m5rtG",
        email: "catalog-user@example.com",
        fullName: "Catalog User",
        roles: ["ROLE_USER"],
        enabled: true,
        accountNonExpired: true,
        accountNonLocked: true,
        credentialsNonExpired: true,
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    }
]);

print('Sample users inserted');
print('MongoDB initialization completed successfully!');