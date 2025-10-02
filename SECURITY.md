# Security Configuration

## Environment Variables

All sensitive credentials have been externalized to environment variables and are **NOT** committed to the repository.

### Setup Instructions

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Update `.env` with your actual credentials:**
   ```bash
   # Edit .env with your preferred editor
   nano .env
   ```

3. **Never commit `.env` to Git:**
   - The `.env` file is already in `.gitignore`
   - Only commit `.env.example` (with placeholder values)

### Environment Variables Reference

| Variable | Description | Default (example) |
|----------|-------------|-------------------|
| `MONGODB_USERNAME` | MongoDB admin username | admin |
| `MONGODB_PASSWORD` | MongoDB admin password | **changeme** |
| `PRODUCT_SERVICE_USERNAME` | Product service auth username | admin |
| `PRODUCT_SERVICE_PASSWORD` | Product service auth password | **changeme** |
| `WORKFLOW_SERVICE_USERNAME` | Workflow service auth username | admin |
| `WORKFLOW_SERVICE_PASSWORD` | Workflow service auth password | **changeme** |
| `SECURITY_USERNAME` | Workflow service Spring Security username | admin |
| `SECURITY_PASSWORD` | Workflow service Spring Security password | **changeme** |
| `POSTGRES_USER` | Temporal database username | temporal |
| `POSTGRES_PASSWORD` | Temporal database password | **changeme** |
| `POSTGRES_DB` | Temporal database name | temporal |

### Files Modified

**Application Configuration Files (using env vars):**
- `backend/product-service/src/main/resources/application.yml`
- `backend/product-service/src/main/resources/application-docker.yml`
- `backend/workflow-service/src/main/resources/application.yml`
- `backend/workflow-service/src/main/resources/application-docker.yml`
- `backend/notification-service/src/main/resources/application.yml`
- `backend/notification-service/src/main/resources/application-docker.yml`

**Docker Compose (using env vars):**
- `docker-compose.simple.yml`

### Security Best Practices

✅ **What We Did:**
- Externalized all credentials to environment variables
- Created `.env.example` template (safe to commit)
- Added `.env` to `.gitignore` (actual credentials NOT committed)
- Used environment variable substitution in YAML files

❌ **Never Do:**
- Commit `.env` files with real credentials
- Hardcode passwords in YAML/properties files
- Share credentials in chat/email
- Use default passwords in production

### Production Deployment

For production environments:

1. **Use a secrets management system:**
   - HashiCorp Vault
   - AWS Secrets Manager
   - Azure Key Vault
   - Kubernetes Secrets

2. **Set environment variables at deployment:**
   ```bash
   # Kubernetes
   kubectl create secret generic app-secrets \
     --from-literal=MONGODB_PASSWORD=<secure-password>

   # Docker Compose
   export MONGODB_PASSWORD=<secure-password>
   docker-compose up -d
   ```

3. **Rotate credentials regularly**

4. **Use strong, unique passwords** (minimum 16 characters, random)

### Local Development

For local development (already configured):

```bash
# .env file contains development credentials
MONGODB_USERNAME=admin
MONGODB_PASSWORD=admin123
PRODUCT_SERVICE_USERNAME=admin
PRODUCT_SERVICE_PASSWORD=admin123
WORKFLOW_SERVICE_USERNAME=admin
WORKFLOW_SERVICE_PASSWORD=admin123
SECURITY_USERNAME=admin
SECURITY_PASSWORD=admin
POSTGRES_USER=temporal
POSTGRES_PASSWORD=temporal
POSTGRES_DB=temporal
```

**Note:** The `.env` file is gitignored and safe for local development only.

### Verification

To verify environment variables are working:

```bash
# Start services
docker-compose -f docker-compose.simple.yml up -d

# Test authentication
curl -u admin:admin123 http://localhost:8082/api/v1/catalog/available
curl -u admin:admin123 http://localhost:8089/api/v1/workflow-templates
```

Both should return HTTP 200.
