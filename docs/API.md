# API outline

Base path: `/api/v1`. Timestamps use RFC 3339 UTC. Coordinate input uses WGS 84
decimal degrees. Error responses use `application/problem+json` (RFC 9457).

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/donations` | Publish a time-bounded surplus lot. |
| `GET` | `/donations` | Browse available donations with optional category, tag, and radius filters. |
| `GET` | `/donations/{id}` | Read one donation and its current lifecycle state. |
| `POST` | `/recipients` | Register a recipient pickup location and capacity. |
| `GET` | `/matches` | Rank available donations for a pickup coordinate. |
| `POST` | `/donations/{id}/claims` | Reserve an available donation for one recipient. |
| `POST` | `/claims/{id}/complete` | Confirm the physical handoff. |
| `DELETE` | `/claims/{id}` | Release a claim before pickup. |

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

