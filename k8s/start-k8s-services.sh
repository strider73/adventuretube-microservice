#!/bin/bash
# Sequential startup script for AdventureTube on k3s (pi2)
# Usage: ./k8s/start-k8s-services.sh

NAMESPACE=adventuretube
CONFIG_URL="http://192.168.1.199:30297/actuator/health"
EUREKA_URL="http://192.168.1.199:30761/actuator/health"

wait_for_health() {
    local name=$1
    local url=$2
    echo "Waiting for $name to be healthy..."
    until curl -sf "$url" | grep -q "UP"; do
        echo "  $name not ready, retrying in 5s..."
        sleep 5
    done
    echo "  $name is UP"
}

# STEP 1 — Apply cloud services (config + eureka + gateway)
echo ""
echo "=== STEP 1: Applying cloud-services ==="
kubectl apply -f k8s/cloud-services.yaml -n $NAMESPACE

# STEP 2 — Wait for config-service
wait_for_health "config-service" "$CONFIG_URL"

# STEP 3 — Wait for eureka-server
wait_for_health "eureka-server" "$EUREKA_URL"

echo ""
echo "=== Cloud services ready. Apply adventure-services when ready ==="
echo "    kubectl apply -f k8s/adventure-services.yaml -n $NAMESPACE"
