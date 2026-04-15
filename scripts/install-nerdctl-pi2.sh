#!/bin/bash
# One-time installer: nerdctl + buildkit on PI2 pointed at K3s containerd.
# Run this ONCE on PI2 (as strider user, will sudo internally).
#
# Why: lets us build images directly into K3s's containerd (namespace=k8s.io)
# so we can drop the `docker save | k3s ctr images import` step that was
# slow and OOM-prone on PI2.
#
# Usage:
#   ssh strider@192.168.1.105
#   cd /home/strider/adventuretube-microservice
#   bash scripts/install-nerdctl-pi2.sh

set -euo pipefail

NERDCTL_VERSION="2.0.3"
ARCH="arm64"
TARBALL="nerdctl-full-${NERDCTL_VERSION}-linux-${ARCH}.tar.gz"
URL="https://github.com/containerd/nerdctl/releases/download/v${NERDCTL_VERSION}/${TARBALL}"

K3S_CONTAINERD_SOCK="/run/k3s/containerd/containerd.sock"
BUILDKIT_SERVICE="/etc/systemd/system/buildkit.service"

echo "=== Installing nerdctl + buildkit ${NERDCTL_VERSION} for ${ARCH} ==="

# 1. Download and extract nerdctl-full (includes buildkit, CNI plugins, runc)
cd /tmp
if [ ! -f "${TARBALL}" ]; then
    echo "Downloading ${URL}..."
    curl -fSL -o "${TARBALL}" "${URL}"
fi

echo "Extracting to /usr/local..."
sudo tar -Cxzvf /usr/local "${TARBALL}" > /dev/null

# 2. Create systemd unit for buildkitd pointed at K3s containerd
echo "Creating buildkit systemd service..."
sudo tee "${BUILDKIT_SERVICE}" > /dev/null <<EOF
[Unit]
Description=BuildKit (points at K3s containerd)
Documentation=https://github.com/moby/buildkit
After=k3s.service k3s-agent.service

[Service]
ExecStart=/usr/local/bin/buildkitd \\
    --oci-worker=false \\
    --containerd-worker=true \\
    --containerd-worker-addr=${K3S_CONTAINERD_SOCK} \\
    --containerd-worker-namespace=k8s.io
Restart=always

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now buildkit.service

# 3. Smoke-test
echo ""
echo "=== Verifying installation ==="
nerdctl --version
echo ""
echo "Listing images in K3s containerd (namespace=k8s.io):"
sudo nerdctl --address "${K3S_CONTAINERD_SOCK}" --namespace k8s.io images | head -10
echo ""
echo "=== Install complete ==="
echo "To build into K3s containerd, use:"
echo "  sudo nerdctl --address ${K3S_CONTAINERD_SOCK} --namespace k8s.io compose build"
