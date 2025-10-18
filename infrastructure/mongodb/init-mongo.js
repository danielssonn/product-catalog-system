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
db.createCollection('entitlements');  // Fine-grained access control

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

// ============================================
// ENTITLEMENTS DOMAIN INDEXES (Fine-Grained Access Control)
// ============================================

// Entitlement indexes - optimized for fast authorization lookups
// Critical: These indexes MUST exist for performance
db.entitlements.createIndex(
    { "tenantId": 1, "partyId": 1, "resourceType": 1 },
    { name: "tenant_party_resource_idx" }
);
db.entitlements.createIndex(
    { "tenantId": 1, "resourceType": 1, "resourceId": 1 },
    { name: "tenant_resource_idx" }
);
db.entitlements.createIndex(
    { "tenantId": 1, "source": 1 },
    { name: "tenant_source_idx" }
);
db.entitlements.createIndex(
    { "expiresAt": 1 },
    { name: "expiry_idx" }
);
db.entitlements.createIndex(
    { "tenantId": 1, "partyId": 1, "active": 1 },
    { name: "tenant_party_active_idx" }
);
print('Entitlements indexes created');

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

// ============================================
// PRODUCT TYPE DEFINITIONS (Data-Driven)
// ============================================

// Create product_types collection
db.createCollection('product_types');
db.product_types.createIndex({ "typeCode": 1 }, { unique: true });
db.product_types.createIndex({ "category": 1 });
db.product_types.createIndex({ "active": 1 });
db.product_types.createIndex({ "displayOrder": 1 });
print('Product types collection and indexes created');

