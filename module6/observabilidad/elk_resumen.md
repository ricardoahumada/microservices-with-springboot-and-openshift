# ELK Stack

## ¿Qué es ELK Stack?

**ELK Stack** es una suite de herramientas de código abierto para búsqueda, análisis y visualización de datos en tiempo real. Está compuesto por Elasticsearch (motor de búsqueda y análisis), Logstash (procesador de datos) y Kibana (plataforma de visualización). Permite buscar, analizar y visualizar datos de logs en tiempo real desde cualquier tipo de fuente y formato.

### Conceptos Fundamentales

**Elasticsearch**: Motor de búsqueda y análisis distribuido que almacena datos en formato JSON y proporciona capacidades de búsqueda full-text, analíticas y de agregación.

**Logstash**: Pipeline de procesamiento de datos que ingiere datos de múltiples fuentes, los transforma y los envía a Elasticsearch.

**Kibana**: Plataforma de visualización y exploración de datos que proporciona dashboards interactivos, gráficos y análisis.

**Beats**: Agentes ligeros que recopilan datos de las máquinas y los envían a Logstash o Elasticsearch.

---

## **Herramientas Principales**

### **Elasticsearch**
- Motor de búsqueda y análisis distribuido
- Almacenamiento de documentos JSON
- APIs RESTful para búsqueda y análisis
- Clustering y replicación automática

### **Logstash**
- Pipeline de procesamiento de datos
- Plugins de input, filter y output
- Transformación y enriquecimiento de datos
- Manejo de múltiples fuentes de datos

### **Kibana**
- Interfaz web para visualización
- Dashboards interactivos
- Análisis de logs y métricas
- Gestión de índices y patrones

### **Beats**
- Filebeat: Recolección de logs de archivos
- Metricbeat: Recolección de métricas del sistema
- Packetbeat: Monitoreo de tráfico de red
- Heartbeat: Monitoreo de disponibilidad

---

## **Casos de Uso**

### **Centralización de Logs**
- Agregación de logs de múltiples servidores
- Búsqueda y análisis centralizado
- Retención y archivado de logs
- Compliance y auditoría

### **Análisis de Aplicaciones**
- Monitoreo de rendimiento de aplicaciones
- Análisis de errores y excepciones
- Trazabilidad de requests
- Métricas de negocio

### **Security Information and Event Management (SIEM)**
- Detección de amenazas de seguridad
- Análisis de eventos de seguridad
- Monitoreo de accesos y actividades
- Respuesta a incidentes

### **Business Intelligence**
- Análisis de datos de negocio
- Reportes y dashboards ejecutivos
- Análisis de tendencias
- KPIs y métricas de rendimiento

### **Observabilidad**
- Monitoreo de infraestructura
- Alertas y notificaciones
- Análisis de performance
- Troubleshooting de sistemas

---

## **Configuración**

### **Elasticsearch Configuration**

```yaml
# elasticsearch.yml
cluster.name: my-elasticsearch-cluster
node.name: node-1
path.data: /var/lib/elasticsearch
path.logs: /var/log/elasticsearch

# Network settings
network.host: 0.0.0.0
http.port: 9200

# Discovery settings
discovery.seed_hosts: ["node-1", "node-2", "node-3"]
cluster.initial_master_nodes: ["node-1"]

# Security settings
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.http.ssl.enabled: true

# Performance settings
indices.memory.index_buffer_size: 30%
cluster.routing.allocation.disk.threshold_enabled: true
cluster.routing.allocation.disk.watermark.low: 85%
cluster.routing.allocation.disk.watermark.high: 90%
cluster.routing.allocation.disk.watermark.flood_stage: 95%

# Monitoring
xpack.monitoring.collection.enabled: true
xpack.watcher.enabled: true
```

### **Logstash Configuration**

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
  
  tcp {
    port => 5000
    codec => json_lines
  }
  
  http {
    port => 8080
    codec => "json"
  }
}

