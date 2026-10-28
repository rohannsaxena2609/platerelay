<div align="center">
  <img src="docs/platerelay-banner.svg" alt="PlateRelay — rescue food, route hope" width="100%" />
  <br />
  <p><strong>The real-time food rescue API.</strong><br />Move surplus meals from local businesses to the people and community fridges that can use them.</p>
  <p>
    <img alt="Java 25" src="https://img.shields.io/badge/Java-25-ef6c35?style=for-the-badge&logo=openjdk&logoColor=white" />
    <img alt="Spring Boot 4" src="https://img.shields.io/badge/Spring_Boot-4-6db33f?style=for-the-badge&logo=springboot&logoColor=white" />
    <img alt="PostgreSQL" src="https://img.shields.io/badge/PostgreSQL-18-4169e1?style=for-the-badge&logo=postgresql&logoColor=white" />
    <img alt="MIT license" src="https://img.shields.io/badge/License-MIT-8b5cf6?style=for-the-badge" />
  </p>
</div>

## Why PlateRelay?

Every day, good food expires while a community fridge nearby sits empty.
PlateRelay turns that timing problem into a coordination problem the backend
can solve: publish a surplus lot, discover nearby matches, reserve it safely,
and confirm the handoff before the pickup window closes.

### At a glance

| Capability | What it does |
| --- | --- |
| **Live listings** | Filter available donations by radius, category, dietary tags, and pickup deadline. |
| **Explainable matching** | Rank recipient opportunities by proximity and urgency with transparent match reasons. |
| **Safe reservations** | Atomic claim transitions prevent two recipients from collecting the same lot. |
| **Expiry lifecycle** | Scheduled cleanup closes expired offers and releases stale reservations. |
| **Built for operations** | PostgreSQL, Flyway, Docker Compose, health checks, metrics, and OpenAPI. |

## The rescue loop

```mermaid
sequenceDiagram
    participant D as Donor
    participant P as PlateRelay API
    participant R as Nearby recipient
    D->>P: Publish surplus lot + pickup window
    P-->>R: Ranked nearby match with distance and expiry
    R->>P: Reserve lot (atomic claim)
    P-->>D: Reservation confirmed
    R->>P: Confirm pickup
    P-->>D: Rescue completed
```

## Tech stack

- Java 25 LTS and Spring Boot 4
- Spring MVC, Jakarta Validation, Spring Data JPA
- PostgreSQL with Flyway migrations
- Spring Security resource-server support
- OpenAPI 3, Actuator, Micrometer / Prometheus
- Docker Compose for local development

## Quick start

> Prerequisites: JDK 25, Docker, and Docker Compose.

```bash
git clone https://github.com/rohannsaxena2609/platerelay.git
cd platerelay
docker compose up --build
```

The API will be available at `http://localhost:8080`. Interactive API docs are
at `/swagger-ui.html`; health is at `/actuator/health`.

## API preview

```http
POST /api/v1/donations
Content-Type: application/json

{
  "donorId": "market-17",
  "title": "Fresh vegetable boxes",
  "description": "Seasonal produce packed this afternoon",
  "quantity": 12,
  "unit": "BOX",
  "availableUntil": "2026-10-28T18:30:00Z",
  "pickup": { "latitude": 12.9716, "longitude": 77.5946 },
  "dietaryTags": ["VEGAN", "GLUTEN_FREE"]
}
```

```http
GET /api/v1/matches?latitude=12.9716&longitude=77.5946&radiusKm=8
POST /api/v1/donations/{donationId}/claims
POST /api/v1/claims/{claimId}/complete
```

## Project map

```text
src/main/java/.../platerelay
├── donation/       listing and donation lifecycle
├── recipient/      pickup locations and capacity
├── matching/       geo-aware, explainable ranking
├── claim/          atomic reservation and handoff
└── platform/       persistence, errors, security, observability
```

See [the product brief](docs/PRODUCT.md) and [the API contract](docs/API.md).

## Roadmap

- [x] Product brief, domain boundaries, and API sketch
- [ ] Spring Boot foundation and local database
- [ ] Donation and recipient workflows
- [ ] Geo-aware match ranking and atomic claims
- [ ] Security, observability, and API documentation
- [ ] Containerized local run and CI

## License

MIT. See [LICENSE](LICENSE).

