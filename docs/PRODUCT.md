# PlateRelay product brief

## The problem

Good food is discarded because independent grocers, cafés, and community fridges
cannot coordinate quickly enough. A static listing does not tell a nearby
recipient whether food is still available, safe to collect, or already claimed.

## The product

PlateRelay is a backend for time-sensitive food rescue. Donors publish a small
surplus lot with an expiry and pickup window. The service finds nearby recipient
locations, lets one recipient reserve the lot, and records handoff or expiry.

## First release

- Create and browse donations with dietary and handling metadata.
- Register recipient pickup locations with coordinates and capacity.
- Rank available donations by distance, freshness, and pickup urgency.
- Enforce a single active claim per donation with an explicit state machine.
- Expire stale listings and reservations automatically.
- Expose health, metrics, API docs, and a reproducible local stack.

## Product guardrails

- A recommendation is explainable: distance, expiry, and dietary compatibility
  are visible inputs, not a black-box score.
- A successful claim is atomic; simultaneous requests cannot claim the same lot.
- Time is stored as UTC instants and every state transition is auditable.
- The first release is a modular monolith with clear domain boundaries. It can
  add asynchronous delivery without making a broker mandatory for local use.

