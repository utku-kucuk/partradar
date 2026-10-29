{{/*
Every object of one service.
Usage: include "partradar-service.all" (dict "root" $ "name" "shop-service" "service" $values)

$values:
  port          HTTP port of the application (also used by the Kubernetes Service)
  tag           image tag of this service, overrides global.image.tag
  env           map of plain environment variables, rendered into a ConfigMap
  secretEnv     map of NAME -> {secret, key} read from existing Secrets
  replicas      default 1, ignored when autoscaling is enabled
  resources     container resources
  autoscaling   {enabled, minReplicas, maxReplicas, targetCPUUtilizationPercentage}
*/}}
{{- define "partradar-service.all" -}}
{{ include "partradar-service.configmap" . }}
---
{{ include "partradar-service.deployment" . }}
---
{{ include "partradar-service.service" . }}
{{- if (.service.autoscaling).enabled }}
---
{{ include "partradar-service.hpa" . }}
{{- end }}
{{- end }}

{{- define "partradar-service.configmap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .name }}
  labels:
    {{- include "partradar-service.labels" . | nindent 4 }}
data:
  PORT: {{ .service.port | quote }}
  SPRING_PROFILES_ACTIVE: prod
  {{- range $key, $value := .service.env }}
  {{ $key }}: {{ tpl (toString $value) $.root | quote }}
  {{- end }}
{{- end }}

{{- define "partradar-service.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .name }}
  labels:
    {{- include "partradar-service.labels" . | nindent 4 }}
spec:
  {{- if not (.service.autoscaling).enabled }}
  replicas: {{ .service.replicas | default 1 }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "partradar-service.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "partradar-service.labels" . | nindent 8 }}
      annotations:
        # Roll the pods when their configuration changes.
        checksum/config: {{ include "partradar-service.configmap" . | sha256sum }}
    spec:
      automountServiceAccountToken: false
      securityContext:
        runAsNonRoot: true
        runAsUser: 1001
        runAsGroup: 1001
        seccompProfile:
          type: RuntimeDefault
      # Spring Boot drains in-flight requests on SIGTERM (server.shutdown: graceful).
      terminationGracePeriodSeconds: 45
      containers:
        - name: {{ .name }}
          image: {{ include "partradar-service.image" . }}
          imagePullPolicy: {{ .root.Values.global.image.pullPolicy }}
          ports:
            - name: http
              containerPort: {{ .service.port }}
          envFrom:
            - configMapRef:
                name: {{ .name }}
          {{- with .service.secretEnv }}
          env:
            {{- range $key, $ref := . }}
            - name: {{ $key }}
              valueFrom:
                secretKeyRef:
                  name: {{ tpl $ref.secret $.root }}
                  key: {{ $ref.key }}
            {{- end }}
          {{- end }}
          startupProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 5
            failureThreshold: 60
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: http
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
            periodSeconds: 5
          lifecycle:
            # Give the Service time to stop routing to the pod before shutdown begins.
            preStop:
              sleep:
                seconds: 5
          resources:
            {{- toYaml .service.resources | nindent 12 }}
          securityContext:
            allowPrivilegeEscalation: false
            readOnlyRootFilesystem: true
            capabilities:
              drop: [ALL]
          volumeMounts:
            - name: tmp
              mountPath: /tmp
      volumes:
        - name: tmp
          emptyDir: {}
{{- end }}

{{- define "partradar-service.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ .name }}
  labels:
    {{- include "partradar-service.labels" . | nindent 4 }}
spec:
  selector:
    {{- include "partradar-service.selectorLabels" . | nindent 4 }}
  ports:
    - name: http
      port: {{ .service.port }}
      targetPort: http
{{- end }}

{{- define "partradar-service.hpa" -}}
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ .name }}
  labels:
    {{- include "partradar-service.labels" . | nindent 4 }}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ .name }}
  minReplicas: {{ .service.autoscaling.minReplicas | default 1 }}
  maxReplicas: {{ .service.autoscaling.maxReplicas | default 3 }}
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: {{ .service.autoscaling.targetCPUUtilizationPercentage | default 70 }}
{{- end }}
