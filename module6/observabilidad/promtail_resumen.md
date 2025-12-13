# Promtail - Resumen Educativo

## ¿Qué es Promtail?

Promtail es un agente de recolección de logs desarrollado por Grafana Labs como parte del stack Loki para logging. Su función principal es descubrir archivos de logs en el sistema, agregar labels a los streams de logs y enviarlos al servidor de Loki para almacenamiento y consulta.

## Características Principales

### Recolección de Logs
- **Descubrimiento automático**: Encuentra y monitorea archivos de logs automáticamente
- **Múltiples fuentes**: Soporta archivos, systemd journals, syslog y más
- **Labels dinámicos**: Agrega metadatos contextuales a los streams de logs
- **Tolerancia a fallos**: Manejo robusto de errores y reconexión automática

### Configuración Declarativa
- **YAML configurado**: Archivos de configuración en formato YAML
- **Scrape configs**: Configuraciones declarativas para diferentes fuentes
- **Relabeling**: Modificación y filtrado de labels
- **Targets**: Definición de destinos para envío de logs

## Arquitectura y Componentes

### Arquitectura de Promtail
```
Local Files → Scrape Discovery → Relabeling → Loki Remote Write
```

#### Componentes Principales
- **Server**: Servidor HTTP interno para métricas y health checks
- **Scrape Manager**: Gestor de configuraciones de scraping
- **Discoverers**: Módulos para descubrir archivos y fuentes
- **Targets**: Gestores de targets para diferentes tipos de logs
- **Client**: Cliente para envío de logs a Loki

#### Tipos de Targets
- **File Target**: Monitoreo de archivos de logs locales
- **Journal Target**: Lectura de systemd journals
- **Syslog Target**: Recepción de logs via syslog
- **Kafka Target**: Integración con Apache Kafka
- **Push Target**: Endpoint para recepción de logs via HTTP

### Pipeline de Procesamiento
1. **Discovery**: Descubrimiento de archivos/fuentes de logs
2. **Scraping**: Lectura de contenido de logs
3. **Relabeling**: Aplicación de reglas de etiquetado
4. **Batching**: Agrupación de logs para envío eficiente
5. **Sending**: Envío a servidor Loki

## Configuración Básica

### Estructura de Configuración
```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  - job_name: system
    static_configs:
      - targets:
          - localhost
        labels:
          job: syslog
          __path__: /var/log/*.log
```

### Configuración con Múltiples Jobs
```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push
    tenant_id: promtail-demo

scrape_configs:
  # Job para logs de aplicación
  - job_name: application
    static_configs:
      - targets:
          - localhost
        labels:
          job: application
          __path__: /var/log/app/*.log
    relabel_configs:
      - source_labels: ['__path__']
        target_label: 'service'
        regex: '/var/log/app/(.*)\.log'
      - source_labels: ['__path__']
        target_label: 'environment'
        regex: '.*/(dev|prod|staging)/.*'
        
  # Job para systemd journal
  - job_name: systemd
    journal:
      max_open_files: 3
      labels:
        job: systemd-journal
    relabel_configs:
      - source_labels: ['__journal__systemd_unit']
        target_label: 'unit'
      - source_labels: ['__journal__hostname']
        target_label: 'hostname'
        
  # Job para logs de Kubernetes
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
      - source_labels: ['__meta_kubernetes_namespace']
        target_label: 'namespace'
      - source_labels: ['__meta_kubernetes_pod_name']
        target_label: 'pod'
      - action: drop
        source_labels: ['__meta_kubernetes_pod_annotation_prometheus_io_scrape']
        regex: 'false'
```

## Casos de Uso Comunes

### 1. Recolección de Logs de Aplicaciones
- **Logs de Spring Boot**: Recolección automática de logs de aplicaciones Java
- **Logs de Docker**: Monitoreo de contenedores y sus logs
- **Logs de microservicios**: Agregación de logs distribuidos
- **Logs de base de datos**: Recolección de logs de PostgreSQL, MySQL, etc.

### 2. Integración con Kubernetes
- **Pods**: Descubrimiento automático de logs de pods
- **Namespaces**: Filtrado por namespaces específicos
- **Labels**: Uso de labels de Kubernetes como metadatos
- **Annotations**: Filtrado basado en anotaciones

