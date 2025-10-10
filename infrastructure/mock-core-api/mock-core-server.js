#!/usr/bin/env node

/**
 * Mock Core Banking System API Server
 * Simulates Temenos T24, Finacle, and FIS Profile APIs for testing
 */

const express = require('express');
const bodyParser = require('body-parser');
// Load environment variables from .env file
require('dotenv').config();
const { MongoClient, ObjectId } = require('mongodb');

// Configuration from environment variables
const TEMENOS_PORT = parseInt(process.env.TEMENOS_PORT || '9190');
const FINACLE_PORT = parseInt(process.env.FINACLE_PORT || '9191');
const FIS_PORT = parseInt(process.env.FIS_PORT || '9192');

// MongoDB connection from environment
const MONGODB_HOST = process.env.MONGODB_HOST || 'localhost';
const MONGODB_PORT = process.env.MONGODB_PORT || '27018';
const MONGODB_USERNAME = process.env.MONGODB_USERNAME;
const MONGODB_PASSWORD = process.env.MONGODB_PASSWORD;
const MONGODB_DATABASE = process.env.MONGODB_DATABASE || 'product_catalog_db';
const MONGODB_AUTH_SOURCE = process.env.MONGODB_AUTH_SOURCE || 'admin';

// Build MongoDB URI
let MONGO_URI;
if (process.env.MONGODB_URI) {
    // Use full URI if provided
    MONGO_URI = process.env.MONGODB_URI;
} else if (MONGODB_USERNAME && MONGODB_PASSWORD) {
    // Build URI from individual components
    MONGO_URI = `mongodb://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@${MONGODB_HOST}:${MONGODB_PORT}/${MONGODB_DATABASE}?authSource=${MONGODB_AUTH_SOURCE}`;
} else {
    console.error('❌ ERROR: MongoDB credentials not provided!');
    console.error('');
    console.error('Please set environment variables:');
    console.error('  MONGODB_USERNAME=admin');
    console.error('  MONGODB_PASSWORD=<your-password>');
    console.error('');
    console.error('Or provide full URI:');
    console.error('  MONGODB_URI=mongodb://user:pass@host:port/database?authSource=admin');
    console.error('');
    process.exit(1);
}

// In-memory storage for mock products (with MongoDB persistence)
let db;
let mockProducts;
let mongoClient;

// Connect to MongoDB
async function connectDB() {
    try {
        console.log('Connecting to MongoDB...');
        console.log('URI:', MONGO_URI.replace(/admin123/g, '***'));

        mongoClient = new MongoClient(MONGO_URI, {
            serverSelectionTimeoutMS: 5000,
            connectTimeoutMS: 10000,
        });

        await mongoClient.connect();

        // Test the connection
        await mongoClient.db('admin').command({ ping: 1 });

        db = mongoClient.db('product_catalog_db');
        mockProducts = db.collection('mock_core_products');

        console.log('✅ Successfully connected to MongoDB');

        // Create indexes if they don't exist
        await mockProducts.createIndex({ coreSystemId: 1, coreProductId: 1 }, { unique: true });
        await mockProducts.createIndex({ catalogSolutionId: 1 });
        await mockProducts.createIndex({ tenantId: 1 });

        console.log('✅ Indexes created/verified');

    } catch (error) {
        console.error('❌ Failed to connect to MongoDB:', error.message);
        console.error('');
        console.error('Troubleshooting:');
        console.error('  1. Check if MongoDB is running: docker-compose ps mongodb');
        console.error('  2. Check connection: mongosh "' + MONGO_URI + '"');
        console.error('  3. Verify credentials in MONGODB_URI environment variable');
        console.error('');
        throw error;
    }
}

