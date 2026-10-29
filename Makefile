SHELL := /bin/bash
.DEFAULT_GOAL := help

COMPOSE  := docker compose -f deploy/compose/compose.yaml
SERVICES := api-gateway shop-service inquiry-service notification-service
SERVICE  ?= shop-service
# Custom resources (Argo CD, CloudNativePG, ...) are validated against the community CRD schema catalog.
# The catalog's ImageUpdater schema is older than the installed CRD, so that kind is skipped.
KUBECONFORM := kubeconform -strict -summary -schema-location default \
	-schema-location 'https://raw.githubusercontent.com/datreeio/CRDs-catalog/main/{{.Group}}/{{.ResourceKind}}_{{.ResourceAPIVersion}}.json'

.PHONY: help build test format infra-up infra-down infra-reset up down ps logs run images helm-lint k8s-up k8s-smoke k8s-down

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "} {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

## --- Build ------------------------------------------------------------------
build: ## Compile, check formatting and run all tests (needs Docker for Testcontainers)
	./mvnw -B verify

test: ## Run the tests of one service: make test SERVICE=inquiry-service
	./mvnw -B verify -pl services/$(SERVICE) -am

format: ## Apply google-java-format to all modules
	./mvnw -B -q spotless:apply

images: ## Build a Docker image for every service
	@for s in $(SERVICES); do \
		docker build -f deploy/docker/Dockerfile --build-arg SERVICE=$$s -t partradar/partradar-$$s:local . || exit 1; \
	done

## --- Local environment ------------------------------------------------------
infra-up: ## Start infrastructure and wait until it is healthy
	$(COMPOSE) up -d --wait

infra-down: ## Stop infrastructure, keep data
	$(COMPOSE) down

infra-reset: ## Stop everything and delete all data volumes
	$(COMPOSE) --profile apps down -v

up: ## Build and start infrastructure plus all services as containers
	$(COMPOSE) --profile apps up -d --build --wait

down: ## Stop infrastructure and services, keep data
	$(COMPOSE) --profile apps down

ps: ## Show container status
	$(COMPOSE) --profile apps ps

logs: ## Follow logs of all containers or one: make logs [C=kafka]
	$(COMPOSE) --profile apps logs -f $(C)

run: infra-up ## Run one service from source against local infrastructure: make run SERVICE=shop-service
	./mvnw -B -q install -pl libs/platform-common -am -DskipTests
	./mvnw -B spring-boot:run -pl services/$(SERVICE)

## --- Kubernetes (kind) ------------------------------------------------------
helm-lint: ## Lint the Helm chart and validate the rendered and Argo CD manifests
	helm dependency build deploy/helm/partradar >/dev/null
	helm lint deploy/helm/partradar -f deploy/helm/partradar/values-kind.yaml
	helm lint deploy/helm/partradar -f deploy/helm/partradar/values-prod.yaml
	helm template partradar deploy/helm/partradar -f deploy/helm/partradar/values-prod.yaml | $(KUBECONFORM)
	$(KUBECONFORM) -skip ImageUpdater deploy/argocd/root.yaml deploy/argocd/apps deploy/argocd/cloudflared deploy/oracle/traefik-config.yaml

k8s-up: ## Create a kind cluster, build the images and install the chart
	deploy/kind/k8s-up.sh

k8s-smoke: ## Run the end-to-end smoke test against the kind cluster
	deploy/kind/smoke-test.sh

k8s-down: ## Delete the kind cluster
	kind delete cluster --name partradar
