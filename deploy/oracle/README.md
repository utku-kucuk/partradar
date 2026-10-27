# Live environment on Oracle Cloud

https://partradar.utkukucuk.com runs on one Oracle Cloud Always Free VM with k3s, deployed by Argo CD from this
repository and reachable only through a Cloudflare Tunnel ([ADR-0007](../../docs/adr/0007-free-live-environment.md)).

```
browser ──https──► Cloudflare (TLS, WAF, Access) ◄──outbound tunnel── cloudflared ──► Traefik ──► /api  api-gateway
                                                                       (k3s on Oracle Cloud)          /auth Keycloak
GitHub ── main ──► GHCR images (sha-*) ◄── Argo CD Image Updater ──► Argo CD ──► cluster state from deploy/argocd
```

## 1. Virtual machine
1. Oracle Cloud account with home region Frankfurt (cannot be changed later). Upgrade it to *Pay As You Go*:
   Always Free resources stay free, idle instances are not reclaimed, and A1 capacity is easier to get.
   Create a budget with an alert at $1.
2. Compute → Instances → Create: shape `VM.Standard.A1.Flex` with 4 OCPU / 24 GB, image Ubuntu 24.04 (aarch64),
   boot volume 100 GB, your SSH public key.
3. The subnet's security list allows inbound TCP 22 only, ideally from your own IP. No ports 80/443.

## 2. Bootstrap
```bash
ssh ubuntu@<vm-ip>
git clone https://github.com/utku-kucuk/partradar.git && cd partradar
deploy/oracle/bootstrap.sh
```
Commit the printed `deploy/oracle/sealed-secrets.pem` (it is the public half of the key).

## 3. Cloudflare Tunnel and Access
1. Zero Trust (free plan) → Networks → Tunnels → Create a tunnel `partradar` (type *cloudflared*), copy its token.
2. Public hostnames of the tunnel:

   | Hostname | Service |
   |---|---|
   | `partradar.utkukucuk.com` | `http://traefik.kube-system.svc.cluster.local:80` |
   | `argocd.utkukucuk.com` | `http://argocd-server.argocd.svc.cluster.local:80` |

3. Access → Applications → Self-hosted `argocd.utkukucuk.com`, policy *Allow* for your e-mail address only.
4. On your machine: `brew install kubeseal && deploy/oracle/seal-secrets.sh`, commit `deploy/argocd/secrets/*.yaml`
   through a pull request. After the merge Argo CD starts the tunnel and the site is live.

## Operations
- Deploys: every merge to `main` builds `sha-*` images; Image Updater rolls them out within a few minutes.
- State: `kubectl get applications -n argocd`, or the Argo CD UI.
- Rotate a secret: run `seal-secrets.sh` again and merge the new files.
- Smoke test from anywhere: `API=https://partradar.utkukucuk.com AUTH=https://partradar.utkukucuk.com/auth deploy/kind/smoke-test.sh`
  (registers a test shop).
