{{/* Name of the CloudNativePG cluster; it also prefixes its Services (-rw) and the app user Secret (-app). */}}
{{- define "partradar.db" -}}
{{ .Release.Name }}-db
{{- end }}

{{- define "partradar.labels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/part-of: partradar
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version }}
{{- end }}
