# Quick Start Guide

## Prerequisites

- Docker and Docker Compose installed
- Java 21 installed (for local development)
- Maven 3.9+ installed (for building)
- Git installed

## Setup (First Time)

### 1. Clone Repository

```bash
git clone <repository-url>
cd product-catalog-system
```

### 2. Configure Environment Variables

```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your credentials
nano .env
```

**Important:** The `.env` file contains sensitive credentials and is gitignored. Never commit it to the repository.

### 3. Build Services

```bash
cd backend
mvn clean install -DskipTests
cd ..
```

### 4. Start Services

```bash
docker-compose -f docker-compose.simple.yml up -d
```

### 5. Wait for Services to Start

```bash
# Check service status
docker-compose -f docker-compose.simple.yml ps

# Wait for all services to be healthy (usually 30-60 seconds)
```

### 6. Verify Services

```bash
# Test product-service
curl -u admin:admin123 http://localhost:8082/actuator/health

# Test workflow-service
curl -u admin:admin123 http://localhost:8089/actuator/health
```

---

## Running the End-to-End Test

### Quick Test

```bash
# Create a solution that triggers approval workflow
curl -u admin:admin123 -X POST http://localhost:8082/api/v1/solutions/configure \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant-test" \
  -H "X-User-ID: test@example.com" \
  -d '{
    "catalogProductId": "premium-checking-001",
    "solutionName": "Test Solution",
    "description": "Test solution requiring approval",
    "pricingVariance": 20,
    "riskLevel": "MEDIUM",
    "businessJustification": "Testing workflow",
    "priority": "HIGH"
  }'
```

This will return a `workflowId`. Use it for approvals:

```bash
# First approval
curl -u admin:admin123 -X POST http://localhost:8089/api/v1/workflows/<WORKFLOW_ID>/approve \
  -H "Content-Type: application/json" \
  -d '{"approverId":"alice@example.com","comments":"Approved"}'

# Second approval
curl -u admin:admin123 -X POST http://localhost:8089/api/v1/workflows/<WORKFLOW_ID>/approve \
  -H "Content-Type: application/json" \
  -d '{"approverId":"bob@example.com","comments":"Approved"}'
```

---

## Common Commands

### View Logs

```bash
# All services
docker-compose -f docker-compose.simple.yml logs -f

# Specific service
docker-compose -f docker-compose.simple.yml logs -f product-service
docker-compose -f docker-compose.simple.yml logs -f workflow-service
```

### Restart Services

```bash
# Restart all services
docker-compose -f docker-compose.simple.yml restart

# Restart specific service
docker-compose -f docker-compose.simple.yml restart product-service
```

### Stop Services

```bash
docker-compose -f docker-compose.simple.yml down
```

### Rebuild After Code Changes

```bash
# Rebuild code
cd backend
mvn clean package -DskipTests
cd ..

# Rebuild and restart Docker containers
docker-compose -f docker-compose.simple.yml build
docker-compose -f docker-compose.simple.yml up -d
```

---

## API Endpoints

### Product Service (Port 8082)

**Authentication:** `admin:admin123`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/catalog/available` | List available catalog products |
| GET | `/api/v1/catalog/{id}` | Get catalog product details |
| POST | `/api/v1/solutions/configure` | Configure new solution (triggers workflow) |
| GET | `/api/v1/solutions/{id}` | Get solution details |
| GET | `/actuator/health` | Health check (public) |

### Workflow Service (Port 8089)

**Authentication:** `admin:admin123`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/workflows/{id}` | Get workflow status |
| POST | `/api/v1/workflows/{id}/approve` | Approve workflow |
| POST | `/api/v1/workflows/{id}/reject` | Reject workflow |
| GET | `/api/v1/workflow-templates` | List workflow templates |
| GET | `/actuator/health` | Health check (public) |

---

## MongoDB Access

```bash
# Connect to MongoDB
docker exec -it product-catalog-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin

# Use product catalog database
use product_catalog_db

# View collections
show collections

# Query solutions
db.solutions.find().pretty()

# Query workflows
db.workflow_subjects.find().pretty()

# Query approval tasks
db.approval_tasks.find().pretty()
```

---

## Temporal UI

Access the Temporal Web UI at: **http://localhost:8088**

- View running workflows
- See workflow history
- Monitor workflow execution
- Debug workflow issues

---

## Troubleshooting

### Services Won't Start

```bash
# Check Docker logs
docker-compose -f docker-compose.simple.yml logs

# Check if ports are in use
lsof -i :8082
lsof -i :8089
lsof -i :27018
```

### Authentication Failing

```bash
# Verify MongoDB users exist
docker exec product-catalog-mongodb mongosh -u admin -p admin123 \
  --authenticationDatabase admin product_catalog_db \
  --eval "db.users.find().pretty()"

# Check environment variables
docker-compose -f docker-compose.simple.yml config
```

### Workflow Not Triggering

```bash
# Check product-service logs
docker logs product-service -f

# Check workflow-service logs
docker logs workflow-service -f

# Verify workflow template exists
curl -u admin:admin123 http://localhost:8089/api/v1/workflow-templates
```

### Solution Not Activating

```bash
# Check workflow status
curl -u admin:admin123 http://localhost:8089/api/v1/workflows/<WORKFLOW_ID>

# Check solution status in MongoDB
docker exec product-catalog-mongodb mongosh -u admin -p admin123 \
  --authenticationDatabase admin product_catalog_db \
  --eval "db.solutions.find({_id: '<SOLUTION_ID>'}).pretty()"

# Check callback handler logs
docker logs workflow-service 2>&1 | grep -i "callback\|activation"
```

---

## Environment Variables Reference

See [SECURITY.md](SECURITY.md) for complete list of environment variables.

**Critical Variables:**
- `MONGODB_USERNAME` - MongoDB admin username
- `MONGODB_PASSWORD` - MongoDB admin password
- `PRODUCT_SERVICE_USERNAME` - Product service auth username
- `PRODUCT_SERVICE_PASSWORD` - Product service auth password
- `WORKFLOW_SERVICE_USERNAME` - Workflow service auth username
- `WORKFLOW_SERVICE_PASSWORD` - Workflow service auth password

---

## Useful Links

- **Product Service Health:** http://localhost:8082/actuator/health
- **Workflow Service Health:** http://localhost:8089/actuator/health
- **Temporal UI:** http://localhost:8088
- **MongoDB:** localhost:27018
- **Kafka:** localhost:9092

---

## Next Steps

1. ✅ Read [END_TO_END_TEST.md](END_TO_END_TEST.md) for complete test documentation
2. ✅ Review [SECURITY.md](SECURITY.md) for security best practices
3. ✅ Check [CLAUDE.md](CLAUDE.md) for architecture and design details
4. ✅ Explore [WORKFLOW_IMPLEMENTATION.md](WORKFLOW_IMPLEMENTATION.md) for workflow system details

---

**For Support:** Check logs, review documentation, or raise an issue in the repository.
