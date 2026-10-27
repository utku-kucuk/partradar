# 4. Small set of microservices

**Status:** Accepted — supersedes [0002](0002-modular-monolith.md)

## Context
ADR-0002 chose a modular monolith while the domain was still being explored. The core flow is now clear:
a customer creates a part inquiry, it is routed to nearby shops, shops quote, and both sides are notified.
These parts behave differently in production:

- **Routing** is a geo query (PostGIS) that fans out on every inquiry and grows with the number of shops.
- **Notifications** depend on slow, unreliable third parties (SMTP, later SMS/push) and must be retried
  without blocking the request path.
- **Inquiries and quotes** are the transactional core and must stay available when the others are degraded.

The project is also meant to show how such a system is built and operated end to end (deployment, messaging,
observability), which a single deployable would hide.

## Decision
Split the backend into four Spring Boot services, each owning its data:

| Service | Responsibility | Data |
|---|---|---|
| `api-gateway` | Single entry point, JWT validation, routing, timeouts and circuit breakers | — |
| `shop-service` | Shops, categories, location; routes inquiries to nearby shops | `shop` database |
| `inquiry-service` | Inquiries and quotes, their lifecycle and expiry | `inquiry` database |
| `notification-service` | E-mail (later SMS/push) delivery with retries | `notification` database |

- Services never read another service's database. They communicate synchronously over HTTP through the gateway
  only for user-facing queries, and asynchronously through messaging for everything else (see [0005](0005-kafka-and-rabbitmq.md)).
- Events are written in the same transaction as the state change (transactional outbox) and published afterwards.
- Consumers are idempotent: processed message IDs are stored and duplicates ignored.
- Identity is delegated to Keycloak (OIDC). Services are resource servers and trust only gateway-forwarded tokens.
- Shared, non-domain code (error handling, security defaults, messaging helpers) lives in `libs/platform-common`.
  Domain types are **not** shared; each service owns its own copy of the event contracts it consumes.
- One Maven multi-module build, one repository, but each service has its own image, version and deployment.

## Consequences
- Each path scales and fails independently; a mail outage no longer affects creating inquiries.
- Higher operational cost: more deployables, network failures, eventual consistency between services.
  Mitigated with outbox, idempotent consumers, retries with dead-letter queues and distributed tracing.
- Cross-service changes need versioned event contracts, documented in `docs/asyncapi.yaml`.
- Module boundaries proven in the modular monolith map directly to service boundaries, so the migration is mechanical.
