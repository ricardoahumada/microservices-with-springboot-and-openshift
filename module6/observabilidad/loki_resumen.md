# Loki

## ¿Qué es Loki?

**Loki** es un sistema de agregación de logs horizontalmente escalable y optimizado para costos, diseñado por Grafana Labs. A diferencia de ELK Stack, Loki no indexa el contenido de los logs, sino que almacena los logs etiquetados y utiliza un índice de etiquetas para búsqueda, lo que resulta en costos significativamente menores de almacenamiento y operación.

### Conceptos Fundamentales

**Log Streams**: Secuencias de logs identificadas por un conjunto de etiquetas (labels) únicas.

**Labels**: Pares clave-valor que identifican y categorizan los logs (ej: job, instance, level, service).

**Chunks**: Bloques de datos de logs comprimidos y almacenados en object storage.

**Index**: Base de datos que mapea etiquetas a ubicaciones de chunks (usualmente DynamoDB, Cassandra o boltdb).

**Querier**: Componente que consulta el índice y recupera logs de chunks para responder queries.

---

## **Herramientas Principales**

### **Loki**
- Servidor principal que ingiere, almacena y sirve logs
- API HTTP para envío y consulta de logs
- Compresión y chunking automático de logs
- Retención configurable por tenant

### **Promtail**
- Agente que recolecta logs y los envía a Loki
- Integración con Docker containers
- Soporte para múltiples fuentes de logs
- Pipeline de procesamiento de logs

### **Grafana**
- Interfaz para visualización de logs
- Dashboards de Loki
- Explore para investigación de logs
- Alerting basado en logs

### **Loki Canary**
- Herramienta para testing de la instalación de Loki
- Verifica ingestión y query de logs
- Monitoreo de performance

---

## **Casos de Uso**

### **Observabilidad en Kubernetes**
- Logging de containers en clusters K8s
- Integración con Prometheus y Grafana
- Logs de aplicaciones cloud-native
- Monitoreo de infraestructura containerizada

### **Cost-Effective Log Management**
- Aplicaciones que generan grandes volúmenes de logs
- Entornos donde el costo de almacenamiento es crítico
- Logging de desarrollo y staging
- Aplicaciones con presupuesto limitado

### **Microservices Logging**
- Agregación de logs de múltiples microservicios
- Trazabilidad distribuida
- Correlación de logs entre servicios
- Debugging de sistemas distribuidos

### **DevOps y SRE**
- Monitoreo de sistemas en tiempo real
- Incident response y debugging
- Compliance y auditoría
- Performance analysis

### **Development y QA**
- Debugging de aplicaciones en desarrollo
- Testing y validación de builds
- Análisis de errores en staging
- CI/CD pipeline monitoring

---

## **Configuración**

### **Loki Configuration (loki.yml)**

```yaml
# Configuración básica de Loki
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

common:
  path_prefix: /tmp/loki
  storage:
    filesystem:
      chunks_directory: /tmp/loki/chunks
      rules_directory: /tmp/loki/rules
  replication_factor: 1
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory

query_range:
  results_cache:
    cache:
      embedded_cache:
        enabled: true
        max_size_mb: 100

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h

ruler:
  alertmanager_url: http://localhost:9093

# Configuración por defecto para límites
limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  creation_grace_period: 10m
  ingestion_rate_mb: 4
  ingestion_burst_size_mb: 6
  max_label_name_length: 1024
  max_label_value_length: 2048
  max_label_names_per_series: 30
  reject_malformed_samples: true
  max_cache_freshness_per_query: 10m
  max_global_streams_per_user: 5000
  max_query_length: 721h
  max_query_parallelism: 32
  max_streams_per_user: 10000
  max_line_size: 256000
  split_queries_by_interval: 15m
  query_timeout: 1m
  volume_enabled: true

# Configuración de almacenamiento
chunk_store_config:
  max_look_back_period: 0s

# Configuración de chunks
chunk_config:
  chunk_block_size: 262144
  chunk_target_size: 1048576
  chunk_idle_period: 1h
  max_chunk_age: 1h
  chunk_retention_period: 0s
  chunk_retain_period: 30s

# Configuración de análisis
analytics:
  reporting_enabled: false

# Configuración de almacenamiento en la nube
storage_config:
  boltdb_shipper:
    active_index_directory: /tmp/loki/boltdb-shipper-active
    cache_location: /tmp/loki/boltdb-shipper-cache
    cache_ttl: 24h
    shared_store: filesystem
  
  filesystem:
    directory: /tmp/loki/chunks

# Configuración de límites por tenant
limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  ingestion_rate_mb: 4
  ingestion_burst_size_mb: 6
  max_label_name_length: 1024
  max_label_value_length: 2048
  max_label_names_per_series: 30
  reject_malformed_samples: true
  max_cache_freshness_per_query: 10m
  max_global_streams_per_user: 5000
  max_query_length: 721h
  max_query_parallelism: 32
  max_streams_per_user: 10000
  max_line_size: 256000
```

