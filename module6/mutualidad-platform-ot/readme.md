# Modulo 6: Observabilidad con OpenTelemetry

## Descripcion

Esta version de mutualidad-platform incluye **OpenTelemetry** para distributed tracing, permitiendo visualizar el flujo de requests a traves de los microservicios.

## Arquitectura

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Afiliado   │───>│ Notificacion│    │ Validacion  │
│  Service    │    │  Service    │    │  Service    │
│   :8081     │    │   :8082     │    │   :8083     │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                  │
       └──────────────────┴──────────────────┘
                          │ OTLP (gRPC)
                   ┌──────▼──────┐
                   │    OTel     │
                   │  Collector  │
                   │   :4317     │
                   └──────┬──────┘
                          │
                   ┌──────▼──────┐
                   │   Jaeger    │
                   │  :16686     │
                   └─────────────┘
```

## Componentes

| Servicio | Puerto | Descripcion |
|----------|--------|-------------|
| afiliado-service | 8081 | Productor de eventos Kafka |
| notificacion-service | 8082 | Consumidor - notificaciones |
| validacion-service | 8083 | Consumidor - validaciones |
| OTel Collector | 4317/4318 | Recolector de trazas (gRPC/HTTP) |
| Jaeger UI | 16686 | Visualizacion de trazas |
| Kafka | 9092 | Message broker |
| Kafka UI | 9000 | Administracion Kafka |

---

## Inicio Rapido

### 1. Iniciar Infraestructura

```bash
cd module6/solutions/mutualidad-platform-ot

# Iniciar OTel Collector + Jaeger + Kafka
docker-compose -f docker-compose-otel.yml up -d

# Verificar servicios
docker-compose -f docker-compose-otel.yml ps
```

### 2. Generar Trazas

```bash
# Crear un afiliado (genera traza)
curl -X POST "http://localhost:8081/api/afiliados?dni=12345678Z&nombre=Juan&apellidos=Garcia&email=juan@test.com&empresaId=EMP001"

# Respuesta incluye traceId
# {"status":"ACCEPTED","traceId":"abc123...","dni":"12345678Z"}
```

### 3. Ver Trazas en Jaeger

1. Abrir http://localhost:16686
2. Seleccionar servicio: `afiliado-service`
3. Click en **Find Traces**
4. Explorar spans y tiempos

---

## Configuracion OpenTelemetry

### Dependencias Maven

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
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-api</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-sdk</artifactId>
    </dependency>
    <dependency>
        <groupId>io.opentelemetry</groupId>
        <artifactId>opentelemetry-exporter-otlp</artifactId>
    </dependency>
</dependencies>
```

### application.yml

```yaml
otel:
  exporter:
    otlp:
      endpoint: http://otel-collector:4317
```

### OpenTelemetryConfig.java

```java
@Configuration
public class OpenTelemetryConfig {

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(
                ResourceAttributes.SERVICE_NAME, "afiliado-service"
            )));

        OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://otel-collector:4317")
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
            .setResource(resource)
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("afiliado-service");
    }
}
```

---

## Instrumentacion de Codigo

### Crear Spans Personalizados

```java
@RestController
public class AfiliadoController {

    private final Tracer tracer;

    @PostMapping("/api/afiliados")
    public ResponseEntity<?> crearAfiliado(...) {
        Span span = tracer.spanBuilder("afiliado.crear")
            .setAttribute("afiliado.dni", dni)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Logica de negocio
            eventPublisher.publish(...);
            
            return ResponseEntity.accepted().body(Map.of(
                "traceId", span.getSpanContext().getTraceId()
            ));
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }
}
```

---

## OTel Collector

### otel-collector-config.yaml

```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317

processors:
  batch:
    timeout: 1s

exporters:
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [jaeger]
```

---

## URLs de Acceso

| Servicio | URL |
|----------|-----|
| Jaeger UI | http://localhost:16686 |
| Kafka UI | http://localhost:9000 |
| OTel Metrics | http://localhost:8888/metrics |
| Afiliado API | http://localhost:8081/api/afiliados |

---

## Estructura de Archivos

```
mutualidad-platform-ot/
├── docker-compose-otel.yml          # Stack completo
├── otel-collector-config.yaml       # Config del collector
├── afiliado-service/
│   ├── pom.xml                       # + deps OpenTelemetry
│   └── src/main/java/.../
│       ├── config/OpenTelemetryConfig.java
│       └── controller/AfiliadoController.java
├── notificacion-service/
│   └── src/main/java/.../config/OpenTelemetryConfig.java
└── validacion-service/
    └── src/main/java/.../config/OpenTelemetryConfig.java
```

---

## Limpieza

```bash
docker-compose -f docker-compose-otel.yml down -v
```
