#!/usr/bin/env bash
# End-to-end check through the ingress: a shop owner signs in with Keycloak and registers a shop
# through the gateway, then a customer finds it with the nearby search.
set -euo pipefail

API=${API:-http://partradar.localtest.me}
AUTH=${AUTH:-http://partradar.localtest.me/auth}
NAMESPACE=${NAMESPACE:-partradar}

fail() {
  echo "FAIL: $*" >&2
  kubectl -n "$NAMESPACE" get pods >&2 || true
  exit 1
}

expect_status() { # expected actual step
  [[ "$2" == "$1" ]] || fail "$3: expected HTTP $1, got $2"
  echo "ok   $3 ($2)"
}

body=$(mktemp)
headers=$(mktemp)
trap 'rm -f "$body" "$headers"' EXIT

shop='{"name":"Smoke Test Parts","phone":"+90 212 555 00 00","address":"Ikitelli OSB, Istanbul",
       "latitude":41.0712,"longitude":28.8026,"categories":["ENGINE","BRAKES"]}'

# A pod that just turned ready can take a moment to be reachable through its Service (endpoint
# propagation), so wait until Keycloak serves the realm and a request travels
# ingress -> gateway -> shop-service and back.
for _ in $(seq 1 60); do
  realm=$(curl -s -o /dev/null -w '%{http_code}' "$AUTH/realms/partradar")
  status=$(curl -s -o /dev/null -w '%{http_code}' -X POST "$API/api/shops" -H 'Content-Type: application/json' -d "$shop")
  [[ "$realm" == 200 && "$status" == 401 ]] && break
  sleep 2
done
[[ "$realm" == 200 ]] || fail "Keycloak realm not reachable at $AUTH (HTTP $realm)"
expect_status 401 "$status" "register without a token is rejected"

sign_in() { # username
  local token
  token=$(curl -sf "$AUTH/realms/partradar/protocol/openid-connect/token" \
    -d grant_type=password -d client_id=partradar-e2e \
    -d username="$1" -d password=demo | jq -r .access_token) || fail "token request for $1"
  [[ -n "$token" && "$token" != null ]] || fail "no access token for $1"
  echo "$token"
}

owner=$(sign_in shop@partradar.dev)
customer=$(sign_in customer@partradar.dev)
echo "ok   shop owner and customer signed in"

status=$(curl -s -o "$body" -D "$headers" -w '%{http_code}' -X POST "$API/api/shops" \
  -H "Authorization: Bearer $owner" -H 'Content-Type: application/json' -d "$shop")
expect_status 201 "$status" "register a shop"
id=$(jq -r .id "$body")
# Links must point at the public host, not at the gateway's view of the service.
location=$(grep -i '^location:' "$headers" | tr -d '\r' | cut -d' ' -f2)
[[ "$location" == "$API/api/shops/$id" ]] || fail "unexpected Location header: $location"
echo "ok   Location header uses the public host"

status=$(curl -s -o "$body" -w '%{http_code}' "$API/api/shops/$id" -H "Authorization: Bearer $customer")
expect_status 200 "$status" "get the shop"

status=$(curl -s -o "$body" -w '%{http_code}' -H "Authorization: Bearer $customer" \
  "$API/api/shops/nearby?lat=41.0700&lon=28.8000&radiusKm=2&category=ENGINE")
expect_status 200 "$status" "nearby search"
jq -e --arg id "$id" 'any(.[]; .id == $id)' "$body" >/dev/null || fail "nearby search does not contain shop $id"
echo "ok   nearby search finds the shop"

echo "Smoke test passed."
