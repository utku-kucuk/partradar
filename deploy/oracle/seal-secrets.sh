#!/usr/bin/env bash
# Encrypts the live environment's secrets into deploy/argocd/secrets with the cluster's public key.
# Needs kubectl and kubeseal locally; no access to the cluster is required.
#
#   deploy/oracle/seal-secrets.sh            asks for the Cloudflare Tunnel token, generates a Keycloak admin password
set -euo pipefail

cd "$(dirname "$0")/../.."

CERT=deploy/oracle/sealed-secrets.pem
OUT=deploy/argocd/secrets
[[ -s "$CERT" ]] || { echo "Missing $CERT; run deploy/oracle/bootstrap.sh on the VM first." >&2; exit 1; }

seal() { # namespace name file key=value...
  local namespace=$1 name=$2 file=$3
  shift 3
  local args=()
  for pair in "$@"; do args+=(--from-literal="$pair"); done
  kubectl create secret generic "$name" --namespace "$namespace" "${args[@]}" --dry-run=client -o yaml \
    | kubeseal --cert "$CERT" --format yaml > "$OUT/$file"
  echo "wrote $OUT/$file"
}

read -r -s -p "Cloudflare Tunnel token: " token
echo
[[ -n "$token" ]] || { echo "No token given." >&2; exit 1; }
seal cloudflared cloudflared-token cloudflared-token.yaml "token=$token"

password=$(openssl rand -base64 24 | tr -d '/+=')
seal partradar keycloak-admin keycloak-admin.yaml "username=admin" "password=$password"
echo
echo "Keycloak admin password (store it in your password manager, it is not shown again): $password"