### 3. Monitoreo de Infraestructura
- **Systemd services**: Lectura de journals del sistema
- **Syslog**: Recepción de logs del sistema
- **Nginx/Apache**: Logs de servidores web
- **Sistema**: Logs del kernel y sistema operativo

### 4. Integración con Cloud
- **AWS CloudWatch**: Integración con servicios de AWS
- **Azure Monitor**: Recolección de logs de Azure
- **GCP Logging**: Integración con Google Cloud Platform
- **Docker Swarm**: Monitoreo de clusters de Docker

## Configuraciones Avanzadas

### Pipeline de Procesamiento
```yaml
scrape_configs:
  - job_name: application
    static_configs:
      - targets:
          - localhost
        labels:
          job: application
          __path__: /var/log/app/*.log
    pipeline_stages:
      - json:
          expressions:
            level: level
            message: message
            timestamp: timestamp
            service: service
      - regex:
          source: level
          expression: '^(?P<level_name>INFO|WARN|ERROR|DEBUG)$'
      - labels:
          level:
          service:
      - timestamp:
          source: timestamp
          format: '2006-01-02T15:04:05Z07:00'
      - output:
          source: message
```

### Configuración con Kafka
```yaml
scrape_configs:
  - job_name: kafka
    kafka_sd_configs:
      - brokers:
          - kafka-broker:9092
        group_id: promtail-kafka
        topics:
          - logs
          - application-logs
        labels:
          job: kafka
    relabel_configs:
      - source_labels: ['__meta_kafka_topic']
        target_label: 'topic'
      - source_labels: ['__meta_kafka_partition']
        target_label: 'partition'
```

### Configuración con Syslog
```yaml
scrape_configs:
  - job_name: syslog
    syslog_sd_configs:
      - hosts:
          - tcp://syslog-server:514
        labels:
          job: syslog
    relabel_configs:
      - source_labels: ['__address__']
        target_label: '__syslog_connection_host'
      - source_labels: ['__syslog_message_hostname']
        target_label: 'hostname'
```

## Características Avanzadas

### Labels y Metadatos

#### Labels Estáticos
```yaml
static_configs:
  - targets:
      - localhost
    labels:
      job: web-server
      environment: production
      datacenter: us-east-1
```

#### Labels Dinámicos con Relabeling
```yaml
relabel_configs:
  - source_labels: ['__path__']
    target_label: 'service'
    regex: '/var/log/([^/]+)/.*'
  - source_labels: ['__path__']
    target_label: 'filename'
    regex: '/var/log/.*/([^/]+)$'
  - action: replace
    source_labels: ['__address__']
    target_label: 'instance'
    regex: '([^:]+):(\d+)'
    replacement: '${1}'
```

### Stages de Pipeline

#### JSON Parsing
```yaml
pipeline_stages:
  - json:
      expressions:
        level: level
        message: message
        user_id: user.id
        duration: duration
```

#### Regex Matching
```yaml
- regex:
    source: message
    expression: 'ERROR: (.*)'
  - regex:
    source: message
    expression: 'User (\w+) performed action'
    groups:
      - user
```

#### Labels y Dropping
```yaml
- labels:
    level:
    error_type:
- drop:
    regex: 'DEBUG.*'
    source: level
```

## Integración con Docker

### Dockerfile para Promtail
```dockerfile
FROM grafana/promtail:latest

# Copiar configuración personalizada
COPY config/promtail-config.yml /etc/promtail/config.yml

# Crear directorio para positions
RUN mkdir -p /tmp/promtail

# Exponer puerto de métricas
EXPOSE 9080

# Configurar health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9080/ready || exit 1

# Comando de inicio
CMD ["-config.file=/etc/promtail/config.yml"]
```

### Docker Compose Completo
```yaml
version: '3.8'

services:
  promtail:
    image: grafana/promtail:latest
    volumes:
      - /var/log:/var/log:ro
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - ./config/promtail-config.yml:/etc/promtail/config.yml
      - /tmp/promtail:/tmp/promtail
    ports:
      - "9080:9080"
    environment:
      - LOG_LEVEL=info
    depends_on:
      - loki

  loki:
    image: grafana/loki:latest
    ports:
      - "3100:3100"
    volumes:
      - ./config/loki-config.yml:/etc/loki/local-config.yaml
      - loki-data:/loki

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    depends_on:
      - loki

volumes:
  loki-data:
```

