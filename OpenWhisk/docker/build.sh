#!/bin/bash

# Docker Build Script for QSE OpenWhisk
# Builds custom Java runtime image with QSE dependencies

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="${1:-}"
TAG="${2:-latest}"
BASE_IMAGE="openwhisk/java8action:latest"

# Image names
JAVA_IMAGE_NAME="qse-openwhisk-java"
FULL_IMAGE_NAME="${REGISTRY:+$REGISTRY/}$JAVA_IMAGE_NAME:$TAG"

echo -e "${GREEN}Building QSE OpenWhisk Docker images...${NC}"

# Check if Maven JAR exists
JAR_FILE="../target/qse-openwhisk-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}Error: JAR file not found. Please run 'mvn package' first.${NC}"
    exit 1
fi

# Copy JAR to docker directory
echo -e "${YELLOW}Copying JAR file...${NC}"
cp "$JAR_FILE" ./qse.jar

# Create Dockerfile if it doesn't exist
if [ ! -f "Dockerfile" ]; then
    echo -e "${YELLOW}Creating Dockerfile...${NC}"
    cat > Dockerfile << 'EOF'
# QSE OpenWhisk Java Runtime
FROM openwhisk/java8action:latest

# Install additional dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Copy QSE dependencies
COPY qse.jar /javaAction/qse.jar

# Copy any additional resources
COPY resources/ /javaAction/resources/

# Set environment variables
ENV MINIO_ENDPOINT=http://minio:9000
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=minioadmin
ENV REDIS_HOST=redis
ENV REDIS_PORT=6379

# Add the JAR to the classpath
ENV CLASSPATH="/javaAction/qse.jar:$CLASSPATH"

# Ensure the proxy is executable
RUN chmod +x /javaAction/proxy

# Set working directory
WORKDIR /javaAction

# Default command
CMD ["/bin/bash", "-c", "/javaAction/proxy"]
EOF
fi

# Create resources directory if it doesn't exist
mkdir -p resources

# Build the Docker image
echo -e "${YELLOW}Building Docker image: $FULL_IMAGE_NAME${NC}"
docker build \
    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
    --build-arg VERSION=$TAG \
    --tag "$FULL_IMAGE_NAME" \
    .

# Clean up
rm -f qse.jar

echo -e "${GREEN}Docker image built successfully!${NC}"
echo "Image name: $FULL_IMAGE_NAME"

# Optionally push to registry
if [ -n "$REGISTRY" ]; then
    read -p "Push image to registry? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${YELLOW}Pushing image to registry...${NC}"
        docker push "$FULL_IMAGE_NAME"
        echo -e "${GREEN}Image pushed successfully!${NC}"
    fi
fi

# Display image info
echo -e "\n${YELLOW}Image information:${NC}"
docker image inspect "$FULL_IMAGE_NAME" --format '{{json .Config.Labels}}' | jq .

echo -e "\n${GREEN}Build complete!${NC}"
echo "To use this image with OpenWhisk:"
echo "  wsk action create myAction --docker $FULL_IMAGE_NAME target/qse-openwhisk-1.0-SNAPSHOT.jar"