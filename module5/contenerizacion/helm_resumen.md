# Helm

## ¿Qué es Helm?

**Helm** es el gestor de paquetes estándar para Kubernetes que simplifica el proceso de definir, instalar y actualizar aplicaciones complejas en clusters de Kubernetes. Helm utiliza "Charts" (paquetes de recursos Kubernetes preconfigurados) que permiten a los desarrolladores y operadores de Kubernetes empaquetar, configurar y desplegar aplicaciones y servicios de manera consistente y reutilizable.

### Conceptos Fundamentales

**Chart**: Paquete que contiene toda la información necesaria para crear una aplicación en Kubernetes, incluyendo templates, valores por defecto, dependencias y metadata.

**Template**: Archivos YAML con placeholders que se renderizan con valores específicos para generar manifests de Kubernetes.

**Values**: Archivo YAML que contiene valores configurables que se usan para renderizar los templates.

**Release**: Instancia de un Chart instalado en un cluster de Kubernetes. Cada release tiene un nombre único.

**Repository**: Repositorio remoto donde se almacenan y comparten Charts (ej: Helm Hub, Artifact Hub).

**Template Functions**: Funciones integradas de Helm para manipulación de datos y lógica condicional (eq, if, range, etc.).

---

## **Herramientas Principales**

### **Helm CLI**
- Herramienta de línea de comandos principal
- Gestión de releases, charts, repositories
- Instalación, actualización y rollback de aplicaciones

### **Helm Charts**
- Estructura de directorios estandarizada
- Templates reutilizables con Go templates
- Configuración por valores YAML

### **Helm Hub/Artifact Hub**
- Repositorio centralizado de Charts públicos
- Búsqueda y descubrimiento de Charts
- Compartir Charts con la comunidad

### **Helmfile**
- Herramienta declarativa para gestionar múltiples releases de Helm
- Gestión de environments y configuraciones
- Orquestación de releases complejos

---

## **Comandos Principales**

### **Gestión de Charts**

```bash
# Crear nuevo chart
helm create myapp

# Instalar chart desde directorio local
helm install myapp ./myapp-chart

# Instalar chart con nombre personalizado
helm install myapp-release ./myapp-chart

# Instalar chart desde repository
helm install stable/nginx-ingress

# Instalar chart con valores personalizados
helm install myapp ./myapp-chart --values values.yaml

# Instalar chart con valores inline
helm install myapp ./myapp-chart --set replicaCount=3 --set image.tag=v2.0

# Listar releases instalados
helm list

# Listar releases en namespace específico
helm list -n default

# Listar todos los releases en todos los namespaces
helm list --all-namespaces

# Ver status de release
helm status myapp-release

# Ver historial de release
helm history myapp-release

# Ver valores de release
helm get values myapp-release

# Ver values con secretos
helm get values myapp-release --reveal-secrets
```

### **Gestión de Repositories**

```bash
# Añadir repository
helm repo add stable https://charts.helm.sh/stable

# Actualizar repositories
helm repo update

# Buscar charts en repositories
helm search repo nginx

# Buscar en Helm Hub
helm search hub wordpress

# Listar repositories configurados
helm repo list

# Remover repository
helm repo remove stable

# Listar charts en repository
helm repo list

# Crear index de repository local
helm repo index ./my-repo
izaciones```

### **Actual y Rollbacks**

```bash
# Actualizar release con nuevo chart
helm upgrade myapp-release ./myapp-chart

# Actualizar con nuevos valores
helm upgrade myapp-release ./myapp-chart --values new-values.yaml

# Actualizar con valores inline
helm upgrade myapp-release ./myapp-chart --set replicaCount=5

# Actualizar con timeout personalizado
helm upgrade myapp-release ./myapp-chart --timeout 300s

# Actualizar en modo interactivo
helm upgrade myapp-release ./myapp-chart --interactive

# Rollback a revisión anterior
helm rollback myapp-release 1

# Rollback a revisión específica
helm rollback myapp-release 3

# Forzar rollback
helm rollback myapp-release 1 --force
```

