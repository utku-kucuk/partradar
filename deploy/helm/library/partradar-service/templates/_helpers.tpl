{{/*
Labels shared by every object of one service.
Usage: include "partradar-service.labels" (dict "root" $ "name" "shop-service")
*/}}
{{- define "partradar-service.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end }}

{{- define "partradar-service.labels" -}}
{{ include "partradar-service.selectorLabels" . }}
app.kubernetes.io/part-of: partradar
app.kubernetes.io/managed-by: {{ .root.Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .root.Chart.Name .root.Chart.Version }}
{{- end }}

{{/*
Image reference: <registry>/partradar-<name>:<tag>. The tag is the service's own `tag` (set per image by
Argo CD Image Updater), else global.image.tag, else the chart's appVersion.
*/}}
{{- define "partradar-service.image" -}}
{{- $image := .root.Values.global.image -}}
{{- $tag := .service.tag | default $image.tag | default .root.Chart.AppVersion -}}
{{- printf "%s/partradar-%s:%s" $image.registry .name $tag -}}
{{- end }}
