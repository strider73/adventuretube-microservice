#!/bin/bash

# Scale up the 3 Spring Cloud infrastructure services in K3s.

NAMESPACE="adventuretube"
SERVICES=(
  eureka-server
  config-service
  gateway-service
)

echo "Scaling up Spring Cloud services in K3s (namespace: $NAMESPACE)..."

for svc in "${SERVICES[@]}"; do
  kubectl scale deployment "$svc" --replicas=1 -n "$NAMESPACE"
done

echo "Done. Run 'kubectl get pods -n $NAMESPACE' to verify."
