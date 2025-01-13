#!/bin/bash

# Step 1: Clean and build Maven project
echo "Cleaning and building Maven project..."
./mvnw clean package

# Step 2: Build the Docker image
echo "Building Docker image..."
docker-compose -f docker compose -f docker-compose-clouds.yml build

# Step 3: Run the Docker container
echo "Starting the Docker container..."
docker-compose -f docker compose -f docker-compose-clouds.yml up

echo "Build and deployment completed successfully!"