### **Docker Compose para Loki Stack**

```yaml
version: '3.8'

services:
  loki:
    image: grafana/loki:2.9.3
    ports:
      - "3100:3100"
    command: -config.file=/etc/loki/local-config.yaml
    volumes:
      - ./loki:/etc/loki
    networks:
      - loki

  promtail:
    image: grafana/promtail:2.9.3
    command: -config.file=/etc/promtail/config.yml
    volumes:
      - ./promtail:/etc/promtail
      - /var/log:/var/log:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /etc/machine-id:/etc/machine-id:ro
    depends_on:
      - loki
    networks:
      - loki

  grafana:
    image: grafana/grafana:10.2.0
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
    networks:
      - loki

  # MinIO para object storage (opcional)
  minio:
    image: minio/minio:latest
    command: server /data
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      - MINIO_ROOT_USER=minioadmin
      - MINIO_ROOT_PASSWORD=minioadmin
    volumes:
      - minio-data:/data
    networks:
      - loki

volumes:
  minio-data:

networks:
  loki:
    driver: bridge
```

### **Promtail Configuration**

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # Logs de aplicación general
  - job_name: applications
    static_configs:
      - targets:
          - localhost
        labels:
          job: app-logs
          __path__: /var/log/company/*.log

  # Logs de Nginx
  - job_name: nginx
    static_configs:
      - targets:
          - localhost
        labels:
          job: nginx
          __path__: /var/log/nginx/*.log

  # Docker containers logs
  - job_name: containers
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
      - source_labels: ['__meta_docker_container_log_stream']
        target_label: 'logstream'
      - source_labels: ['__meta_docker_container_label_logging']
        regex: 'promtail'
        action: keep

  # System logs
  - job_name: system
    journal:
      max_age: 12h
      labels:
        job: system
        __path__: /var/log/journal/*/*.journal

  # Kubernetes logs (si se ejecuta en K8s)
  - job_name: kubernetes
    kubernetes_sd_configs:
      - role: pod
        namespaces:
          names:
            - default
            - kube-system
    relabel_configs:
      - source_labels: ['__meta_kubernetes_pod_label_app']
        target_label: 'app'
      - source_labels: ['__meta_kubernetes_pod_label_version']
        target_label: 'version'
      - source_labels: ['__meta_kubernetes_pod_label_component']
        target_label: 'component'
      - source_labels: ['__meta_kubernetes_namespace']
        target_label: 'namespace'
      - source_labels: ['__meta_kubernetes_pod_name']
        target_label: 'pod'
      - source_labels: ['__meta_kubernetes_pod_container_name']
        target_label: 'container'
      - action: replace
        source_labels: ['__address__']
        target_label: '__address__'
      - action: replace
        target_label: 'job'
        replacement: 'kubernetes-pods'

  # Pipeline de procesamiento
  pipeline_stages:
    # Parsear logs estructurados
    - json:
        expressions:
          timestamp: time
          level: level
          message: message
          service: service
          environment: environment
          user_id: user_id
          request_id: request_id
    
    # Agregar labels estáticas
    - labels:
        level:
        service:
        environment:
    
    # Timestamp
    - timestamp:
        source: timestamp
        format: RFC3339Nano
    
    # Reemplazar message si está vacío
    - output:
        source: message

  # Multi-line para stack traces
  - job_name: java-applications
    static_configs:
      - targets:
          - localhost
        labels:
          job: java-logs
          __path__: /var/log/company/java/*.log
    pipeline_stages:
      - multiline:
          pattern: '^\d{4}-\d{2}-\d{2}'
          negate: true
          match: after
      - json:
          expressions:
            timestamp: time
            level: level
            logger: logger
            message: message
      - labels:
          level:
          logger:
      - timestamp:
          source: timestamp
          format: RFC3339Nano
```

### **Grafana Configuration**

```ini
# grafana.ini
[paths]
data = /var/lib/grafana/
logs = /var/log/grafana
plugins = /var/lib/grafana/plugins
provisioning = /etc/grafana/provisioning

[server]
protocol = http
http_addr = 0.0.0.0
http_port = 3000
domain = localhost
root_url = %(protocol)s://%(domain)s/grafana/
serve_from_sub_path = false

[security]
admin_user = admin
admin_password = admin
disable_gravatar = false
data_source_proxy_whitelist =

[users]
allow_sign_up = false
allow_org_create = false
auto_assign_org = true
auto_assign_org_role = Viewer

[alerting]
enabled = true
execute_alerts = true

[log]
mode = console
level = info

[log.file]
path = /var/log/grafana/grafana.log
log_rotate = true
max_lines = 1000000
max_size_shift = 28
daily_rotate = 7

[log.syslog]
level = info
network = 
address = 
facility = 
tag =

[log.console]
level = info

[log.frontend]
enabled = false

[metrics]
enabled = true
interval_seconds = 10
disable_metrics = false
disable_total_stats = false

[analytics]
reporting_enabled = false
check_for_updates = false

[feature_toggles]
enable = ngalert
```

---

## **Ejemplos de Configuración**

### **Kubernetes con Loki**

```yaml
# loki-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: loki-config
  namespace: logging
data:
  loki.yaml: |
    auth_enabled: false
    
    server:
      http_listen_port: 3100
      grpc_listen_port: 9096
    
    common:
      path_prefix: /loki
      storage:
        filesystem:
          chunks_directory: /loki/chunks
          rules_directory: /loki/rules
      replication_factor: 1
      ring:
        instance_addr: 127.0.0.1
        kvstore:
          store: inmemory
    
    query_range:
      results_cache:
        cache:
          embedded_cache:
            enabled: true
            max_size_mb: 100
    
    schema_config:
      configs:
        - from: 2020-10-24
          store: boltdb-shipper
          object_store: filesystem
          schema: v11
          index:
            prefix: index_
            period: 24h
    
    limits_config:
      enforce_metric_name: false
      reject_old_samples: true
      reject_old_samples_max_age: 168h
      ingestion_rate_mb: 4
      ingestion_burst_size_mb: 6
      max_query_length: 721h
      max_query_parallelism: 32
    
    chunk_store_config:
      max_look_back_period: 0s
    
    chunk_config:
      chunk_block_size: 262144
      chunk_target_size: 1048576
      chunk_idle_period: 1h
      max_chunk_age: 1h
      chunk_retention_period: 0s
    
    analytics:
      reporting_enabled: false

---
# loki-statefulset.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: loki
  namespace: logging
spec:
  serviceName: loki
  replicas: 1
  selector:
    matchLabels:
      app: loki
  template:
    metadata:
      labels:
        app: loki
    spec:
      containers:
      - name: loki
        image: grafana/loki:2.9.3
        args:
          - -config.file=/etc/loki/local-config.yaml
        ports:
        - containerPort: 3100
          name: http
        - containerPort: 9096
          name: grpc
        volumeMounts:
        - name: config
          mountPath: /etc/loki
        - name: storage
          mountPath: /loki
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /ready
            port: 3100
          initialDelaySeconds: 10
          periodSeconds: 10
      volumes:
      - name: config
        configMap:
          name: loki-config

---
# loki-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: loki
  namespace: logging
spec:
  type: ClusterIP
  ports:
  - port: 3100
    targetPort: 3100
    name: http
  - port: 9096
    targetPort: 9096
    name: grpc
  selector:
    app: loki

---
# promtail-configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: promtail-config
  namespace: logging
data:
  promtail.yaml: |
    server:
      http_listen_port: 9080
      grpc_listen_port: 0
    
    positions:
      filename: /tmp/positions.yaml
    
    clients:
      - url: http://loki:3100/loki/api/v1/push
    
    scrape_configs:
    - job_name: kubernetes-pods
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: ['__meta_kubernetes_pod_label_app']
        target_label: 'app'
      - source_labels: ['__meta_kubernetes_pod_label_version']
        target_label: 'version'
      - source_labels: ['__meta_kubernetes_pod_label_component']
        target_label: 'component'
      - source_labels: ['__meta_kubernetes_namespace']
        target_label: 'namespace'
      - source_labels: ['__meta_kubernetes_pod_name']
        target_label: 'pod'
      - source_labels: ['__meta_kubernetes_pod_container_name']
        target_label: 'container'
      - action: replace
        target_label: 'job'
        replacement: 'kubernetes-pods'
```

### **Application Logs con Estructura**

```yaml
# Promtail para aplicaciones estructuradas
scrape_configs:
  - job_name: structured-logs
    static_configs:
      - targets:
          - localhost
        labels:
          job: structured-app
          __path__: /var/log/company/app/*.json
    
    pipeline_stages:
    # Parsear JSON logs
    - json:
        expressions:
          timestamp: timestamp
          level: level
          message: message
          service: service
          environment: environment
          user_id: user_id
          request_id: request_id
          correlation_id: correlation_id
          duration_ms: duration_ms
          endpoint: endpoint
          method: method
          status_code: status_code
    
    # Labels para filtrado eficiente
    - labels:
        level:
        service:
        environment:
        method:
        status_code:
    
    # Timestamp parsing
    - timestamp:
        source: timestamp
        format: RFC3339Nano
    
    # Metrics generation
    - metrics:
        log_lines_total:
          type: Counter
          config:
            match_all: true
            action: inc
        
        error_log_lines_total:
          type: Counter
          config:
            match_all: true
            action: inc
          config:
            level: error
        
        response_time_bucket:
          type: Histogram
          config:
            match_all: true
            action: observe
            config:
              duration_ms: duration_ms
    
    # Output
    - output:
        source: message
```

### **Multi-environment Setup**

```yaml
# Configuración para desarrollo
dev-config.yml:
  limits_config:
    ingestion_rate_mb: 2
    ingestion_burst_size_mb: 4
    max_query_length: 24h
    max_streams_per_user: 1000
  
  chunk_config:
    chunk_idle_period: 30m
    max_chunk_age: 1h
  
  retention:
    period: 72h

# Configuración para producción
prod-config.yml:
  limits_config:
    ingestion_rate_mb: 16
    ingestion_burst_size_mb: 32
    max_query_length: 721h
    max_streams_per_user: 10000
  
  chunk_config:
    chunk_idle_period: 1h
    max_chunk_age: 24h
  
  retention:
    period: 720h  # 30 días
```

---

## **Ejemplos en Java**

### **Logback con Loki**

```java
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;
import net.logstash.logback.stacktrace.ShortenedThrowableConverter;

@Configuration
public class LokiLoggingConfig {
    
    @Value("${loki.url:http://localhost:3100}")
    private String lokiUrl;
    
    @Value("${loki.service.name:myapp}")
    private String serviceName;
    
    @Value("${loki.environment:dev}")
    private String environment;
    
    @Bean
    public Appender lokiAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        LogstashTcpSocketAppender<ILoggingEvent> appender = new LogstashTcpSocketAppender<>();
        appender.setName("LOKI");
        appender.setContext(context);
        appender.addDestination(lokiUrl.replace("http://", "").replace("https://", "") + ":3100");
        
        // Configurar encoder JSON
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setContext(context);
        
        // Providers para campos personalizados
        encoder.addProvider("timestamp", new TimestampProvider());
        encoder.addProvider("level", new LevelProvider());
        encoder.addProvider("logger", new LoggerNameProvider());
        encoder.addProvider("message", new MessageProvider());
        encoder.addProvider("thread_name", new ThreadNameProvider());
        encoder.addProvider("stack_trace", new StackTraceProvider());
        
        // Agregar campos estáticos
        Map<String, Object> staticFields = Map.of(
            "service", serviceName,
            "environment", environment,
            "app_version", getAppVersion()
        );
        
        encoder.addProvider("static_fields", new StaticFieldsProvider(staticFields));
        
        // Agregar campos MDC
        encoder.addProvider("mdc", new MDCProvider());
        
        appender.setEncoder(encoder);
        appender.start();
        
        return appender;
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(lokiAppender());
        rootLogger.setLevel(Level.INFO);
    }
    
    private String getAppVersion() {
        try {
            return getClass().getPackage().getImplementationVersion() != null 
                ? getClass().getPackage().getImplementationVersion() 
                : "1.0.0";
        } catch (Exception e) {
            return "unknown";
        }
    }
}

// Provider para campos estáticos
public class StaticFieldsProvider implements JsonProvider<ILoggingEvent> {
    private final Map<String, Object> staticFields;
    
    public StaticFieldsProvider(Map<String, Object> staticFields) {
        this.staticFields = staticFields;
    }
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        generator.writeStartObject();
        for (Map.Entry<String, Object> entry : staticFields.entrySet()) {
            generator.writeObjectField(entry.getKey(), entry.getValue());
        }
        generator.writeEndObject();
    }
}

// Provider para MDC
public class MDCProvider implements JsonProvider<ILoggingEvent> {
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            generator.writeStartObject();
            for (Map.Entry<String, String> entry : mdc.entrySet()) {
                generator.writeStringField(entry.getKey(), entry.getValue());
            }
            generator.writeEndObject();
        }
    }
}

// Provider para stack trace
public class StackTraceProvider implements JsonProvider<ILoggingEvent> {
    private final ShortenedThrowableConverter throwableConverter = new ShortcludedThrowableConverter();
    
    public StackTraceProvider() {
        throwableConverter.setMaxDepth(10);
        throwableConverter.setRootCauseFirst(true);
    }
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (event.getThrowableProxy() != null) {
            generator.writeStringField("stack_trace", 
                throwableConverter.convert(event));
        }
    }
}

// Servicio para logging estructurado
@Service
public class LokiLoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LokiLoggingService.class);
    
    public void logApiRequest(String endpoint, String method, int statusCode, 
                             long duration, String userId, String requestId) {
        // MDC para trazabilidad
        MDC.put("request_id", requestId);
        MDC.put("user_id", userId);
        MDC.put("endpoint", endpoint);
        MDC.put("method", method);
        
        logger.info("API Request completed: endpoint={}, method={}, statusCode={}, duration={}ms", 
                   endpoint, method, statusCode, duration);
        
        MDC.clear();
    }
    
    public void logBusinessEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("event_type", eventType);
        eventData.put("timestamp", Instant.now().toEpochMilli());
        eventData.putAll(data);
        
        logger.info("Business event: {}", eventData);
    }
    
    public void logError(String operation, Exception exception, Map<String, Object> context) {
        // Añadir contexto al MDC
        context.forEach((key, value) -> MDC.put(key, value.toString()));
        
        logger.error("Error in {}: {}", operation, exception.getMessage(), exception);
        
        // Limpiar MDC
        context.keySet().forEach(key -> MDC.remove(key));
    }
    
    public void logSecurityEvent(String event, String userId, String ipAddress, 
                                String userAgent, boolean success) {
        MDC.put("user_id", userId);
        MDC.put("ip_address", ipAddress);
        MDC.put("user_agent", userAgent);
        
        String message = String.format("Security event: %s, success: %s", event, success);
        if (success) {
            logger.info(message);
        } else {
            logger.warn(message);
        }
        
        MDC.clear();
    }
}
```

### **Microservicios con Trazabilidad**

```java
// Configuración para trazabilidad distribuida
@Component
public class DistributedTracingConfig {
    
    @EventListener
    public void onApplicationStarted(ApplicationStartedEvent event) {
        // Configurar OpenTelemetry
        OpenTelemetrySdk otelSdk = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(
                    LoggingSpanExporter.create())
                    .build())
                .build())
            .build();
        
        GlobalOpenTelemetry.set(otelSdk);
    }
    
    @Bean
    public Tracer tracer() {
        return GlobalOpenTelemetry.get().getTracer("myapp");
    }
}

// Interceptor para tracing automático
@Component
public class TracingInterceptor implements HandlerInterceptor {
    
    private final Tracer tracer;
    
    public TracingInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = UUID.randomUUID().toString();
        String spanId = UUID.randomUUID().toString();
        
        // Añadir headers para propagación
        response.setHeader("X-Trace-ID", traceId);
        response.setHeader("X-Span-ID", spanId);
        
        // MDC para Loki
        MDC.put("trace_id", traceId);
        MDC.put("span_id", spanId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        MDC.clear();
    }
}

// Servicio con logging correlacionado
@Service
public class CorrelatedLoggingService {
    
    private final Tracer tracer;
    private final LokiLoggingService lokiLogger;
    
    public CorrelatedLoggingService(Tracer tracer, LokiLoggingService lokiLogger) {
        this.tracer = tracer;
        this.lokiLogger = lokiLogger;
    }
    
    public <T> T executeWithTracing(String operationName, Supplier<T> operation) {
        String traceId = MDC.get("trace_id");
        String spanId = MDC.get("span_id");
        
        try (Span span = tracer.spanBuilder(operationName).startSpan()) {
            span.setAttribute("trace_id", traceId);
            span.setAttribute("span_id", spanId);
            
            logger.info("Starting operation: {}", operationName);
            
            T result = operation.get();
            
            logger.info("Completed operation: {}", operationName);
            
            span.setStatus(StatusCode.OK);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed operation: {}", operationName, e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        }
    }
}
```

### **Performance Logging**

```java
// Aspect para logging automático de performance
@Aspect
@Component
public class PerformanceLoggingAspect {
    
    private final LokiLoggingService lokiLogger;
    
    public PerformanceLoggingAspect(LokiLoggingService lokiLogger) {
        this.lokiLogger = lokiLogger;
    }
    
    @Around("@annotation(LogPerformance)")
    public Object logPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> context = Map.of(
                "class", className,
                "method", methodName,
                "duration_ms", duration,
                "status", "success"
            );
            
            logger.info("Method executed successfully: {}.{} took {}ms", 
                       className, methodName, duration);
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> context = Map.of(
                "class", className,
                "method", methodName,
                "duration_ms", duration,
                "status", "error"
            );
            
            lokiLogger.logError("method_execution", e, context);
            throw e;
        }
    }
}

// Anotación para marcar métodos que requieren logging de performance
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogPerformance {
}
```

---

## **Ventajas y Desventajas**

### **Ventajas**

**Costo-Efectivo**
- Almacenamiento de logs más barato que ELK
- No indexación completa de contenido
- Compresión eficiente de chunks
- Retención configurable por costo

**Escalabilidad Horizontal**
- Distribución de ingestión y queries
- Separación de almacenamiento e índice
- Scaling independiente de componentes

**Integración con Grafana**
- Interfaz unificada con Prometheus
- Dashboards consistentes
- Explore para investigación de logs
- Alerting basado en logs

**Simplicidad Operacional**
- Menos componentes que configurar
- Menos recursos computacionales requeridos
- Deployment más simple

**Label-Based Querying**
- Búsqueda rápida por etiquetas
- Queries eficientes con LogQL
- Filtrado potente y flexible

### **Desventajas**

**Búsqueda de Texto Limitada**
- No búsqueda full-text nativa
- Limitaciones en análisis de texto libre
- Requiere etiquetas para búsqueda eficiente

**Ecosistema Menos Maduro**
- Menos plugins y integraciones
- Comunidad más pequeña que ELK
- Menos herramientas de terceros

**Funcionalidad de Analytics Limitada**
- Agregaciones básicas
- Análisis de texto limitado
- Funciones de búsqueda menos potentes

**Curva de Aprendizaje**
- LogQL es diferente a Lucene
- Conceptos de labels requieren comprensión
- Debugging de queries puede ser complejo

**Retención Compleja**
- Gestión de lifecycle manual
- Configuración de retención por tenant
- Cleanup de índices antiguos

---

## **Buenas Prácticas**

### **1. Diseño de Labels**

```yaml
# ✅ Labels bien diseñados
# Bueno: labels específicos y útiles
labels:
  service: user-service
  environment: production
  version: v1.2.3
  region: us-east-1
  datacenter: dc1
  node: node-1
  level: error

# ❌ Evitar labels con alta cardinalidad
# Malo: timestamps, IDs únicos, IPs dinámicas
labels:
  timestamp: "2024-01-01T12:00:00Z"  # No usar
  user_id: "12345"                   # No usar para usuarios únicos
  request_id: "abc-123-def"          # No usar para requests únicos
  client_ip: "192.168.1.100"         # No usar IPs dinámicas
```

### **2. Query Optimization**

```logql
# ✅ Queries eficientes
# Filtrar primero por labels
{service="user-service", environment="production"} |= "error" | logfmt | duration > 1s

# ✅ Usar filtros específicos
{job="api"} |= "GET /api/users" | status_code >= 400

# ✅ Agregación eficiente
sum(rate({service="api"}[5m])) by (status_code)

# ❌ Queries lentas
# Sin filtros de label
"database error"  # Lento - escanea todos los logs

# ❌ Regex complejos
|= ~"error.*timeout.*connection"  # Lento - regex costoso
```

### **3. Retention Configuration**

```yaml
# Configuración de retención por tenant
limits_config:
  enforce_metric_name: false
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  
# Retención específica por bucket
chunk_store_config:
  max_look_back_period: 0s

chunk_config:
  chunk_retention_period: 720h  # 30 días
  chunk_retain_period: 30s

# Bucket de retención configurado externamente
# TTL: 24h para logs de desarrollo
# TTL: 30d para logs de producción
# TTL: 90d para logs de auditoría
```

### **4. Monitoring Loki**

```yaml
# Métricas de Loki para monitoreo
scrape_configs:
  - job_name: loki
    static_configs:
      - targets: ['loki:3100']
    metrics_path: /metrics
    scrape_interval: 15s

# Queries útiles para monitoreo
# Tasa de ingestión
sum(rate(loki_ingester_samples_received_total[5m])) by (tenant)

# Latencia de queries
histogram_quantile(0.99, sum(rate(loki_query_duration_seconds_bucket[5m])) by (le))

# Tamaño de chunks
sum(loki_chunk_stored_bytes) by (tenant)

# Errores de ingestión
sum(rate(loki_ingester_samples_dropped_total[5m])) by (reason)
```

### **5. Security Configuration**

```yaml
# Configuración de autenticación
auth_enabled: true

# CORS configuration
server:
  http_listen_port: 3100
  grpc_listen_port: 9096

# Headers de seguridad
server:
  http_listen_port: 3100
  log_format: json

# Rate limiting
limits_config:
  ingestion_rate_mb: 4
  ingestion_burst_size_mb: 6
  max_query_parallelism: 32

# Content-type validation
validation:
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  reject_malformed_samples: true
```

### **6. Capacity Planning**

```bash
# Cálculo de recursos necesarios
# Ingest rate: 1MB/s por instance
# Storage: 1GB/día por instance
# Memory: 4-8GB por instance
# CPU: 2-4 cores por instance

# Ejemplo para 10GB/día de logs
# 10 instances de ingesters
# 3 instances de queriers
# 2TB de storage total
```

### **7. Alerting Setup**

```yaml
# Alertas en Grafana
groups:
- name: loki.rules
  rules:
  - alert: LokiIngestionHigh
    expr: sum(rate(loki_ingester_samples_received_total[5m])) by (tenant) > 1000000
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High ingestion rate detected"
      
  - alert: LokiQueryLatencyHigh
    expr: histogram_quantile(0.99, sum(rate(loki_query_duration_seconds_bucket[5m])) by (le)) > 5
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "High query latency detected"
      
  - alert: LokiDiskSpaceLow
    expr: (loki_ingester_chunk_stored_bytes / (loki_ingester_chunk_stored_bytes + loki_ingester_chunk_stored_bytes)) > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Disk space running low"
```

---

## **Referencias Oficiales**

1. **Loki Documentation**  
   https://grafana.com/docs/loki/latest/

2. **Grafana Loki Overview**  
   https://grafana.com/docs/loki/latest/fundamentals/overview/

3. **LogQL Query Language**  
   https://grafana.com/docs/loki/latest/logql/

4. **Loki Configuration**  
   https://grafana.com/docs/loki/latest/configuration/

5. **Promtail Configuration**  
   https://grafana.com/docs/loki/latest/clients/promtail/configuration/