### **Desarrollo y Testing**

```bash# Renderizar templates sin instalar
helm template myapp ./myapp-chart

# Renderizar con valores específicos
helm template myapp ./myapp-chart --values values.yaml

# Renderizar con namespace
helm template myapp ./myapp-chart --namespace myapp

# Test de instalación (dry run)
helm install myapp ./myapp-chart --dry-run

# Test de actualización (dry run)
helm upgrade myapp-release ./myapp-chart --dry-run

# Verificar templates
helm lint ./myapp-chart

# Verificar con valores específicos
helm lint ./myapp-chart --values values.yaml

# Desinstalar release
helm uninstall myapp-release

# Desinstalar con retención de history
helm uninstall myapp-release --keep-history

# Desinstalar forzado
helm uninstall myapp-release --no-hooks
```

### **Gestión de Dependencias**

```bash
# Añadir dependencias en Chart.yaml
helm dependency build ./myapp-chart

# Actualizar dependencias
helm dependency update ./myapp-chart

# Listar dependencias
helm dependency list ./myapp-chart

# Verificar dependencias
helm dependency verify ./myapp-chart
```

### **Plugin Management**

```bash
# Listar plugins
helm plugin list

# Instalar plugin
helm plugin install https://github.com/helm/helm-2to3

# Desinstalar plugin
helm plugin uninstall 2to3

# Actualizar plugin
helm plugin update 2to3
```

---

## **Estructura de Chart**

### **Chart.yaml**

```yaml
apiVersion: v2
name: myapp
description: A Helm chart for MyApp application
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - application
  - web
  - api
home: https://github.com/myorg/myapp
sources:
  - https://github.com/myorg/myapp
maintainers:
  - name: John Doe
    email: john@example.com
    url: https://github.com/johndoe
dependencies:
  - name: postgresql
    version: "12.1.1"
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
    tags:
      - database
  - name: redis
    version: "17.4.3"
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
annotations:
  category: Application
  licenses: Apache-2.0
```

### **values.yaml**

```yaml
# Configuración global
global:
  imageRegistry: ""
  imagePullSecrets: []
  storageClass: ""

# Imagen del contenedor
image:
  registry: docker.io
  repository: myorg/myapp
  tag: "1.0.0"
  pullPolicy: IfNotPresent
  pullSecrets: []

# Configuración de despliegue
replicaCount: 3

# Configuración del servicio
service:
  type: ClusterIP
  port: 80
  targetPort: 8080

# Configuración de ingress
ingress:
  enabled: true
  className: ""
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: myapp.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: myapp-tls
      hosts:
        - myapp.example.com

# Variables de entorno
env:
  - name: NODE_ENV
    value: production
  - name: DATABASE_URL
    valueFrom:
      secretKeyRef:
        name: myapp-secrets
        key: database-url
  - name: API_KEY
    valueFrom:
      secretKeyRef:
        name: myapp-secrets
        key: api-key

# Configuración de recursos
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 256Mi

# Configuración de autoscaling
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80

# Configuración de health checks
livenessProbe:
  httpGet:
    path: /health
    port: http
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /ready
    port: http
  initialDelaySeconds: 5
  periodSeconds: 5

# Configuración de seguridad
securityContext:
  enabled: true
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true

# Configuración de persistence
persistence:
  enabled: true
  storageClass: ""
  accessMode: ReadWriteOnce
  size: 10Gi

# Configuración de base de datos
postgresql:
  enabled: true
  auth:
    postgresPassword: "admin123"
    database: "myapp"
  primary:
    persistence:
      enabled: true
      size: 20Gi

# Configuración de Redis
redis:
  enabled: true
  auth:
    enabled: true
    password: "redis123"
  master:
    persistence:
      enabled: true
      size: 8Gi

# Configuración de monitoring
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
    path: /metrics

# Configuración de logging
logging:
  enabled: true
  level: INFO

# Configuración de backup
backup:
  enabled: false
  schedule: "0 2 * * *"
  retention: 30

# Configuración de red
networkPolicy:
  enabled: true
  ingress:
    enabled: true
    from:
      - namespaceSelector:
          matchLabels:
            name: ingress-nginx

# Configuración de labels y annotations
podLabels: {}
podAnnotations: {}

# Configuración de toleraciones y node selectors
nodeSelector: {}
tolerations: []
affinity: {}
```

