# Guía de Evolución: Observabilidad con Logs Estructurados

## Objetivo
Esta guía muestra cómo evolucionar el proyecto `mutualidad-platform-base` hacia `mutualidad-platform` añadiendo observabilidad completa con logs estructurados JSON y stacks de análisis (Loki/Grafana y ELK).

---

## Estructura del Proyecto

```
mutualidad-platform/
├── docker-compose.yml          # Servicios base
├── docker-compose-loki.yml     # Stack Loki + Promtail + Grafana
├── docker-compose-elk.yml      # Stack ELK
├── loki-config.yml             # Configuración de Loki
├── promtail-config.yml         # Configuración de Promtail
├── logstash.conf               # Pipeline de Logstash
├── grafana-datasources.yml     # Datasources de Grafana
├── afiliado-service/
├── notificacion-service/
└── validacion-service/
```

---

## Ejercicio 1: Logs Estructurados JSON (20 min)

### 1.1: Configurar dependencias Maven

**Archivo:** `{servicio}/pom.xml`

Añadir las dependencias para logging estructurado:

```xml
<!-- Logging estructurado JSON -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>6.6</version>
</dependency>

<!-- Distributed Tracing (opcional pero recomendado) -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
    <version>1.1.6</version>
</dependency>
```

**Por qué:**
- `logstash-logback-encoder`: Convierte logs a formato JSON compatible con Logstash/Loki
- `micrometer-tracing-bridge-brave`: Añade traceId/spanId para trazabilidad distribuida

**Aplicar a:** afiliado-service, notificacion-service, validacion-service

---

### 1.2: Crear configuración logback-spring.xml

**Archivo:** `{servicio}/src/main/resources/logback-spring.xml`

#### Paso 1: Definir propiedades de Spring

```xml
<springProperty scope="context" name="appName" source="spring.application.name" defaultValue="afiliado-service"/>
<springProperty scope="context" name="appVersion" source="app.version" defaultValue="1.0.0"/>
```

#### Paso 2: Crear Console Appender con JSON

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>clientIp</includeMdcKeyName>
        <customFields>{"service":"${appName}","version":"${appVersion}","environment":"${ENVIRONMENT:-dev}"}</customFields>
    </encoder>
</appender>
```

**Campos incluidos:**
| Campo | Origen | Propósito |
|-------|--------|-----------|
| `traceId` | MDC (Micrometer) | Trazabilidad distribuida |
| `spanId` | MDC (Micrometer) | Identificar operación |
| `userId` | MDC (custom) | Auditoría de usuario |
| `requestId` | MDC (custom) | Correlación de peticiones |
| `clientIp` | MDC (custom) | Origen de petición |
| `service` | customFields | Identificar microservicio |
| `version` | customFields | Versión de la aplicación |
| `environment` | customFields | Entorno (dev/prod) |

#### Paso 3: Crear File Appender para persistencia

```xml
<appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>./logs/${appName}.json</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>./logs/${appName}-%d{yyyy-MM-dd}.json</fileNamePattern>
        <maxHistory>30</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <!-- Misma configuración que CONSOLE -->
    </encoder>
