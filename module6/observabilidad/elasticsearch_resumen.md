# Elasticsearch

## ¿Qué es Elasticsearch?

**Elasticsearch** es un motor de búsqueda y análisis distribuido y altamente escalable, construido sobre Apache Lucene. Proporciona capacidades de búsqueda full-text, analíticas en tiempo real, búsqueda estructurada y exploración de datos. Es el componente central del ELK Stack y se utiliza para almacenar, buscar y analizar grandes volúmenes de datos de manera rápida y en tiempo real.

### Conceptos Fundamentales

**Cluster**: Conjunto de uno o más nodos de Elasticsearch que trabajan juntos para almacenar datos y proporcionar capacidades de indexación y búsqueda.

**Node**: Instancia individual de Elasticsearch que forma parte de un cluster y almacena datos.

**Index**: Colección lógica de documentos que tienen características similares, similar a una base de datos en el mundo relacional.

**Document**: Unidad básica de información que se puede indexar, representada en formato JSON.

**Sharding**: División de un índice en múltiples partes para mejorar el rendimiento y permitir escalabilidad horizontal.

**Replication**: Proceso de crear copias de shards para redundancia y alta disponibilidad.

---

## **Herramientas Principales**

### **Elasticsearch**
- Motor de búsqueda y análisis distribuido
- API RESTful para operaciones CRUD
- Búsqueda full-text avanzada
- Aggregaciones y analytics

### **Kibana**
- Interfaz web para visualización y análisis
- Dashboards interactivos
- Herramientas de desarrollo (Dev Tools)
- Gestión de índices y patrones

### **Logstash**
- Pipeline de procesamiento de datos
- Transformación y enriquecimiento
- Múltiples fuentes de input
- Output hacia Elasticsearch

### **Beats**
- Agentes ligeros para recolección de datos
- Filebeat para logs
- Metricbeat para métricas
- Heartbeat para monitoreo de disponibilidad

---

## **Casos de Uso**

### **Búsqueda de Aplicaciones**
- Búsqueda en aplicaciones web y móviles
- E-commerce y catálogos de productos
- Sistemas de recomendación
- Búsqueda empresarial

### **Análisis de Logs y Monitoreo**
- Centralización y análisis de logs
- SIEM (Security Information and Event Management)
- APM (Application Performance Monitoring)
- Compliance y auditoría

### **Business Intelligence**
- Análisis de datos en tiempo real
- Reportes y dashboards
- Análisis de tendencias
- KPIs y métricas de negocio

### **Análisis de Texto y Contenido**
- Procesamiento de lenguaje natural
- Análisis de sentimientos
- Clasificación de documentos
- Extracción de entidades

### **E-commerce y Retail**
- Recomendaciones de productos
- Análisis de comportamiento del usuario
- Optimización de inventario
- Pricing dinámico

### **IoT y Analytics**
- Análisis de datos de sensores
- Time series analytics
- Predicción de fallos
- Optimización de recursos

---

## **Configuración**

### **elasticsearch.yml Configuration**

```yaml
# Configuración básica del cluster
cluster.name: my-cluster
node.name: node-1
path.data: /var/lib/elasticsearch
path.logs: /var/log/elasticsearch

# Configuración de red
network.host: 0.0.0.0
http.port: 9200
transport.tcp.port: 9300

# Configuración de descubrimiento
discovery.seed_hosts:
  - 192.168.1.10
  - 192.168.1.11
  - 192.168.1.12

cluster.initial_master_nodes:
  - node-1
  - node-2
  - node-3

# Configuración de heap
bootstrap.memory_lock: true

# Configuración de seguridad
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.keystore.path: elastic-certificates.p12
xpack.security.transport.ssl.truststore.path: elastic-certificates.p12
xpack.security.http.ssl.enabled: true
xpack.security.http.ssl.keystore.path: http.p12

# Configuración de roles
xpack.security.authc.api_key.enabled: true
xpack.security.authc.realms.file.file1.order: 0
xpack.security.authc.realms.ldap.ldap1.order: 1
xpack.security.authc.realms.ldap.ldap1.url: ldaps://ldap.company.com:636
xpack.security.authc.realms.ldap.ldap1.bind_dn: cn=admin,dc=company,dc=com

# Configuración de licenciamiento
xpack.license.self_generated.type: basic

# Configuración de índices
action.destructive_requires_name: true

# Configuración de caching
node.roles: [master, data, ingest]

# Configuración de performance
indices.memory.index_buffer_size: 30%
cluster.routing.allocation.disk.threshold_enabled: true
cluster.routing.allocation.disk.watermark.low: 85%
cluster.routing.allocation.disk.watermark.high: 90%
cluster.routing.allocation.disk.watermark.flood_stage: 95%

# Configuración de snapshots
path.repo: ["/backup/elasticsearch"]

# Configuración de thread pools
thread_pool.write.queue_size: 1000
thread_pool.search.queue_size: 1000

# Configuración de circuit breakers
indices.breaker.total.limit: 70%
indices.breaker.request.limit: 40%
indices.breaker.fielddata.limit: 40%

# Configuración de compression
index.codec: best_compression
index.refresh_interval: 30s
index.number_of_replicas: 1
index.number_of_shards: 3

# Configuración de análisis
analysis.analyzer.default.type: standard
analysis.analyzer.default.stopwords: _english_
analysis.analyzer.default.max_token_length: 255

# Configuración de timeout
action.bulk.timeout: 5m
action.bulk.backoff.initial_delay: 500ms
action.bulk.backoff.type: exponential
action.bulk.backoff.retries: 3

# Configuración de rate limiting
indices.queries.cache.size: 10%
indices.requests.cache.size: 1%

# Configuración de reindexing
reindex.remote.whitelist: ["otherhost:9200", "otherhost:9300"]

# Configuración de cluster coordination
cluster.coordination.election_initial_timeout: 5s
cluster.coordination.join_timeout: 30s
cluster.coordination.last_accepted_configuration.timeout: 30s

# Configuración de shards
cluster.routing.allocation.cluster_concurrent_rebalances: 2
cluster.routing.allocation.node_concurrent_incoming_recoveries: 2
cluster.routing.allocation.node_concurrent_outgoing_recoveries: 2
cluster.routing.allocation.node_initial_primaries_recoveries: 4

# Configuración de HTTP
http.max_content_length: 100mb
http.compression: true
http.compression_level: 6

# Configuración de threads
thread_pool.write.size: 16
thread_pool.search.size: 10
thread_pool.get.size: 10
thread_pool.index.size: 8
thread_pool.bulk.size: 8
```

