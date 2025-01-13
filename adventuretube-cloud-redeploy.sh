#!/bin/bash

# Step 1: Pull the latest updates from the 'add-kafka' branch
echo "Pulling latest updates from 'add-kafka' branch..."
git checkout add-kafka
git pull origin add-kafka

# Step 2: Clean and build Maven project
echo "Cleaning and building Maven project..."
./mvnw clean package

# Step 3: Build the Docker image
echo "Building Docker image..."
docker compose -f docker-compose-clouds.yml build

# Step 4: Stop and remove existing Docker containers (if any)
echo "Stopping and removing existing Docker containers..."
docker compose -f docker-compose-clouds.yml down

# Step 5: Start the Docker container with the new image
echo "Starting the Docker container..."
docker compose -f docker-compose-clouds.yml up -d

echo "Build and deployment completed successfully!"