</appender>
```

#### Paso 4: Configurar root logger

```xml
<root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="JSON_FILE"/>
</root>
```

---

### 1.3: Implementar MDC para contexto de negocio

**Archivo:** `{servicio}/src/main/java/com/mutualidad/{servicio}/filter/AuditMDCFilter.java`

```java
@Component
@Order(1)
public class AuditMDCFilter implements Filter {

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String CLIENT_IP = "clientIp";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            // Generar o recuperar Request ID
            String requestId = httpRequest.getHeader("X-Request-ID");
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }
            
            // Agregar contexto al MDC
            MDC.put(REQUEST_ID, requestId);
            MDC.put(CLIENT_IP, getClientIp(httpRequest));
            
            // Recuperar usuario si está autenticado
            String userId = httpRequest.getHeader("X-User-ID");
            if (userId != null && !userId.isEmpty()) {
                MDC.put(USER_ID, userId);
            }
            
            chain.doFilter(request, response);
            
        } finally {
            // IMPORTANTE: Limpiar MDC al finalizar
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

**Conceptos clave:**
- **MDC (Mapped Diagnostic Context):** Almacena contexto por thread
- **X-Request-ID:** Header estándar para correlación entre servicios
- **X-Forwarded-For:** IP real cuando hay proxies/load balancers
- **MDC.clear():** Evita memory leaks y contaminación entre peticiones

---

## Ejercicio 2: Stack Loki + Promtail + Grafana (20 min)

### 2.1: Desplegar stack Loki con Docker Compose

**Archivo:** `docker-compose-loki.yml`

```yaml
version: '3.8'

services:
  loki:
    image: grafana/loki:2.9.0
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yml:/etc/loki/local-config.yaml
    command: -config.file=/etc/loki/local-config.yaml

  promtail:
    image: grafana/promtail:2.9.0
    volumes:
      - ./promtail-config.yml:/etc/promtail/config.yml
      - ./logs:/var/log/apps
    command: -config.file=/etc/promtail/config.yml

  grafana:
    image: grafana/grafana:10.0.0
    ports:
      - "3000:3000"
    volumes:
      - ./grafana-datasources.yml:/etc/grafana/provisioning/datasources/datasources.yml
    environment:
      - GF_AUTH_ANONYMOUS_ENABLED=true
      - GF_AUTH_ANONYMOUS_ORG_ROLE=Admin
```

**Ejecutar:**
```bash
docker-compose -f docker-compose-loki.yml up -d
```

---

### 2.2: Configurar Promtail para microservicios

**Archivo:** `promtail-config.yml`

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: microservices
    static_configs:
      - targets:
          - localhost
        labels:
          job: mutualidad
          __path__: /var/log/apps/*.json
    pipeline_stages:
      - json:
          expressions:
            level: level
            service: service
            message: message
            requestId: requestId
      - labels:
          level:
          service:
```

**Explicación:**
| Campo | Propósito |
|-------|-----------|
| `__path__` | Ruta de los archivos de log a procesar |
| `pipeline_stages.json` | Parsear JSON y extraer campos |
| `labels` | Campos que se convierten en labels de Loki |

---

### 2.3: Crear datasource en Grafana

**Archivo:** `grafana-datasources.yml`

```yaml
apiVersion: 1
datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    isDefault: true
```

---

### 2.4: Ejecutar consultas LogQL

Acceder a Grafana: http://localhost:3000

**Consultas de ejemplo:**

```logql
# Todos los logs del servicio afiliado
{service="afiliado-service"}

# Logs de error
{service=~".*"} |= "ERROR"

# Logs de un requestId específico
{service=~".*"} | json | requestId="abc-123"

# Contar errores por servicio
sum by (service) (rate({service=~".*"} |= "ERROR" [5m]))
```

---

## Ejercicio 3: Stack ELK (20 min)

### 3.1: Desplegar stack ELK con Docker Compose

**Archivo:** `docker-compose-elk.yml`

```yaml
version: '3.8'

services:
  elasticsearch:
    image: elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"

  logstash:
    image: logstash:8.10.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
      - ./logs:/var/log/apps
    depends_on:
      - elasticsearch

  kibana:
    image: kibana:8.10.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
```

**Ejecutar:**
```bash
docker-compose -f docker-compose-elk.yml up -d
```

---

### 3.2: Configurar pipeline de Logstash

**Archivo:** `logstash.conf`

```ruby
input {
  file {
    path => "/var/log/apps/*.json"
    start_position => "beginning"
    sincedb_path => "/dev/null"
    codec => json
  }
}

filter {
  # Parsear timestamp
  date {
    match => ["@timestamp", "ISO8601"]
    target => "@timestamp"
  }
  
  # Agregar campos calculados
  mutate {
    add_field => { "[@metadata][index]" => "mutualidad-logs" }
  }
  
  # Filtrar logs de auditoría
  if [logType] == "AUDIT" {
    mutate {
      add_field => { "[@metadata][index]" => "mutualidad-audit" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{[@metadata][index]}-%{+YYYY.MM.dd}"
  }
}
```

**Explicación:**
| Sección | Propósito |
|---------|-----------|
| `input.file` | Lee archivos JSON de logs |
| `filter.date` | Parsea timestamp para ordenar correctamente |
| `filter.mutate` | Separa logs de auditoría en índice diferente |
| `output.elasticsearch` | Envía a Elasticsearch con índice dinámico |

---

### 3.3: Crear Index Pattern en Kibana

1. Acceder a Kibana: http://localhost:5601
2. Ir a **Management > Stack Management > Index Patterns**
3. Crear pattern: `mutualidad-logs-*`
4. Seleccionar `@timestamp` como Time field
5. Repetir para `mutualidad-audit-*`

---

### 3.4: Crear Dashboard de Auditoría

1. Ir a **Analytics > Dashboard > Create**
2. Añadir visualizaciones:

**Visualización 1: Logs por servicio (Pie Chart)**
- Aggregation: Count
- Split slices: Terms on `service.keyword`

**Visualización 2: Errores en el tiempo (Line Chart)**
- Y-axis: Count
- X-axis: Date Histogram on `@timestamp`
- Split series: Filters
  - Filter 1: `level:ERROR`
  - Filter 2: `level:WARN`

**Visualización 3: Top Request IDs (Data Table)**
- Metrics: Count
- Buckets: Terms on `requestId.keyword`

**Visualización 4: Logs de auditoría (Saved Search)**
- Index pattern: `mutualidad-audit-*`
- Columns: `@timestamp`, `userId`, `requestUri`, `message`

---

## Resumen de Archivos por Ejercicio

| Ejercicio | Archivos Modificados/Creados |
|-----------|------------------------------|
| 1.1 | `{servicio}/pom.xml` |
| 1.2 | `{servicio}/src/main/resources/logback-spring.xml` |
| 1.3 | `{servicio}/src/main/java/.../filter/AuditMDCFilter.java` |
| 2.1 | `docker-compose-loki.yml` |
| 2.2 | `promtail-config.yml`, `loki-config.yml` |
| 2.3 | `grafana-datasources.yml` |
| 3.1 | `docker-compose-elk.yml` |
| 3.2 | `logstash.conf` |

---

## Flujo de Logs

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ afiliado-service│     │notificacion-svc │     │ validacion-svc  │
│                 │     │                 │     │                 │
│ LogstashEncoder │     │ LogstashEncoder │     │ LogstashEncoder │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                          ./logs/*.json
                                 │
              ┌──────────────────┴──────────────────┐
              │                                     │
              ▼                                     ▼
    ┌─────────────────┐                  ┌─────────────────┐
    │    Promtail     │                  │    Logstash     │
    └────────┬────────┘                  └────────┬────────┘
             │                                    │
             ▼                                    ▼
    ┌─────────────────┐                  ┌─────────────────┐
    │      Loki       │                  │  Elasticsearch  │
    └────────┬────────┘                  └────────┬────────┘
             │                                    │
             ▼                                    ▼
    ┌─────────────────┐                  ┌─────────────────┐
    │     Grafana     │                  │     Kibana      │
    │   (LogQL)       │                  │   (KQL/Lucene)  │
    └─────────────────┘                  └─────────────────┘
```

---

## Verificación

### Logs JSON funcionando
```bash
# Ver logs en formato JSON
tail -f logs/afiliado-service.json | jq .

# Verificar campos MDC
curl -H "X-Request-ID: test-123" http://localhost:8081/api/afiliados
tail -1 logs/afiliado-service.json | jq '.requestId'
```

### Loki funcionando
```bash
# Verificar que Loki recibe logs
curl http://localhost:3100/ready
curl http://localhost:3100/loki/api/v1/labels
```

### Elasticsearch funcionando
```bash
# Verificar índices
curl http://localhost:9200/_cat/indices

# Buscar logs
curl http://localhost:9200/mutualidad-logs-*/_search?q=level:ERROR
```
