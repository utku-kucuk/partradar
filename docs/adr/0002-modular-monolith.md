# 2. Modular monolith with Spring Modulith

**Status:** Superseded by [0004](0004-microservices.md)

## Context
The domain has clear sub-areas (identity, shops, catalog, inquiries, routing, inventory, billing, ...) but the team is one
person and the product is pre-validation. Microservices would add network, deployment and data-consistency cost without benefit.

## Decision
Build a single Spring Boot application split into top-level packages per business module under `com.partradar`.
Spring Modulith verifies that modules only use each other's public API. Cross-module side effects are communicated through
application events, persisted in the event publication registry so they survive restarts (transactional outbox).

## Consequences
- One deployable, one database, simple local development and cheap hosting.
- Boundaries are tested in CI (`ApplicationModules.verify()`), so a module can be extracted into a service later.
- Modules must not share JPA entities; they reference each other by ID.
