#!/bin/bash

# Step 1: Check if the environment argument is provided
if [ -z "$1" ]; then
    echo "Please specify the environment (pi or mac)."
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
if [ "$1" == "pi" ]; then
    export ENV_FILE=env.pi
    echo "$(date) - Using 'env.pi' configuration"
else
    export ENV_FILE=env.mac
    echo "$(date) - Using 'env.mac' configuration"
fi

# Step 4: Clean and build Maven project
echo "$(date) - Cleaning and building Maven project..."
MODULES=${2:-common-domain,eureka-server,config-service,gateway-service}
./mvnw clean package -DskipTests -pl $MODULES
if [ $? -ne 0 ]; then
    echo "$(date) - Maven build failed."
    exit 1
fi

# Step 5: Clean up Docker system (pruning)
echo "$(date) - Pruning Docker system..."
docker system prune -af --volumes
if [ $? -ne 0 ]; then
    echo "$(date) - Docker system prune failed."
    exit 1
fi

# Step 6: Build the Docker image
echo "$(date) - Building Docker image..."
docker compose --env-file $ENV_FILE -f docker-compose-clouds.yml build
if [ $? -ne 0 ]; then
    echo "$(date) - Docker build failed."
    exit 1
fi

# Step 7: Stop and remove existing Docker containers (if any)
echo "$(date) - Stopping and removing existing Docker containers..."
docker compose --env-file $ENV_FILE -f docker-compose-clouds.yml down
if [ $? -ne 0 ]; then
    echo "$(date) - Failed to stop and remove Docker containers."
    exit 1
fi

# Step 8: Start the Docker container with the new image
echo "$(date) - Starting the Docker container..."
docker compose --env-file $ENV_FILE -f docker-compose-clouds.yml up -d
if [ $? -ne 0 ]; then
    echo "$(date) - Docker container start failed."
    exit 1
fi

# Completion message
echo "$(date) - Build and deployment completed successfully!"
