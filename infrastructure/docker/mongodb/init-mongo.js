// Initialize MongoDB database and collections

db = db.getSiblingDB('product_catalog');

// Create collections
db.createCollection('products');
db.createCollection('product_versions');
db.createCollection('bundles');
db.createCollection('cross_sell_rules');
db.createCollection('consumers');
db.createCollection('audit_logs');
db.createCollection('api_versions');
db.createCollection('schema_versions');

// Create indexes for products collection
db.products.createIndex({ "tenantId": 1, "productId": 1 }, { unique: true });
db.products.createIndex({ "tenantId": 1, "productCode": 1 }, { unique: true });
db.products.createIndex({ "tenantId": 1, "status": 1, "apiVersion": 1 });
db.products.createIndex({ "schemaVersion": 1 });
db.products.createIndex({ "category": 1 });
db.products.createIndex({ "channels": 1 });

// Create indexes for product_versions collection
db.product_versions.createIndex({ "tenantId": 1, "productId": 1, "productVersion": 1 });
db.product_versions.createIndex({ "effectiveDate": 1, "endDate": 1 });

// Create indexes for bundles collection
db.bundles.createIndex({ "tenantId": 1, "bundleId": 1 }, { unique: true });
db.bundles.createIndex({ "tenantId": 1, "bundleCode": 1 }, { unique: true });
db.bundles.createIndex({ "tenantId": 1, "status": 1 });
db.bundles.createIndex({ "products.productId": 1 });

// Create indexes for cross_sell_rules collection
db.cross_sell_rules.createIndex({ "tenantId": 1, "ruleId": 1 }, { unique: true });
db.cross_sell_rules.createIndex({ "tenantId": 1, "sourceProductId": 1 });
db.cross_sell_rules.createIndex({ "targetProducts.productId": 1 });
db.cross_sell_rules.createIndex({ "status": 1 });

// Create indexes for consumers collection
db.consumers.createIndex({ "tenantId": 1, "consumerId": 1 }, { unique: true });
db.consumers.createIndex({ "tenantId": 1, "apiVersion": 1 });
db.consumers.createIndex({ "subscribedProducts.productId": 1 });
db.consumers.createIndex({ "status": 1 });

// Create indexes for audit_logs collection
db.audit_logs.createIndex({ "tenantId": 1, "entityType": 1, "entityId": 1 });
db.audit_logs.createIndex({ "timestamp": -1 });
db.audit_logs.createIndex({ "userId": 1 });
db.audit_logs.createIndex({ "action": 1 });

// Create indexes for api_versions collection
db.api_versions.createIndex({ "apiVersion": 1 }, { unique: true });
db.api_versions.createIndex({ "supportStatus": 1 });

// Create indexes for schema_versions collection
db.schema_versions.createIndex({ "schemaName": 1, "schemaVersion": 1 }, { unique: true });

print('MongoDB initialization completed successfully!');
print('Created collections: products, product_versions, bundles, cross_sell_rules, consumers, audit_logs, api_versions, schema_versions');
print('Created indexes for all collections');