// Generate mock product ID
function generateProductId(coreType) {
    const prefix = {
        'TEMENOS_T24': 'T24',
        'FINACLE': 'FIN',
        'FIS_PROFILE': 'FIS'
    }[coreType] || 'CORE';

    return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`;
}

// Create Temenos T24 Mock Server
function createTemenosServer() {
    const app = express();
    app.use(bodyParser.json());

    // Health check
    app.get('/mock-temenos-api/health', (req, res) => {
        res.json({ status: 'UP', system: 'Temenos T24', timestamp: new Date() });
    });

    // Create product
    app.post('/mock-temenos-api/products', async (req, res) => {
        console.log('[Temenos T24] Creating product:', req.body.name);

        const productId = generateProductId('TEMENOS_T24');
        const product = {
            productId,
            name: req.body.name,
            description: req.body.description,
            type: req.body.type,
            monthlyFee: req.body.monthlyFee,
            interestRate: req.body.interestRate,
            minimumBalance: req.body.minimumBalance,
            features: req.body.features,
            metadata: req.body.metadata,
            status: 'ACTIVE',
            createdAt: new Date(),
            activeAccounts: 0
        };

        // Persist to MongoDB
        await mockProducts.insertOne({
            coreSystemId: req.get('X-Core-System-Id') || 'temenos-unknown',
            coreSystemType: 'TEMENOS_T24',
            coreProductId: productId,
            catalogSolutionId: req.body.metadata?.catalogSolutionId,
            tenantId: req.body.metadata?.tenantId,
            productData: product,
            createdAt: new Date()
        });

        res.status(201).json(product);
    });

    // Update product
    app.put('/mock-temenos-api/products/:productId', async (req, res) => {
        console.log('[Temenos T24] Updating product:', req.params.productId);

        const update = {
            name: req.body.name,
            description: req.body.description,
            monthlyFee: req.body.monthlyFee,
            interestRate: req.body.interestRate,
            minimumBalance: req.body.minimumBalance,
            features: req.body.features,
            updatedAt: new Date()
        };

        await mockProducts.updateOne(
            { coreProductId: req.params.productId },
            { $set: { 'productData': { ...update }, updatedAt: new Date() } }
        );

        res.json({ ...update, productId: req.params.productId });
    });

    // Get product
    app.get('/mock-temenos-api/products/:productId', async (req, res) => {
        console.log('[Temenos T24] Getting product:', req.params.productId);

        const product = await mockProducts.findOne({ coreProductId: req.params.productId });

        if (!product) {
            return res.status(404).json({ error: 'Product not found' });
        }

        res.json(product.productData);
    });

    // Check product exists
    app.head('/mock-temenos-api/products/:productId', async (req, res) => {
        const product = await mockProducts.findOne({ coreProductId: req.params.productId });
        res.status(product ? 200 : 404).end();
    });

    // Deactivate product
    app.post('/mock-temenos-api/products/:productId/deactivate', async (req, res) => {
        console.log('[Temenos T24] Deactivating product:', req.params.productId);

        await mockProducts.updateOne(
            { coreProductId: req.params.productId },
            { $set: { 'productData.status': 'INACTIVE' } }
        );

        res.json({ productId: req.params.productId, status: 'INACTIVE' });
    });

    // Delete product (sunset)
    app.delete('/mock-temenos-api/products/:productId', async (req, res) => {
        console.log('[Temenos T24] Deleting product:', req.params.productId);

        await mockProducts.updateOne(
            { coreProductId: req.params.productId },
            { $set: { 'productData.status': 'DELETED', deletedAt: new Date() } }
        );

        res.json({ productId: req.params.productId, status: 'DELETED' });
    });

    return app;
}

// Create Finacle Mock Server
function createFinacleServer() {
    const app = express();
    app.use(bodyParser.json());

    app.get('/mock-finacle-api/health', (req, res) => {
        res.json({ status: 'UP', system: 'Finacle', timestamp: new Date() });
    });

    app.post('/mock-finacle-api/products', async (req, res) => {
        console.log('[Finacle] Creating product:', req.body.name);

        const productId = generateProductId('FINACLE');
        const product = {
            productId,
            productName: req.body.name,
            productDesc: req.body.description,
            productCategory: req.body.type,
            monthlyCharge: req.body.monthlyFee,
            intRate: req.body.interestRate,
            minBalance: req.body.minimumBalance,
            features: req.body.features,
            metadata: req.body.metadata,
            productStatus: 'A', // Active
            createDate: new Date()
        };

        await mockProducts.insertOne({
            coreSystemId: req.get('X-Core-System-Id') || 'finacle-unknown',
            coreSystemType: 'FINACLE',
            coreProductId: productId,
            catalogSolutionId: req.body.metadata?.catalogSolutionId,
            tenantId: req.body.metadata?.tenantId,
            productData: product,
            createdAt: new Date()
        });

        res.status(201).json(product);
    });

    app.put('/mock-finacle-api/products/:productId', async (req, res) => {
        console.log('[Finacle] Updating product:', req.params.productId);

        const update = {
            productName: req.body.name,
            monthlyCharge: req.body.monthlyFee,
            intRate: req.body.interestRate,
            modifyDate: new Date()
        };

        await mockProducts.updateOne(
            { coreProductId: req.params.productId },
            { $set: { 'productData': { ...update }, updatedAt: new Date() } }
        );

        res.json(update);
    });

    app.get('/mock-finacle-api/products/:productId', async (req, res) => {
        const product = await mockProducts.findOne({ coreProductId: req.params.productId });

        if (!product) {
            return res.status(404).json({ error: 'Product not found' });
        }

        res.json(product.productData);
    });

    return app;
}

// Create FIS Profile Mock Server
function createFISServer() {
    const app = express();
    app.use(bodyParser.json());

    app.get('/mock-fis-api/health', (req, res) => {
        res.json({ status: 'UP', system: 'FIS Profile', timestamp: new Date() });
    });

    app.post('/mock-fis-api/products', async (req, res) => {
        console.log('[FIS Profile] Creating product:', req.body.name);

        const productId = generateProductId('FIS_PROFILE');
        const product = {
            productCode: productId,
            productTitle: req.body.name,
            productDescription: req.body.description,
            productType: req.body.type,
            serviceFee: req.body.monthlyFee,
            interestRatePercent: req.body.interestRate,
            minimumBalanceRequired: req.body.minimumBalance,
            featureSet: req.body.features,
            clientMetadata: req.body.metadata,
            productState: 'ACTIVE',
            effectiveDate: new Date()
        };

        await mockProducts.insertOne({
            coreSystemId: req.get('X-Core-System-Id') || 'fis-unknown',
            coreSystemType: 'FIS_PROFILE',
            coreProductId: productId,
            catalogSolutionId: req.body.metadata?.catalogSolutionId,
            tenantId: req.body.metadata?.tenantId,
            productData: product,
            createdAt: new Date()
        });

        res.status(201).json(product);
    });

    app.put('/mock-fis-api/products/:productId', async (req, res) => {
        console.log('[FIS Profile] Updating product:', req.params.productId);

        const update = {
            productTitle: req.body.name,
            serviceFee: req.body.monthlyFee,
            lastModifiedDate: new Date()
        };

        await mockProducts.updateOne(
            { coreProductId: req.params.productId },
            { $set: { 'productData': { ...update }, updatedAt: new Date() } }
        );

        res.json(update);
    });

    app.get('/mock-fis-api/products/:productId', async (req, res) => {
        const product = await mockProducts.findOne({ coreProductId: req.params.productId });

        if (!product) {
            return res.status(404).json({ error: 'Product not found' });
        }

        res.json(product.productData);
    });

    return app;
}

// Start all servers
async function startServers() {
    await connectDB();

    const temenosApp = createTemenosServer();
    const finacleApp = createFinacleServer();
    const fisApp = createFISServer();

    temenosApp.listen(TEMENOS_PORT, () => {
        console.log(`✅ Temenos T24 Mock API running on http://localhost:${TEMENOS_PORT}/mock-temenos-api`);
    });

    finacleApp.listen(FINACLE_PORT, () => {
        console.log(`✅ Finacle Mock API running on http://localhost:${FINACLE_PORT}/mock-finacle-api`);
    });

    fisApp.listen(FIS_PORT, () => {
        console.log(`✅ FIS Profile Mock API running on http://localhost:${FIS_PORT}/mock-fis-api`);
    });

    console.log('\n📋 Mock Core Banking Systems Ready!');
    console.log('   - Use these endpoints in your tenant_core_mappings');
    console.log('   - Products will be persisted to mock_core_products collection');
    console.log('\n💡 Test commands:');
    console.log('   curl http://localhost:8090/mock-temenos-api/health');
    console.log('   curl http://localhost:8091/mock-finacle-api/health');
    console.log('   curl http://localhost:8092/mock-fis-api/health\n');
}

// Handle shutdown
process.on('SIGINT', async () => {
    console.log('\nShutting down mock core banking servers...');
    if (mongoClient) {
        try {
            await mongoClient.close();
            console.log('✅ MongoDB connection closed');
        } catch (error) {
            console.error('Error closing MongoDB connection:', error.message);
        }
    }
    process.exit(0);
});

// Start
startServers().catch((error) => {
    console.error('Failed to start servers:', error);
    process.exit(1);
});
