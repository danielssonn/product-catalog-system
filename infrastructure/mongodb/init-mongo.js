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
// Passwords are BCrypt encrypted with $2a$ prefix (Java BCryptPasswordEncoder compatible):
// admin123 -> $2a$10$5CtRTM8hzH5taYbBM4jHbuDOmeHCFSvbHvZOHcnX7rKHK0QDf3G8m
// catalog123 -> $2a$10$/pT2VGBxsY6d7bQ5nB.6dOA3Rc7zkge2fu.q310NiHbNooMz7kT5q
db.users.insertMany([
    {
        username: "admin",
        password: "$2a$10$5CtRTM8hzH5taYbBM4jHbuDOmeHCFSvbHvZOHcnX7rKHK0QDf3G8m",  // admin123 - Java BCrypt compatible
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
        password: "$2a$10$/pT2VGBxsY6d7bQ5nB.6dOA3Rc7zkge2fu.q310NiHbNooMz7kT5q",  // catalog123 - Java BCrypt compatible
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

// ============================================
// WORKFLOW TEMPLATES
// ============================================

// Create workflow_templates collection
db.createCollection('workflow_templates');

// Workflow template indexes
db.workflow_templates.createIndex({ "templateId": 1 }, { unique: true });
db.workflow_templates.createIndex({ "entityType": 1, "active": 1 });
print('Workflow template indexes created');

// Insert Solution Configuration workflow template
db.workflow_templates.insertOne({
    templateId: "SOLUTION_CONFIG_V1",
    entityType: "SOLUTION_CONFIGURATION",
    version: "1",
    name: "Solution Configuration Workflow",
    description: "Approval workflow for solution configuration",
    active: true,
    decisionTables: [{
        name: "Solution Approval Rules",
        hitPolicy: "FIRST",
        inputs: [
            { name: "solutionType", type: "string" },
            { name: "pricingVariance", type: "number" },
            { name: "riskLevel", type: "string" }
        ],
        outputs: [
            { name: "approvalRequired", type: "boolean" },
            { name: "approverRoles", type: "array" },
            { name: "approvalCount", type: "number" },
            { name: "isSequential", type: "boolean" },
            { name: "slaHours", type: "number" }
        ],
        rules: [
            {
                ruleId: "AUTO_APPROVE_LOW_RISK",
                priority: 1,
                conditions: {
                    solutionType: "CHECKING|SAVINGS",
                    pricingVariance: "< 5",
                    riskLevel: "LOW"
                },
                outputs: {
                    approvalRequired: false
                }
            },
            {
                ruleId: "SINGLE_APPROVAL_MEDIUM_VARIANCE",
                priority: 2,
                conditions: {
                    pricingVariance: ">= 5 && <= 15",
                    riskLevel: "LOW|MEDIUM"
                },
                outputs: {
                    approvalRequired: true,
                    approverRoles: ["PRODUCT_MANAGER"],
                    approvalCount: 1,
                    isSequential: false,
                    slaHours: 24
                }
            },
            {
                ruleId: "DUAL_APPROVAL_HIGH_VARIANCE",
                priority: 3,
                conditions: {
                    pricingVariance: "> 15",
                    riskLevel: "MEDIUM|HIGH"
                },
                outputs: {
                    approvalRequired: true,
                    approverRoles: ["PRODUCT_MANAGER", "RISK_MANAGER"],
                    approvalCount: 2,
                    isSequential: false,
                    slaHours: 48
                }
            }
        ]
    }],
    callbackHandlers: {
        onApprove: "SolutionConfigApprovalHandler",
        onReject: "SolutionConfigRejectionHandler"
    },
    createdBy: "system",
    updatedBy: "system"
});

print('Workflow template inserted');
print('MongoDB initialization completed successfully!');