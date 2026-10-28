#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${PLATERELAY_API_URL:-http://localhost:8080}"
IDENTITY_BASE_URL="${PLATERELAY_IDENTITY_URL:-http://localhost:9090}"
DONOR_USER="${PLATERELAY_DONOR_USER:-donor-dev}"
DONOR_PASSWORD="${PLATERELAY_DONOR_PASSWORD:-donor-local-2026}"
RECIPIENT_USER="${PLATERELAY_RECIPIENT_USER:-recipient-dev}"
RECIPIENT_PASSWORD="${PLATERELAY_RECIPIENT_PASSWORD:-recipient-local-2026}"
ADMIN_USER="${PLATERELAY_ADMIN_USER:-admin-dev}"
ADMIN_PASSWORD="${PLATERELAY_ADMIN_PASSWORD:-admin-local-2026}"

token_for() {
  local username="$1"
  local password="$2"
  curl --silent --show-error --fail-with-body \
    "${IDENTITY_BASE_URL}/realms/platerelay/protocol/openid-connect/token" \
    --data-urlencode 'client_id=platerelay-cli' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "username=${username}" \
    --data-urlencode "password=${password}" | jq -er '.access_token'
}

api() {
  curl --silent --show-error --fail-with-body "$@"
}

ADMIN_TOKEN="$(token_for "$ADMIN_USER" "$ADMIN_PASSWORD")"
DONOR_TOKEN="$(token_for "$DONOR_USER" "$DONOR_PASSWORD")"
RECIPIENT_TOKEN="$(token_for "$RECIPIENT_USER" "$RECIPIENT_PASSWORD")"

if ! api -H "Authorization: Bearer ${ADMIN_TOKEN}" "${API_BASE_URL}/api/v1/recipients" \
    | jq -e 'any(.[]; .id == "fridge-east")' >/dev/null; then
  api --request POST "${API_BASE_URL}/api/v1/recipients" \
    --header "Authorization: Bearer ${ADMIN_TOKEN}" \
    --header 'Content-Type: application/json' \
    --data '{"id":"fridge-east","name":"Eastside Community Fridge","latitude":12.9730,"longitude":77.6000,"dailyCapacity":30,"dietaryTags":["VEGAN","GLUTEN_FREE"]}' \
    | jq '{id, name, dailyCapacity}'
fi

AVAILABLE_UNTIL="$(python3 -c 'from datetime import datetime, timedelta, timezone; print((datetime.now(timezone.utc) + timedelta(hours=3)).isoformat(timespec="seconds").replace("+00:00", "Z"))')"
DONATION="$(api --request POST "${API_BASE_URL}/api/v1/donations" \
  --header "Authorization: Bearer ${DONOR_TOKEN}" \
  --header 'Content-Type: application/json' \
  --data "{\"donorId\":\"market-17\",\"title\":\"Fresh vegetable boxes\",\"description\":\"Seasonal produce packed this afternoon\",\"quantity\":12,\"unit\":\"BOX\",\"availableUntil\":\"${AVAILABLE_UNTIL}\",\"pickup\":{\"latitude\":12.9716,\"longitude\":77.5946},\"dietaryTags\":[\"VEGAN\",\"GLUTEN_FREE\"]}")"
DONATION_ID="$(jq -er '.id' <<<"${DONATION}")"
printf 'Published donation %s\n' "$DONATION_ID"

MATCHES="$(api "${API_BASE_URL}/api/v1/matches/recipients/fridge-east?radiusKm=8" \
  --header "Authorization: Bearer ${RECIPIENT_TOKEN}")"
printf '%s\n' "$MATCHES" | jq '.[0] | {title: .donation.title, distanceKm, score, reasons}'

CLAIM="$(api --request POST "${API_BASE_URL}/api/v1/donations/${DONATION_ID}/claims" \
  --header "Authorization: Bearer ${RECIPIENT_TOKEN}" \
  --header "Idempotency-Key: $(python3 -c 'import uuid; print(uuid.uuid4())')" \
  --header 'Content-Type: application/json' \
  --data '{"recipientId":"fridge-east"}')"
CLAIM_ID="$(jq -er '.id' <<<"${CLAIM}")"
printf 'Reserved claim %s\n' "$CLAIM_ID"

api --request POST "${API_BASE_URL}/api/v1/claims/${CLAIM_ID}/complete" \
  --header "Authorization: Bearer ${RECIPIENT_TOKEN}" | jq '{id, status, completedAt}'
