#!/bin/bash

# Step 1: Check if the environment argument is provided
if [ -z "$1" ]; then
    echo "Please specify the environment (pi or mac)."
    exit 1
fi

# Step 2: Pull the latest updates from the branch
BRANCH="${2:-main}"
echo "$(date) - Pulling latest updates from '${BRANCH}' branch..."
git checkout ${BRANCH}
git pull origin ${BRANCH}
if [ $? -ne 0 ]; then
    echo "$(date) - Failed to pull latest updates from '${BRANCH}' branch."
    exit 1
fi

# Step 3: Set the environment file based on the argument
if [ "$1" == "pi" ]; then
    export ENV_FILE=env.pi
    echo "$(date) - Using 'env.pi' configuration"
elif [ "$1" == "pi2" ]; then
    export ENV_FILE=env.pi2
    echo "$(date) - Using 'env.pi2' configuration"
elif [ "$1" == "prod" ]; then
    export ENV_FILE=env.prod
    echo "$(date) - Using 'env.prod' configuration"
else
    export ENV_FILE=env.mac
    echo "$(date) - Using 'env.mac' configuration"
fi

# Step 3.5: Install parent pom and common module
echo "$(date) - Installing parent pom..."
./mvnw -N install -DskipTests || {
    echo "$(date) - Parent pom install failed."; exit 1;
}
echo "$(date) - Installing common-api module..."
./mvnw clean install -pl common-api -DskipTests || {
    echo "$(date) - common-api install failed."; exit 1;
}

# Step 4: Clean and build Maven project
echo "$(date) - Cleaning and building Maven project..."
MODULES=${3:-auth-service,member-service,web-service,geospatial-service,youtube-service}
./mvnw clean package -DskipTests -pl $MODULES
if [ $? -ne 0 ]; then
    echo "$(date) - Maven build failed."
    exit 1
fi

# Step 5: Build Docker images
echo "$(date) - Building Docker images..."
docker compose --env-file $ENV_FILE -f docker-compose-adventuretubes.yml build
if [ $? -ne 0 ]; then
    echo "$(date) - Docker build failed."
    exit 1
fi

# Completion message
echo "$(date) - Docker images created successfully!"
echo "$(date) - Images ready: auth-service, member-service, web-service, geospatial-service, youtube-service"
