# Guía: afiliado-service-chart

## Descripción
Helm Chart para el microservicio de Afiliados, que actúa como **productor de eventos Kafka**.

---

## Estructura del Chart

```
afiliado-service-chart/
├── Chart.yaml           # Metadatos del chart
├── values.yaml          # Valores de configuración por defecto
└── templates/           # Plantillas de recursos Kubernetes
    ├── _helpers.tpl     # Funciones auxiliares reutilizables
    ├── deployment.yaml  # Definición del Deployment
    └── service.yaml     # Definición del Service
```

---

## Archivo: Chart.yaml

Define los metadatos y dependencias del chart.

```yaml
apiVersion: v2
name: afiliado-service
description: Microservicio de Afiliados - Productor Kafka
type: application
version: 0.1.0
appVersion: "1.0.0"
```

### Campos Explicados

| Campo | Valor | Propósito |
|-------|-------|-----------|
| `apiVersion` | `v2` | Versión de la especificación Helm (v2 = Helm 3+) |
| `name` | `afiliado-service` | Nombre del chart, usado en templates |
| `description` | `...` | Descripción legible del chart |
| `type` | `application` | Tipo de chart (`application` o `library`) |
| `version` | `0.1.0` | Versión del chart (SemVer) |
| `appVersion` | `1.0.0` | Versión de la aplicación desplegada |

### Sección keywords

```yaml
keywords:
  - microservices
  - spring-boot
  - kafka
  - producer
```

**Propósito:** Facilita la búsqueda del chart en repositorios Helm.

### Sección maintainers

```yaml
maintainers:
  - name: Team Backend
    email: backend@mutualidad.com
```

**Propósito:** Identifica responsables del chart para contacto.

### Sección dependencies

```yaml
dependencies:
  - name: postgresql
    version: 12.1.9
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
```

**Propósito:** Declara PostgreSQL como dependencia opcional.

| Campo | Propósito |
|-------|-----------|
| `name` | Nombre del chart dependiente |
| `version` | Versión específica a usar |
| `repository` | URL del repositorio Helm |
| `condition` | Variable que habilita/deshabilita la dependencia |

---

## Archivo: values.yaml

Contiene los valores de configuración por defecto. Los usuarios pueden sobreescribirlos al instalar.

### Sección replicaCount

```yaml
replicaCount: 2
```

**Propósito:** Número de réplicas del pod. Con 2 réplicas hay alta disponibilidad.

### Sección image

```yaml
image:
  repository: mutualidad/afiliado-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent
```

| Campo | Propósito |
|-------|-----------|
| `repository` | Nombre de la imagen Docker |
| `tag` | Versión de la imagen |
| `pullPolicy` | Cuándo descargar la imagen (`Always`, `IfNotPresent`, `Never`) |

### Sección service

```yaml
service:
  type: ClusterIP
  port: 80
  targetPort: 8081
```

| Campo | Propósito |
|-------|-----------|
| `type` | Tipo de Service (`ClusterIP` = solo interno, `LoadBalancer` = externo) |
| `port` | Puerto expuesto por el Service |
| `targetPort` | Puerto del contenedor (Spring Boot usa 8081) |

### Sección resources

```yaml
resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi
```

| Campo | Propósito |
|-------|-----------|
| `limits` | Máximo de recursos que puede usar el pod |
| `requests` | Recursos reservados (garantizados) |
| `cpu` | En milicores (500m = 0.5 CPU) |
| `memory` | En bytes (512Mi = 512 MiB) |

**Por qué está aquí:** Kubernetes usa estos valores para scheduling y para evitar que un pod consuma todos los recursos del nodo.

### Sección kafka

```yaml
kafka:
  bootstrapServers: kafka:9092
  topic: afiliado-eventos
```

**Propósito:** Configuración específica de Kafka para inyectar como variables de entorno.

### Sección env

```yaml
env:
  SPRING_PROFILES_ACTIVE: kubernetes
```

**Propósito:** Variables de entorno adicionales. El perfil `kubernetes` activa configuración específica para K8s.

### Sección livenessProbe

```yaml
livenessProbe:
  path: /actuator/health/liveness
  initialDelaySeconds: 60
  periodSeconds: 10
```

**Propósito:** Kubernetes verifica si el contenedor está vivo.

| Campo | Propósito |
|-------|-----------|
| `path` | Endpoint HTTP a verificar |
| `initialDelaySeconds` | Espera antes de la primera verificación (tiempo de arranque) |
| `periodSeconds` | Intervalo entre verificaciones |

**Si falla:** Kubernetes reinicia el contenedor.

### Sección readinessProbe

```yaml
readinessProbe:
  path: /actuator/health/readiness
  initialDelaySeconds: 10
  periodSeconds: 5
```

**Propósito:** Kubernetes verifica si el contenedor puede recibir tráfico.