### **Docker Compose para Elasticsearch**

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
      - xpack.security.http.ssl.enabled=false
      - xpack.security.transport.ssl.enabled=false
    ulimits:
      memlock:
        soft: -1
        hard: -1
      nofile:
        soft: 65536
        hard: 65536
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
      - elasticsearch_logs:/usr/share/elasticsearch/logs
    ports:
      - "9200:9200"
      - "9300:9300"
    networks:
      - elasticsearch
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200 || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    environment:
      ELASTICSEARCH_HOSTS: '["http://elasticsearch:9200"]'
      ELASTICSEARCH_USERNAME: kibana_system
      ELASTICSEARCH_PASSWORD: kibana_password
    ports:
      - "5601:5601"
    volumes:
      - kibana_data:/usr/share/kibana/data
    networks:
      - elasticsearch
    depends_on:
      - elasticsearch
    restart: unless-stopped

  logstash:
    image: docker.elastic.co/logstash/logstash:8.11.0
    volumes:
      - ./logstash/config/logstash.yml:/usr/share/logstash/config/logstash.yml
      - ./logstash/pipeline:/usr/share/logstash/pipeline
      - ./logs:/var/log/company:ro
    ports:
      - "5044:5044"
      - "5000:5000/tcp"
      - "5000:5000/udp"
      - "9600:9600"
    environment:
      LS_JAVA_OPTS: "-Xmx1g -Xmx1g"
    networks:
      - elasticsearch
    depends_on:
      - elasticsearch
    restart: unless-stopped

volumes:
  elasticsearch_data:
    driver: local
  elasticsearch_logs:
    driver: local
  kibana_data:
    driver: local

networks:
  elasticsearch:
    driver: bridge
```

### **Logstash Configuration**

```yaml
# logstash/config/logstash.yml
http.host: "0.0.0.0"
path.config: /usr/share/logstash/pipeline
path.data: /usr/share/logstash/data
path.logs: /usr/share/logstash/logs

# Pipeline configuration
pipeline.workers: 4
pipeline.batch.size: 125
pipeline.batch.delay: 50
queue.type: persisted
queue.page_capacity: 64mb
queue.max_events: 0
dead_letter_queue.enable: true

# Monitoring
xpack.monitoring.enabled: true
xpack.monitoring.elasticsearch.hosts: ["http://elasticsearch:9200"]
xpack.monitoring.elasticsearch.username: logstash_system
xpack.monitoring.elasticsearch.password: logstash_password

