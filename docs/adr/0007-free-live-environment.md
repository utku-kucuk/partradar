# 7. Free live environment: Oracle Cloud, k3s, Argo CD and Cloudflare Tunnel

**Status:** Accepted — extends [0006](0006-kubernetes-with-helm.md)

## Context
ADR-0006 keeps kind as the reference cluster and accepted "no public live demo". A link that people can open
says more than a README, so the project should also run publicly — still at zero cost, and without turning
operations into a second project.

## Decision
- **Compute:** one Oracle Cloud *Always Free* VM (Ampere A1, 4 OCPU / 24 GB, arm64) running **k3s**.
  Images are built for `linux/amd64` and `linux/arm64`, so CI (amd64) and the VM (arm64) run the same tags.
- **Delivery:** **Argo CD** pulls the desired state from `deploy/argocd` (app-of-apps) — the cluster needs no
  credentials in CI and CI needs none for the cluster. **Argo CD Image Updater** rolls out the newest `sha-*`
  image of each service after a merge to `main`.
- **Ingress:** a **Cloudflare Tunnel** (outbound connection from `cloudflared` in the cluster) on
  `partradar.utkukucuk.com`. The VM accepts no inbound traffic except SSH; TLS, DDoS protection and caching are
  handled by Cloudflare. Admin UIs sit behind **Cloudflare Access**.
- **One host:** the gateway is served under `/api` and Keycloak under `/auth` (the web app will take `/`).
  Cloudflare's free certificates cover one subdomain level only, and a single origin needs no CORS.
  kind uses the same layout on `partradar.localtest.me`, so CI tests what runs live.
- **Secrets:** **Sealed Secrets**; only the controller in the cluster can decrypt what is committed.
- **Keycloak** runs in production mode with its own database in the CloudNativePG cluster.

## Consequences
- Public demo at no cost; every merge reaches it without manual steps.
- A single VM is a single point of failure — acceptable for a demo; the manifests stay portable to any cluster.
- Dependence on Cloudflare for ingress and on Oracle's free tier terms. Moving means changing DNS and one Application.
- Messaging (Strimzi, RabbitMQ operator) and observability are added to the same app-of-apps as they arrive.
