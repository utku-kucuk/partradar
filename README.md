# PartRadar

> Find the car part you need — in stock, in a shop near you, today.

PartRadar connects drivers and independent mechanics with **physical auto-parts shops and repair services nearby**.
Instead of phoning shops one by one or walking around the industrial zone while the car waits in the garage,
a user describes the vehicle and the part once. The request is routed to the most relevant nearby shops, and they reply
with availability, price and delivery time.

> **Status:** portfolio / proof-of-concept SaaS running on demo data. Under active development.

## Problem
- There is no digital way to know which nearby shop has a part **in stock right now**. Online stores ship by cargo; same-day local stock is invisible.
- Fitment, fair price and which mechanic really knows a job are opaque. Existing reviews are not tied to a real, completed job.
- Local shops have weak digital visibility, answer the same phone questions all day, and cannot measure the demand they miss.

## Solution — phased
| Phase | Capability | Status |
|---|---|---|
| 1 | **Part inquiry:** vehicle + part → routed to nearby shops → quotes (available, price, ETA) | planned |
| 2 | **Shop panel & inventory:** inbox, stock list built from answered requests, analytics, freemium plans | planned |
| 3 | **Verified jobs:** jobs confirmed by a one-time code / QR; only verified jobs can be reviewed | planned |
| 4 | Modification visualizer | roadmap only |

## Architecture
Modular monolith (Spring Modulith) with enforced module boundaries and event-driven communication between modules.
See [`docs/adr`](docs/adr) for the decisions behind it.

```
partradar/
├── backend/            Java 25 · Spring Boot 4 · PostgreSQL/PostGIS
├── frontend/           Next.js · TypeScript · Tailwind
├── infra/terraform/    Google Cloud Run · Neon · Vercel
└── docs/               ADRs and diagrams
```

## Tech stack
**Backend:** Java 25, Spring Boot 4, Spring Modulith, Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL + PostGIS + pg_trgm, OpenAPI, Testcontainers, ArchUnit
**Frontend:** Next.js, TypeScript, Tailwind CSS, TanStack Query, Leaflet/OpenStreetMap
**Platform:** Docker, GitHub Actions, Terraform, Google Cloud Run, Neon Postgres, Vercel

## Getting started
_Coming with the first milestone._

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md).

## License
[MIT](LICENSE)
