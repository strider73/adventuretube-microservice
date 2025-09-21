#!/bin/bash

# AdventureTube Service Redeploy Script for Current Branch
# This script performs a complete clean rebuild and redeploy of all services

set -e  # Exit on any error

# Check if env file is provided
ENV_FILE=${1:-env.pi2}
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ Environment file '$ENV_FILE' not found!"
    echo "Usage: $0 [env-file]"
    echo "Example: $0 env.pi2"
    exit 1
fi

echo "=== AdventureTube Service Redeploy Started ==="
echo "Current branch: $(git branch --show-current)"
echo "Environment file: $ENV_FILE"
echo "Timestamp: $(date)"
echo

# Step 1: Stop and remove existing containers
echo "🛑 Stopping and removing existing containers..."
docker compose --env-file "$ENV_FILE" -f docker-compose-adventuretubes.yml down --volumes --remove-orphans

# Step 2: Remove old Docker images
echo "🗑️ Removing old Docker images..."
docker rmi $(docker images -q adventuretube-microservice_auth-service adventuretube-microservice_member-service adventuretube-microservice_web-service adventuretube-microservice_geospatial-service 2>/dev/null) 2>/dev/null || echo "No existing images to remove"

# Step 3: Clean and compile with Maven
echo "🧹 Maven clean and package..."
if command -v mvn &> /dev/null; then
    mvn clean package -DskipTests
elif [ -f "./mvnw" ]; then
    ./mvnw clean package -DskipTests
else
    echo "⚠️ Maven not found, skipping clean package (Docker will handle build)"
fi

# Step 4: Build Docker images with no cache
echo "🐳 Building Docker images (no cache)..."
docker compose --env-file "$ENV_FILE" -f docker-compose-adventuretubes.yml build --no-cache

# Step 5: Start all services
echo "🚀 Starting all services..."
docker compose --env-file "$ENV_FILE" -f docker-compose-adventuretubes.yml up -d

# Step 6: Show running containers
echo "📊 Checking running containers..."
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo
echo "✅ AdventureTube Service Redeploy Complete!"
echo "Services are starting up. Check logs with:"
echo "docker compose --env-file $ENV_FILE -f docker-compose-adventuretubes.yml logs -f"