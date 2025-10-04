#!/bin/bash

# Script to create Kafka topics for outbox pattern

set -e

echo "Waiting for Kafka to be ready..."
sleep 10

echo "Creating Kafka topics..."

docker exec product-catalog-kafka kafka-topics \
  --create \
  --if-not-exists \
  --topic solution.created \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec product-catalog-kafka kafka-topics \
  --create \
  --if-not-exists \
  --topic workflow.completed \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

docker exec product-catalog-kafka kafka-topics \
  --create \
  --if-not-exists \
  --topic solution.status-changed \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

echo ""
echo "Listing created topics:"
docker exec product-catalog-kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092

echo ""
echo "Kafka topics created successfully!"