// Insert initial product type definitions
db.product_types.insertMany([
    // Account Products
    {
        typeCode: "CHECKING_ACCOUNT",
        name: "Checking Account",
        description: "Standard checking account for daily transactions",
        category: "ACCOUNT",
        subcategory: "DEPOSIT",
        active: true,
        displayOrder: 1,
        icon: "account_balance",
        tags: ["account", "deposit", "checking", "transaction"],
        metadata: {
            regulatoryCategory: "Deposit Account",
            fdic_insured: true
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "SAVINGS_ACCOUNT",
        name: "Savings Account",
        description: "Interest-bearing savings account",
        category: "ACCOUNT",
        subcategory: "DEPOSIT",
        active: true,
        displayOrder: 2,
        icon: "savings",
        tags: ["account", "deposit", "savings", "interest"],
        metadata: {
            regulatoryCategory: "Deposit Account",
            fdic_insured: true
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "MONEY_MARKET_ACCOUNT",
        name: "Money Market Account",
        description: "High-yield money market account with check-writing privileges",
        category: "ACCOUNT",
        subcategory: "DEPOSIT",
        active: true,
        displayOrder: 3,
        icon: "trending_up",
        tags: ["account", "deposit", "money_market", "high_yield"],
        metadata: {
            regulatoryCategory: "Deposit Account",
            fdic_insured: true
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "CERTIFICATE_OF_DEPOSIT",
        name: "Certificate of Deposit (CD)",
        description: "Time deposit with fixed term and interest rate",
        category: "ACCOUNT",
        subcategory: "DEPOSIT",
        active: true,
        displayOrder: 4,
        icon: "receipt_long",
        tags: ["account", "deposit", "cd", "time_deposit", "fixed_rate"],
        metadata: {
            regulatoryCategory: "Deposit Account",
            fdic_insured: true,
            typical_terms: ["3 months", "6 months", "1 year", "5 years"]
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "CREDIT_CARD",
        name: "Credit Card",
        description: "Revolving credit card account",
        category: "LENDING",
        subcategory: "CREDIT",
        active: true,
        displayOrder: 10,
        icon: "credit_card",
        tags: ["credit", "card", "revolving", "lending"],
        metadata: {
            regulatoryCategory: "Credit Product",
            regulation: "Regulation Z (TILA)"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "PERSONAL_LOAN",
        name: "Personal Loan",
        description: "Unsecured personal installment loan",
        category: "LENDING",
        subcategory: "LOAN",
        active: true,
        displayOrder: 11,
        icon: "account_balance_wallet",
        tags: ["loan", "personal", "unsecured", "installment"],
        metadata: {
            regulatoryCategory: "Loan Product",
            regulation: "Regulation Z (TILA)"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "MORTGAGE",
        name: "Mortgage Loan",
        description: "Secured real estate mortgage loan",
        category: "LENDING",
        subcategory: "LOAN",
        active: true,
        displayOrder: 12,
        icon: "home",
        tags: ["loan", "mortgage", "secured", "real_estate"],
        metadata: {
            regulatoryCategory: "Mortgage",
            regulation: "Regulation Z (TILA), RESPA"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "BUSINESS_ACCOUNT",
        name: "Business Account",
        description: "Commercial business checking or savings account",
        category: "ACCOUNT",
        subcategory: "COMMERCIAL",
        active: true,
        displayOrder: 5,
        icon: "business",
        tags: ["account", "business", "commercial", "corporate"],
        metadata: {
            regulatoryCategory: "Commercial Account",
            fdic_insured: true
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "INVESTMENT_ACCOUNT",
        name: "Investment Account",
        description: "Brokerage and investment account",
        category: "INVESTMENT",
        subcategory: "BROKERAGE",
        active: true,
        displayOrder: 20,
        icon: "show_chart",
        tags: ["investment", "brokerage", "securities", "trading"],
        metadata: {
            regulatoryCategory: "Investment Product",
            regulation: "SEC, FINRA",
            fdic_insured: false
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "CASH_MANAGEMENT",
        name: "Cash Management",
        description: "Treasury and cash management services",
        category: "TREASURY",
        subcategory: "CASH_MANAGEMENT",
        active: true,
        displayOrder: 30,
        icon: "payments",
        tags: ["treasury", "cash_management", "corporate", "liquidity"],
        metadata: {
            regulatoryCategory: "Treasury Service"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "TREASURY_SERVICE",
        name: "Treasury Service",
        description: "Corporate treasury and financial management services",
        category: "TREASURY",
        subcategory: "CORPORATE",
        active: true,
        displayOrder: 31,
        icon: "corporate_fare",
        tags: ["treasury", "corporate", "financial_services"],
        metadata: {
            regulatoryCategory: "Treasury Service"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    // Payment Processing Products
    {
        typeCode: "ACH_TRANSFER",
        name: "ACH Transfer",
        description: "Automated Clearing House electronic transfer",
        category: "PAYMENT",
        subcategory: "TRANSFER",
        active: true,
        displayOrder: 40,
        icon: "swap_horiz",
        tags: ["payment", "ach", "transfer", "electronic"],
        metadata: {
            regulatoryCategory: "Payment Service",
            regulation: "NACHA Rules"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "WIRE_TRANSFER",
        name: "Wire Transfer",
        description: "Real-time wire transfer (domestic and international)",
        category: "PAYMENT",
        subcategory: "TRANSFER",
        active: true,
        displayOrder: 41,
        icon: "fast_forward",
        tags: ["payment", "wire", "transfer", "real_time"],
        metadata: {
            regulatoryCategory: "Payment Service",
            regulation: "Fedwire, SWIFT"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "REAL_TIME_PAYMENT",
        name: "Real-Time Payment (RTP)",
        description: "Instant payment via RTP network",
        category: "PAYMENT",
        subcategory: "TRANSFER",
        active: true,
        displayOrder: 42,
        icon: "flash_on",
        tags: ["payment", "rtp", "instant", "real_time"],
        metadata: {
            regulatoryCategory: "Payment Service",
            regulation: "The Clearing House RTP"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "P2P_PAYMENT",
        name: "Peer-to-Peer Payment",
        description: "Person-to-person payment transfer",
        category: "PAYMENT",
        subcategory: "P2P",
        active: true,
        displayOrder: 43,
        icon: "people",
        tags: ["payment", "p2p", "peer_to_peer", "consumer"],
        metadata: {
            regulatoryCategory: "Payment Service"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "BILL_PAYMENT",
        name: "Bill Payment",
        description: "Online bill payment service",
        category: "PAYMENT",
        subcategory: "BILLPAY",
        active: true,
        displayOrder: 44,
        icon: "receipt",
        tags: ["payment", "bill_pay", "online", "autopay"],
        metadata: {
            regulatoryCategory: "Payment Service"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "CARD_PAYMENT",
        name: "Card Payment Processing",
        description: "Debit and credit card payment processing",
        category: "PAYMENT",
        subcategory: "CARD",
        active: true,
        displayOrder: 45,
        icon: "payment",
        tags: ["payment", "card", "processing", "merchant"],
        metadata: {
            regulatoryCategory: "Payment Service",
            regulation: "PCI-DSS"
        },
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    },
    {
        typeCode: "OTHER",
        name: "Other Product Type",
        description: "Miscellaneous product type",
        category: "OTHER",
        subcategory: "GENERAL",
        active: true,
        displayOrder: 999,
        icon: "more_horiz",
        tags: ["other", "miscellaneous"],
        metadata: {},
        createdAt: new Date(),
        updatedAt: new Date(),
        createdBy: "system",
        updatedBy: "system"
    }
]);

print('Product type definitions inserted: 17 types');

// ============================================
// SAMPLE ENTITLEMENTS (Fine-Grained Access Control)
// ============================================

// Sample entitlements for testing
// These demonstrate different entitlement patterns:
// 1. Resource-specific (solution-123)
// 2. Type-level (all CHECKING solutions)
// 3. Constrained (amount limits, channel restrictions)

db.entitlements.insertMany([
    // Example 1: Alice can VIEW and CONFIGURE a specific solution
    {
        tenantId: "tenant-001",
        partyId: "alice-party-001",
        resourceType: "SOLUTION",
        resourceId: "solution-checking-premium-001",
        operations: ["VIEW", "CONFIGURE", "UPDATE"],
        constraints: {
            maxAmount: NumberDecimal("50000"),
            minAmount: NumberDecimal("0"),
            allowedChannels: ["WEB", "MOBILE"],
            blockedChannels: [],
            allowedProductTypes: ["CHECKING"],
            requiresApproval: false,
            requiresMfa: false,
            allowedCountries: ["US"],
            blockedCountries: []
        },
        source: "EXPLICIT_GRANT",
        grantedBy: "admin-party-001",
        grantedAt: new Date(),
        active: true,
        priority: 10,
        metadata: {
            grantReason: "Product manager assigned to checking products"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },

    // Example 2: Bob can VIEW all CHECKING solutions (type-level permission)
    {
        tenantId: "tenant-001",
        partyId: "bob-party-002",
        resourceType: "SOLUTION",
        resourceId: null,  // null = applies to all CHECKING solutions
        operations: ["VIEW", "LIST"],
        constraints: {
            allowedProductTypes: ["CHECKING"],
            requiresApproval: false,
            requiresMfa: false
        },
        source: "ROLE_BASED",
        sourceReference: "ROLE_VIEWER",
        grantedBy: "admin-party-001",
        grantedAt: new Date(),
        active: true,
        priority: 5,
        metadata: {
            grantReason: "Viewer role for checking products"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },

    // Example 3: Carol can TRANSACT on a specific account with limits
    {
        tenantId: "tenant-001",
        partyId: "carol-party-003",
        resourceType: "ACCOUNT",
        resourceId: "account-checking-12345",
        operations: ["VIEW", "TRANSACT", "INITIATE_PAYMENT"],
        constraints: {
            maxAmount: NumberDecimal("10000"),
            minAmount: NumberDecimal("0"),
            dailyLimit: NumberDecimal("25000"),
            monthlyLimit: NumberDecimal("100000"),
            allowedChannels: ["WEB", "MOBILE"],
            blockedChannels: ["ATM"],
            allowedTransactionTypes: ["ACH", "WIRE"],
            requiresApproval: true,
            approvalThreshold: NumberDecimal("5000"),
            requiresMfa: true,
            allowedCountries: ["US"],
            blockedCountries: []
        },
        source: "RELATIONSHIP_BASED",
        sourceReference: "AuthorizedSigner",
        grantedBy: "system",
        grantedAt: new Date(),
        active: true,
        priority: 15,
        metadata: {
            grantReason: "Authorized signer on account",
            relationshipId: "rel-auth-signer-001"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },

    // Example 4: Dave (admin) has full access to catalog products
    {
        tenantId: "tenant-001",
        partyId: "dave-party-004",
        resourceType: "CATALOG_PRODUCT",
        resourceId: null,  // All catalog products
        operations: ["VIEW", "CREATE", "UPDATE", "DELETE", "CONFIGURE", "ACTIVATE", "DEACTIVATE"],
        constraints: {
            requiresApproval: false,
            requiresMfa: false
        },
        source: "ROLE_BASED",
        sourceReference: "ROLE_ADMIN",
        grantedBy: "system",
        grantedAt: new Date(),
        active: true,
        priority: 100,
        metadata: {
            grantReason: "Administrator full access"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    },

    // Example 5: Eve has delegated authority (temporary, expires in 30 days)
    {
        tenantId: "tenant-001",
        partyId: "eve-party-005",
        resourceType: "WORKFLOW",
        resourceId: null,
        operations: ["VIEW", "APPROVE_WORKFLOW", "REJECT_WORKFLOW"],
        constraints: {
            maxAmount: NumberDecimal("100000"),
            requiresApproval: false,
            requiresMfa: true
        },
        source: "DELEGATED",
        sourceReference: "alice-party-001",
        grantedBy: "alice-party-001",
        grantedAt: new Date(),
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),  // 30 days
        active: true,
        priority: 12,
        grantReason: "Temporary delegation while Alice is on vacation",
        metadata: {
            delegationType: "TEMPORARY_ABSENCE",
            originalOwner: "alice-party-001"
        },
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

print('Sample entitlements inserted: 5 entitlements');
print('MongoDB initialization completed successfully!');