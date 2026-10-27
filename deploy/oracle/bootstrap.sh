#!/usr/bin/env bash
# One-time setup of the live environment on an Oracle Cloud Always Free VM (Ampere A1, Ubuntu 24.04 arm64).
# Installs k3s and Argo CD and hands over to Git: Argo CD then installs everything in deploy/argocd.
# Safe to run again (upgrades in place).
#
#   git clone https://github.com/utku-kucuk/partradar.git && cd partradar && deploy/oracle/bootstrap.sh
set -euo pipefail

cd "$(dirname "$0")/../.."

K3S_VERSION=v1.37.1+k3s1
ARGOCD_CHART_VERSION=10.11.6

echo "==> Firewall: let pod traffic through the image's default REJECT rule"
# Oracle's Ubuntu images reject forwarded packets, which breaks pod networking. Inbound traffic stays
# limited by the VCN security list (SSH only); the INPUT chain is left untouched.
if sudo iptables -C FORWARD -j REJECT --reject-with icmp-host-prohibited 2>/dev/null; then
  sudo iptables -D FORWARD -j REJECT --reject-with icmp-host-prohibited
  sudo netfilter-persistent save
fi

echo "==> k3s $K3S_VERSION"
sudo mkdir -p /var/lib/rancher/k3s/server/manifests
sudo cp deploy/oracle/traefik-config.yaml /var/lib/rancher/k3s/server/manifests/traefik-config.yaml
# No service load balancer: the only way in is the outbound Cloudflare Tunnel.
curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION="$K3S_VERSION" sh -s - server --disable servicelb
mkdir -p ~/.kube
sudo install -m 600 -o "$USER" /etc/rancher/k3s/k3s.yaml ~/.kube/config
kubectl wait --for=condition=Ready node --all --timeout=180s

if ! command -v helm >/dev/null; then
  echo "==> Helm"
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-4 | bash
fi

echo "==> Argo CD"
helm repo add argo https://argoproj.github.io/argo-helm --force-update >/dev/null
helm upgrade --install argocd argo/argo-cd --version "$ARGOCD_CHART_VERSION" \
  --namespace argocd --create-namespace -f deploy/oracle/argocd-values.yaml --wait

echo "==> Root application (everything else comes from Git)"
kubectl apply -f deploy/argocd/root.yaml

echo "==> Waiting for the Sealed Secrets controller"
until kubectl -n kube-system get secret -l sealedsecrets.bitnami.com/sealed-secrets-key -o name 2>/dev/null | grep -q .; do
  sleep 5
done
kubectl -n kube-system get secret -l sealedsecrets.bitnami.com/sealed-secrets-key \
  -o jsonpath='{.items[0].data.tls\.crt}' | base64 -d > deploy/oracle/sealed-secrets.pem

echo
echo "Done. Next steps (see deploy/oracle/README.md):"
echo "  1. Commit deploy/oracle/sealed-secrets.pem (public key) and run deploy/oracle/seal-secrets.sh on your machine."
echo "  2. Argo CD admin password: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
