#!/usr/bin/env node
require('dotenv').config();
const { MongoClient } = require('mongodb');

const MONGODB_HOST = process.env.MONGODB_HOST || 'localhost';
const MONGODB_PORT = process.env.MONGODB_PORT || '27018';
const MONGODB_USERNAME = process.env.MONGODB_USERNAME;
const MONGODB_PASSWORD = process.env.MONGODB_PASSWORD;
const MONGODB_DATABASE = process.env.MONGODB_DATABASE || 'product_catalog_db';
const MONGODB_AUTH_SOURCE = process.env.MONGODB_AUTH_SOURCE || 'admin';

let MONGO_URI;
if (process.env.MONGODB_URI) {
    MONGO_URI = process.env.MONGODB_URI;
} else if (MONGODB_USERNAME && MONGODB_PASSWORD) {
    MONGO_URI = `mongodb://${MONGODB_USERNAME}:${MONGODB_PASSWORD}@${MONGODB_HOST}:${MONGODB_PORT}/${MONGODB_DATABASE}?authSource=${MONGODB_AUTH_SOURCE}`;
} else {
    console.error('❌ ERROR: MongoDB credentials not provided!');
    process.exit(1);
}

async function testConnection() {
    console.log('Testing MongoDB connection...');
    console.log('URI:', MONGO_URI.replace(/:[^:@]+@/, ':***@'));
    
    try {
        const client = new MongoClient(MONGO_URI, { serverSelectionTimeoutMS: 5000 });
        await client.connect();
        await client.db('admin').command({ ping: 1 });
        const db = client.db(MONGODB_DATABASE);
        const count = await db.collection('mock_core_products').countDocuments();
        console.log('\n✅ SUCCESS! MongoDB connection working');
        console.log('   Database:', MONGODB_DATABASE);
        console.log('   Documents:', count);
        await client.close();
    } catch (error) {
        console.error('\n❌ FAILED:', error.message);
        process.exit(1);
    }
}

testConnection();