**Si falla:** El pod se remueve del Service (no recibe tráfico) pero NO se reinicia.

### Sección postgresql

```yaml
postgresql:
  enabled: true
  auth:
    postgresPassword: "admin123"
    database: "afiliadodb"
  primary:
    persistence:
      size: 1Gi
```

**Propósito:** Configuración del subchart PostgreSQL.

### Sección ingress

```yaml
ingress:
  enabled: false
  className: nginx
  hosts:
    - host: afiliado.mutualidad.local
      paths:
        - path: /
          pathType: Prefix
```

**Propósito:** Configuración opcional de Ingress para exponer externamente.

---

## Archivo: templates/deployment.yaml

Define el Deployment de Kubernetes que gestiona los pods.

### Estructura Principal

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "afiliado-service.fullname" . }}
  labels:
    {{- include "afiliado-service.labels" . | nindent 4 }}
```

| Elemento | Propósito |
|----------|-----------|
| `apiVersion: apps/v1` | API de Kubernetes para Deployments |
| `kind: Deployment` | Tipo de recurso |
| `metadata.name` | Nombre generado dinámicamente usando helper |
| `metadata.labels` | Labels para identificación y selección |

### Sección spec.replicas

```yaml
spec:
  replicas: {{ .Values.replicaCount }}
```

**Propósito:** Número de pods a mantener, tomado de `values.yaml`.

### Sección spec.selector

```yaml
selector:
  matchLabels:
    {{- include "afiliado-service.selectorLabels" . | nindent 6 }}
```

**Propósito:** Define qué pods pertenecen a este Deployment (por labels).

### Sección containers

```yaml
containers:
  - name: {{ .Chart.Name }}
    image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
    imagePullPolicy: {{ .Values.image.pullPolicy }}
```

**Propósito:** Configuración del contenedor principal.

### Sección env (variables de entorno)

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: {{ .Values.env.SPRING_PROFILES_ACTIVE | quote }}
  - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
    value: {{ .Values.kafka.bootstrapServers | quote }}
```

**Propósito:** Inyecta configuración como variables de entorno que Spring Boot lee automáticamente.

### Sección PostgreSQL condicional

```yaml
{{- if .Values.postgresql.enabled }}
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://{{ .Release.Name }}-postgresql:5432/..."
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Name }}-postgresql
      key: postgres-password
{{- end }}
```

**Propósito:** Configura base de datos solo si PostgreSQL está habilitado.

**Nota:** La contraseña se lee de un Secret, no se pone en texto plano.

### Sección probes

```yaml
livenessProbe:
  httpGet:
    path: {{ .Values.livenessProbe.path }}
    port: http
  initialDelaySeconds: {{ .Values.livenessProbe.initialDelaySeconds }}
```

**Propósito:** Configura las verificaciones de salud del contenedor.

---

## Archivo: templates/service.yaml

Define el Service de Kubernetes para exponer los pods.

```yaml
apiVersion: v1
kind: Service
metadata:
  name: {{ include "afiliado-service.fullname" . }}
spec:
  type: {{ .Values.service.type }}
  ports:
    - port: {{ .Values.service.port }}
      targetPort: {{ .Values.service.targetPort }}
      protocol: TCP
      name: http
  selector:
    {{- include "afiliado-service.selectorLabels" . | nindent 4 }}
```

| Elemento | Propósito |
|----------|-----------|
| `type: ClusterIP` | Solo accesible dentro del cluster |
| `port: 80` | Puerto en el que escucha el Service |
| `targetPort: 8081` | Puerto del contenedor al que redirige |
| `selector` | Labels para identificar pods destino |

---

## Archivo: templates/_helpers.tpl

Contiene funciones reutilizables en templates.

### Función: afiliado-service.name

```yaml
{{- define "afiliado-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}
```

**Propósito:** Genera el nombre del chart, truncado a 63 caracteres (límite de DNS).

### Función: afiliado-service.fullname

```yaml
{{- define "afiliado-service.fullname" -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
```

**Propósito:** Genera nombre completo combinando release + chart name.

### Función: afiliado-service.labels

```yaml
{{- define "afiliado-service.labels" -}}
helm.sh/chart: {{ include "afiliado-service.chart" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
```

**Propósito:** Labels estándar de Kubernetes para identificación y trazabilidad.

### Función: afiliado-service.selectorLabels

```yaml
{{- define "afiliado-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "afiliado-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
```

**Propósito:** Labels mínimos para selección de pods (usados por Service y Deployment).

---

## Comandos Útiles

```bash
# Instalar el chart
helm install afiliado ./afiliado-service-chart

# Ver valores por defecto
helm show values ./afiliado-service-chart

# Instalar con valores personalizados
helm install afiliado ./afiliado-service-chart --set replicaCount=3

# Actualizar
helm upgrade afiliado ./afiliado-service-chart

# Desinstalar
helm uninstall afiliado
```
