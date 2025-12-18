# Guía: validacion-service-chart

## Descripción
Helm Chart para el microservicio de Validación, que actúa como **consumidor de eventos Kafka con Dead Letter Queue (DLQ)**.

---

## Estructura del Chart

```
validacion-service-chart/
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
name: validacion-service
description: Microservicio de Validacion - Consumidor Kafka con DLQ
type: application
version: 0.1.0
appVersion: "1.0.0"
```

### Diferencia Clave

| Aspecto | notificacion-service | validacion-service |
|---------|---------------------|-------------------|
| Descripción | Consumidor Kafka | **Consumidor Kafka con DLQ** |
| Manejo errores | Simple | **Con reintentos y DLQ** |

---

## Archivo: values.yaml

```yaml
replicaCount: 2

image:
  repository: mutualidad/validacion-service
  tag: "1.0.0"
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80
  targetPort: 8083

resources:
  limits:
    cpu: 500m
    memory: 512Mi
  requests:
    cpu: 250m
    memory: 256Mi

kafka:
  bootstrapServers: kafka:9092
  groupId: validacion-group

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
  targetPort: 8083
```

**Puerto 8083:** Cada microservicio usa un puerto diferente para evitar conflictos en desarrollo local.

| Servicio | Puerto |
|----------|--------|
| afiliado-service | 8081 |
| notificacion-service | 8082 |
| **validacion-service** | **8083** |

#### Sección kafka

```yaml
kafka:
  bootstrapServers: kafka:9092
  groupId: validacion-group
```

| Campo | Propósito |
|-------|-----------|
| `groupId` | `validacion-group` - Grupo separado de notificacion |

**Consumer Groups independientes:** Tanto `notificacion-group` como `validacion-group` reciben **todos** los mensajes del topic. Cada grupo procesa los mensajes de forma independiente.

---

## Concepto: Múltiples Consumer Groups

```
                    ┌─────────────────────────┐
                    │   Topic: afiliado-eventos│
                    └───────────┬─────────────┘
                                │
              ┌─────────────────┴─────────────────┐
              │                                   │
              ▼                                   ▼
   ┌──────────────────────┐          ┌──────────────────────┐
   │  notificacion-group  │          │  validacion-group    │
   │  (recibe TODOS)      │          │  (recibe TODOS)      │
   └──────────┬───────────┘          └──────────┬───────────┘
              │                                  │
    ┌─────────┴─────────┐              ┌─────────┴─────────┐
    ▼                   ▼              ▼                   ▼
┌────────┐         ┌────────┐    ┌────────┐         ┌────────┐
│ Pod 1  │         │ Pod 2  │    │ Pod 1  │         │ Pod 2  │
│ notif  │         │ notif  │    │ valid  │         │ valid  │
└────────┘         └────────┘    └────────┘         └────────┘
```

**Resultado:** Cada evento de afiliado:
1. Es procesado por UN pod de notificacion (envía email/SMS)
2. Es procesado por UN pod de validacion (valida datos)

---

## Concepto: Dead Letter Queue (DLQ)

### Flujo de Errores

```
Mensaje recibido
      │
      ▼
┌──────────────┐
│  Procesar    │
└──────┬───────┘
       │
       ├── Éxito ──────────▶ Commit offset
       │
       └── Error
            │
            ▼
      ┌──────────────┐
      │  Reintento 1 │
      └──────┬───────┘
             │
             ├── Éxito ──▶ Commit offset
             │
             └── Error
                  │
                  ▼
            ┌──────────────┐
            │  Reintento 2 │
            └──────┬───────┘
                   │
                   ├── Éxito ──▶ Commit offset
                   │
                   └── Error
                        │
                        ▼
                  ┌──────────────┐
                  │  Reintento 3 │
                  └──────┬───────┘
                         │
                         └── Error (final)
                              │
                              ▼
                    ┌─────────────────────┐
                    │  Enviar a DLQ       │
                    │  (afiliado-eventos.dlt) │
                    └─────────────────────┘
```

### Configuración en la Aplicación

La lógica de DLQ está en el código Java (`KafkaConfig.java`), no en el chart. El chart solo proporciona la configuración de conexión.

### Topics Involucrados

| Topic | Propósito |
|-------|-----------|
| `afiliado-eventos` | Topic principal de eventos |
| `afiliado-eventos.dlt` | Dead Letter Topic para mensajes fallidos |

---

## Templates

Los templates son idénticos en estructura a los otros servicios. La lógica de DLQ está implementada en el código de la aplicación, no en Kubernetes.

### Diferencias de Configuración

El chart podría extenderse para configurar DLQ vía variables de entorno:

```yaml
# Posible extensión en values.yaml
kafka:
  bootstrapServers: kafka:9092
  groupId: validacion-group
  dlt:
    topic: afiliado-eventos.dlt
    retries: 3
    backoffMs: 1000
```

```yaml
# Posible extensión en deployment.yaml
env:
  - name: APP_KAFKA_DLT_TOPIC
    value: {{ .Values.kafka.dlt.topic | quote }}
  - name: APP_KAFKA_DLT_RETRIES
    value: {{ .Values.kafka.dlt.retries | quote }}
```

---

## Comparativa de los 3 Servicios

| Aspecto | afiliado | notificacion | validacion |
|---------|----------|--------------|------------|
| Rol Kafka | Producer | Consumer | Consumer + DLQ |
| Puerto | 8081 | 8082 | 8083 |
| Consumer Group | N/A | notificacion-group | validacion-group |
| Base de datos | PostgreSQL | No | No |
| Manejo errores | N/A | Simple | Reintentos + DLQ |

---

## Comandos Útiles

```bash
# Instalar
helm install validacion ./validacion-service-chart

# Ver logs de errores y DLQ
kubectl logs -l app.kubernetes.io/name=validacion-service -f

# Escalar para mayor throughput
helm upgrade validacion ./validacion-service-chart \
  --set replicaCount=3
```

---

## Monitoreo de DLQ

### Ver mensajes en DLT

```bash
# Con kafka-console-consumer
kubectl exec -it kafka-0 -- kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic afiliado-eventos.dlt \
  --from-beginning
```

### Métricas Importantes

| Métrica | Indica |
|---------|--------|
| Mensajes en DLT | Errores que requieren intervención manual |
| Tasa de reintentos | Inestabilidad o problemas de datos |
| Lag del consumer group | Acumulación de mensajes sin procesar |

---

## Reprocesamiento de DLT

Los mensajes en el DLT requieren intervención manual:

1. **Revisar el error** en los logs
2. **Corregir el problema** (datos, bug, configuración)
3. **Republicar el mensaje** al topic principal

```bash
# Mover mensaje de DLT al topic principal
kafka-console-producer --bootstrap-server localhost:9092 \
  --topic afiliado-eventos < mensaje-corregido.json
```
