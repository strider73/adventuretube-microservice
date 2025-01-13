#!/bin/bash

# Step 1: Clean and build Maven project
echo "Cleaning and building Maven project..."
./mvnw clean package

# Step 2: Run the Docker container
echo "Starting the Docker container..."
docker compose -f docker-compose-clouds.yml build

# Step 3: down the Docker image
echo "Building Docker image..."
docker compose -f docker-compose-clouds.yml down


# Step 4: Run the Docker container
echo "Starting the Docker container..."
docker-compose -f docker compose -f docker-compose-clouds.yml up

echo "Build and deployment completed successfully!"
