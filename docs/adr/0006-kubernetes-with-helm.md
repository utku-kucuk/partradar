# 6. Kubernetes with Helm, local cluster with kind

**Status:** Accepted — supersedes [0003](0003-free-tier-cloud-platform.md)

## Context
ADR-0003 targeted Cloud Run, Neon and Vercel free tiers for a single backend container. With four services,
Kafka, RabbitMQ and Keycloak (ADR-0004, ADR-0005), the platform needs a common way to deploy, configure and
observe all of them. A managed Kafka and Kubernetes cluster does not fit a zero budget.

## Decision
- **Kubernetes** is the deployment target. Everything is described with **Helm**:
  - an umbrella chart `deploy/helm/partradar` and a shared library chart for the services
    (Deployment, Service, ConfigMap, Secret, readiness/liveness probes, resource limits, HorizontalPodAutoscaler);
  - infrastructure through operators: **Strimzi** (Kafka), **RabbitMQ Cluster Operator**, **CloudNativePG** (PostgreSQL with PostGIS).
- **kind** is the reference cluster: `make k8s-up` creates a cluster, installs the operators and the chart.
  CI runs the same steps and an end-to-end smoke test on every pull request that touches services or charts.
- Images are built per service and pushed to **GitHub Container Registry**, tagged with the release version and commit SHA.
- Day-to-day development uses Docker Compose for infrastructure (`deploy/compose`) and runs services from the IDE.
- Observability is shipped with the chart: OpenTelemetry exporters to a Grafana LGTM stack (metrics, logs, traces).

## Consequences
- The same manifests run locally and in CI, and could be applied to any conformant cluster later.
- No public live demo by default; the repository documents how to run the full system with one command.
- Running everything locally needs roughly 8 GB of memory for Docker.
