#!/bin/bash

# Scale down the 5 business services in K3s for local Java development.
# Keeps eureka-server, config-service, gateway-service, and adventurevictoria running.

NAMESPACE="adventuretube"
SERVICES=(
  auth-service
  member-service
  geospatial-service
  web-service
  youtube-service
)

echo "Scaling down business services in K3s (namespace: $NAMESPACE)..."

for svc in "${SERVICES[@]}"; do
  kubectl scale deployment "$svc" --replicas=0 -n "$NAMESPACE"
done

echo "Done. Run 'kubectl get pods -n $NAMESPACE' to verify."
