#!/bin/bash

# Start the 5 business services locally for development.
# Assumes eureka-server, config-service, and gateway-service are running in K3s.

PROJECT_DIR="/Volumes/Programming HD/AdventureTube/adventuretube-microservice"
cd "$PROJECT_DIR"

if [ -f "env.mac" ]; then
    set -a
    source "env.mac"
    set +a
    echo "Environment loaded from env.mac"
else
    echo "Error: env.mac not found in $PROJECT_DIR"
    exit 1
fi

log_service() {
    local name=$1
    while IFS= read -r line; do
        echo "[$(date '+%H:%M:%S')] [$name] $line"
    done
}

echo "Starting 5 business services locally..."

mvn -pl auth-service spring-boot:run 2>&1 | log_service "AUTH" &
sleep 2

mvn -pl member-service spring-boot:run 2>&1 | log_service "MEMBER" &
sleep 2

mvn -pl geospatial-service spring-boot:run 2>&1 | log_service "GEO" &
sleep 2

mvn -pl web-service spring-boot:run 2>&1 | log_service "WEB" &
sleep 2

mvn -pl youtube-service spring-boot:run 2>&1 | log_service "YOUTUBE" &

echo "All 5 services started. Press Ctrl+C to stop all."

wait
