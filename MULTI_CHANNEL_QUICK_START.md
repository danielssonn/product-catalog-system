# Multi-Channel API Gateway - Quick Start Guide

**5-Minute Setup Guide for Developers**

---

## 🚀 Get Started in 3 Steps

### Step 1: Start Dependencies (30 seconds)

```bash
cd product-catalog-system
docker-compose up -d mongodb redis kafka product-service workflow-service party-service
```

### Step 2: Start API Gateway (20 seconds)

```bash
docker-compose up -d api-gateway
```

### Step 3: Test (10 seconds)

```bash
curl http://localhost:8080/actuator/health
```

✅ If you see `{"status":"UP"}`, you're ready!

---

## 📖 Quick Channel Reference

### Channel 1: Public API (REST JSON)

**Use Case**: Mobile apps, third-party integrations

```bash
curl -X POST http://localhost:8080/api/v1/solutions/configure \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: admin@example.com" \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-savings-001",
    "solutionName": "My Product",
    "pricingVariance": 5
  }'
```

---

### Channel 2: Host-to-Host (File Upload)

**Use Case**: Batch CSV uploads

```bash
# Create test file
echo "catalogProductId,solutionName,pricingVariance
cat-savings-001,Bulk Product 1,3
cat-checking-001,Bulk Product 2,5" > test.csv

# Upload
curl -X POST http://localhost:8080/channel/host-to-host/files/upload \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: system@example.com" \
  -H "X-File-Format: CSV" \
  -F "file=@test.csv"

# Check status
curl http://localhost:8080/channel/host-to-host/files/{fileId}/status \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001"
```

---

### Channel 3: ERP Integration

**Use Case**: Kyriba, SAP Treasury

```bash
curl -X POST http://localhost:8080/channel/erp/products/configure \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: erp-system" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

---

### Channel 4: Client Portal

**Use Case**: Customer web portal

```bash
curl http://localhost:8080/channel/portal/products/available \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: customer@example.com" \
  -H "X-Party-ID: party-12345"
```

---

### Channel 5: Salesforce

**Use Case**: CRM integration

```bash
curl http://localhost:8080/channel/salesforce/workflows \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001"
```

---

## 🔑 Required Headers

**Every request needs**:
```http
X-Tenant-ID: tenant-001
X-User-ID: user@example.com
```

**Optional**:
```http
X-Channel: PUBLIC_API
X-Party-ID: party-12345
X-API-Version: v1
X-Idempotency-Key: unique-key-123
```

---

## 🧪 Test Scripts

```bash
# Test all Public API features
./test-gateway-public-api.sh

# Test file upload and processing
./test-gateway-host-to-host.sh
```

---

## 📊 Monitoring

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Circuit breaker state
curl http://localhost:8080/actuator/health/circuitBreakers
```

---

## 🐛 Troubleshooting

**Problem**: HTTP 400 "Missing tenant ID"  
**Solution**: Add `X-Tenant-ID` and `X-User-ID` headers

**Problem**: HTTP 429 "Rate limit exceeded"  
**Solution**: Wait 1 minute or reduce request rate

**Problem**: HTTP 503 "Service unavailable"  
**Solution**: Circuit breaker is open, check downstream service health

**Problem**: Gateway not starting  
**Solution**: Check Redis and MongoDB are running
```bash
docker-compose ps
docker-compose up -d redis mongodb
```

---

## 📚 Full Documentation

- **Architecture**: [API_GATEWAY_ARCHITECTURE.md](API_GATEWAY_ARCHITECTURE.md)
- **Summary**: [API_GATEWAY_SUMMARY.md](API_GATEWAY_SUMMARY.md)
- **Service README**: [backend/api-gateway/README.md](backend/api-gateway/README.md)

---

## 🎯 Common Use Cases

### Use Case 1: Configure a Product via Public API

```bash
curl -X POST http://localhost:8080/api/v1/solutions/configure \
  -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: admin@example.com" \
  -H "Content-Type: application/json" \
  -d '{
    "catalogProductId": "cat-savings-001",
    "solutionName": "Premium Savings Account",
    "description": "High-yield savings product",
    "pricingVariance": 5,
    "riskLevel": "LOW",
    "businessJustification": "Competitive offering"
  }'
```

### Use Case 2: Batch Upload Products via File

```bash
# 1. Create CSV file
cat > products.csv << 'CSV'
catalogProductId,solutionName,pricingVariance,riskLevel,businessJustification
cat-savings-001,Auto Savings 1,3,LOW,Batch import
cat-checking-001,Auto Checking 1,5,MEDIUM,Batch import
cat-savings-001,Auto Savings 2,4,LOW,Batch import
CSV

# 2. Upload
FILE_RESPONSE=$(curl -s -u admin:admin123 -X POST \
  -H "X-Tenant-ID: tenant-001" \
  -H "X-User-ID: system@example.com" \
  -H "X-File-Format: CSV" \
  -F "file=@products.csv" \
  http://localhost:8080/channel/host-to-host/files/upload)

# 3. Get file ID
FILE_ID=$(echo $FILE_RESPONSE | jq -r '.fileId')
echo "File ID: $FILE_ID"

# 4. Check status
curl -s -u admin:admin123 \
  -H "X-Tenant-ID: tenant-001" \
  http://localhost:8080/channel/host-to-host/files/$FILE_ID/status | jq '.'
```

### Use Case 3: Query Audit Logs

```bash
# Via MongoDB
docker exec -it product-catalog-mongodb mongosh \
  mongodb://admin:admin123@localhost:27017/product_catalog_db?authSource=admin

# Then in mongo shell:
db.api_audit_logs.find({
  tenantId: "tenant-001",
  channel: "PUBLIC_API"
}).limit(10).pretty()
```

---

## 🔐 Authentication Quick Reference

| Channel | Header | Example |
|---------|--------|---------|
| Public API | `Authorization: Bearer {JWT}` | Basic auth for testing |
| Host-to-Host | `X-API-Key: {key}` | Basic auth for testing |
| ERP Integration | `Authorization: Bearer {OAuth}` | OAuth 2.0 token |
| Client Portal | `Authorization: Bearer {OAuth}` | OAuth 2.0 token |
| Salesforce | `Authorization: Bearer {SF-Token}` | Salesforce token |

**For testing**, use Basic Auth:
```bash
-u admin:admin123
```

---

## 📈 Performance Tips

1. **Use connection pooling** - Gateway has 100 connections configured
2. **Implement caching** - Gateway caches party lookups for 5 minutes
3. **Use idempotency keys** - Prevent duplicate operations
   ```http
   X-Idempotency-Key: unique-operation-id-123
   ```
4. **Respect rate limits** - Check `X-RateLimit-Remaining` header

---

## 🎓 Next Steps

1. Read [API_GATEWAY_ARCHITECTURE.md](API_GATEWAY_ARCHITECTURE.md) for deep dive
2. Review [NEW_SERVICE_CHECKLIST.md](NEW_SERVICE_CHECKLIST.md) for standards
3. Check [BUSINESS_ARCHITECTURE.md](BUSINESS_ARCHITECTURE.md) for domain model
4. Explore other services:
   - [Product Service](backend/product-service/README.md)
   - [Workflow Service](backend/workflow-service/README.md)
   - [Party Service](backend/party-service/README.md)

---

**Happy coding! 🚀**
