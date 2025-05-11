#!/bin/bash

# OpenWhisk Deployment Script for QSE

# Set environment variables
export REDIS_HOST=${REDIS_HOST:-redis}
export MINIO_ENDPOINT=${MINIO_ENDPOINT:-http://minio:9000}
export MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-minioadmin}
export MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-minioadmin}

# Build the project
echo "Building QSE OpenWhisk package..."
mvn clean package

# Deploy to OpenWhisk
echo "Deploying to OpenWhisk..."

# Create the package
wsk package create qse

# Deploy actions
echo "Deploying actions..."
wsk action create qse/orchestrator target/qse-openwhisk-1.0-SNAPSHOT.jar \
  --main com.qse.actions.OrchestratorAction \
  --memory 512 \
  --timeout 300000 \
  --param REDIS_HOST $REDIS_HOST \
  --param MINIO_ENDPOINT $MINIO_ENDPOINT \
  --param MINIO_ACCESS_KEY $MINIO_ACCESS_KEY \
  --param MINIO_SECRET_KEY $MINIO_SECRET_KEY

wsk action create qse/entity-extraction target/qse-openwhisk-1.0-SNAPSHOT.jar \
  --main com.qse.actions.EntityExtractionAction \
  --memory 1024 \
  --timeout 600000 \
  --param REDIS_HOST $REDIS_HOST \
  --param MINIO_ENDPOINT $MINIO_ENDPOINT \
  --param MINIO_ACCESS_KEY $MINIO_ACCESS_KEY \
  --param MINIO_SECRET_KEY $MINIO_SECRET_KEY

wsk action create qse/entity-constraints-extraction target/qse-openwhisk-1.0-SNAPSHOT.jar \
  --main com.qse.actions.EntityConstraintsExtractionAction \
  --memory 1024 \
  --timeout 600000 \
  --param REDIS_HOST $REDIS_HOST \
  --param MINIO_ENDPOINT $MINIO_ENDPOINT \
  --param MINIO_ACCESS_KEY $MINIO_ACCESS_KEY \
  --param MINIO_SECRET_KEY $MINIO_SECRET_KEY

wsk action create qse/shapes-extraction target/qse-openwhisk-1.0-SNAPSHOT.jar \
  --main com.qse.actions.ShapesExtractionAction \
  --memory 1024 \
  --timeout 600000 \
  --param REDIS_HOST $REDIS_HOST \
  --param MINIO_ENDPOINT $MINIO_ENDPOINT \
  --param MINIO_ACCESS_KEY $MINIO_ACCESS_KEY \
  --param MINIO