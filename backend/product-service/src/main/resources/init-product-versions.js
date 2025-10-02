// Initialize Product Service API Versions
// This script registers v1 and v2 API versions with transformation rules

use productcatalog;

// Delete existing version data for clean initialization
db.api_versions.deleteMany({ "serviceId": "product-service" });
db.api_endpoint_versions.deleteMany({ "serviceId": "product-service" });

// Insert API version configurations
db.api_versions.insertMany([
    // ============================================================
    // V1 API (STABLE)
    // ============================================================
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
            "Solution configuration with customFees field",
            "Workflow integration for approvals"
        ],
        "bugFixes": [],
        "migrationGuideUrl": null,
        "documentationUrl": "https://docs.bank.com/api/product-service/v1",
        "openApiSpecUrl": "https://api.bank.com/product-service/v1/openapi.json",
        "transformations": {
            // Transformation rules: v1 -> v2
            "v2": {
                "fromVersion": "v1",
                "toVersion": "v2",
                "fieldMappings": {
                    "customFees": "customFeesFX"
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
        "metadata": {
            "description": "Original stable API version"
        },
        "createdBy": "system",
        "createdAt": new Date(),
        "updatedBy": null,
        "updatedAt": null
    },

    // ============================================================
    // V2 API (BETA)
    // ============================================================
    {
        "serviceId": "product-service",
        "version": "v2",
        "semanticVersion": "2.0.0",
        "status": "BETA",
        "releasedAt": new Date("2025-10-02T00:00:00Z"),
        "deprecatedAt": null,
        "sunsetAt": null,
        "eolAt": null,
        "breakingChanges": [
            {
                "type": "FIELD_RENAMED",
                "affectedEndpoint": "/api/v2/solutions/configure",
                "affectedField": "customFees",
                "description": "Field 'customFees' renamed to 'customFeesFX' for better clarity",
                "migrationStrategy": "Update all API clients to use 'customFeesFX' instead of 'customFees'. The field structure remains the same (Map<String, BigDecimal>).",
                "exampleBefore": '{\n  "catalogProductId": "cat-001",\n  "solutionName": "Premium Checking",\n  "customFees": {\n    "monthlyMaintenance": 12.00,\n    "overdraft": 30.00\n  }\n}',
                "exampleAfter": '{\n  "catalogProductId": "cat-001",\n  "solutionName": "Premium Checking",\n  "customFeesFX": {\n    "monthlyMaintenance": 12.00,\n    "overdraft": 30.00\n  }\n}'
            },
            {
                "type": "REQUIRED_FIELD_ADDED",
                "affectedEndpoint": "/api/v2/solutions/configure",
                "affectedField": "metadata",
                "description": "New optional field 'metadata' added for extensibility",
                "migrationStrategy": "Add 'metadata' object to solution configuration requests. Can be empty object {}.",
                "exampleBefore": '{}',
                "exampleAfter": '{\n  "metadata": {\n    "segment": "enterprise",\n    "region": "APAC"\n  }\n}'
            }
        ],
        "newFeatures": [
            "Renamed customFees to customFeesFX for clarity",
            "Added metadata field for extensible configuration",
            "Enhanced validation for fee structures",
            "Improved error messages with field-level details"
        ],
        "bugFixes": [],
        "migrationGuideUrl": "https://docs.bank.com/api/product-service/migration-v1-to-v2",
        "documentationUrl": "https://docs.bank.com/api/product-service/v2",
        "openApiSpecUrl": "https://api.bank.com/product-service/v2/openapi.json",
        "transformations": {
            // Transformation rules: v2 -> v1
            "v1": {
                "fromVersion": "v2",
                "toVersion": "v1",
                "fieldMappings": {
                    "customFeesFX": "customFees"
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
            "description": "Beta version with improved field naming",
            "releaseNotes": "Breaking change: customFees renamed to customFeesFX"
        },
        "createdBy": "system",
        "createdAt": new Date(),
        "updatedBy": null,
        "updatedAt": null
    }
]);

// Insert endpoint-level version information
db.api_endpoint_versions.insertMany([
    // ============================================================
    // V1 Endpoints
    // ============================================================
    {
        "serviceId": "product-service",
        "apiVersion": "v1",
        "endpointPath": "/api/v1/solutions/configure",
        "httpMethod": "POST",
        "status": "STABLE",
        "requestSchemaRef": "ConfigureSolutionRequest_v1",
        "responseSchemaRef": "ConfigureSolutionResponse_v1",
        "errorSchemaRefs": {
            "400": "BadRequestError_v1",
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
        "endpointPath": "/api/v1/solutions/{solutionId}",
        "httpMethod": "GET",
        "status": "STABLE",
        "requestSchemaRef": null,
        "responseSchemaRef": "Solution_v1",
        "errorSchemaRefs": {
            "401": "UnauthorizedError_v1",
            "404": "NotFoundError_v1",
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
    },

    // ============================================================
    // V2 Endpoints
    // ============================================================
    {
        "serviceId": "product-service",
        "apiVersion": "v2",
        "endpointPath": "/api/v2/solutions/configure",
        "httpMethod": "POST",
        "status": "BETA",
        "requestSchemaRef": "ConfigureSolutionRequestV2",
        "responseSchemaRef": "ConfigureSolutionResponseV2",
        "errorSchemaRefs": {
            "400": "BadRequestError_v2",
            "401": "UnauthorizedError_v2",
            "500": "InternalServerError_v2"
        },
        "deprecationNotice": null,
        "replacementEndpoint": null,
        "createdAt": new Date("2025-10-02T00:00:00Z"),
        "deprecatedAt": null,
        "sunsetAt": null
    },
    {
        "serviceId": "product-service",
        "apiVersion": "v2",
        "endpointPath": "/api/v2/solutions/{solutionId}",
        "httpMethod": "GET",
        "status": "BETA",
        "requestSchemaRef": null,
        "responseSchemaRef": "Solution_v2",
        "errorSchemaRefs": {
            "401": "UnauthorizedError_v2",
            "404": "NotFoundError_v2",
            "500": "InternalServerError_v2"
        },
        "deprecationNotice": null,
        "replacementEndpoint": null,
        "createdAt": new Date("2025-10-02T00:00:00Z"),
        "deprecatedAt": null,
        "sunsetAt": null
    }
]);

print("=============================================================");
print("Product Service API Versions Initialized");
print("=============================================================");
print("");
print("V1 (STABLE):");
print("  - Endpoint: POST /api/v1/solutions/configure");
print("  - Field: customFees (Map<String, BigDecimal>)");
print("  - Status: Production-ready");
print("");
print("V2 (BETA):");
print("  - Endpoint: POST /api/v2/solutions/configure");
print("  - Breaking Change: customFees → customFeesFX");
print("  - New Field: metadata (Map<String, Object>)");
print("  - Status: Testing/Preview");
print("");
print("Transformation Rules:");
print("  - v1 → v2: customFees → customFeesFX, add metadata={}");
print("  - v2 → v1: customFeesFX → customFees, remove metadata");
print("");
print("=============================================================");
