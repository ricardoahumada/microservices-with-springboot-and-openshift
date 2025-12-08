# Modulo 6: Observabilidad en Entornos Regulados

## Estructura del Proyecto

```
mutualidad-platform/
├── docker-compose.yml              # Kafka + Zookeeper
├── docker-compose-loki.yml         # Stack Loki + Promtail + Grafana
├── docker-compose-elk.yml          # Stack Elasticsearch + Logstash + Kibana
├── loki-config.yml                 # Configuracion de Loki
├── promtail-config.yml             # Configuracion de Promtail
├── grafana-datasources.yml         # Datasources para Grafana
├── logstash.conf                   # Pipeline de Logstash
├── afiliado-service/
│   ├── src/main/resources/logback-spring.xml
│   └── src/main/java/.../filter/AuditMDCFilter.java
├── notificacion-service/
│   ├── src/main/resources/logback-spring.xml
│   └── src/main/java/.../filter/AuditMDCFilter.java
└── validacion-service/
    ├── src/main/resources/logback-spring.xml
    └── src/main/java/.../filter/AuditMDCFilter.java
```

## Requisitos Previos

- Docker Desktop
- Java 11+
- Maven 3.6+

---

## Ejercicio 1: Logs Estructurados JSON (20 min)

### 1.1 Dependencias Agregadas

Cada microservicio incluye en su `pom.xml`:

```xml
<!-- Logging estructurado JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>

<!-- Distributed Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```

### 1.2 Configuracion logback-spring.xml

Cada servicio tiene su `src/main/resources/logback-spring.xml` con:

- **LogstashEncoder**: Genera logs en formato JSON
- **MDC Fields**: traceId, spanId, userId, requestId, clientIp
- **Custom Fields**: service, version, environment
- **Audit Logger**: Logger separado para eventos de auditoria

### 1.3 Filtro AuditMDCFilter

El filtro `AuditMDCFilter.java` agrega contexto de auditoria:

```java
MDC.put("requestId", UUID.randomUUID().toString());
MDC.put("clientIp", getClientIp(request));
MDC.put("userId", request.getHeader("X-User-ID"));
```

### 1.4 Verificar Logs JSON

```bash
# Compilar servicios
cd afiliado-service
mvn clean package -DskipTests

# Ejecutar con Docker Compose
docker-compose up -d

# Ejecutar servicio
java -jar target/afiliado-service-1.0.0-SNAPSHOT.jar

# Los logs saldran en formato JSON:
# {"@timestamp":"2025-...","level":"INFO","service":"afiliado-service",...}
```

---

## Ejercicio 2: Stack Loki + Promtail + Grafana (20 min)

### 2.1 Arquitectura

```
┌─────────────┐     ┌──────────┐     ┌───────┐     ┌─────────┐
│ Microservices│────▶│ Promtail │────▶│ Loki  │────▶│ Grafana │
│ (JSON Logs) │     │ (Agent)  │     │(Store)│     │  (UI)   │
└─────────────┘     └──────────┘     └───────┘     └─────────┘
```

### 2.2 Iniciar Stack Loki

```bash
cd module6/solutions/mutualidad-platform

# Iniciar Loki stack
docker-compose -f docker-compose-loki.yml up -d

# Verificar servicios
docker-compose -f docker-compose-loki.yml ps
```

Servicios disponibles:
- **Loki**: http://localhost:3100
- **Grafana**: http://localhost:3000 (admin/admin123)

### 2.3 Configurar Grafana

1. Acceder a http://localhost:3000
2. Login: admin / admin123
3. Ir a **Explore** > Seleccionar **Loki**
4. Query: `{job="mutualidad"}`

### 2.4 Consultas LogQL

```logql
# Filtrar por servicio
{service="afiliado-service"}

# Filtrar errores
{job="mutualidad"} |= "ERROR"

# Filtrar por traceId
{job="mutualidad"} | json | traceId="abc123"

# Contar errores por servicio
sum by (service) (count_over_time({job="mutualidad"} |= "ERROR" [5m]))
```

### 2.5 Detener Stack Loki

```bash
docker-compose -f docker-compose-loki.yml down -v
```

---

## Ejercicio 3: Stack ELK (20 min)

### 3.1 Arquitectura

```
┌─────────────┐     ┌───────────┐     ┌───────────────┐     ┌────────┐
│ Microservices│────▶│ Logstash  │────▶│ Elasticsearch │────▶│ Kibana │
│ (JSON Logs) │     │ (Ingest)  │     │   (Store)     │     │  (UI)  │
└─────────────┘     └───────────┘     └───────────────┘     └────────┘
```

