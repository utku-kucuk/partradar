# 5. Kafka for domain events, RabbitMQ for commands

**Status:** Accepted

## Context
Services need two different kinds of asynchronous communication:

1. **Domain events**: facts that already happened (`InquiryCreated`, `InquiryRouted`, `QuoteSubmitted`).
   Several services may react, new consumers may appear later and need history, and order per inquiry matters.
2. **Commands / jobs**: work that one worker must do exactly once from the business point of view
   (`SendEmail` to a shop or a customer). It needs per-message acknowledgement, delayed retries and a place
   to park messages that keep failing.

One broker can be bent to do both, but each model fits one broker much better.

## Decision
- **Apache Kafka (KRaft mode)** carries domain events.
  - One topic per aggregate (`inquiry.events`, `shop.events`), keyed by aggregate ID to keep per-inquiry ordering.
  - Events are retained so a new or recovering consumer can replay them.
  - Producers publish through the transactional outbox; consumers commit offsets after their own transaction.
- **RabbitMQ** carries commands to `notification-service`.
  - Direct exchange `notification.commands`, one queue per channel (`email`).
  - Failed messages go to a retry queue with a TTL and dead-letter back to the work queue; after N attempts they land
    in `notification.dlq` for inspection.
- Messages are JSON with an envelope (`id`, `type`, `version`, `occurredAt`, `traceparent`, `payload`).
  Contracts are documented in `docs/asyncapi.yaml`.

## Consequences
- The two use cases get the semantics they need: replayable, ordered streams on one side, acknowledged
  work queues with retry and dead-lettering on the other.
- Two brokers to run and monitor. On Kubernetes they are managed by operators (Strimzi, RabbitMQ Cluster Operator),
  which keeps the configuration declarative.
- Trace context is propagated through message headers so one request can be followed across both brokers.
