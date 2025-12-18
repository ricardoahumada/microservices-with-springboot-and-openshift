# Integración de OpenTelemetry en Mutualidad Platform

## Arquitectura General

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ afiliado-service│────>│validacion-service│     │notificacion-svc │
│   (Producer)    │     │   (Consumer)     │     │   (Consumer)    │
└────────┬────────┘     └────────┬─────────┘     └────────┬────────┘
         │                       │                        │
         │ OTLP (gRPC:4317)      │ OTLP                   │ OTLP
         v                       v                        v
┌─────────────────────────────────────────────────────────────────┐
│                    OpenTelemetry Collector                      │
│                      (otel-collector)                           │
└────────────────────────────┬────────────────────────────────────┘
                             │ OTLP (gRPC:4317)
                             v
                    ┌─────────────────┐
                    │     Jaeger      │
                    │   (UI: 16686)   │
                    └─────────────────┘
```

---

## 1. Dependencias Maven (pom.xml)

Cada servicio incluye las siguientes dependencias de OpenTelemetry:

```xml
<properties>
    <opentelemetry.version>1.32.0</opentelemetry.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-bom</artifactId>
            <version>${opentelemetry.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- API de OpenTelemetry -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-api</artifactId>
    </dependency>
    
    <!-- SDK de OpenTelemetry -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk</artifactId>
    </dependency>
    
    <!-- Auto-configuración -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk-extension-autoconfigure</artifactId>
    </dependency>
    
    <!-- Exportador OTLP -->
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
    
    <!-- Semantic Conventions -->
    <dependency>
        <groupId>io.opentelemetry.semconv</groupId>
        <artifactId>opentelemetry-semconv</artifactId>
        <version>1.23.1-alpha</version>
    </dependency>
</dependencies>
```

**Archivos modificados:**
- `afiliado-service/pom.xml`
- `validacion-service/pom.xml`
- `notificacion-service/pom.xml`

---

## 2. Configuración de OpenTelemetry (OpenTelemetryConfig.java)

Cada servicio tiene una clase de configuración que inicializa OpenTelemetry:

```java
package com.mutualidad.{servicio}.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

    @Value("${spring.application.name:servicio-name}")
    private String serviceName;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        // Recurso que identifica el servicio
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, serviceName,
                ResourceAttributes.SERVICE_VERSION, "1.0.0"
            )));

        // Exportador OTLP para enviar trazas al collector
        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint(otlpEndpoint)
            .build();

        // Proveedor de trazas con procesamiento batch
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setResource(resource)
            .build();

        // Configurar OpenTelemetry SDK con propagación W3C
        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal();

        Runtime.getRuntime().addShutdownHook(new Thread(tracerProvider::close));

        return openTelemetry;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(serviceName, "1.0.0");
    }
}
```

**Archivos:**
- `afiliado-service/src/main/java/com/mutualidad/afiliado/config/OpenTelemetryConfig.java`
- `validacion-service/src/main/java/com/mutualidad/validacion/config/OpenTelemetryConfig.java`
- `notificacion-service/src/main/java/com/mutualidad/notificacion/config/OpenTelemetryConfig.java`

---

## 3. Configuración de Conexión (application.yml)

Cada servicio configura el endpoint del collector:

```yaml
spring:
  application:
    name: afiliado-service  # o validacion-service, notificacion-service

# OpenTelemetry
otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
```

**Archivos:**
- `afiliado-service/src/main/resources/application.yml`
- `validacion-service/src/main/resources/application.yml`
- `notificacion-service/src/main/resources/application.yml`

---

## 4. Instrumentación de Kafka

### 4.1 Producer (afiliado-service)

El `AfiliadoEventPublisher` inyecta el contexto de trazas en los headers de Kafka:

```java
@Service
public class AfiliadoEventPublisher {

    private final KafkaTemplate<String, AfiliadoEvent> kafkaTemplate;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    // Constructor con inyección de dependencias
    public AfiliadoEventPublisher(KafkaTemplate<String, AfiliadoEvent> kafkaTemplate, 
                                   OpenTelemetry openTelemetry) {
        this.kafkaTemplate = kafkaTemplate;
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("afiliado-service");
    }

    // Setter para inyectar contexto en headers Kafka
    private static final TextMapSetter<Headers> KAFKA_SETTER = (headers, key, value) -> {
        if (headers != null && key != null && value != null) {
            headers.remove(key);
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    };

    private void sendWithTracing(String key, AfiliadoEvent event) {
        Span span = tracer.spanBuilder("kafka.send")
                .setSpanKind(SpanKind.PRODUCER)
                .setAttribute("messaging.system", "kafka")
                .setAttribute("messaging.destination", topic)
                .startSpan();

        try {
            ProducerRecord<String, AfiliadoEvent> record = new ProducerRecord<>(topic, key, event);

            // Inyectar contexto W3C en headers
            openTelemetry.getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current().with(span), record.headers(), KAFKA_SETTER);

            kafkaTemplate.send(record);
        } finally {
            span.end();
        }
    }
}
```

**Archivo:** `afiliado-service/src/main/java/com/mutualidad/afiliado/service/AfiliadoEventPublisher.java`

### 4.2 Consumers (validacion-service y notificacion-service)

Los consumers extraen el contexto de trazas de los headers Kafka:

```java
@Service
public class AfiliadoEventConsumer {

    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    public AfiliadoEventConsumer(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.tracer = openTelemetry.getTracer("notificacion-service");
    }