### 3.2 Iniciar Stack ELK

```bash
cd module6/solutions/mutualidad-platform

# Iniciar ELK stack
docker-compose -f docker-compose-elk.yml up -d

# Verificar servicios (puede tardar 1-2 minutos)
docker-compose -f docker-compose-elk.yml ps

# Ver logs de Elasticsearch
docker logs elasticsearch
```

Servicios disponibles:
- **Elasticsearch**: http://localhost:9200
- **Kibana**: http://localhost:5601
- **Logstash**: puerto 5044 (Beats), 5000 (TCP/UDP)

### 3.3 Configurar Kibana

1. Acceder a http://localhost:5601
2. Ir a **Management** > **Stack Management** > **Index Patterns**
3. Crear pattern: `mutualidad-logs-*`
4. Seleccionar `@timestamp` como time field
5. Ir a **Discover** para ver logs

### 3.4 Consultas en Kibana (KQL)

```
# Filtrar por servicio
service: "afiliado-service"

# Filtrar errores
level: "ERROR"

# Filtrar por usuario
userId: "user123"

# Combinar filtros
service: "afiliado-service" AND level: "ERROR"

# Buscar texto
message: *afiliado*
```

### 3.5 Crear Dashboards

1. Ir a **Dashboard** > **Create dashboard**
2. Agregar visualizaciones:
   - **Logs por nivel**: Pie chart con field `level`
   - **Logs por servicio**: Bar chart con field `service`
   - **Timeline de errores**: Line chart filtrando `level: ERROR`
   - **Top usuarios**: Table con field `userId`

### 3.6 Detener Stack ELK

```bash
docker-compose -f docker-compose-elk.yml down -v
```

---

## Comparativa Loki vs ELK

| Aspecto | Loki | ELK |
|---------|------|-----|
| **Almacenamiento** | Solo indexa labels | Full-text indexing |
| **Recursos** | Bajo consumo | Alto consumo (RAM) |
| **Escalabilidad** | Excelente | Requiere cluster |
| **Consultas** | LogQL (simple) | KQL (potente) |
| **Caso de uso** | Logs + metricas Grafana | Analisis profundo |
| **Compliance** | Basico | Avanzado |

---

## Campos de Auditoria

Los logs incluyen los siguientes campos para cumplimiento regulatorio:

| Campo | Descripcion | Origen |
|-------|-------------|--------|
| `@timestamp` | Momento del evento | Automatico |
| `service` | Nombre del servicio | logback-spring.xml |
| `version` | Version de la app | logback-spring.xml |
| `environment` | Entorno (dev/prod) | Variable ENVIRONMENT |
| `traceId` | ID de traza distribuida | Micrometer |
| `spanId` | ID de span | Micrometer |
| `requestId` | ID unico de peticion | AuditMDCFilter |
| `userId` | Usuario autenticado | Header X-User-ID |
| `clientIp` | IP del cliente | AuditMDCFilter |

---

## Comandos Utiles

### Docker Compose

```bash
# Ver logs de todos los servicios
docker-compose -f docker-compose-loki.yml logs -f

# Reiniciar servicio especifico
docker-compose -f docker-compose-elk.yml restart logstash

# Ver consumo de recursos
docker stats
```

### Verificar Servicios

```bash
# Loki
curl http://localhost:3100/ready

# Elasticsearch
curl http://localhost:9200/_cluster/health?pretty

# Grafana
curl http://localhost:3000/api/health
```

---

## Limpieza

```bash
# Detener todos los stacks
docker-compose down -v
docker-compose -f docker-compose-loki.yml down -v
docker-compose -f docker-compose-elk.yml down -v

# Eliminar volumenes huerfanos
docker volume prune -f
```

---

## Troubleshooting

### Elasticsearch no arranca

```bash
# Ver logs
docker logs elasticsearch

# Si es error de memoria, aumentar vm.max_map_count:
sudo sysctl -w vm.max_map_count=262144
```

### Loki no recibe logs

```bash
# Verificar Promtail
docker logs promtail

# Verificar que los logs estan en /app/logs/
ls -la logs/
```

### Logs no aparecen en formato JSON

```bash
# Verificar que logback-spring.xml esta en src/main/resources/
# Verificar dependencia logstash-logback-encoder en pom.xml
mvn dependency:tree | grep logstash
```
