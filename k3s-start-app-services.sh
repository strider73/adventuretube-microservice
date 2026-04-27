#!/bin/bash

# Scale up the 5 business services in K3s after local Java development.

NAMESPACE="adventuretube"
SERVICES=(
  auth-service
  member-service
  geospatial-service
  web-service
  youtube-service
)

echo "Scaling up business services in K3s (namespace: $NAMESPACE)..."

for svc in "${SERVICES[@]}"; do
  kubectl scale deployment "$svc" --replicas=1 -n "$NAMESPACE"
done

echo "Done. Run 'kubectl get pods -n $NAMESPACE' to verify."