## Monitoreo y Métricas

### Métricas de Promtail
- **promtail_received_bytes_total**: Bytes recibidos por Promtail
- **promtail_sent_bytes_total**: Bytes enviados a Loki
- **promtail_sent_entries_total**: Entradas enviadas exitosamente
- **promtail_dropped_entries_total**: Entradas descartadas
- **promtail_file_bytes_total**: Bytes leídos de archivos

### Health Checks
```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0
  
# Health check endpoint
curl http://localhost:9080/ready
```

### Logging Configuration
```yaml
server:
  log_level: info
  log_format: json

# Logs estructurados para mejor observabilidad
```

## Buenas Prácticas

### Configuración
- **Separación de concerns**: Separar configuraciones por tipo de fuente
- **Labels consistentes**: Usar convenciones de nombres consistentes
- **Documentación**: Documentar configuraciones complejas
- **Versionado**: Control de versiones de configuraciones

### Rendimiento
- **Batch size**: Configurar tamaño apropiado de lotes
- **Buffer size**: Ajustar buffers para throughput
- **Concurrency**: Configurar concurrencia apropiada
- **Memory limits**: Establecer límites de memoria

### Seguridad
- **Permisos**: Restringir acceso a archivos de logs
- **Network**: Usar TLS para comunicaciones
- **Authentication**: Configurar autenticación para Loki
- **Network policies**: Restringir tráfico de red

### Mantenimiento
- **Rotación de logs**: Configurar rotación apropiada
- **Cleanup**: Limpiar archivos de positions antiguos
- **Monitoring**: Monitorear salud de Promtail
- **Updates**: Mantener Promtail actualizado

## Solución de Problemas Comunes

### Problemas de Recolección
- **Archivos no descubiertos**: Verificar configuraciones de discovery
- **Permisos denegados**: Revisar permisos de archivos
- **Encoding issues**: Verificar encoding de archivos
- **Rotation problems**: Configurar seguimiento de rotación

### Problemas de Envío
- **Timeouts**: Ajustar timeouts de cliente
- **Rate limits**: Respetar límites de Loki
- **Connection failures**: Verificar conectividad de red
- **Queue overflow**: Ajustar tamaños de cola

### Debugging
- **Verbose logging**: Activar logs detallados
- **Metrics**: Usar métricas para diagnóstico
- **Test mode**: Probar configuraciones localmente
- **Dry run**: Ejecutar sin enviar a Loki

## Comparación con Alternativas

### Promtail vs Filebeat
- **Ecosistema**: Promtail integrado con Grafana/Loki
- **Configuración**: Promtail usa YAML, Filebeat YAML también
- **Labels**: Promtail tiene capacidades superiores de etiquetado
- **Recursos**: Promtail generalmente más eficiente

### Promtail vs Fluentd
- **Flexibilidad**: Fluentd más flexible pero más complejo
- **Rendimiento**: Promtail optimizado para Loki específicamente
- **Configuración**: Promtail más simple para casos de uso básicos
- **Plugins**: Fluentd tiene ecosistema más grande

## Casos de Uso Avanzados

### Multi-tenant Logging
```yaml
clients:
  - url: http://loki:3100/loki/api/v1/push
    tenant_id: tenant-a
    tenant_id_source: label
    tenant_id_label_name: tenant

scrape_configs:
  - job_name: app-a
    static_configs:
      - targets: [localhost]
        labels:
          job: app-a
          tenant: tenant-a
```

### Aggregation de Múltiples Fuentes
```yaml
scrape_configs:
  - job_name: aggregated-logs
    file_sd_configs:
      - files:
          - /etc/promtail/discovery/*.yml
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
```

## Conclusión

Promtail es una herramienta especializada en recolección de logs diseñada específicamente para integrarse con el stack Loki de Grafana. Su enfoque en simplicidad, eficiencia y etiquetado inteligente lo convierte en una excelente opción para casos de uso de logging en entornos modernos, especialmente cuando se utiliza junto con Grafana para visualización y análisis.

Las ventajas clave de Promtail incluyen su configuración declarativa en YAML, capacidades avanzadas de etiquetado y relabeling, y su integración nativa con el ecosistema Grafana. Para organizaciones que ya utilizan Grafana para monitoreo, Promtail ofrece una solución coherente y eficiente para la recolección de logs.