filter {
  # Parse common log formats
  if [fields][log_type] == "nginx" {
    grok {
      match => { 
        "message" => "%{NGINXACCESS}" 
      }
    }
    
    date {
      match => [ "timestamp", "dd/MMM/yyyy:HH:mm:ss Z" ]
    }
  }
  
  # Parse application logs
  if [fields][log_type] == "application" {
    json {
      source => "message"
    }
    
    # Extract fields from JSON log
    if [level] {
      mutate {
        add_field => { "log_level" => "%{[level]}" }
      }
    }
    
    # Add timestamp
    if [@timestamp] {
      date {
        match => [ "@timestamp", "ISO8601" ]
      }
    }
  }
  
  # Add geoip information
  if [clientip] {
    geoip {
      source => "clientip"
    }
  }
  
  # Clean up fields
  mutate {
    remove_field => [ "host", "agent", "ecs" ]
  }
  
  # Add environment information
  mutate {
    add_field => {
      "environment" => "production"
      "application" => "myapp"
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "logs-%{+YYYY.MM.dd}"
    template_name => "logs"
    template_pattern => "logs-*"
  }
  
  # Output for debugging
  stdout {
    codec => rubydebug
  }
  
  # Conditional outputs
  if [level] == "ERROR" {
    email {
      to => "ops-team@company.com"
      subject => "Error in %{[application]}"
      body => "Error message: %{message}"
    }
  }
}
```

### **Kibana Configuration**

```yaml
# kibana.yml
server.name: kibana
server.host: "0.0.0.0"
server.port: 5601

# Elasticsearch configuration
elasticsearch.hosts: ["http://localhost:9200"]
elasticsearch.username: "kibana_system"
elasticsearch.password: "kibana_password"

# Data directory
path.data: /var/lib/kibana

# Monitoring
monitoring.ui.container.elasticsearch.enabled: true
monitoring.ui.container.logstash.enabled: true

# Security
xpack.security.enabled: true
xpack.security.encryptionKey: "something_at_least_32_characters"

# License management
xpack.license.self_generated.type: basic

# Saved objects
savedObjects.index: ".kibana"

# Logging
logging.appenders:
  file:
    type: file
    fileName: /var/log/kibana/kibana.log
    layout:
      type: json
```

### **Docker Compose para ELK Stack**

```yaml
version: '3.8'

services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - node.name=elasticsearch
      - cluster.name=es-docker-cluster
      - discovery.type=single-node
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"
      - xpack.security.enabled=false
    ulimits:
      memlock:
        soft: -1
        hard: -1
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    ports:
      - "9200:9200"
    networks:
      - elk

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    volumes:
      - ./logstash/config:/usr/share/logstash/config
      - ./logstash/pipeline:/usr/share/logstash/pipeline
      - ./logs:/var/log/company:ro
    ports:
      - "5044:5044"
      - "5000:5000/tcp"
      - "5000:5000/udp"
      - "9600:9600"
    environment:
      LS_JAVA_OPTS: "-Xmx1g -Xms1g"
    networks:
      - elk
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_URL: http://elasticsearch:9200
      ELASTICSEARCH_HOSTS: '["http://elasticsearch:9200"]'
    networks:
      - elk
    depends_on:
      - elasticsearch

  filebeat:
    image: docker.elastic.co/beats/filebeat:8.11.0
    user: root
    volumes:
      - ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - /var/log:/var/log:ro
    networks:
      - elk
    depends_on:
      - logstash

volumes:
  elasticsearch_data:

networks:
  elk:
    driver: bridge
```

### **Filebeat Configuration**

```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/company/*.log
    - /var/log/company/**/*.log
  fields:
    log_type: application
    environment: production
  fields_under_root: true
  
  # Multi-line patterns
  multiline.pattern: '^\d{4}-\d{2}-\d{2}'
  multiline.negate: true
  multiline.match: after
  
  # JSON parsing
  json.keys_under_root: true
  json.add_error_key: true

- type: log
  enabled: true
  paths:
    - /var/log/nginx/access.log
    - /var/log/nginx/error.log
  fields:
    log_type: nginx
    service: nginx
  fields_under_root: true

- type: log
  enabled: true
  paths:
    - /var/log/company/database/*.log
  fields:
    log_type: database
    service: postgresql
  fields_under_root: true

# Logstash output
output.logstash:
  hosts: ["logstash:5044"]

# Elasticsearch output (alternative)
# output.elasticsearch:
#   hosts: ["localhost:9200"]
#   index: "filebeat-%{+yyyy.MM.dd}"

# Logging
logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0644

# Monitoring
monitoring.enabled: true
monitoring.elasticsearch:
  hosts: ["localhost:9200"]
```

---

## **Ejemplos de Configuración**

### **Análisis de Logs de Aplicación**

```ruby
# Aplicación Spring Boot logs
input {
  beats {
    port => 5044
  }
}

filter {
  # Parse Spring Boot logs
  if [fields][log_type] == "application" {
    grok {
      match => { 
        "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} \[%{DATA:thread}\] %{DATA:logger} : %{GREEDYDATA:msg}" 
      }
    }
    
    # Extract stack traces
    if [msg] =~ /\n\tat / {
      multiline {
        pattern => "^\s+at"
        negate => false
        match => after
      }
    }
    
    # Parse exception details
    if [msg] =~ /Exception:/ {
      grok {
        match => { 
          "msg" => "(?<exception_type>[A-Za-z.]+Exception):(?<exception_message>.+)" 
        }
      }
    }
    
    # Add application context
    mutate {
      add_field => {
        "application_name" => "myapp"
        "environment" => "production"
      }
    }
    
    # Parse request ID from MDC
    if [msg] =~ /requestId=/ {
      grok {
        match => { 
          "msg" => "requestId=(?<request_id>[a-f0-9-]+)" 
        }
      }
    }
  }
  
  # Standardize timestamp
  date {
    match => [ "timestamp", "yyyy-MM-dd HH:mm:ss.SSS", "ISO8601" ]
  }
  
  # Add geoip for IP addresses
  if [client_ip] {
    geoip {
      source => "client_ip"
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "application-logs-%{+YYYY.MM.dd}"
  }
  
  # Error alerts
  if [level] == "ERROR" {
    http {
      url => "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
      http_method => "post"
      content_type => "application/json"
      mapping => {
        "text" => "Error in %{[application_name]}: %{msg}"
        "channel" => "#alerts"
      }
    }
  }
}
```

### **Análisis de Nginx Logs**

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  # Parse Nginx access logs
  if [fields][service] == "nginx" {
    grok {
      match => { 
        "message" => "%{NGINXACCESS}" 
      }
    }
    
    # Convert response time to integer
    if [responsetime] {
      mutate {
        convert => { "responsetime" => "float" }
      }
    }
    
    # Add status code categories
    if [response] =~ /^2\d\d/ {
      mutate {
        add_field => { "status_category" => "success" }
      }
    } else if [response] =~ /^3\d\d/ {
      mutate {
        add_field => { "status_category" => "redirect" }
      }
    } else if [response] =~ /^4\d\d/ {
      mutate {
        add_field => { "status_category" => "client_error" }
      }
    } else if [response] =~ /^5\d\d/ {
      mutate {
        add_field => { "status_category" => "server_error" }
      }
    }
    
    # Calculate response time categories
    if [responsetime] {
      if [responsetime] < 0.1 {
        mutate {
          add_field => { "response_time_category" => "fast" }
        }
      } else if [responsetime] < 0.5 {
        mutate {
          add_field => { "response_time_category" => "normal" }
        }
      } else {
        mutate {
          add_field => { "response_time_category" => "slow" }
        }
      }
    }
    
    # Add geoip information
    if [clientip] {
      geoip {
        source => "clientip"
      }
    }
    
    # Parse user agent
    if [agent] {
      useragent {
        source => "agent"
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "nginx-logs-%{+YYYY.MM.dd}"
  }
  
  # Slow requests alert
  if [response_time_category] == "slow" {
    http {
      url => "https://api.company.com/alerts"
      http_method => "post"
      content_type => "application/json"
      mapping => {
        "alert_type" => "slow_response"
        "url" => "%{url}"
        "response_time" => "%{responsetime}"
        "timestamp" => "%{@timestamp}"
      }
    }
  }
}
```

### **Métricas de Sistema**

```yaml
# metricbeat.yml
metricbeat.modules:
- module: system
  metricsets:
    - cpu
    - memory
    - network
    - process
    - socket
  enabled: true
  period: 10s
  processes: ['.*']

- module: nginx
  metricsets:
    - stubstatus
  enabled: true
  period: 10s
  hosts: ["http://localhost:80/nginx_status"]

- module: postgresql
  metricsets:
    - status
    - activity
    - database
  enabled: true
  period: 10s
  hosts: ["postgres://user:pass@localhost:5432"]

- module: docker
  metricsets:
    - container
    - cpu
    - diskio
    - event
    - health
    - image
    - info
    - memory
    - network
  enabled: true
  period: 10s
  hosts: ["unix:///var/run/docker.sock"]

output.logstash:
  hosts: ["logstash:5044"]

# Elasticsearch output (alternative)
# output.elasticsearch:
#   hosts: ["localhost:9200"]
#   index: "metricbeat-%{+yyyy.MM.dd}"

setup.template.settings:
  index.number_of_shards: 1
  index.number_of_replicas: 1

setup.kibana:
  host: "kibana:5601"
```

### **Security Monitoring**

```ruby
input {
  beats {
    port => 5044
  }
}

filter {
  # Parse authentication logs
  if [fields][log_type] == "auth" {
    grok {
      match => { 
        "message" => "%{TIMESTAMP_ISO8601:timestamp} %{WORD:auth_type} %{DATA:user} %{IP:source_ip} %{WORD:action}" 
      }
    }
    
    # Detect failed login attempts
    if [action] == "failed" {
      mutate {
        add_field => { "threat_level" => "medium" }
        add_field => { "alert_type" => "failed_login" }
      }
    }
    
    # Detect privilege escalation
    if [message] =~ /sudo/ {
      mutate {
        add_field => { "threat_level" => "high" }
        add_field => { "alert_type" => "privilege_escalation" }
      }
    }
    
    # Detect multiple failed attempts
    aggregate {
      task_id => "%{user}%{source_ip}"
      code => "
        map['failed_attempts'] ||= 0
        map['failed_attempts'] += 1 if event.get('action') == 'failed'
        map['last_attempt'] = event.get('@timestamp')
        if map['failed_attempts'] >= 5
          event.set('multiple_failures', true)
        end
      "
      push_map_as_event_on_timeout => true
      timeout => 300
      timeout_tags => ['_timeout']
    }
  }
  
  # Add threat intelligence
  if [source_ip] {
    translate {
      field => "source_ip"
      destination => "threat_intel"
      dictionary_path => "/etc/logstash/dictionaries/threat_intel.yml"
      fallback => "clean"
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "security-logs-%{+YYYY.MM.dd}"
  }
  
  # Send high priority alerts
  if [threat_level] == "high" {
    http {
      url => "https://siem.company.com/api/alerts"
      http_method => "post"
      content_type => "application/json"
      mapping => {
        "severity" => "%{threat_level}"
        "alert_type" => "%{alert_type}"
        "source_ip" => "%{source_ip}"
        "user" => "%{user}"
        "timestamp" => "%{@timestamp}"
      }
    }
  }
  
  # Send to SIEM
  if [multiple_failures] == "true" {
    http {
      url => "https://siem.company.com/api/incidents"
      http_method => "post"
      content_type => "application/json"
      mapping => {
        "incident_type" => "brute_force"
        "source_ip" => "%{source_ip}"
        "target_user" => "%{user}"
        "failed_attempts" => "%{failed_attempts}"
      }
    }
  }
}
```

---

## **Ejemplos en Java**

### **Logback con Logstash**

```java
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.net.SyslogAppender;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder;

@Configuration
public class LogstashConfiguration {
    
    @Bean
    public Appender<ILoggingEvent> logstashAppender() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        LogstashTcpSocketAppender<ILoggingEvent> appender = new LogstashTcpSocketAppender<>();
        appender.setName("LOGSTASH");
        appender.setContext(context);
        appender.addDestination("logstash-server:5000");
        
        // JSON encoder with custom fields
        LoggingEventCompositeJsonEncoder encoder = new LoggingEventCompositeJsonEncoder();
        encoder.setContext(context);
        
        // Add custom fields
        encoder.addProvider("timestamp", new TimestampProvider());
        encoder.addProvider("level", new LevelProvider());
        encoder.addProvider("logger", new LoggerNameProvider());
        encoder.addProvider("message", new MessageProvider());
        encoder.addProvider("thread_name", new ThreadNameProvider());
        encoder.addProvider("custom_fields", new CustomFieldsProvider());
        
        appender.setEncoder(encoder);
        appender.start();
        
        return appender;
    }
    
    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(logstashAppender());
    }
}

// Custom provider for MDC fields
public class CustomFieldsProvider implements JsonProvider<ILoggingEvent> {
    
    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            generator.writeStartObject();
            for (Map.Entry<String, String> entry : mdc.entrySet()) {
                generator.writeStringField(entry.getKey(), entry.getValue());
            }
            generator.writeEndObject();
        }
    }
}

// Application service for structured logging
@Service
public class LoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);
    
    public void logUserAction(String userId, String action, String resource) {
        Map<String, Object> logData = Map.of(
            "event_type", "user_action",
            "user_id", userId,
            "action", action,
            "resource", resource,
            "timestamp", System.currentTimeMillis()
        );
        
        logger.info("User action performed: {}", logData);
    }
    
    public void logApiCall(String endpoint, String method, int statusCode, long duration) {
        Map<String, Object> logData = Map.of(
            "event_type", "api_call",
            "endpoint", endpoint,
            "method", method,
            "status_code", statusCode,
            "duration_ms", duration
        );
        
        logger.info("API call completed: {}", logData);
    }
    
    public void logError(String operation, Exception exception, Map<String, Object> context) {
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("event_type", "error");
        errorData.put("operation", operation);
        errorData.put("error_message", exception.getMessage());
        errorData.put("error_class", exception.getClass().getSimpleName());
        errorData.putAll(context);
        
        logger.error("Error occurred: {}", errorData, exception);
    }
}
```

### **Elasticsearch Client**

```java
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

@Configuration
public class ElasticsearchConfig {
    
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        RestClient client = RestClient.builder(
            new HttpHost("localhost", 9200, "http")
        ).build();
        
        return new RestHighLevelClient(client);
    }
}

@Service
public class ElasticsearchService {
    
    private final RestHighLevelClient client;
    
    public ElasticsearchService(RestHighLevelClient client) {
        this.client = client;
    }
    
    public void indexLogDocument(String index, Map<String, Object> document) {
        try {
            IndexRequest indexRequest = new IndexRequest(index)
                .source(document, XContentType.JSON);
            
            IndexResponse response = client.index(indexRequest);
            
            if (response.status().getStatus() >= 200 && response.status().getStatus() < 300) {
                logger.debug("Document indexed successfully: {}", response.getId());
            }
            
        } catch (IOException e) {
            logger.error("Failed to index document", e);
        }
    }
    
    public SearchResponse searchLogs(String index, String query, LocalDateTime from, LocalDateTime to) {
        try {
            SearchRequest searchRequest = new SearchRequest(index);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            
            // Build query
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.matchQuery("message", query));
            
            // Add date range filter
            if (from != null || to != null) {
                RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("@timestamp");
                if (from != null) {
                    rangeQuery.from(from);
                }
                if (to != null) {
                    rangeQuery.to(to);
                }
                boolQuery.filter(rangeQuery);
            }
            
            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.size(100);
            searchSourceBuilder.sort("@timestamp", SortOrder.DESC);
            
            searchRequest.source(searchSourceBuilder);
            
            return client.search(searchRequest);
            
        } catch (IOException e) {
            logger.error("Failed to search logs", e);
            return null;
        }
    }
    
    public void createApplicationIndex(String indexName) {
        try {
            // Check if index exists
            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            boolean exists = client.indices().exists(getIndexRequest);
            
            if (!exists) {
                // Create index with mapping
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
                
                String mapping = """
                    {
                      "mappings": {
                        "properties": {
                          "@timestamp": {
                            "type": "date"
                          },
                          "level": {
                            "type": "keyword"
                          },
                          "logger": {
                            "type": "keyword"
                          },
                          "message": {
                            "type": "text",
                            "analyzer": "standard"
                          },
                          "thread": {
                            "type": "keyword"
                          },
                          "application": {
                            "type": "keyword"
                          },
                          "environment": {
                            "type": "keyword"
                          },
                          "user_id": {
                            "type": "keyword"
                          },
                          "request_id": {
                            "type": "keyword"
                          }
                        }
                      }
                    }
                    """;
                
                createIndexRequest.mapping(mapping, XContentType.JSON);
                
                client.indices().create(createIndexRequest);
                
                logger.info("Created index: {}", indexName);
            }
            
        } catch (IOException e) {
            logger.error("Failed to create index", e);
        }
    }
}
```

---

## **Ventajas y Desventajas**

### **Ventajas**

**Escalabilidad**
- Arquitectura distribuida y escalable horizontalmente
- Manejo de grandes volúmenes de datos
- Clustering automático para alta disponibilidad

**Flexibilidad**
- Múltiples tipos de datos (logs, métricas, eventos)
- Búsqueda full-text potente y flexible
- Agregaciones complejas para análisis

**Ecosistema Rico**
- Integración con múltiples fuentes de datos
- Plugins y extensiones disponibles
- APIs RESTful para integración

**Visualización Avanzada**
- Dashboards interactivos y personalizables
- Gráficos y visualizaciones en tiempo real
- Análisis exploratorio de datos

**Búsqueda Potente**
- Búsqueda full-text con análisis de texto
- Filtros y queries complejas
- Búsqueda geoespacial

### **Desventajas**

**Complejidad de Configuración**
- Configuración inicial compleja
- Múltiples componentes que configurar
- Requiere expertise en clustering

**Recursos de Hardware**
- Consumo alto de memoria y CPU
- Requiere almacenamiento SSD para óptimo rendimiento
- Costos de infraestructura significativos

**Curva de Aprendizaje**
- DSL de queries complejo (Lucene)
- Conceptos de índices y mappings
- Troubleshooting de problemas de rendimiento

**Gestión de Datos**
- Retención y lifecycle management
- Backups y recuperación complejos
- Crecimiento descontrolado de datos

---

## **Buenas Prácticas**

### **1. Diseño de Índices**

```ruby
# Configuración optimizada de índice
PUT logs-2024.01.01
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "30s",
    "index.codec": "best_compression",
    "index.translog.flush_threshold_size": "1gb",
    "index.translog.sync_interval": "30s"
  },
  "mappings": {
    "properties": {
      "@timestamp": {
        "type": "date",
        "format": "strict_date_optional_time||epoch_millis"
      },
      "level": {
        "type": "keyword"
      },
      "message": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "application": {
        "type": "keyword"
      },
      "environment": {
        "type": "keyword"
      }
    }
  }
}
```

### **2. Pipeline de Procesamiento Eficiente**

```ruby
# Logstash pipeline optimizado
input {
  beats {
    port => 5044
    threads => 4
  }
}

filter {
  # Procesamiento temprano para filtrar
  if ![message] or [message] == "" {
    drop { }
  }
  
  # Parsing eficiente
  mutate {
    lowercase => [ "level" ]
    strip => [ "message" ]
  }
  
  # Cache de DNS lookups
  if [clientip] {
    dns {
      resolve => [ "clientip" ]
      action => "replace"
      hit_cache_size => 1000
      hit_cache_ttl => 300
      failed_cache_size => 100
      failed_cache_ttl => 60
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch1:9200", "elasticsearch2:9200"]
    index => "logs-%{+YYYY.MM.dd}"
    template_name => "logs"
    template_pattern => "logs-*"
    template => "/etc/logstash/templates/logs_template.json"
    
    # Performance settings
    workers => 2
    flush_size => 500
    idle_flush_time => 1
  }
}
```

### **3. Retención y Lifecycle Management**

```json
// Index Lifecycle Policy
PUT _ilm/policy/logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "10GB",
            "max_age": "1d"
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "set_priority": {
            "priority": 50
          },
          " "number_of_reallocate": {
           plicas": 0
          }
        }
      },
      "cold": {
        "min_age": "30d",
        "actions": {
          "set_priority": {
            "priority": 0
          },
          "allocate": {
            "number_of_replicas": 0
          },
          "freeze": {}
        }
      },
      "delete": {
        "min_age": "90d"
      }
    }
  }
}
```

### **4. Monitoreo y Alertas**

```json
// Watcher para alertas
PUT _watcher/watch/high_error_rate
{
  "trigger": {
    "schedule": {
      "interval": "5m"
    }
  },
  "input": {
    "search": {
      "request": {
        "search_type": "query_then_fetch",
        "indices": ["logs-*"],
        "body": {
          "query": {
            "bool": {
              "must": [
                {
                  "range": {
                    "@timestamp": {
                      "gte": "now-5m"
                    }
                  }
                },
                {
                  "term": {
                    "level": "error"
                  }
                }
              ]
            }
          },
          "aggs": {
            "error_rate": {
              "terms": {
                "field": "application.keyword",
                "size": 10
              }
            }
          }
        }
      }
    }
  },
  "condition": {
    "compare": {
      "ctx.payload.hits.total": {
        "gt": 100
      }
    }
  },
  "actions": {
    "send_email": {
      "email": {
        "to": "ops@company.com",
        "subject": "High Error Rate Detected",
        "body": "High error rate detected in the last 5 minutes"
      }
    }
  }
}
```

### **5. Seguridad**

```yaml
# elasticsearch.yml con seguridad
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.keystore.path: elastic-certificates.p12
xpack.security.transport.ssl.truststore.path: elastic-certificates.p12
xpack.security.http.ssl.enabled: true
xpack.security.http.ssl.keystore.path: http.p12

# Kibana security
xpack.security.enabled: true
xpack.security.encryptionKey: "your_32_character_encryption_key"

# Role-based access control
PUT _security/role/log_reader
{
  "cluster": ["monitor"],
  "indices": [
    {
      "names": ["logs-*"],
      "privileges": ["read", "view_index_metadata"]
    }
  ]
}
```

### **6. Backup y Recovery**

```bash
#!/bin/bash
# Snapshot script

# Create snapshot repository
curl -X PUT "localhost:9200/_snapshot/my_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/mnt/backups/elasticsearch"
  }
}'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/my_backup/snapshot_$(date +%Y%m%d)" -H 'Content-Type: application/json' -d'
{
  "indices": "logs-*",
  "ignore_unavailable": true,
  "include_global_state": false
}'

# Restore snapshot
curl -X POST "localhost:9200/_snapshot/my_backup/snapshot_20240101/_restore" -H 'Content-Type: application/json' -d'
{
  "indices": "logs-2024.01.01",
  "ignore_unavailable": true
}'
```

---

## **Referencias Oficiales**

1. **Elasticsearch Documentation**  
   https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html

2. **Logstash Documentation**  
   https://www.elastic.co/guide/en/logstash/current/index.html

3. **Kibana Documentation**  
   https://www.elastic.co/guide/en/kibana/current/index.html

4. **Elastic Stack Overview**  
   https://www.elastic.co/elastic-stack

5. **Beats Documentation**  
   https://www.elastic.co/guide/en/beats/libbeat/current/index.html