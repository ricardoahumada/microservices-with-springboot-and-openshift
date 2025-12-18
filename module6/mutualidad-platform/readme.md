# Mutualidad Platform - Observabilidad Completa

Stack de observabilidad completo con **OpenTelemetry + Grafana Stack**.

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MICROSERVICIOS                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐          │
│  │ afiliado-service│  │validacion-svc   │  │notificacion-svc │          │
│  │     :8081       │  │     :8083       │  │     :8082       │          │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘          │
└───────────┼────────────────────┼────────────────────┼───────────────────┘
            │                    │                    │
            │     OTLP (gRPC:4317)                    │
            v                    v                    v
┌─────────────────────────────────────────────────────────────────────────┐
│                    OpenTelemetry Collector (:4317)                      │
│                                                                         │
│   Receivers: otlp, prometheus    Processors: batch, resource            │
│                                                                         │
│   Exporters:                                                            │
│   ├── otlp/jaeger  ──────────────────> Jaeger (trazas)                  │
│   ├── prometheus   ──────────────────> Prometheus (metricas)            │
│   └── loki         ──────────────────> Loki (logs)                      │
└─────────────────────────────────────────────────────────────────────────┘
            │                    │                    │
            v                    v                    v
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│     Jaeger      │  │   Prometheus    │  │      Loki       │
│  UI: :16686     │  │   UI: :9090     │  │    API: :3100   │
│   (Trazas)      │  │   (Metricas)    │  │     (Logs)      │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         └────────────────────┼────────────────────┘
                              v
                    ┌─────────────────┐
                    │     Grafana     │
                    │   UI: :3000     │
                    │  (Dashboard)    │
                    └─────────────────┘
```

## Componentes

| Componente | Puerto | Descripcion |
|------------|--------|-------------|
| **Grafana** | 3000 | Dashboard unificado (trazas, metricas, logs) |
| **Jaeger** | 16686 | UI de trazas distribuidas |
| **Prometheus** | 9090 | Almacen y UI de metricas |
| **Loki** | 3100 | Almacen de logs |
| **OTel Collector** | 4317/4318 | Receptor y enrutador de telemetria |
| **Kafka UI** | 9000 | UI para gestionar Kafka |
| **afiliado-service** | 8081 | API de afiliados |
| **notificacion-service** | 8082 | Consumer de notificaciones |
| **validacion-service** | 8083 | Consumer de validaciones |

---

## Inicio Rapido

### 1. Construir imagenes

```bash
cd module6/solutions/mutualidad-platform-ot-completo
docker compose -f docker-compose-otel.yml build
```

### 2. Iniciar stack completo

```bash
docker compose -f docker-compose-otel.yml up -d
```

### 3. Verificar servicios

```bash
docker compose -f docker-compose-otel.yml ps
```

### 4. Acceder a las UIs

| UI | URL | Credenciales |
|----|-----|--------------|
| **Grafana** | http://localhost:3000 | admin / admin |
| **Jaeger** | http://localhost:16686 | - |
| **Prometheus** | http://localhost:9090 | - |
| **Kafka UI** | http://localhost:9000 | - |

---

## Generar Trazas de Prueba

```bash
# Crear un afiliado
curl -X POST "http://localhost:8081/api/afiliados?dni=12345678A&nombre=Juan&apellidos=Garcia&email=juan@test.com&empresaId=EMP001"
```

---

## Explorar en Grafana

1. Acceder a http://localhost:3000 (admin/admin)
2. Ir a **Explore**
3. Seleccionar datasource:
   - **Jaeger**: Ver trazas distribuidas
   - **Prometheus**: Consultar metricas
   - **Loki**: Buscar logs

### Queries de ejemplo

**Prometheus - Metricas HTTP:**
```promql
http_server_requests_seconds_count{application="afiliado-service"}
```

**Loki - Logs por servicio:**
```logql
{container_name=~".*afiliado.*"}
```

**Jaeger - Trazas:**
- Seleccionar servicio: `afiliado-service`
- Click en "Find Traces"

---

## Correlacion Logs-Trazas

Grafana permite navegar de logs a trazas usando el TraceID:

1. En **Loki**, buscar logs con traceId
2. Click en el link del TraceID
3. Se abre automaticamente en **Jaeger**

---

## Archivos de Configuracion

| Archivo | Descripcion |
|---------|-------------|
| `docker-compose-otel.yml` | Stack completo con todos los servicios |
| `otel-collector-config.yaml` | Configuracion del OpenTelemetry Collector |
| `prometheus.yml` | Configuracion de scraping de Prometheus |
| `loki-config.yml` | Configuracion de Loki |
| `promtail-config.yml` | Configuracion de Promtail (recolector de logs) |
| `grafana-datasources.yml` | Datasources preconfigurados para Grafana |

---

## Flujo de Datos

```
1. afiliado-service recibe POST /api/afiliados
   └── Crea Span "POST /api/afiliados"
       └── Publica evento en Kafka con trace context en headers
           │
           ├── validacion-service consume evento
           │   └── Extrae trace context, crea Span hijo
           │
           └── notificacion-service consume evento
               └── Extrae trace context, crea Span hijo

2. Todos los spans se envian al OTel Collector via OTLP
   └── Collector los enruta a Jaeger

3. Metricas de los servicios se exponen en /actuator/prometheus
   └── Prometheus hace scrape periodicamente

4. Logs de contenedores son recolectados por Promtail
   └── Promtail los envia a Loki
```

---

## Detener Servicios

```bash
docker compose -f docker-compose-otel.yml down
```

Para eliminar volumenes (datos persistentes):

```bash
docker compose -f docker-compose-otel.yml down -v
```

---

## Troubleshooting

### Ver logs del collector
```bash
docker compose -f docker-compose-otel.yml logs otel-collector
```

### Ver logs de un servicio
```bash
docker compose -f docker-compose-otel.yml logs afiliado-service
```

### Reiniciar un servicio
```bash
docker compose -f docker-compose-otel.yml restart afiliado-service
```

### Verificar que Prometheus recibe metricas
```bash
curl http://localhost:9090/api/v1/targets
```

### Verificar que Loki recibe logs
```bash
curl http://localhost:3100/ready
```
