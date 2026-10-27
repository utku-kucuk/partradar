#!/usr/bin/env bash
# Creates the kind cluster (if missing), installs the ingress controller and the PostgreSQL operator,
# loads freshly built service images into the node and installs the PartRadar chart.
#
#   deploy/kind/k8s-up.sh            build images and install everything
#   SKIP_BUILD=1 deploy/kind/k8s-up.sh   reuse images already loaded into the cluster
set -euo pipefail

cd "$(dirname "$0")/../.."

CLUSTER=partradar
NAMESPACE=partradar
SERVICES=(api-gateway shop-service inquiry-service notification-service)
TRAEFIK_CHART_VERSION=42.1.3
CNPG_CHART_VERSION=0.29.1

if ! kind get clusters | grep -qx "$CLUSTER"; then
  kind create cluster --config deploy/kind/cluster.yaml
fi
kubectl config use-context "kind-$CLUSTER" >/dev/null

helm repo add traefik https://traefik.github.io/charts --force-update >/dev/null
helm repo add cnpg https://cloudnative-pg.github.io/charts --force-update >/dev/null

echo "==> Ingress controller (Traefik)"
helm upgrade --install traefik traefik/traefik --version "$TRAEFIK_CHART_VERSION" \
  --namespace traefik --create-namespace -f deploy/kind/traefik-values.yaml --wait

echo "==> PostgreSQL operator (CloudNativePG)"
helm upgrade --install cnpg cnpg/cloudnative-pg --version "$CNPG_CHART_VERSION" \
  --namespace cnpg-system --create-namespace --wait

if [[ -z "${SKIP_BUILD:-}" ]]; then
  for service in "${SERVICES[@]}"; do
    echo "==> Image $service"
    image="partradar/partradar-$service:local"
    docker build -q -f deploy/docker/Dockerfile --build-arg SERVICE="$service" -t "$image" . >/dev/null
    kind load docker-image --name "$CLUSTER" "$image"
  done
fi

echo "==> PartRadar"
helm dependency build deploy/helm/partradar >/dev/null
helm upgrade --install partradar deploy/helm/partradar \
  --namespace "$NAMESPACE" --create-namespace -f deploy/helm/partradar/values-kind.yaml \
  --wait --timeout 10m

kubectl -n "$NAMESPACE" get pods
echo
echo "API gateway: http://api.localtest.me   Keycloak: http://auth.localtest.me (admin / admin)"