    // Getter para extraer contexto de headers Kafka
    private static final TextMapGetter<Headers> KAFKA_GETTER = new TextMapGetter<Headers>() {
        @Override
        public Iterable<String> keys(Headers headers) {
            return () -> StreamSupport.stream(headers.spliterator(), false)
                    .map(Header::key)
                    .iterator();
        }

        @Override
        public String get(Headers headers, String key) {
            Header header = headers.lastHeader(key);
            return header != null ? new String(header.value(), StandardCharsets.UTF_8) : null;
        }
    };

    @KafkaListener(topics = "${app.kafka.topic.afiliado-eventos}")
    public void handleAfiliadoEvent(ConsumerRecord<String, AfiliadoEvent> record) {
        // Extraer contexto de trazas desde headers
        Context extractedContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), KAFKA_GETTER);

        // Crear span hijo vinculado al trace original
        Span span = tracer.spanBuilder("notificacion-service.process")
                .setParent(extractedContext)
                .setSpanKind(SpanKind.CONSUMER)
                .startSpan();

        try {
            // Procesar evento...
        } finally {
            span.end();
        }
    }
}
```

**Archivos:**
- `validacion-service/src/main/java/com/mutualidad/validacion/service/AfiliadoValidationConsumer.java`
- `notificacion-service/src/main/java/com/mutualidad/notificacion/service/AfiliadoEventConsumer.java`

---

## 5. OpenTelemetry Collector

### 5.1 Configuración (otel-collector-config.yaml)

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024

exporters:
  otlp/jaeger:
    endpoint: jaeger:4317
    tls:
      insecure: true
  logging:
    loglevel: debug

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/jaeger, logging]
```

**Archivo:** `otel-collector-config.yaml`

### 5.2 Docker Compose

```yaml
services:
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.91.0
    command: ["--config=/etc/otel-collector-config.yaml"]
    volumes:
      - ./otel-collector-config.yaml:/etc/otel-collector-config.yaml
    ports:
      - "4317:4317"   # OTLP gRPC
    depends_on:
      - jaeger
    networks:
      - mutualidad-network

  jaeger:
    image: jaegertracing/all-in-one:1.52
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    ports:
      - "16686:16686"  # UI
      - "4317"         # OTLP gRPC (interno)
    networks:
      - mutualidad-network
```

**Archivo:** `docker-compose-otel.yml`

---

## 6. Flujo de Trazas Distribuidas

```
1. Cliente HTTP -> afiliado-service
   └── Span: "POST /api/afiliados"
       └── Span: "kafka.send" (PRODUCER)
           │
           │  [Headers Kafka: traceparent, tracestate]
           │
           ├──> validacion-service
           │    └── Span: "validacion-service.process" (CONSUMER)
           │        └── Span: "validacion-service.validateAfiliado"
           │
           └──> notificacion-service
                └── Span: "notificacion-service.process" (CONSUMER)
                    └── Span: "notificacion-service.sendNotification"
```

---

## 7. Verificación en Jaeger

1. Acceder a Jaeger UI: `http://localhost:16686`
2. Seleccionar servicio: `afiliado-service`
3. Click en "Find Traces"
4. Ver la traza completa con spans de los 3 servicios

---

## Resumen de Archivos Modificados/Creados

| Servicio | Archivo | Descripción |
|----------|---------|-------------|
| afiliado-service | `pom.xml` | Dependencias OpenTelemetry |
| afiliado-service | `OpenTelemetryConfig.java` | Configuración SDK |
| afiliado-service | `AfiliadoEventPublisher.java` | Instrumentación Kafka Producer |
| afiliado-service | `application.yml` | Endpoint OTLP |
| validacion-service | `pom.xml` | Dependencias OpenTelemetry |
| validacion-service | `OpenTelemetryConfig.java` | Configuración SDK |
| validacion-service | `AfiliadoValidationConsumer.java` | Instrumentación Kafka Consumer |
| validacion-service | `application.yml` | Endpoint OTLP |
| notificacion-service | `pom.xml` | Dependencias OpenTelemetry |
| notificacion-service | `OpenTelemetryConfig.java` | Configuración SDK |
| notificacion-service | `AfiliadoEventConsumer.java` | Instrumentación Kafka Consumer |
| notificacion-service | `application.yml` | Endpoint OTLP |
| infraestructura | `otel-collector-config.yaml` | Configuración Collector |
| infraestructura | `docker-compose-otel.yml` | Servicios Docker |
