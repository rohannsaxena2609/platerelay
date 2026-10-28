# API outline

Base path: `/api/v1`. Timestamps use RFC 3339 UTC. Coordinate input uses WGS 84
decimal degrees. Error responses use `application/problem+json` (RFC 9457).
Every API route requires a bearer access token. Donor, recipient, and admin
roles are checked on write operations; donor and recipient IDs are taken from
the signed token and must match the requested resource. Local demo identities
are provisioned by the included Keycloak realm import.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/donations` | Publish a time-bounded surplus lot. |
| `GET` | `/donations?page=0&size=20` | Browse available donations, ordered by soonest expiry. Page size is capped at 100. |
| `GET` | `/donations/{id}` | Read one donation and its current lifecycle state. |
| `POST` | `/donations/{id}/cancel` | Cancel an available donation. |
| `POST` | `/recipients` | Admin registers a recipient pickup location and capacity. |
| `GET` | `/recipients` | List registered recipients. |
| `GET` | `/recipients/{id}` | Read one recipient profile. |
| `GET` | `/matches` | Rank available donations for a coordinate and optional dietary tags. |
| `GET` | `/matches/recipients/{id}` | Rank matches for the signed-in recipient's location and tags. |
| `POST` | `/donations/{id}/claims` | Reserve an available donation for one recipient. |
| `POST` | `/claims/{id}/complete` | Confirm the physical handoff. |
| `POST` | `/claims/{id}/release` | Release a claim before pickup. |

`GET /matches?latitude=...&longitude=...&radiusKm=...&dietaryTags=...` returns
at most 50 available offers within a 0.1–50 km radius. The match score is
explainable: 70% proximity and 30% expiry urgency over a 72-hour window. The
response includes distance, minutes until expiry, score, and human-readable
reasons. Candidate selection uses a coordinate bounding box followed by an
exact Haversine distance check.

The `Idempotency-Key` header on claim creation is optional. When supplied, a
retry with the same key, donation, and recipient returns the original claim.
Keys are unique and limited to 128 characters.

## Donation lifecycle

```text
AVAILABLE ──claim──> RESERVED ──complete──> COMPLETED
    │                    │
    ├──expiry──> EXPIRED  ├──release──> AVAILABLE
    └──cancel──> CANCELLED└──expiry──> AVAILABLE / EXPIRED
```

The database transaction that moves a donation from `AVAILABLE` to `RESERVED`
is the concurrency boundary. A losing concurrent claimant receives `409
Conflict`; it never receives a second active claim.

