# Logstash - Resumen Educativo

## ¿Qué es Logstash?

Logstash es una herramienta de procesamiento de datos de código abierto que forma parte del ecosistema ELK (Elasticsearch, Logstash, Kibana). Su función principal es recopilar, transformar y enviar datos de logs a diferentes destinos, especialmente a Elasticsearch.

## Características Principales

### Procesamiento de Datos
- **Pipeline de procesamiento**: Sistema de flujo de datos con input, filter y output
- **Transformación en tiempo real**: Procesa eventos de logs mientras ocurren
- **Múltiples formatos de entrada**: Soporta diversos tipos de datos y fuentes
- **Parsing inteligente**: Extrae información estructurada de logs no estructurados

### Capacidades de Filtrado
- **Parseo de logs**: Separación de campos, extracción de patrones
- **Enriquecimiento de datos**: Adición de información contextual
- **Filtrado condicional**: Procesamiento selectivo basado en criterios
- **Agregación**: Combinación de múltiples eventos

## Arquitectura y Componentes

### Pipeline de Logstash
```
Input → Filter → Output
```

#### Input (Entradas)
- **File**: Lectura de archivos de logs
- **Beats**: Recibe datos de Filebeat, Metricbeat, etc.
- **HTTP**: Recepción de datos via API REST
- **TCP/UDP**: Conexiones de red directas
- **Syslog**: Protocolo estándar de logs del sistema
- **Kafka**: Integración con Apache Kafka

#### Filter (Filtros)
- **Grok**: Parseo de patrones de texto complejos
- **Date**: Conversión y normalización de fechas
- **Mutate**: Manipulación de campos (agregar, remover, renombrar)
- **GeoIP**: Enriquecimiento con información geográfica
- **JSON**: Parseo de estructuras JSON
- **Drop**: Filtrado de eventos no deseados

#### Output (Salidas)
- **Elasticsearch**: Envío directo al motor de búsqueda
- **File**: Escritura a archivos de salida
- **TCP/UDP**: Envío via red
- **HTTP**: POST a servicios web
- **Kafka**: Publicación en topics de Kafka

## Configuración Básica

### Estructura de Configuración
```ruby
# Input section
input {
  file {
    path => "/var/log/application/*.log"
    start_position => "beginning"
  }
}

# Filter section
filter {
  if [path] =~ "access" {
    grok {
      match => { "message" => "%{COMBINEDAPACHELOG}" }
    }
    date {
      match => [ "timestamp", "dd/MMM/yyyy:HH:mm:ss Z" ]
    }
  }
}

# Output section
output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "logs-%{+YYYY.MM.dd}"
  }
  stdout { codec => rubydebug }
}
```

### Ejemplo con Múltiples Fuentes
```ruby
input {
  file {
    path => "/var/log/nginx/access.log"
    type => "nginx_access"
  }
  
  file {
    path => "/var/log/application/error.log"
    type => "application_error"
  }
  
  http {
    port => 8080
  }
}

filter {
  if [type] == "nginx_access" {
    grok {
      match => { 
        "message" => "%{NGINXACCESS}" 
      }
    }
  }
  
  if [type] == "application_error" {
    grok {
      match => {
        "message" => "(?<timestamp>%{YEAR}-%{MONTHNUM}-%{MONTHDAY} %{TIME}) \[%{LOGLEVEL:level}\] %{GREEDYDATA:message}"
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{type}-%{+YYYY.MM.dd}"
  }
}
```

## Casos de Uso Comunes

### 1. Procesamiento de Logs de Aplicación
- Parseo de logs de Spring Boot
- Extracción de stack traces
- Identificación de niveles de log (INFO, WARN, ERROR)
- Enriquecimiento con información del servidor

### 2. Análisis de Logs de Servidor Web
- Procesamiento de logs de Apache/Nginx
- Extracción de URLs, códigos de respuesta
- Análisis de métodos HTTP y tiempos de respuesta
- Geolocalización de IPs de clientes

### 3. Monitoreo de Seguridad
- Procesamiento de logs de firewall
- Detección de intentos de intrusión
- Análisis de patrones de acceso
- Alertas de eventos críticos

### 4. Integración con Beats
- Recepción de datos de Filebeat
- Procesamiento de métricas de Metricbeat
- Agregación de datos de múltiples fuentes
- Normalización de formatos

## Ventajas y Beneficios

### Flexibilidad
- **Configuración declarativa**: Sintaxis clara y mantenible
- **Múltiples plugins**: Amplia gama de extensiones disponibles
- **Procesamiento complejo**: Capacidades avanzadas de transformación
- **Escalabilidad horizontal**: Distribución en múltiples nodos