---

## **Templates y Funciones**

### **Deployment Template**

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  {{- if not .Values.autoscaling.enabled }}
  replicas: {{ .Values.replicaCount }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      {{- with .Values.podAnnotations }}
      annotations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      labels:
        {{- include "myapp.selectorLabels" . | nindent 8 }}
    spec:
      {{- with .Values.imagePullSecrets }}
      imagePullSecrets:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      serviceAccountName: {{ include "myapp.serviceAccountName" . }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
      - name: {{ .Chart.Name }}
        securityContext:
          {{- toYaml .Values.securityContext | nindent 12 }}
        image: "{{ .Values.image.registry }}/{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: {{ .Values.service.targetPort }}
          protocol: TCP
        livenessProbe:
          {{- toYaml .Values.livenessProbe | nindent 10 }}
        readinessProbe:
          {{- toYaml .Values.readinessProbe | nindent 10 }}
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
        {{- with .Values.volumeMounts }}
        volumeMounts:
          {{- toYaml . | nindent 10 }}
        {{- end }}
        env:
        {{- range .Values.env }}
        - name: {{ .name }}
          {{- if .value }}
          value: {{ .value | quote }}
          {{- else if .valueFrom }}
          valueFrom:
            {{- toYaml .valueFrom | nindent 12 }}
          {{- end }}
        {{- end }}
      {{- with .Values.volumes }}
      volumes:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
```

### **Service Template**

```yaml
# templates/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
  {{- with .Values.service.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
spec:
  type: {{ .Values.service.type }}
  {{- with .Values.service.clusterIP }}
  clusterIP: {{ . }}
  {{- end }}
  {{- if .Values.service.sessionAffinity }}
  sessionAffinity: {{ .Values.service.sessionAffinity }}
  {{- if .Values.service.sessionAffinityConfig }}
  sessionAffinityConfig: {{- toYaml .Values.service.sessionAffinityConfig | nindent 4 }}
  {{- end }}
  {{- end }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: {{ .Values.service.targetPort }}
      protocol: TCP
      name: http
      {{- if and (eq .Values.service.type "NodePort") .Values.service.nodePort }}
      nodePort: {{ .Values.service.nodePort }}
      {{- end }}
  selector:
    {{- include "myapp.selectorLabels" . | nindent 4 }}
```

### **Ingress Template**

```yaml
# templates/ingress.yaml
{{- if .Values.ingress.enabled -}}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
  {{- with .Values.ingress.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
spec:
  {{- if .Values.ingress.className }}
  ingressClassName: {{ .Values.ingress.className }}
  {{- end }}
  {{- if .Values.ingress.tls }}
  tls:
    {{- range .Values.ingress.tls }}
    - hosts:
        {{- range .hosts }}
        - {{ . | quote }}
        {{- end }}
      secretName: {{ .secretName }}
    {{- end }}
  {{- end }}
  rules:
    {{- range .Values.ingress.hosts }}
    - host: {{ .host | quote }}
      http:
        paths:
          {{- range .paths }}
          - path: {{ .path }}
            pathType: {{ .pathType }}
            backend:
              service:
                name: {{ include "myapp.fullname" $ }}
                port:
                  number: {{ $.Values.service.port }}
          {{- end }}
    {{- end }}
{{- end }}
```

### **ConfigMap Template**

```yaml
# templates/configmap.yaml
{{- if .Values.configMap.enabled }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
data:
  {{- range $key, $value := .Values.configMap.data }}
  {{ $key }}: |
    {{- $value | nindent 4 }}
  {{- end }}
{{- end }}
```

### **Secret Template**

```yaml
# templates/secret.yaml
{{- if .Values.secret.enabled }}
apiVersion: v1
kind: Secret
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
type: Opaque
data:
  {{- range $key, $value := .Values.secret.data }}
  {{ $key }}: {{ $value | b64enc }}
  {{- end }}
{{- end }}
```

---

## **Funciones y Helpers**

### **Helpers Personalizados**

```yaml
# templates/_helpers.tpl
{{/*
Expand the name of the chart.
*/}}
{{- define "myapp.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "myapp.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "myapp.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "myapp.labels" -}}
helm.sh/chart: {{ include "myapp.chart" . }}
{{ include "myapp.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "myapp.selectorLabels" -}}
app.kubernetes.io/name: {{ include "myapp.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "myapp.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "myapp.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Render los valores para la base de datos
*/}}
{{- define "myapp.databaseUrl" -}}
{{- if .Values.postgresql.enabled }}
{{- printf "postgresql://%s:%s@%s-postgresql:%d/%s" 
    .Values.postgresql.auth.username 
    .Values.postgresql.auth.password 
    .Release.Name 
    .Values.postgresql.primary.service.ports.postgresql 
    .Values.postgresql.auth.database
}}
{{- else if .Values.externalDatabase.host }}
{{- printf "%s://%s" .Values.externalDatabase.host .Values.externalDatabase.name }}
{{- end }}
{{- end }}

{{/*
Renderizar valores para Redis
*/}}
{{- define "myapp.redisUrl" -}}
{{- if .Values.redis.enabled }}
{{- printf "redis://:%s@%s-redis-master:%d" 
    .Values.redis.auth.password 
    .Release.Name 
    .Values.redis.master.service.ports.redis
}}
{{- else if .Values.externalRedis.host }}
{{- printf "redis://%s:%s" .Values.externalRedis.host .Values.externalRedis.port }}
{{- end }}
{{- end }}

{{/*
Conditionally render image pull secrets, coming from values (if configured)
*/}}
{{- define "myapp.imagePullSecrets" -}}
{{- include "myapp.defaultImagePullSecrets" . }}
{{- end }}

{{- define "myapp.defaultImagePullSecrets" -}}
{{- with .Values.global.imagePullSecrets }}
imagePullSecrets:
{{- toYaml . | nindent 2 }}
{{- end }}
{{- end }}
```

### **Funciones de Template**

```yaml
# Ejemplos de uso de funciones en templates
{{- /* Funciones de comparación */ -}}
{{- if eq .Values.environment "production" }}
  replicas: {{ .Values.production.replicaCount }}
{{- else }}
  replicas: {{ .Values.development.replicaCount }}
{{- end }}

{{- /* Funciones de string */ -}}
{{- $imageName := printf "%s:%s" .Values.image.repository .Values.image.tag }}

{{- /* Funciones de lista */ -}}
{{- range .Values.ingress.hosts }}
- host: {{ .host }}
  paths:
  {{- range .paths }}
  - path: {{ .path }}
    pathType: {{ .pathType }}
  {{- end }}
{{- end }}

{{- /* Funciones condicionales */ -}}
{{- if .Values.ingress.enabled }}
apiVersion: networking.k8s.io/v1
kind: Ingress
# ... resto del manifest
{{- end }}

{{- /* Funciones de dict/object */ -}}
{{- with .Values.labels }}
labels:
  {{- toYaml . | nindent 2 }}
{{- end }}

{{- /* Funciones de default */ -}}
image: "{{ .Values.image.repository }}:{{ .Values.image.tag | default .Chart.AppVersion }}"

{{- /* Funciones de lookup */ -}}
{{- if (lookup "v1" "Secret" .Release.Namespace "myapp-secrets") }}
apiVersion: v1
kind: Secret
# ...
{{- end }}

{{- /* Funciones de include */ -}}
metadata:
  labels:
    {{- include "myapp.labels" . | nindent 4 }}

{{- /* Funciones de tpl (template) */ -}}
{{- $template := .Values.customTemplate }}
{{- tpl $template . }}
```

---

## **Ejemplos de Configuración Completa**

### **Chart Completo para Aplicación Web**

```yaml
# Chart.yaml
apiVersion: v2
name: myapp-web
description: A Helm chart for MyApp web application
type: application
version: 1.0.0
appVersion: "1.0.0"
dependencies:
  - name: postgresql
    version: "12.1.1"
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
  - name: redis
    version: "17.4.3"
    repository: https://charts.bitnami.com/bitnami
    condition: redis.enabled
  - name: nginx-ingress
    version: "4.7.1"
    repository: https://kubernetes.github.io/ingress-nginx
    condition: nginx-ingress.enabled

---
# values.yaml
global:
  imageRegistry: ""
  imagePullSecrets: []

image:
  registry: docker.io
  repository: myorg/myapp
  tag: "1.0.0"
  pullPolicy: IfNotPresent

replicaCount: 3

imagePullSecrets: []
nameOverride: ""
fullnameOverride: ""

serviceAccount:
  create: true
  annotations: {}
  name: ""

podAnnotations: {}

podSecurityContext: {}
  # fsGroup: 2000

securityContext: {}
  # capabilities:
  #   drop:
  #   - ALL
  # readOnlyRootFilesystem: true
  # runAsNonRoot: true
  # runAsUser: 1000

service:
  type: ClusterIP
  port: 80
  targetPort: 8080
  annotations: {}

ingress:
  enabled: true
  className: ""
  annotations:
    kubernetes.io/ingress.class: nginx
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"
  hosts:
    - host: myapp.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: myapp-tls
      hosts:
        - myapp.example.com

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 256Mi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80

nodeSelector: {}
tolerations: []
affinity: {}

# Configuración de persistence
persistence:
  enabled: true
  storageClass: ""
  accessMode: ReadWriteOnce
  size: 10Gi
  existingClaim: ""

# Configuración de configmap
configMap:
  enabled: true
  data: {}
    # app.properties: |
    #   server.port=8080
    #   logging.level.root=INFO

# Configuración de secrets
secret:
  enabled: true
  data: {}
    # database-password: cGFzc3dvcmQxMjM=

# Configuración de base de datos
postgresql:
  enabled: true
  auth:
    postgresPassword: "admin123"
    database: "myapp"
  primary:
    persistence:
      enabled: true
      size: 20Gi

# Configuración de Redis
redis:
  enabled: true
  auth:
    enabled: true
    password: "redis123"
  master:
    persistence:
      enabled: true
      size: 8Gi

# Configuración de nginx ingress
nginx-ingress:
  enabled: true
  controller:
    service:
      type: LoadBalancer
    publishService:
      enabled: true
  defaultBackend:
    enabled: false

# Configuración de monitoreo
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
    interval: 30s
    path: /metrics

# Configuración de backup
backup:
  enabled: false
  schedule: "0 2 * * *"
  retention: 30

# Configuración de logs
logging:
  enabled: true
  level: INFO
  format: json

# Configuración de red
networkPolicy:
  enabled: true
  ingress:
    enabled: true
    from:
      - namespaceSelector:
          matchLabels:
            name: ingress-nginx
  egress:
    enabled: true
    to:
      - podSelector:
          matchLabels:
            app: postgresql
      - podSelector:
          matchLabels:
            app: redis

# Configuración de labels adicionales
extraLabels: {}

# Configuración de probes
livenessProbe:
  httpGet:
    path: /health
    port: http
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /ready
    port: http
  initialDelaySeconds: 5
  periodSeconds: 5

startupProbe:
  httpGet:
    path: /startup
    port: http
  initialDelaySeconds: 10
  periodSeconds: 5

# Configuración de volúmenes
extraVolumes: []
extraVolumeMounts: []

# Configuración de init containers
initContainers: []

# Configuración de sidecar containers
sidecars: []

# Configuración de external services
externalDatabase:
  enabled: false
  host: ""
  port: 5432
  database: ""
  username: ""
  password: ""

externalRedis:
  enabled: false
  host: ""
  port: 6379
  password: ""

# Configuración de ambiente
environment: production

# Configuración de debug
debug:
  enabled: false
  port: 9229

# Configuración de métricas custom
metrics:
  enabled: true
  port: 9090
  path: /metrics

# Configuración de health checks externos
externalHealthChecks:
  enabled: false
  url: ""
  timeout: 5s
```

### **Templates Adicionales**

```yaml
# templates/serviceaccount.yaml
{{- if .Values.serviceAccount.create -}}
apiVersion: v1
kind: ServiceAccount
metadata:
  name: {{ include "myapp.serviceAccountName" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
  {{- with .Values.serviceAccount.annotations }}
  annotations:
    {{- toYaml . | nindent 4 }}
  {{- end }}
{{- end }}

---
# templates/hpa.yaml
{{- if .Values.autoscaling.enabled }}
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {{ include "myapp.fullname" . }}
  minReplicas: {{ .Values.autoscaling.minReplicas }}
  maxReplicas: {{ .Values.autoscaling.maxReplicas }}
  metrics:
    {{- if .Values.autoscaling.targetCPUUtilizationPercentage }}
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: {{ .Values.autoscaling.targetCPUUtilizationPercentage }}
    {{- end }}
    {{- if .Values.autoscaling.targetMemoryUtilizationPercentage }}
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: {{ .Values.autoscaling.targetMemoryUtilizationPercentage }}
    {{- end }}
  {{- with .Values.autoscaling.behavior }}
  behavior:
    {{- toYaml . | nindent 4 }}
  {{- end }}
{{- end }}

---
# templates/pdb.yaml
{{- if .Values.podDisruptionBudget.enabled }}
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  {{- if .Values.podDisruptionBudget.minAvailable }}
  minAvailable: {{ .Values.podDisruptionBudget.minAvailable }}
  {{- end }}
  {{- if .Values.podDisruptionBudget.maxUnavailable }}
  maxUnavailable: {{ .Values.podDisruptionBudget.maxUnavailable }}
  {{- end }}
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
{{- end }}

---
# templates/networkpolicy.yaml
{{- if .Values.networkPolicy.enabled }}
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  podSelector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  policyTypes:
    {{- if .Values.networkPolicy.ingress.enabled }}
    - Ingress
    {{- end }}
    {{- if .Values.networkPolicy.egress.enabled }}
    - Egress
    {{- end }}
  {{- if .Values.networkPolicy.ingress.enabled }}
  ingress:
    {{- if .Values.networkPolicy.ingress.from }}
    {{- toYaml .Values.networkPolicy.ingress.from | nindent 4 }}
    {{- else }}
    - from:
        - podSelector: {}
    {{- end }}
    {{- if .Values.networkPolicy.ingress.ports }}
    ports:
      {{- toYaml .Values.networkPolicy.ingress.ports | nindent 4 }}
    {{- end }}
  {{- end }}
  {{- if .Values.networkPolicy.egress.enabled }}
  egress:
    {{- if .Values.networkPolicy.egress.to }}
    {{- toYaml .Values.networkPolicy.egress.to | nindent 4 }}
    {{- else }}
    - to:
        - podSelector: {}
    {{- end }}
    {{- if .Values.networkPolicy.egress.ports }}
    ports:
      {{- toYaml .Values.networkPolicy.egress.ports | nindent 4 }}
    {{- end }}
  {{- end }}
{{- end }}

---
# templates/servicemonitor.yaml
{{- if and .Values.monitoring.enabled .Values.monitoring.serviceMonitor.enabled }}
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: {{ include "myapp.fullname" . }}
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
spec:
  selector:
    matchLabels:
      {{- include "myapp.selectorLabels" . | nindent 6 }}
  endpoints:
    - port: http
      path: {{ .Values.monitoring.path }}
      interval: {{ .Values.monitoring.interval }}
      scrapeTimeout: {{ .Values.monitoring.scrapeTimeout }}
{{- end }}
```

---

## **Buenas Prácticas**

### **1. Estructura de Chart**

```
myapp-chart/
├── Chart.yaml
├── values.yaml
├── templates/
│   ├── _helpers.tpl
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── ingress.yaml
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── serviceaccount.yaml
│   ├── hpa.yaml
│   ├── pdb.yaml
│   ├── networkpolicy.yaml
│   └── servicemonitor.yaml
├── charts/  # Dependencies
└── README.md
```

### **2. Configuración de Valores**

```yaml
# values.yaml con estructura clara
# Configuración global
global:
  imageRegistry: ""
  imagePullSecrets: []

# Configuración de imagen
image:
  registry: docker.io
  repository: myorg/myapp
  tag: "1.0.0"
  pullPolicy: IfNotPresent

# Configuración de despliegue
replicaCount: 3

# Configuración de recursos
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 100m
    memory: 256Mi

# Configuración condicional para ambiente
{{- if eq .Values.environment "production" }}
production:
  replicaCount: 5
  resources:
    limits:
      cpu: 1000m
      memory: 1Gi
{{- else }}
development:
  replicaCount: 1
  debug:
    enabled: true
{{- end }}
```

### **3. Seguridad**

```yaml
# securityContext en templates
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  fsGroup: 1000
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
    - ALL
    add:
    - NET_BIND_SERVICE

# ServiceAccount con RBAC
serviceAccount:
  create: true
  annotations: {}
  automountServiceAccountToken: false

# Network Policies
networkPolicy:
  enabled: true
  ingress:
    enabled: true
    from:
      - namespaceSelector:
          matchLabels:
            name: ingress-nginx
  egress:
    enabled: true
    to:
      - podSelector:
          matchLabels:
            app: postgresql
```

### **4. Gestión de Dependencias**

```yaml
# Chart.yaml con dependencias
apiVersion: v2
name: myapp
version: 1.0.0
dependencies:
  - name: postgresql
    version: "12.1.1"
    repository: "https://charts.bitnami.com/bitnami"
    condition: postgresql.enabled
    tags:
      - database
  - name: redis
    version: "17.4.3"
    repository: "https://charts.bitnami.com/bitnami"
    condition: redis.enabled
    tags:
      - cache
  - name: nginx-ingress
    version: "4.7.1"
    repository: "https://kubernetes.github.io/ingress-nginx"
    condition: nginx-ingress.enabled
    alias: ingress

# values.yaml con configuración de dependencias
postgresql:
  enabled: true
  auth:
    postgresPassword: "admin123"
    database: "myapp"

redis:
  enabled: true
  auth:
    enabled: true
    password: "redis123"

nginx-ingress:
  enabled: true
  controller:
    service:
      type: LoadBalancer
```

### **5. Testing y Validación**

```yaml
# tests/ en templates
# tests/test-connection.yaml
apiVersion: v1
kind: Pod
metadata:
  name: "{{ include "myapp.fullname" . }}-test-connection"
  labels:
    {{- include "myapp.labels" . | nindent 4 }}
  annotations:
    "helm.sh/hook": test
spec:
  containers:
    - name: wget
      image: busybox
      command: ['wget']
      args: ['{{ include "myapp.fullname" . }}:{{ .Values.service.port }}/health']
  restartPolicy: Never

# Comandos de testing
helm lint ./myapp-chart
helm template test ./myapp-chart --values values.yaml --dry-run
helm test myapp-release
```

### **6. Configuración por Ambiente**

```yaml
# values-dev.yaml
environment: development
replicaCount: 1
resources:
  limits:
    cpu: 200m
    memory: 256Mi

ingress:
  enabled: false

debug:
  enabled: true
  port: 9229

# values-prod.yaml
environment: production
replicaCount: 5
resources:
  limits:
    cpu: 1000m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 20

ingress:
  enabled: true
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod

securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  readOnlyRootFilesystem: true

monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
```

### **7. Documentación**

```yaml
# README.md para el Chart
# MyApp Helm Chart

## Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- PV provisioner support in the underlying infrastructure

## Parameters

### Global parameters

| Name                      | Description                                  | Value |
| ------------------------- | -------------------------------------------- | ----- |
| `global.imageRegistry`    | Global Docker image registry                  | `""`  |
| `global.imagePullSecrets` | Global Docker registry secret names as an array | `[]`  |

### Common parameters

| Name               | Description                                           | Value           |
| ------------------ | ----------------------------------------------------- | --------------- |
| `nameOverride`     | String to partially override common.names.fullname    | `""`            |
| `fullnameOverride` | String to fully override common.names.fullname        | `""`            |

### Image parameters

| Name                | Description                                        | Value               |
| ------------------- | -------------------------------------------------- | ------------------- |
| `image.registry`    | MyApp image registry                               | `docker.io`         |
| `image.repository`  | MyApp image repository                             | `myorg/myapp`       |
| `image.tag`         | MyApp image tag (immutable tags are recommended)   | `1.0.0`             |
| `image.pullPolicy`  | MyApp image pull policy                            | `IfNotPresent`      |

### Deployment parameters

| Name                  | Description                               | Value  |
| --------------------- | ----------------------------------------- | ------ |
| `replicaCount`       | Number of MyApp replicas                  | `3`    |
| `resources.limits`   | The resources limits for the MyApp container | `{}`   |
| `resources.requests` | The resources requests for the MyApp container | `{}`   |

### Service parameters

| Name                  | Description                               | Value               |
| --------------------- | ----------------------------------------- | ------------------- |
| `service.type`       | MyApp service type                        | `ClusterIP`         |
| `service.port`       | MyApp service port                        | `80`                |
| `service.targetPort` | MyApp service target port                 | `8080`              |

### Ingress parameters

| Name                  | Description                               | Value               |
| --------------------- | ----------------------------------------- | ------------------- |
| `ingress.enabled`    | Enable ingress record for MyApp           | `true`              |
| `ingress.className`  | IngressClass that will be be used         | `""`                |
| `ingress.hosts`      | Host definition                           | `[]`                |
| `ingress.tls`        | TLS configuration                         | `[]`                |

### Persistence parameters

| Name                          | Description                              | Value               |
| ----------------------------- | ---------------------------------------- | ------------------- |
| `persistence.enabled`         | Enable persistence using PVCs            | `true`              |
| `persistence.storageClass`    | PVC Storage Class                        | `""`                |
| `persistence.accessMode`      | PVC Access Mode                          | `ReadWriteOnce`     |
| `persistence.size`            | PVC Size                                 | `10Gi`              |

### Database parameters

| Name                    | Description                      | Value   |
| ----------------------- | -------------------------------- | ------- |
| `postgresql.enabled`    | Enable PostgreSQL database       | `true`  |
| `postgresql.auth.postgresPassword` | PostgreSQL password         | `admin123` |

### Redis parameters

| Name               | Description                    | Value   |
| ------------------ | ------------------------------ | ------- |
| `redis.enabled`    | Enable Redis cache             | `true`  |
| `redis.auth.password` | Redis password              | `redis123` |

## Installing the Chart

To install the chart with the release name `myapp`:

```bash
$ helm install myapp ./myapp-chart
```

## Configuration

Refer to the chart's `values.yaml` file for configuration options.

## Uninstalling the Chart

To uninstall/delete the `myapp` deployment:

```bash
$ helm uninstall myapp
```

## Troubleshooting

### Pod is in CrashLoopBackOff

Check the logs of the failing pod:
```bash
kubectl logs <pod-name>
```

Check the events:
```bash
kubectl describe pod <pod-name>
```

### Database connection issues

Ensure PostgreSQL is running:
```bash
kubectl get pods -l app.kubernetes.io/name=postgresql
```

### Ingress not working

Check the ingress controller:
```bash
kubectl get ingress
kubectl describe ingress myapp-ingress
```
```

---

## **Referencias Oficiales**

1. **Helm Documentation**  
   https://helm.sh/docs/

2. **Helm Chart Template Guide**  
   https://helm.sh/docs/chart_template_guide/

3. **Helm Chart Development Guide**  
   https://helm.sh/docs/chart_development/

4. **Helm Commands Reference**  
   https://helm.sh/docs/helm/

5. **Artifact Hub**  
   https://artifacthub.io/