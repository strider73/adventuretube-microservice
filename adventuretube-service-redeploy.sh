#!/bin/bash

# Step 1: Check if the environment argument is provided
if [ -z "$1" ]; then
    echo "Please specify the environment (dev or prod)."
    exit 1
fi

# Step 2: Pull the latest updates from the 'add-kafka' branch
echo "$(date) - Pulling latest updates from 'add-kafka' branch..."
git checkout add-kafka
git pull origin add-kafka
if [ $? -ne 0 ]; then
    echo "$(date) - Failed to pull latest updates from 'add-kafka' branch."
    exit 1
fi

# Step 3: Set the environment file based on the argument (dev or prod)
if [ "$1" == "prod" ]; then
    export ENV_FILE=env.prod
    echo "$(date) - Using 'env.prod' configuration"
else
    export ENV_FILE=env.dev
    echo "$(date) - Using 'env.dev' configuration"
fi

# Step 4: Clean and build Maven project
echo "$(date) - Cleaning and building Maven project..."
MODULES=${2:-common-domain,auth-service,member-service,web-service,geospatial-service}
./mvnw clean package -DskipTests -pl $MODULES
if [ $? -ne 0 ]; then
    echo "$(date) - Maven build failed."
    exit 1
fi

# Step 5: Prune Docker system (optional but recommended)
echo "$(date) - Pruning Docker system..."
docker system prune -af --volumes
if [ $? -ne 0 ]; then
    echo "$(date) - Docker system prune failed."
    exit 1
fi

# Step 6: Build the Docker images
echo "$(date) - Building Docker images..."
docker compose --env-file $ENV_FILE -f docker-compose-adventuretubes.yml build
if [ $? -ne 0 ]; then
    echo "$(date) - Docker build failed."
    exit 1
fi

# Step 7: Stop and remove existing Docker containers (if any)
echo "$(date) - Stopping and removing existing Docker containers..."
docker compose --env-file $ENV_FILE -f docker-compose-adventuretubes.yml down
if [ $? -ne 0 ]; then
    echo "$(date) - Failed to stop and remove Docker containers."
    exit 1
fi

# Step 8: Start the Docker containers with the new images
echo "$(date) - Starting the Docker containers..."
docker compose --env-file $ENV_FILE -f docker-compose-adventuretubes.yml up -d
if [ $? -ne 0 ]; then
    echo "$(date) - Docker container start failed."
    exit 1
fi

# Completion message
echo "$(date) - Build and deployment for service modules completed successfully!"
