# API Versioning - Quick Start Guide

## 5-Minute Quick Start

### Prerequisites
- Version-service running on port 8090
- MongoDB connected and healthy

### Step 1: Check Service Health (10 seconds)

```bash
curl http://localhost:8090/actuator/health
```

Expected: `{"status":"UP"}`

### Step 2: Register API Version v1 (30 seconds)

```bash
curl -X POST http://localhost:8090/api/v1/versions \
  -H "Content-Type: application/json" \
  -H "X-User-ID: admin" \
  -d '{
    "serviceId": "my-service",
    "version": "v1",
    "semanticVersion": "1.0.0",
    "status": "ACTIVE",
    "transformations": {
      "v2": {
        "fromVersion": "v1",
        "toVersion": "v2",
        "fieldMappings": {
          "oldField": "newField"
        }
      }
    }
  }'
```

### Step 3: Test Transformation (30 seconds)

```bash
curl -X POST "http://localhost:8090/api/v1/transformations/test?serviceId=my-service&fromVersion=v1&toVersion=v2" \
  -H "Content-Type: application/json" \
  -d '{
    "oldField": "test value"
  }'
```

Expected:
```json
{
  "success": true,
  "transformedData": {
    "newField": "test value"
  }
}
```

---

## Common Use Cases

### Use Case 1: Rename a Field

```json
{
  "fieldMappings": {
    "productName": "name"
  }
}
```

### Use Case 2: Nest a Field

```json
{
  "fieldMappings": {
    "price": "pricing.amount"
  }
}
```

### Use Case 3: Add Default Values

```json
{
  "defaultValues": {
    "currency": "USD",
    "createdAt": "2025-01-01T00:00:00Z"
  }
}
```

### Use Case 4: Transform with Function

```json
{
  "fieldTransformations": [
    {
      "sourceField": "email",
      "targetField": "email",
      "transformFunction": "toLowerCase"
    }
  ]
}
```

### Use Case 5: Remove Deprecated Fields

```json
{
  "fieldsToRemove": ["legacyId", "oldStatus"]
}
```

---

## API Cheat Sheet

### Version Management

```bash
# Register version
POST /api/v1/versions

# Get all versions
GET /api/v1/versions/{serviceId}

# Get specific version
GET /api/v1/versions/{serviceId}/{version}

# Update version status
PATCH /api/v1/versions/{serviceId}/{version}/status?status=DEPRECATED

# Deprecate version
POST /api/v1/versions/{serviceId}/{version}/deprecate?reason=NewVersionAvailable
```

### Transformations

```bash
# Transform request
POST /api/v1/transformations/request?serviceId=X&fromVersion=v1&toVersion=v2

# Transform response  
POST /api/v1/transformations/response?serviceId=X&fromVersion=v2&toVersion=v1

# Batch transform
POST /api/v1/transformations/request/batch?serviceId=X&fromVersion=v1&toVersion=v2

# Chain transform
POST /api/v1/transformations/chain?serviceId=X

# Test transformation
POST /api/v1/transformations/test?serviceId=X&fromVersion=v1&toVersion=v2

# Validate rules
POST /api/v1/transformations/validate

# Get available transformations
GET /api/v1/transformations/available?serviceId=X

# Get transformation details
GET /api/v1/transformations/details?serviceId=X&fromVersion=v1&toVersion=v2
```

---

## Built-in Transformation Functions

| Function | Description | Example Input | Example Output |
|----------|-------------|---------------|----------------|
| `toLowerCase` | Convert to lowercase | "HELLO" | "hello" |
| `toUpperCase` | Convert to uppercase | "hello" | "HELLO" |
| `trim` | Remove whitespace | " text " | "text" |
| `toNumber` | Convert to number | "123" | 123 |
| `toString` | Convert to string | 123 | "123" |
| `toBoolean` | Convert to boolean | "true" | true |
| `format` | Format string | 99.99, "$%.2f" | "$99.99" |

---

## Troubleshooting

### Problem: Transformation not working

**Solution**: Validate transformation rules first
```bash
curl -X POST http://localhost:8090/api/v1/transformations/validate \
  -H "Content-Type: application/json" \
  -d '{"fromVersion":"v1","toVersion":"v2","fieldMappings":{"old":"new"}}'
```

### Problem: Field not transforming

**Solution**: Check transformation details
```bash
curl "http://localhost:8090/api/v1/transformations/details?serviceId=my-service&fromVersion=v1&toVersion=v2"
```

### Problem: Nested field not working

**Solution**: Use dot notation
```json
{
  "fieldMappings": {
    "price": "pricing.amount"
  }
}
```

---

## Testing Checklist

Before deploying transformations:

- [ ] Validate transformation rules
- [ ] Test with sample data
- [ ] Verify round-trip (v1→v2→v1)
- [ ] Check nested fields work
- [ ] Confirm defaults are added
- [ ] Verify deprecated fields removed
- [ ] Test batch transformation
- [ ] Monitor performance (<200ms)

---

## Production Deployment

### Pre-Deployment

1. Review transformation rules
2. Run test suite
3. Validate round-trip integrity
4. Document breaking changes

### Deployment

1. Deploy new version (v2) in parallel
2. Register v2 with transformation rules
3. Test v1→v2 transformation
4. Monitor error rates
5. Gradually migrate clients

### Post-Deployment

1. Monitor transformation metrics
2. Track client usage by version
3. Deprecate old version (after 6 months)
4. Sunset old version (after 12 months)
5. Retire old version (after 18 months)

---

## Best Practices

✅ **DO**:
- Test transformations before deploying
- Use semantic versioning (v1.0.0, v2.0.0)
- Document breaking changes
- Set clear deprecation timelines
- Validate round-trip integrity
- Keep transformations simple

❌ **DON'T**:
- Skip validation
- Deploy without testing
- Use complex custom scripts initially
- Remove versions without warning
- Break backward compatibility
- Forget to document migrations

---

## Getting Help

- **Design Doc**: [API_VERSIONING_DESIGN.md](API_VERSIONING_DESIGN.md)
- **Test Results**: [TEST_VERSIONING_RESULTS.md](TEST_VERSIONING_RESULTS.md)
- **Summary**: [VERSIONING_SUMMARY.md](VERSIONING_SUMMARY.md)
- **Health Check**: http://localhost:8090/actuator/health

---

**Quick Start Complete!** 🎉

You now know how to:
- Register API versions
- Create transformation rules
- Test transformations
- Deploy new versions safely
