# Guía: notificacion-service-chart

## Descripción
Helm Chart para el microservicio de Notificaciones, que actúa como **consumidor de eventos Kafka**.

---

## Estructura del Chart

```
notificacion-service-chart/
├── Chart.yaml           # Metadatos del chart
├── values.yaml          # Valores de configuración
└── templates/
    ├── _helpers.tpl     # Funciones auxiliares
    ├── deployment.yaml  # Definición del Deployment
    └── service.yaml     # Definición del Service
```

---

## Archivo: Chart.yaml

```yaml
apiVersion: v2
name: notificacion-service
description: Microservicio de Notificaciones - Consumidor Kafka
type: application
version: 0.1.0
appVersion: "1.0.0"

keywords:
  - microservices
  - spring-boot
  - kafka
  - consumer
```

### Diferencias con afiliado-service

| Aspecto | afiliado-service | notificacion-service |
|---------|------------------|----------------------|
| Rol Kafka | Producer | **Consumer** |
| Dependencias | PostgreSQL | Ninguna |
| Keywords | `producer` | `consumer` |

**Sin dependencias:** Este servicio no necesita base de datos propia.

---

## Archivo: values.yaml

```yaml
replicaCount: 2

image:
  repository: mutualidad/notificacion-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80
  targetPort: 8082

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

kafka:
  bootstrapServers: kafka:9092
  groupId: notificacion-group

env:
  SPRING_PROFILES_ACTIVE: kubernetes

livenessProbe:
  path: /actuator/health/liveness
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  path: /actuator/health/readiness
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Campos Explicados

#### Sección service

```yaml
service:
  type: ClusterIP
  port: 80
  targetPort: 8082
```

| Campo | Valor | Propósito |
|-------|-------|-----------|
| `targetPort` | `8082` | Puerto de Spring Boot (diferente a afiliado) |

#### Sección kafka

```yaml
kafka:
  bootstrapServers: kafka:9092
  groupId: notificacion-group
```

| Campo | Propósito |
|-------|-----------|
| `bootstrapServers` | Dirección del broker Kafka |
| `groupId` | **Consumer Group** - Identifica este grupo de consumidores |

**Consumer Group:** Todos los pods de notificacion-service comparten el mismo groupId. Kafka distribuye los mensajes entre ellos (cada mensaje lo procesa solo un pod del grupo).

### Diferencias con afiliado-service

| Campo | afiliado | notificacion |
|-------|----------|--------------|
| `targetPort` | 8081 | **8082** |
| `kafka.topic` | Sí | No (usa `groupId`) |
| `kafka.groupId` | No | **Sí** |
| `postgresql` | Sí | **No** |

---

## Templates

Los templates (`deployment.yaml`, `service.yaml`, `_helpers.tpl`) siguen el mismo patrón que afiliado-service-chart con las siguientes diferencias:

### deployment.yaml - Variables de entorno Kafka

```yaml
env:
  - name: SPRING_KAFKA_CONSUMER_GROUP_ID
    value: {{ .Values.kafka.groupId | quote }}
  - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
    value: {{ .Values.kafka.bootstrapServers | quote }}
```

**Diferencia clave:** Inyecta `GROUP_ID` en lugar de configuración de producer.

---

## Concepto: Consumer Groups

```
                    ┌─────────────────────────┐
                    │   Topic: afiliado-eventos│
                    │   (3 particiones)        │
                    └───────────┬─────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
         Partición 0       Partición 1       Partición 2
              │                 │                 │
              └────────┬────────┴────────┬────────┘
                       │                 │
                       ▼                 ▼
              ┌─────────────────┐ ┌─────────────────┐
              │  Pod 1          │ │  Pod 2          │
              │  notificacion   │ │  notificacion   │
              │  (consume P0,P1)│ │  (consume P2)   │
              └─────────────────┘ └─────────────────┘
                       │
                       └── Mismo groupId: "notificacion-group"
```

**Beneficio:** Escalado horizontal automático. Si añades más pods, Kafka rebalancea las particiones.

---

## Comandos Útiles

```bash
# Instalar solo notificacion-service
helm install notificacion ./notificacion-service-chart

# Con Kafka externo
helm install notificacion ./notificacion-service-chart \
  --set kafka.bootstrapServers=kafka-prod:9092

# Escalar consumidores
helm upgrade notificacion ./notificacion-service-chart \
  --set replicaCount=5
```

---

## Consideraciones de Escalado

| Réplicas | Particiones | Comportamiento |
|----------|-------------|----------------|
| 2 | 3 | Cada pod consume ~1.5 particiones |
| 3 | 3 | Cada pod consume 1 partición (óptimo) |
| 5 | 3 | 2 pods inactivos (desperdicio) |

**Regla:** `replicaCount <= número de particiones` para máxima eficiencia.
