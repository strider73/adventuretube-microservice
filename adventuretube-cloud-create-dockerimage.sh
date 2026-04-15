#!/bin/bash
# Cloud image build script using nerdctl (writes directly into K3s containerd).
# Parallel of adventuretube-service-create-dockerimage.sh for the
# infrastructure layer: eureka-server, config-service, gateway-service.
#
# Prereq: run scripts/install-nerdctl-pi2.sh once on PI2.
#
# Usage:
#   ./adventuretube-cloud-create-dockerimage.sh pi2 main [module1,module2,...]

set -u

if [ -z "${1:-}" ]; then
    echo "Please specify the environment (pi, pi2, prod, or mac)."
    exit 1
fi

BRANCH="${2:-main}"
echo "$(date) - Pulling latest updates from '${BRANCH}' branch..."
git checkout "${BRANCH}"
git pull origin "${BRANCH}" || {
    echo "$(date) - Failed to pull latest updates from '${BRANCH}' branch."; exit 1;
}

case "$1" in
    pi)   export ENV_FILE=env.pi   ;;
    pi2)  export ENV_FILE=env.pi2  ;;
    prod) export ENV_FILE=env.prod ;;
    *)    export ENV_FILE=env.mac  ;;
esac
echo "$(date) - Using '${ENV_FILE}' configuration"

# K3s containerd config for nerdctl
NERDCTL_ARGS="--address /run/k3s/containerd/containerd.sock --namespace k8s.io"

# Parent pom + common-api (sequential by nature — single modules)
echo "$(date) - Installing parent pom..."
./mvnw -N install -DskipTests || { echo "$(date) - Parent pom install failed."; exit 1; }
echo "$(date) - Installing common-api module..."
./mvnw clean install -pl common-api -DskipTests || { echo "$(date) - common-api install failed."; exit 1; }

# Maven build — single-threaded (-T 1) to avoid simultaneous JVM memory spikes
echo "$(date) - Cleaning and building Maven project (single-threaded)..."
MODULES=${3:-eureka-server,config-service,gateway-service}
./mvnw -T 1 clean package -DskipTests -pl "$MODULES" || { echo "$(date) - Maven build failed."; exit 1; }

# Nerdctl build — sequential, one service at a time, directly into K3s containerd
echo "$(date) - Building images with nerdctl (sequential, into K3s containerd)..."
IFS=',' read -ra SERVICES <<< "$MODULES"
for SERVICE in "${SERVICES[@]}"; do
    SERVICE=$(echo "$SERVICE" | xargs)  # trim whitespace
    echo "$(date) - Building ${SERVICE}..."
    sudo nerdctl ${NERDCTL_ARGS} compose --env-file "$ENV_FILE" -f docker-compose-clouds.yml build "$SERVICE" || {
        echo "$(date) - Nerdctl build failed for ${SERVICE}."; exit 1;
    }
done

# Prune buildkit cache (keep last 24h)
echo "$(date) - Pruning buildkit cache (keep last 24h)..."
sudo buildctl --addr unix:///run/buildkit/buildkitd.sock prune --keep-duration 24h || true

echo "$(date) - Images built successfully into K3s containerd (namespace=k8s.io)!"
echo "$(date) - Images ready: ${MODULES}"
echo "$(date) - No import step needed — K3s can use them immediately."
