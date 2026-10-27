# 3. Cloud Run + Neon + Vercel on free tiers

**Status:** Superseded by [0006](0006-kubernetes-with-helm.md)

## Context
The project runs on demo data and must cost nothing per month, while still using a production-style platform with
infrastructure as code and keyless CI/CD.

## Decision
- Backend container on **Google Cloud Run** (always-free tier, scale to zero), images in Artifact Registry.
- **Neon** serverless PostgreSQL (free tier, supports PostGIS and pg_trgm).
- Frontend on **Vercel** (Hobby).
- Provisioned with **Terraform**; GitHub Actions authenticates to GCP through Workload Identity Federation (no stored keys).
- A $1 budget alert guards against unexpected charges.

AWS was considered. Its free plan is credit-based and time-limited, and a managed database plus a container service
exceeds a $0 budget.

## Consequences
- Cold starts after idle periods; mitigated with Spring AOT and Class Data Sharing.
- Neon free compute suspends when idle, adding a short delay to the first query.