# Dead letter queue
dead_letter_queue.max_events: 5000
```

### **Index Lifecycle Management**

```json
// ILM Policy
PUT _ilm/policy/my-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "10GB",
            "max_age": "1d",
            "max_docs": 50000000
          },
          "set_priority": {
            "priority": 100
          },
          "allocate": {
            "number_of_replicas": 1
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "set_priority": {
            "priority": 50
          },
          "allocate": {
            "number_of_replicas": 0
          },
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
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

---

## **Ejemplos de Configuración**

### **Index Templates**

```json
// Template para logs de aplicación
PUT _index_template/logs-template
{
  "index_patterns": ["logs-*"],
  "priority": 100,
  "template": {
    "settings": {
      "number_of_shards": 3,
      "number_of_replicas": 1,
      "refresh_interval": "30s",
      "index.codec": "best_compression",
      "index.lifecycle.name": "my-policy",
      "index.lifecycle.rollover_alias": "logs"
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
        "logger": {
          "type": "keyword"
        },
        "message": {
          "type": "text",
          "analyzer": "standard",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "service": {
          "type": "keyword"
        },
        "environment": {
          "type": "keyword"
        },
        "version": {
          "type": "keyword"
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword"
            },
            "ip": {
              "type": "ip"
            }
          }
        },
        "user": {
          "properties": {
            "id": {
              "type": "keyword"
            },
            "email": {
              "type": "keyword"
            },
            "session_id": {
              "type": "keyword"
            }
          }
        },
        "request": {
          "properties": {
            "id": {
              "type": "keyword"
            },
            "method": {
              "type": "keyword"
            },
            "path": {
              "type": "text",
              "fields": {
                "keyword": {
                  "type": "keyword"
                }
              }
            },
            "status_code": {
              "type": "integer"
            },
            "duration_ms": {
              "type": "float"
            }
          }
        },
        "error": {
          "properties": {
            "type": {
              "type": "keyword"
            },
            "message": {
              "type": "text"
            },
            "stack_trace": {
              "type": "text"
            }
          }
        },
        "geoip": {
          "properties": {
            "location": {
              "type": "geo_point"
            },
            "country_name": {
              "type": "keyword"
            },
            "city_name": {
              "type": "keyword"
            }
          }
        }
      }
    }
  }
}

// Template para métricas
PUT _index_template/metrics-template
{
  "index_patterns": ["metrics-*"],
  "priority": 100,
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 1,
      "refresh_interval": "5s"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "metric": {
          "type": "keyword"
        },
        "value": {
          "type": "double"
        },
        "unit": {
          "type": "keyword"
        },
        "tags": {
          "type": "object",
          "dynamic": true
        }
      }
    }
  }
}
```

### **Security Configuration**

```yaml
# Configuración de roles
PUT _security/role/log_writer
{
  "cluster": [
    "monitor"
  ],
  "indices": [
    {
      "names": ["logs-*"],
      "privileges": ["create_index", "write", "read", "view_index_metadata"]
    }
  ]
}

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

# Configuración de usuarios
PUT _security/user/log_writer
{
  "password": "password123",
  "roles": ["log_writer"],
  "full_name": "Log Writer User",
  "email": "logwriter@company.com"
}

PUT _security/user/kibana_system
{
  "password": "kibana_password",
  "roles": ["kibana_system"]
}

# API Keys
POST _security/api_key
{
  "name": "logstash-api-key",
  "role_descriptors": {
    "log_writer": {
      "cluster": ["monitor"],
      "indices": [
        {
          "names": ["logs-*"],
          "privileges": ["create_index", "write", "read", "view_index_metadata"]
        }
      ]
    }
  },
  "expiration": "1h"
}
```

### **Performance Tuning**

```yaml
# JVM Configuration
# jvm.options
-Xms4g
-Xmx4g

-XX:+UseConcMarkSweepGC
-XX:CMSInitiatingOccupancyFraction=75
-XX:+UseCMSInitiatingOccupancyOnly

-XX:+AlwaysPreTouch
-Xss1m
-Djava.awt.headless=true
-Dfile.encoding=UTF-8
-Djna.nosys=true
-XX:-OmitStackTraceInFastThrow
-Dio.netty.noUnsafe=true
-Dio.netty.noKeySetOptimization=true
-Dio.netty.recycler.maxCapacityPerThread=0
-Dlog4j.shutdownHookEnabled=false
-Dlog4j2.disable.jmx=true

-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=data
-XX:ErrorFile=logs/hs_err_pid%p.log

# Elasticsearch performance settings
cluster.name: performance-cluster
node.name: performance-node

# Shard allocation
cluster.routing.allocation.disk.threshold_enabled: true
cluster.routing.allocation.disk.watermark.low: 85%
cluster.routing.allocation.disk.watermark.high: 90%
cluster.routing.allocation.disk.watermark.flood_stage: 95%

# Thread pools
thread_pool.write.queue_size: 2000
thread_pool.search.queue_size: 1000
thread_pool.get.queue_size: 1000

# Cache settings
indices.queries.cache.size: 10%
indices.requests.cache.size: 2%
indices.fielddata.cache.size: 40%

# Circuit breakers
indices.breaker.total.limit: 70%
indices.breaker.request.limit: 40%
indices.breaker.fielddata.limit: 60%

# Refresh interval
index.refresh_interval: 30s
index.number_of_replicas: 0

# Compression
index.codec: best_compression
index.compress.stored: true
```

---

## **Ejemplos en Java**

### **Elasticsearch Client Configuration**

```java
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

@Configuration
public class ElasticsearchConfig {
    
    @Value("${elasticsearch.host:localhost}")
    private String host;
    
    @Value("${elasticsearch.port:9200}")
    private int port;
    
    @Value("${elasticsearch.username:elastic}")
    private String username;
    
    @Value("${elasticsearch.password:password}")
    private String password;
    
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        RestClientBuilder builder = RestClient.builder(
            new HttpHost(host, port, "http")
        );
        
        // Configuración de autenticación
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(username, password));
        
        builder.setHttpClientConfigCallback(httpClientBuilder -> 
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        
        // Configuración de timeout
        builder.setRequestConfigCallback(requestConfigBuilder -> 
            requestConfigBuilder
                .setConnectTimeout(5000)
                .setSocketTimeout(30000)
                .setConnectionRequestTimeout(1000));
        
        // Configuración de thread pool
        builder.setHttpClientConfigCallback(httpClientBuilder -> 
            httpClientBuilder.setMaxConnTotal(100)
                .setMaxConnPerRoute(100));
        
        return new RestHighLevelClient(builder.build());
    }
    
    @Bean
    public ElasticsearchOperations elasticsearchTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }
}
```

### **Index Management**

```java
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.IndexTemplatesClient;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.settings.Settings;

@Service
public class IndexManagementService {
    
    private final RestHighLevelClient client;
    
    public IndexManagementService(RestHighLevelClient client) {
        this.client = client;
    }
    
    public void createApplicationLogsIndex(String indexName) {
        try {
            // Verificar si el índice existe
            GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
            boolean exists = client.indices().exists(getIndexRequest);
            
            if (exists) {
                logger.info("Index {} already exists", indexName);
                return;
            }
            
            // Configuración del índice
            CreateIndexRequest request = new CreateIndexRequest(indexName);
            
            // Settings
            Settings settings = Settings.builder()
                .put("number_of_shards", 3)
                .put("number_of_replicas", 1)
                .put("refresh_interval", "30s")
                .put("index.codec", "best_compression")
                .build();
            request.settings(settings);
            
            // Mappings
            String mapping = """
                {
                  "mappings": {
                    "properties": {
                      "@timestamp": {
                        "type": "date",
                        "format": "strict_date_optional_time||epoch_millis"
                      },
                      "level": {
                        "type": "keyword"
                      },
                      "logger": {
                        "type": "keyword"
                      },
                      "message": {
                        "type": "text",
                        "analyzer": "standard",
                        "fields": {
                          "keyword": {
                            "type": "keyword",
                            "ignore_above": 256
                          }
                        }
                      },
                      "service": {
                        "type": "keyword"
                      },
                      "environment": {
                        "type": "keyword"
                      },
                      "host": {
                        "properties": {
                          "name": {
                            "type": "keyword"
                          },
                          "ip": {
                            "type": "ip"
                          }
                        }
                      },
                      "request": {
                        "properties": {
                          "id": {
                            "type": "keyword"
                          },
                          "method": {
                            "type": "keyword"
                          },
                          "path": {
                            "type": "text",
                            "fields": {
                              "keyword": {
                                "type": "keyword"
                              }
                            }
                          },
                          "status_code": {
                            "type": "integer"
                          },
                          "duration_ms": {
                            "type": "float"
                          }
                        }
                      },
                      "user": {
                        "properties": {
                          "id": {
                            "type": "keyword"
                          },
                          "session_id": {
                            "type": "keyword"
                          }
                        }
                      },
                      "error": {
                        "properties": {
                          "type": {
                            "type": "keyword"
                          },
                          "message": {
                            "type": "text"
                          },
                          "stack_trace": {
                            "type": "text"
                          }
                        }
                      },
                      "geoip": {
                        "properties": {
                          "location": {
                            "type": "geo_point"
                          }
                        }
                      }
                    }
                  }
                }
                """;
            
            request.mapping(mapping, XContentType.JSON);
            
            // Alias
            request.alias(new Alias("logs"));
            
            // Crear índice
            CreateIndexResponse createIndexResponse = client.indices().create(request);
            
            if (createIndexResponse.isAcknowledged()) {
                logger.info("Index {} created successfully", indexName);
            } else {
                logger.warn("Index {} creation not acknowledged", indexName);
            }
            
        } catch (IOException e) {
            logger.error("Failed to create index {}", indexName, e);
            throw new ElasticsearchException("Failed to create index: " + indexName, e);
        }
    }
    
    public void deleteIndex(String indexName) {
        try {
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            DeleteIndexResponse response = client.indices().delete(request);
            
            if (response.isAcknowledged()) {
                logger.info("Index {} deleted successfully", indexName);
            } else {
                logger.warn("Index {} deletion not acknowledged", indexName);
            }
            
        } catch (IOException e) {
            logger.error("Failed to delete index {}", indexName, e);
            throw new ElasticsearchException("Failed to delete index: " + indexName, e);
        }
    }
    
    public void createIndexTemplate(String templateName, String indexPattern) {
        try {
            PutIndexTemplateRequest request = new PutIndexTemplateRequest(templateName);
            request.indexPatterns(indexPattern);
            request.order(100);
            
            // Settings
            Settings settings = Settings.builder()
                .put("number_of_shards", 2)
                .put("number_of_replicas", 1)
                .put("refresh_interval", "30s")
                .build();
            request.settings(settings);
            
            // Mapping
            String mapping = """
                {
                  "properties": {
                    "@timestamp": {
                      "type": "date"
                    },
                    "message": {
                      "type": "text"
                    },
                    "level": {
                      "type": "keyword"
                    }
                  }
                }
                """;
            
            request.mapping(mapping, XContentType.JSON);
            
            PutIndexTemplateResponse response = client.indices().putTemplate(request);
            
            if (response.isAcknowledged()) {
                logger.info("Template {} created successfully", templateName);
            }
            
        } catch (IOException e) {
            logger.error("Failed to create template {}", templateName, e);
            throw new ElasticsearchException("Failed to create template: " + templateName, e);
        }
    }
}
```

### **Document Operations**

```java
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;

@Service
public class DocumentService {
    
    private final RestHighLevelClient client;
    
    public DocumentService(RestHighLevelClient client) {
        this.client = client;
    }
    
    public String indexDocument(String index, String id, Map<String, Object> document) {
        try {
            IndexRequest request = new IndexRequest(index);
            request.id(id);
            request.source(document, XContentType.JSON);
            
            IndexResponse response = client.index(request);
            
            logger.info("Document indexed: {} in index: {}", response.getId(), index);
            return response.getId();
            
        } catch (IOException e) {
            logger.error("Failed to index document", e);
            throw new ElasticsearchException("Failed to index document", e);
        }
    }
    
    public void updateDocument(String index, String id, Map<String, Object> updates) {
        try {
            UpdateRequest request = new UpdateRequest(index, id);
            request.doc(updates, XContentType.JSON);
            request.docAsUpsert(true);
            
            UpdateResponse response = client.update(request);
            
            logger.info("Document updated: {} in index: {}", response.getId(), index);
            
        } catch (IOException e) {
            logger.error("Failed to update document", e);
            throw new ElasticsearchException("Failed to update document", e);
        }
    }
    
    public void deleteDocument(String index, String id) {
        try {
            DeleteRequest request = new DeleteRequest(index, id);
            
            DeleteResponse response = client.delete(request);
            
            logger.info("Document deleted: {} from index: {}", response.getId(), index);
            
        } catch (IOException e) {
            logger.error("Failed to delete document", e);
            throw new ElasticsearchException("Failed to delete document", e);
        }
    }
    
    public SearchResponse searchDocuments(String index, QueryBuilder query, int size) {
        try {
            SearchRequest request = new SearchRequest(index);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            
            searchSourceBuilder.query(query);
            searchSourceBuilder.size(size);
            searchSourceBuilder.sort(SortBuilders.fieldSort("@timestamp").order(SortOrder.DESC));
            
            request.source(searchSourceBuilder);
            
            SearchResponse response = client.search(request);
            
            logger.info("Search completed, found {} hits", response.getHits().getTotalHits().value);
            
            return response;
            
        } catch (IOException e) {
            logger.error("Failed to search documents", e);
            throw new ElasticsearchException("Failed to search documents", e);
        }
    }
    
    public SearchResponse searchWithAggregations(String index, QueryBuilder query) {
        try {
            SearchRequest request = new SearchRequest(index);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            
            searchSourceBuilder.query(query);
            
            // Aggregations
            TermsAggregationBuilder serviceAggregation = 
                AggregationBuilders.terms("services")
                    .field("service")
                    .size(10);
            
            AvgAggregationBuilder avgDuration = 
                AggregationBuilders.avg("avg_duration")
                    .field("request.duration_ms");
            
            MaxAggregationBuilder maxDuration = 
                AggregationBuilders.max("max_duration")
                    .field("request.duration_ms");
            
            searchSourceBuilder.aggregation(serviceAggregation);
            searchSourceBuilder.aggregation(avgDuration);
            searchSourceBuilder.aggregation(maxDuration);
            
            request.source(searchSourceBuilder);
            
            SearchResponse response = client.search(request);
            
            return response;
            
        } catch (IOException e) {
            logger.error("Failed to search with aggregations", e);
            throw new ElasticsearchException("Failed to search with aggregations", e);
        }
    }
    
    public void bulkIndexDocuments(String index, List<Map<String, Object>> documents) {
        BulkRequest bulkRequest = new BulkRequest();
        
        for (Map<String, Object> document : documents) {
            String id = UUID.randomUUID().toString();
            
            IndexRequest request = new IndexRequest(index);
            request.id(id);
            request.source(document, XContentType.JSON);
            
            bulkRequest.add(request);
        }
        
        try {
            BulkResponse bulkResponse = client.bulk(bulkRequest);
            
            if (bulkResponse.hasFailures()) {
                logger.warn("Bulk indexing had failures: {}", bulkResponse.buildFailureMessage());
                
                // Procesar errores específicos
                for (BulkItemResponse item : bulkResponse) {
                    if (item.isFailed()) {
                        logger.error("Failed to index document: {}", item.getFailureMessage());
                    }
                }
            } else {
                logger.info("Bulk indexed {} documents successfully", documents.size());
            }
            
        } catch (IOException e) {
            logger.error("Failed to bulk index documents", e);
            throw new ElasticsearchException("Failed to bulk index documents", e);
        }
    }
}
```

### **Query Builders**

```java
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.WildcardQueryBuilder;
import org.elasticsearch.index.query.GeoDistanceQueryBuilder;

@Service
public class QueryService {
    
    public BoolQueryBuilder buildComplexQuery(String service, String level, 
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             String messageText, String userId) {
        
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Filtro por servicio
        if (service != null) {
            boolQuery.filter(QueryBuilders.termQuery("service", service));
        }
        
        // Filtro por nivel de log
        if (level != null) {
            boolQuery.filter(QueryBuilders.termQuery("level", level));
        }
        
        // Rango de tiempo
        if (startTime != null && endTime != null) {
            RangeQueryBuilder timeRange = QueryBuilders.rangeQuery("@timestamp");
            timeRange.gte(startTime);
            timeRange.lte(endTime);
            boolQuery.filter(timeRange);
        }
        
        // Búsqueda full-text
        if (messageText != null) {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("message", messageText);
            matchQuery.operator(MatchQueryBuilder.Operator.AND);
            boolQuery.must(matchQuery);
        }
        
        // Filtro por usuario
        if (userId != null) {
            boolQuery.filter(QueryBuilders.termQuery("user.id", userId));
        }
        
        return boolQuery;
    }
    
    public BoolQueryBuilder buildErrorQuery(String service, LocalDateTime since) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Solo errores
        boolQuery.filter(QueryBuilders.termQuery("level", "ERROR"));
        
        // Servicio específico
        boolQuery.filter(QueryBuilders.termQuery("service", service));
        
        // Desde cuándo
        if (since != null) {
            RangeQueryBuilder timeRange = QueryBuilders.rangeQuery("@timestamp");
            timeRange.gte(since);
            boolQuery.filter(timeRange);
        }
        
        return boolQuery;
    }
    
    public BoolQueryBuilder buildGeospatialQuery(double lat, double lon, double distanceKm) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        GeoDistanceQueryBuilder geoQuery = QueryBuilders.geoDistanceQuery("geoip.location");
        geoQuery.point(lat, lon);
        geoQuery.distance(distanceKm, DistanceUnit.KILOMETERS);
        
        boolQuery.filter(geoQuery);
        
        return boolQuery;
    }
    
    public BoolQueryBuilder buildAPMQuery(String endpoint, int statusCode, long minDuration) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        
        // Filtro por endpoint
        boolQuery.filter(QueryBuilders.termQuery("request.path.keyword", endpoint));
        
        // Rango de códigos de estado
        RangeQueryBuilder statusRange = QueryBuilders.rangeQuery("request.status_code");
        statusRange.gte(statusCode);
        boolQuery.filter(statusRange);
        
        // Duración mínima
        RangeQueryBuilder durationRange = QueryBuilders.rangeQuery("request.duration_ms");
        durationRange.gte(minDuration);
        boolQuery.filter(durationRange);
        
        return boolQuery;
    }
}
```

### **Reindexing Service**

```java
import org.elasticsearch.action.admin.indices.reindex.ReindexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.ReindexResponse;
import org.elasticsearch.index.query.QueryBuilders;

@Service
public class ReindexingService {
    
    public void reindexToNewIndex(String sourceIndex, String destIndex, 
                                 QueryBuilder query, int slices) {
        try {
            ReindexRequest request = new ReindexRequest();
            request.setSourceIndices(sourceIndex);
            request.setDestIndex(destIndex);
            request.setRefresh(true);
            request.setSlices(slices);
            
            if (query != null) {
                request.setSourceQuery(query);
            }
            
            ReindexResponse response = client.reindex(request, RequestOptions.DEFAULT);
            
            logger.info("Reindex completed: {} documents processed, {} created, {} updated, {} deleted",
                       response.getTotal(),
                       response.getCreated(),
                       response.getUpdated(),
                       response.getDeleted());
            
        } catch (IOException e) {
            logger.error("Failed to reindex from {} to {}", sourceIndex, destIndex, e);
            throw new ElasticsearchException("Failed to reindex", e);
        }
    }
    
    public void migrateIndexStructure(String oldIndex, String newIndex) {
        try {
            // Obtener mapping del índice old
            GetMappingsRequest getMappingsRequest = new GetMappingsRequest();
            getMappingsRequest.indices(oldIndex);
            
            GetMappingsResponse mappingsResponse = client.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT);
            
            // Crear nuevo índice con el mismo mapping
            CreateIndexRequest createRequest = new CreateIndexRequest(newIndex);
            
            // Obtener settings del old index
            GetSettingsRequest getSettingsRequest = new GetSettingsRequest();
            getSettingsRequest.indices(oldIndex);
            
            GetSettingsResponse settingsResponse = client.indices().getSettings(getSettingsRequest, RequestOptions.DEFAULT);
            
            // Aplicar settings y mappings al nuevo índice
            IndexSettings oldSettings = settingsResponse.getIndexToSettings().get(oldIndex);
            if (oldSettings != null) {
                createRequest.settings(oldSettings.getSettings());
            }
            
            // Copiar mappings
            for (String mappingType : mappingsResponse.getMappings().keySet()) {
                MappingMetadata mapping = mappingsResponse.getMappings().get(mappingType);
                createRequest.mapping(mappingType, mapping.sourceAsMap(), XContentType.JSON);
            }
            
            CreateIndexResponse createResponse = client.indices().create(createRequest, RequestOptions.DEFAULT);
            
            if (createResponse.isAcknowledged()) {
                // Reindex con todos los documentos
                reindexToNewIndex(oldIndex, newIndex, null, 4);
                logger.info("Migration completed: {} -> {}", oldIndex, newIndex);
            }
            
        } catch (IOException e) {
            logger.error("Failed to migrate index from {} to {}", oldIndex, newIndex, e);
            throw new ElasticsearchException("Failed to migrate index", e);
        }
    }
}
```

---

## **Ventajas y Desventajas**

### **Ventajas**

**Búsqueda Potente**
- Búsqueda full-text avanzada con Lucene
- Scoring y relevancia configurable
- Búsqueda fuzzy y autocomplete
- Análisis de texto multiidioma

**Escalabilidad Horizontal**
- Distribución automática de shards
- Replicación para alta disponibilidad
- Scaling dinámico de clusters
- Balanceo de carga automático

**Flexibilidad de Esquema**
- Mapping dinámico flexible
- Actualización de esquema sobre la marcha
- Soporte para datos semi-estructurados
- Multi-field mappings

**Analytics Avanzado**
- Aggregaciones complejas y rápidas
- Bucket aggregations
- Metric aggregations
- Pipeline aggregations

**Ecosistema Maduro**
- Integración con múltiples herramientas
- APIs RESTful simples
- Drivers para múltiples lenguajes
- Documentación extensa

**Performance**
- Índices invertidos optimizados
- Cache de query y resultados
- Parallel processing
- Compresión de datos

### **Desventajas**

**Complejidad Operacional**
- Configuración compleja para optimización
- Tuning manual de performance
- Gestión de shards y réplicas
- Monitoring y debugging complejo

**Consumo de Recursos**
- Alto uso de memoria y CPU
- Almacenamiento duplicado (shards + réplicas)
- Costos de infraestructura significativos
- Backup y recovery costosos

**Consistencia**
- Consistencia eventual en distribución
- Problemas de split-brain en clusters
- Gestión de transacciones compleja
- Rollback de operaciones limitado

**Learning Curve**
- Conceptos de shards y réplicas
- DSL de queries complejo
- Troubleshooting de problemas
- Best practices no obvias

**Schema Evolution**
- Cambios de mapping complejos
- Reindexing costoso
- Downtime durante migraciones
- Versioning de esquemas

---

## **Buenas Prácticas**

### **1. Index Design**

```json
// Configuración óptima de índices
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "refresh_interval": "30s",
    "index.codec": "best_compression",
    "index.translog.flush_threshold_size": "1gb",
    "index.translog.sync_interval": "30s",
    "index.queries.cache.enabled": true,
    "index.requests.cache.enable": true,
    "cluster.routing.allocation.disk.threshold_enabled": true,
    "cluster.routing.allocation.disk.watermark.low": "85%",
    "cluster.routing.allocation.disk.watermark.high": "90%"
  },
  "mappings": {
    "dynamic": "strict",
    "properties": {
      "@timestamp": {
        "type": "date",
        "format": "strict_date_optional_time||epoch_millis"
      },
      "message": {
        "type": "text",
        "analyzer": "standard",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      }
    }
  }
}
```

### **2. Query Optimization**

```json
// Query optimizada
{
  "query": {
    "bool": {
      "must": [
        {
          "range": {
            "@timestamp": {
              "gte": "now-1h",
              "lte": "now"
            }
          }
        }
      ],
      "filter": [
        {
          "term": {
            "service": "api"
          }
        }
      ]
    }
  },
  "sort": [
    {
      "@timestamp": {
        "order": "desc"
      }
    }
  ],
  "size": 100,
  "from": 0,
  "_source": {
    "excludes": ["stack_trace"]
  }
}
```

### **3. Bulk Operations**

```java
// Bulk indexing optimizado
public void bulkIndexOptimized(String index, List<Document> documents) {
    BulkRequest bulkRequest = new BulkRequest();
    
    for (Document doc : documents) {
        IndexRequest request = new IndexRequest(index);
        request.id(doc.getId());
        request.source(convertToMap(doc), XContentType.JSON);
        
        // Configurar routing para mejorar performance
        request.routing(doc.getRoutingKey());
        
        bulkRequest.add(request);
    }
    
    // Configuración de bulk
    bulkRequest.timeout(TimeValue.timeValueMinutes(5));
    bulkRequest.refreshPolicy(WriteRequest.RefreshPolicy.NONE);
    
    try {
        BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        
        if (response.hasFailures()) {
            // Manejar errores
            for (BulkItemResponse item : response) {
                if (item.isFailed()) {
                    logger.error("Bulk operation failed: {}", item.getFailureMessage());
                }
            }
        }
        
    } catch (IOException e) {
        throw new RuntimeException("Bulk operation failed", e);
    }
}
```

### **4. Monitoring y Alertas**

```json
// Alertas de cluster health
{
  "trigger": {
    "schedule": {
      "interval": "1m"
    }
  },
  "input": {
    "search": {
      "request": {
        "indices": ["_cluster"],
        "body": {
          "query": {
            "match_all": {}
          }
        }
      }
    }
  },
  "condition": {
    "compare": {
      "ctx.payload.cluster.health.status": {
        "neq": "green"
      }
    }
  },
  "actions": {
    "send_email": {
      "email": {
        "to": ["admin@company.com"],
        "subject": "Elasticsearch Cluster Health Alert",
        "body": "Cluster health status: {{ctx.payload.cluster.health.status}}"
      }
    }
  }
}
```

### **5. Security Hardening**

```yaml
# Configuración de seguridad
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.keystore.path: elastic-certificates.p12
xpack.security.transport.ssl.truststore.path: elastic-certificates.p12
xpack.security.http.ssl.enabled: true

# Network security
network.host: 127.0.0.1
http.max_content_length: 100mb

# User authentication
xpack.security.authc.api_key.enabled: true
xpack.security.authc.realms.file.file1.order: 0
xpack.security.authc.realms.ldap.ldap1.order: 1

# Authorization
xpack.security.authz.roles_mapping_file:
  logstash_writer:
    enabled: true
    roles: ["logstash_writer"]
    rules:
      - field: { "username": "logstash_user" }
```

### **6. Backup Strategy**

```bash
#!/bin/bash
# Snapshot backup script

SNAPSHOT_REPO="backup-repo"
DATE=$(date +%Y%m%d_%H%M%S)

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/$SNAPSHOT_REPO/snapshot_$DATE?wait_for_completion=true" -H 'Content-Type: application/json' -d'
{
  "indices": "logs-*,metrics-*",
  "ignore_unavailable": true,
  "include_global_state": false,
  "metadata": {
    "taken_by": "backup-script",
    "taken_because": "Scheduled backup"
  }
}'

# Cleanup old snapshots (keep last 7 days)
curl -X DELETE "localhost:9200/_snapshot/$SNAPSHOT_REPO/*?keep=-7"

# Verify backup
curl -X GET "localhost:9200/_snapshot/$SNAPSHOT_REPO/snapshot_$DATE"
```

### **7. Performance Tuning**

```yaml
# JVM tuning
-Xms8g
-Xmx8g
-XX:+UseG1GC
-XX:G1HeapRegionSize=32m
-XX:+UnlockExperimentalVMOptions
-XX:+UseCGroupMemoryLimitForHeap
-XX:MaxGCPauseMillis=200
-XX:+ParallelRefProcEnabled

# Elasticsearch tuning
cluster.routing.allocation.disk.threshold_enabled: true
cluster.routing.allocation.disk.watermark.low: 85%
cluster.routing.allocation.disk.watermark.high: 90%
cluster.routing.allocation.disk.watermark.flood_stage: 95%

indices.memory.index_buffer_size: 30%
indices.queries.cache.size: 10%
indices.fielddata.cache.size: 30%

thread_pool.write.queue_size: 2000
thread_pool.search.queue_size: 1000
```

---

## **Referencias Oficiales**

1. **Elasticsearch Documentation**  
   https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html

2. **Elasticsearch SQL Reference**  
   https://www.elastic.co/guide/en/elasticsearch/reference/current/sql-reference.html

3. **Elasticsearch Mapping and Analysis**  
   https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html

4. **Elasticsearch Query DSL**  
   https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html

5. **Elasticsearch Aggregations**  
   https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html