# Guía: mutualidad-platform-chart (Umbrella Chart)

## Descripción
**Umbrella Chart** que agrupa todos los microservicios y dependencias de infraestructura en un único despliegue coordinado.

---

## Estructura del Chart

```
mutualidad-platform-chart/
├── Chart.yaml      # Metadatos y dependencias
└── values.yaml     # Configuración global y por subchart
```

**Nota:** Este chart NO tiene carpeta `templates/` porque solo orquesta subcharts.

---

## Archivo: Chart.yaml

### Metadatos Básicos

```yaml
apiVersion: v2
name: mutualidad-platform
description: Umbrella Chart - Plataforma completa de Mutualidad
type: application
version: 0.1.0
appVersion: "1.0.0"
```

| Campo | Propósito |
|-------|-----------|
| `name` | Nombre del umbrella chart |
| `description` | Indica que es un umbrella chart |
| `type: application` | Chart desplegable (no library) |

### Sección dependencies

Define todos los subcharts que componen la plataforma.

#### Dependencias de Infraestructura

```yaml
dependencies:
  # Kafka
  - name: kafka
    version: 26.8.0
    repository: https://charts.bitnami.com/bitnami
    condition: kafka.enabled

  # PostgreSQL
  - name: postgresql
    version: 12.1.9
    repository: https://charts.bitnami.com/bitnami
    condition: postgresql.enabled
```

| Campo | Propósito |
|-------|-----------|
| `name` | Nombre del chart en el repositorio |
| `version` | Versión específica (importante para reproducibilidad) |
| `repository` | URL del repositorio Helm (Bitnami es muy usado) |
| `condition` | Variable en `values.yaml` que habilita/deshabilita |

**Por qué usar Bitnami:** Proporciona charts mantenidos y probados para infraestructura común.

#### Dependencias de Microservicios

```yaml
dependencies:
  - name: afiliado-service
    version: 0.1.0
    repository: "file://../afiliado-service-chart"
    condition: afiliado-service.enabled

  - name: notificacion-service
    version: 0.1.0
    repository: "file://../notificacion-service-chart"
    condition: notificacion-service.enabled

  - name: validacion-service
    version: 0.1.0
    repository: "file://../validacion-service-chart"
    condition: validacion-service.enabled
```

| Campo | Valor | Propósito |
|-------|-------|-----------|
| `repository` | `file://../xxx-chart` | Referencia a chart local (no remoto) |
| `condition` | `xxx.enabled` | Permite desplegar selectivamente |

**Ventaja de file://:** Desarrollo local sin necesidad de publicar charts.

---

## Archivo: values.yaml

Configuración centralizada para todos los subcharts.

### Sección kafka

```yaml
kafka:
  enabled: true
  replicaCount: 1
  listeners:
    client:
      protocol: PLAINTEXT
    controller:
      protocol: PLAINTEXT
  controller:
    replicaCount: 1
  kraft:
    enabled: true
  zookeeper:
    enabled: false
```

| Campo | Propósito |
|-------|-----------|
| `enabled: true` | Activa el subchart de Kafka |
| `replicaCount: 1` | Una réplica (desarrollo) |
| `kraft.enabled: true` | Usa KRaft en lugar de Zookeeper (Kafka moderno) |
| `zookeeper.enabled: false` | Desactiva Zookeeper (no necesario con KRaft) |
| `listeners.client.protocol` | Protocolo sin encriptación (desarrollo) |

**KRaft vs Zookeeper:** KRaft es el nuevo modo de Kafka que elimina la dependencia de Zookeeper.

### Sección postgresql

```yaml
postgresql:
  enabled: true
  image:
    tag: "latest"
  auth:
    postgresPassword: "admin123"
    database: "mutualidaddb"
  primary:
    persistence:
      size: 1Gi
```

| Campo | Propósito |
|-------|-----------|
| `enabled: true` | Activa PostgreSQL compartido |
| `auth.postgresPassword` | Contraseña del usuario postgres |
| `auth.database` | Base de datos a crear |
| `primary.persistence.size` | Tamaño del volumen persistente |

### Sección afiliado-service

```yaml
afiliado-service:
  enabled: true
  replicaCount: 2
  image:
    repository: mutualidad/afiliado-service
    tag: "1.0.0"
  kafka:
    bootstrapServers: "{{ .Release.Name }}-kafka:9092"
```

| Campo | Propósito |
|-------|-----------|
| `enabled: true` | Despliega este microservicio |
| `replicaCount: 2` | Sobreescribe el valor del subchart |
| `kafka.bootstrapServers` | Usa el nombre del release para conectar a Kafka |

**Nota sobre {{ .Release.Name }}:** Se resuelve dinámicamente al nombre del release Helm.

### Sección notificacion-service

```yaml
notificacion-service:
  enabled: true
  replicaCount: 2
  image:
    repository: mutualidad/notificacion-service
    tag: "1.0.0"
  kafka:
    bootstrapServers: "{{ .Release.Name }}-kafka:9092"
```

**Mismo patrón:** Habilita el servicio y configura conexión a Kafka.

### Sección validacion-service

```yaml
validacion-service:
  enabled: true
  replicaCount: 2
  image:
    repository: mutualidad/validacion-service
    tag: "1.0.0"
  kafka:
    bootstrapServers: "{{ .Release.Name }}-kafka:9092"
```

---

## Concepto: Umbrella Chart

### ¿Qué es?

Un **Umbrella Chart** es un chart que:
1. No tiene templates propios
2. Solo define dependencias a otros charts
3. Centraliza configuración en un único `values.yaml`

### Ventajas

| Ventaja | Descripción |
|---------|-------------|
| **Despliegue unificado** | Un solo `helm install` despliega toda la plataforma |
| **Versionado conjunto** | La versión del umbrella representa el estado de la plataforma |
| **Configuración centralizada** | Un único archivo de valores |
| **Despliegue selectivo** | Puedes deshabilitar componentes con `xxx.enabled: false` |

### Diagrama

```
mutualidad-platform (Umbrella)
├── kafka (Bitnami)
├── postgresql (Bitnami)
├── afiliado-service (Local)
├── notificacion-service (Local)
└── validacion-service (Local)
```

---

## Comandos Útiles

### Actualizar dependencias

```bash
cd mutualidad-platform-chart
helm dependency update
```

**Propósito:** Descarga los charts de Bitnami y empaqueta los locales.

### Instalar toda la plataforma

```bash
helm install mutualidad ./mutualidad-platform-chart
```

### Instalar sin Kafka (ejemplo)

```bash
helm install mutualidad ./mutualidad-platform-chart \
  --set kafka.enabled=false
```

### Ver qué se va a desplegar

```bash
helm template mutualidad ./mutualidad-platform-chart
```

### Actualizar la plataforma

```bash
helm upgrade mutualidad ./mutualidad-platform-chart
```

### Desinstalar todo

```bash
helm uninstall mutualidad
```

---

## Escenarios de Uso

### Desarrollo Local

```yaml
kafka:
  enabled: true
  replicaCount: 1
postgresql:
  enabled: true
afiliado-service:
  replicaCount: 1
```

### Producción

```yaml
kafka:
  enabled: true
  replicaCount: 3
postgresql:
  enabled: true
  primary:
    persistence:
      size: 50Gi
afiliado-service:
  replicaCount: 5
  resources:
    limits:
      cpu: 2000m
      memory: 2Gi
```

### Solo un servicio (debugging)

```yaml
kafka:
  enabled: true
postgresql:
  enabled: false
afiliado-service:
  enabled: true
notificacion-service:
  enabled: false
validacion-service:
  enabled: false
```