### Rendimiento
- **Procesamiento en memoria**: Alta velocidad de procesamiento
- **Paralelización**: Múltiples workers para mejor throughput
- **Buffering**: Manejo eficiente de picos de carga
- **Compresión**: Reducción del uso de ancho de banda

### Integración
- **Ecosistema ELK**: Integración nativa con Elasticsearch y Kibana
- **Beats**: Complemento perfecto para recolección de datos
- **APIs**: Interfaces programáticas para automatización
- **Docker**: Soporte nativo para contenedores

## Buenas Prácticas

### Configuración
- **Modularidad**: Separar configuraciones por tipo de log
- **Validación**: Probar configuraciones antes del despliegue
- **Documentación**: Comentar configuraciones complejas
- **Versionado**: Control de versiones de configuraciones

### Rendimiento
- **Heap Size**: Configurar memoria JVM apropiadamente
- **Workers**: Ajustar número de workers según CPU
- **Batch Size**: Optimizar tamaño de lotes para throughput
- **Memory**: Configurar límites de memoria por pipeline

### Monitoreo
- **Métricas**: Monitorear throughput y latencia
- **Errores**: Registrar y analizar errores de procesamiento
- **Recursos**: Supervisar uso de CPU y memoria
- **Logs**: Mantener logs de Logstash para debugging

### Seguridad
- **Permisos**: Restringir acceso a archivos de configuración
- **Red**: Usar TLS para comunicaciones en red
- **Autenticación**: Configurar autenticación para Elasticsearch
- **Firewall**: Restringir puertos de entrada no utilizados

## Integración con Docker

### Dockerfile para Logstash
```dockerfile
FROM docker.elastic.co/logstash/logstash:8.8.0

# Copiar configuraciones personalizadas
COPY config/logstash.conf /usr/share/logstash/pipeline/

# Instalar plugins adicionales
RUN logstash-plugin install logstash-filter-geoip

# Exponer puerto de monitoreo
EXPOSE 9600

# Comando de inicio
CMD ["logstash"]
```

### Docker Compose
```yaml
version: '3.8'

services:
  logstash:
    image: docker.elastic.co/logstash/logstash:8.8.0
    volumes:
      - ./config:/usr/share/logstash/config
      - ./pipeline:/usr/share/logstash/pipeline
      - /var/log:/var/log:ro
    ports:
      - "5044:5044"
      - "9600:9600"
    environment:
      - "LS_JAVA_OPTS=-Xmx1g -Xms1g"
    depends_on:
      - elasticsearch
```

## Solución de Problemas Comunes

### Problemas de Rendimiento
- **Alta latencia**: Aumentar workers y batch size
- **Pérdida de eventos**: Revisar configuración de memory
- **Bloqueos**: Verificar configuraciones de file input
- **OOM**: Ajustar heap size y garbage collection

### Problemas de Configuración
- **Errores de sintaxis**: Validar configuraciones con logstash -t
- **Plugins faltantes**: Instalar plugins requeridos
- **Permisos de archivo**: Verificar permisos en directorios de logs
- **Conectividad**: Probar conexiones a outputs

### Debugging
- **Modo verbose**: Usar --verbose para más información
- **Logs de Logstash**: Revisar logs en /var/log/logstash
- **Test mode**: Probar configuraciones sin ejecutar
- **Métricas**: Monitorear vía API de estadísticas

## Comparación con Alternativas

### Logstash vs Fluentd
- **Configuración**: Logstash usa Ruby, Fluentd usa JSON/YAML
- **Plugins**: Ambos tienen ecosistemas ricos de plugins
- **Rendimiento**: Fluentd generalmente más eficiente en memoria
- **Ecosistema**: Logstash mejor integrado con ELK stack

### Logstash vs Filebeat
- **Función**: Logstash procesa, Filebeat recolecta
- **Complejidad**: Filebeat más simple, Logstash más flexible
- **Recursos**: Filebeat consume menos recursos
- **Uso**: Filebeat para recolección, Logstash para transformación

## Conclusión

Logstash es una herramienta fundamental en el ecosistema de logging moderno, proporcionando capacidades robustas de procesamiento y transformación de datos. Su flexibilidad y potencia lo convierten en una opción ideal para casos de uso complejos de procesamiento de logs, especialmente cuando se requiere integración con el stack ELK completo.

La clave para un uso efectivo de Logstash está en entender su arquitectura de pipeline, configurar apropiadamente los plugins de input, filter y output, y seguir las mejores prácticas de rendimiento y seguridad. Con la configuración correcta, Logstash puede manejar grandes volúmenes de datos de logs de manera eficiente y confiable.