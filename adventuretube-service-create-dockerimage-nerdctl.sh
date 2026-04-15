#!/bin/bash
# Service image build script using nerdctl (writes directly into K3s containerd).
#
# Changes vs the docker version:
#   - Maven: -T 1 (single-threaded) so 5 module compiles don't spike memory together
#   - Docker replaced with: sudo nerdctl --address /run/k3s/containerd/containerd.sock --namespace k8s.io
#   - Docker-compose build loop: one service at a time (sequential) to cap peak RAM
#   - No more `docker save | k3s ctr images import` — nerdctl writes into K3s containerd directly
#
# Prereq: run scripts/install-nerdctl-pi2.sh once on PI2.
#
# Usage (same as the docker version):
#   ./adventuretube-service-create-dockerimage-nerdctl.sh pi2 main [module1,module2,...]

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
MODULES=${3:-auth-service,member-service,web-service,geospatial-service,youtube-service}
./mvnw -T 1 clean package -DskipTests -pl "$MODULES" || { echo "$(date) - Maven build failed."; exit 1; }

# Nerdctl build — sequential, one service at a time, directly into K3s containerd
echo "$(date) - Building images with nerdctl (sequential, into K3s containerd)..."
IFS=',' read -ra SERVICES <<< "$MODULES"
for SERVICE in "${SERVICES[@]}"; do
    SERVICE=$(echo "$SERVICE" | xargs)  # trim whitespace
    echo "$(date) - Building ${SERVICE}..."
    sudo nerdctl ${NERDCTL_ARGS} compose --env-file "$ENV_FILE" -f docker-compose-adventuretubes.yml build "$SERVICE" || {
        echo "$(date) - Nerdctl build failed for ${SERVICE}."; exit 1;
    }
done

# Prune old buildkit cache (keep last 24h)
echo "$(date) - Pruning old buildkit cache (older than 24h)..."
sudo nerdctl ${NERDCTL_ARGS} builder prune -f --filter "until=24h" || true

echo "$(date) - Images built successfully into K3s containerd (namespace=k8s.io)!"
echo "$(date) - Images ready: ${MODULES}"
echo "$(date) - No import step needed — K3s can use them immediately."
