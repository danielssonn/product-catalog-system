// Initialize Version Service Data
// Run this script after MongoDB is initialized

use productcatalog;

// Create indexes for api_versions collection
db.api_versions.createIndex({ "serviceId": 1, "version": 1 }, { unique: true });
db.api_versions.createIndex({ "serviceId": 1, "status": 1 });
db.api_versions.createIndex({ "serviceId": 1, "releasedAt": -1 });

// Create indexes for api_endpoint_versions collection
db.api_endpoint_versions.createIndex({
    "serviceId": 1,
    "apiVersion": 1,
    "endpointPath": 1,
    "httpMethod": 1
}, { unique: true });
db.api_endpoint_versions.createIndex({ "serviceId": 1, "status": 1 });

// Insert initial version configurations for product-service
db.api_versions.insertMany([
    {
        "serviceId": "product-service",
        "version": "v1",
        "semanticVersion": "1.0.0",
        "status": "STABLE",
        "releasedAt": new Date("2025-01-01T00:00:00Z"),
        "deprecatedAt": null,
        "sunsetAt": null,
        "eolAt": null,
        "breakingChanges": [],
        "newFeatures": [
            "Product catalog browsing",
            "Solution configuration",
            "Workflow integration"
        ],
        "bugFixes": [],
        "migrationGuideUrl": null,
        "documentationUrl": "https://docs.bank.com/api/product-service/v1",
        "openApiSpecUrl": "https://api.bank.com/product-service/v1/openapi.json",
        "transformations": {
            "v2": {
                "fromVersion": "v1",
                "toVersion": "v2",
                "fieldMappings": {
                    "catalogProductId": "templateId",
                    "solutionName": "name"
                },
                "fieldTransformations": [],
                "defaultValues": {
                    "metadata": {}
                },
                "fieldsToRemove": [],
                "customScript": null,
                "type": "SIMPLE"
            }
        },
        "contentTypes": ["application/json"],
        "defaultContentType": "application/json",
        "metadata": {},
        "createdBy": "system",
        "createdAt": new Date(),
        "updatedBy": null,
        "updatedAt": null
    },
    {
        "serviceId": "product-service",
        "version": "v2",
        "semanticVersion": "2.0.0",
        "status": "BETA",
        "releasedAt": null,
        "deprecatedAt": null,
        "sunsetAt": null,
        "eolAt": null,
        "breakingChanges": [
            {
                "type": "FIELD_RENAMED",
                "affectedEndpoint": "/api/v2/solutions/configure",
                "affectedField": "catalogProductId",
                "description": "Field renamed from catalogProductId to templateId",
                "migrationStrategy": "Update all API clients to use 'templateId' instead of 'catalogProductId'",
                "exampleBefore": '{"catalogProductId": "cat-001"}',
                "exampleAfter": '{"templateId": "cat-001"}'
            },
            {
                "type": "FIELD_RENAMED",
                "affectedEndpoint": "/api/v2/solutions/configure",
                "affectedField": "solutionName",
                "description": "Field renamed from solutionName to name",
                "migrationStrategy": "Update all API clients to use 'name' instead of 'solutionName'",
                "exampleBefore": '{"solutionName": "Premium Checking"}',
                "exampleAfter": '{"name": "Premium Checking"}'
            },
            {
                "type": "REQUIRED_FIELD_ADDED",
                "affectedEndpoint": "/api/v2/solutions/configure",
                "affectedField": "metadata",
                "description": "New required field 'metadata' for extensibility",
                "migrationStrategy": "Add 'metadata' object to all solution configuration requests",
                "exampleBefore": '{}',
                "exampleAfter": '{"metadata": {}}'
            }
        ],
        "newFeatures": [
            "Enhanced schema with metadata field",
            "Improved validation",
            "Better error messages"
        ],
        "bugFixes": [],
        "migrationGuideUrl": "https://docs.bank.com/api/product-service/migration-v1-to-v2",
        "documentationUrl": "https://docs.bank.com/api/product-service/v2",
        "openApiSpecUrl": "https://api.bank.com/product-service/v2/openapi.json",
        "transformations": {
            "v1": {
                "fromVersion": "v2",
                "toVersion": "v1",
                "fieldMappings": {
                    "templateId": "catalogProductId",
                    "name": "solutionName"
                },
                "fieldTransformations": [],
                "defaultValues": {},
                "fieldsToRemove": ["metadata"],
                "customScript": null,
                "type": "SIMPLE"
            }
        },
        "contentTypes": ["application/json", "application/vnd.bank.v2+json"],
        "defaultContentType": "application/json",
        "metadata": {
            "releaseNotes": "Major version with breaking changes"
        },
        "createdBy": "system",
        "createdAt": new Date(),
        "updatedBy": null,
        "updatedAt": null
    }
]);

// Insert endpoint-level version info
db.api_endpoint_versions.insertMany([
    {
        "serviceId": "product-service",
        "apiVersion": "v1",
        "endpointPath": "/api/v1/solutions/configure",
        "httpMethod": "POST",
        "status": "STABLE",
        "requestSchemaRef": "ConfigureSolutionRequest_v1",
        "responseSchemaRef": "ConfigureSolutionResponse_v1",
        "errorSchemaRefs": {
            "400": "ErrorResponse_v1",
            "401": "UnauthorizedError_v1",
            "500": "InternalServerError_v1"
        },
        "deprecationNotice": null,
        "replacementEndpoint": null,
        "createdAt": new Date("2025-01-01T00:00:00Z"),
        "deprecatedAt": null,
        "sunsetAt": null
    },
    {
        "serviceId": "product-service",
        "apiVersion": "v1",
        "endpointPath": "/api/v1/catalog/available",
        "httpMethod": "GET",
        "status": "STABLE",
        "requestSchemaRef": null,
        "responseSchemaRef": "CatalogListResponse_v1",
        "errorSchemaRefs": {
            "401": "UnauthorizedError_v1",
            "500": "InternalServerError_v1"
        },
        "deprecationNotice": null,
        "replacementEndpoint": null,
        "createdAt": new Date("2025-01-01T00:00:00Z"),
        "deprecatedAt": null,
        "sunsetAt": null
    }
]);

print("Version service initialization complete");
print("API versions created for product-service: v1 (STABLE), v2 (BETA)");
print("Endpoint versions created: 2 endpoints");
