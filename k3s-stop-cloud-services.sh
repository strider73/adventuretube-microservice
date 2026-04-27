#!/bin/bash

# Scale down the 3 Spring Cloud infrastructure services in K3s for local development.

NAMESPACE="adventuretube"
SERVICES=(
  eureka-server
  config-service
  gateway-service
)

echo "Scaling down Spring Cloud services in K3s (namespace: $NAMESPACE)..."

for svc in "${SERVICES[@]}"; do
  kubectl scale deployment "$svc" --replicas=0 -n "$NAMESPACE"
done

echo "Done. Run 'kubectl get pods -n $NAMESPACE' to verify."
