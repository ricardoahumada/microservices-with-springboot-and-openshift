# OpenTelemetry

OpenTelemetry es un framework de observabilidad de código abierto que proporciona APIs, SDKs y herramientas para instrumentar, generar, recopilar y exportar datos de telemetría (traces, metrics y logs) de aplicaciones. Diseñado como un estándar unificado, permite la observabilidad vendor-neutral en sistemas distribuidos modernos.

## **¿Qué es OpenTelemetry?**

OpenTelemetry (OTel) es un proyecto de la Cloud Native Computing Foundation (CNCF) que unifica las especificaciones para tracing distribuido, métricas y logging. Proporciona una implementación estándar y portable de observabilidad que permite a las aplicaciones generar telemetría consistente y exportarla a múltiples backends de observabilidad.

### Características Principales

- **API unificada**: Estándares para traces, metrics y logs
- **Instrumentación automática**: Auto-instrumentation para frameworks populares
- **Instrumentación manual**: APIs para instrumentación programática
- **Vendor-agnostic**: Compatible con múltiples backends de observabilidad
- **Ecosistema robusto**: Soporte para múltiples lenguajes y tecnologías
- **Escalabilidad**: Diseñado para sistemas distribuidos a gran escala

## **Conceptos Clave**

### Traces (Trazas)
Los **traces** representan la ejecución completa de una solicitud a través de múltiples servicios:

```java
// Crear un trace
Tracer tracer = GlobalOpenTelemetry.get().getTracer("mi-aplicacion");

Span span = tracer.spanBuilder("operacion-usuario")
    .setSpanKind(SpanKind.SERVER)
    .setAttribute("http.method", "GET")
    .setAttribute("http.url", "http://localhost:8080/usuarios/123")
    .startSpan();

try (Scope scope = span.makeCurrent()) {
    // Operación de negocio
    Usuario usuario = usuarioService.obtenerPorId(123L);
    
    // Crear span hijo para operación de base de datos
    Span dbSpan = tracer.spanBuilder("consulta-usuario")
        .setParent(Context.current())
        .startSpan();
    
    try (Scope dbScope = dbSpan.makeCurrent()) {
        dbSpan.setAttribute("db.statement", "SELECT * FROM usuarios WHERE id = ?");
        dbSpan.setAttribute("db.type", "postgresql");
        
        // Operación de base de datos
        usuarioRepository.findById(123L);
    } finally {
        dbSpan.end();
    }
    
    span.setAttribute("usuario.id", usuario.getId());
    span.setAttribute("usuario.email", usuario.getEmail());
} catch (Exception e) {
    span.recordException(e);
    span.setStatus(StatusCode.ERROR, e.getMessage());
    throw e;
} finally {
    span.end();
}
```

### Spans (Intervalos)
Los **spans** son las unidades básicas de trabajo en un trace:

```java
// Span con atributos
Span span = tracer.spanBuilder("procesar-pedido")
    .setAttribute("pedido.id", pedido.getId())
    .setAttribute("pedido.total", pedido.getTotal().doubleValue())
    .setAttribute("usuario.id", pedido.getUsuario().getId())
    .startSpan();

// Span con eventos
span.addEvent("iniciando-procesamiento", 
    Attributes.of(
        AttributeKey.stringKey("procesador"), "main-thread",
        AttributeKey.longKey("items-count"), pedido.getItems().size()
    ));

// Span con métricas
span.addEvent("pago-completado",
    Attributes.of(
        AttributeKey.stringKey("metodo-pago"), "tarjeta-credito",
        AttributeKey.doubleKey("monto"), pedido.getTotal().doubleValue()
    ));

span.end();
```

### Metrics (Métricas)
Las **métricas** representan mediciones numéricas de aspectos del sistema:

```java
// Contador
Meter meter = GlobalOpenTelemetry.get().getMeter("mi-aplicacion");
LongCounter requestCounter = meter.counterBuilder("http_requests_total")
    .setDescription("Total de requests HTTP")
    .build();

requestCounter.add(1, 
    Attributes.of(
        AttributeKey.stringKey("method"), "GET",
        AttributeKey.stringKey("status"), "200",
        AttributeKey.stringKey("endpoint"), "/usuarios"
    ));

// Histograma
DoubleHistogram requestDuration = meter.histogramBuilder("http_request_duration")
    .setDescription("Duración de requests HTTP en segundos")
    .setExplicitBucketBoundaries(
        0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0)
    .build();

requestDuration.record(0.156,
    Attributes.of(
        AttributeKey.stringKey("method"), "GET",
        AttributeKey.stringKey("endpoint"), "/usuarios"
    ));
```

### Context Propagation (Propagación de Contexto)
El **context propagation** mantiene la trazabilidad entre servicios:

```java
// En el cliente
Span outSpan = tracer.spanBuilder("llamada-externa")
    .setSpanKind(SpanKind.CLIENT)
    .startSpan();

try (Scope scope = outSpan.makeCurrent()) {
    // Propagar contexto en headers HTTP
    HttpURLConnection connection = 
        (HttpURLConnection) new URL("http://external-service/api/data").openConnection();
    
    // Inyectar contexto en headers
    OpenTelemetryPropagators.getGlobalPropagators().getTextMapPropagator()
        .inject(Context.current(), connection, HttpURLConnectionCarrier::setHeader);
    
    connection.getResponseCode();
} finally {
    outSpan.end();
}

// En el servidor
@Component
public class OpenTelemetryInterceptor implements HandlerInterceptor {
    
    private final TextMapPropagator propagator = 
        OpenTelemetryPropagators.getGlobalPropagators().getTextMapPropagator();
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler) {
        
        // Extraer contexto de headers
        Context context = propagator.extract(
            Context.current(), 
            request, 
            HttpServletCarrier::getHeader
        );
        
        // Crear span servidor con contexto propagado
        Span serverSpan = tracer.spanBuilder("handle-request")
            .setParent(context)
            .setSpanKind(SpanKind.SERVER)
            .startSpan();
        
        // Hacer span disponible para el request
        request.setAttribute("otel-server-span", serverSpan);
        
        return true;
    }
}
```

### Exporters (Exportadores)
Los **exporters** envían datos de telemetría a backends:

```java
@Configuration
public class OpenTelemetryConfig {
    
    @Bean
    public OpenTelemetry openTelemetry() {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(
                        BatchSpanProcessor.builder(
                            JaegerGrpcSpanExporter.builder()
                                .setEndpoint("http://jaeger:14250")
                                .build()
                        )
                        .setMaxExportBatchSize(512)
                        .setExportTimeout(Duration.ofSeconds(30))
                        .build()
                    )
                    .build()
            )
            .setMeterProvider(
                SdkMeterProvider.builder()
                    .registerMetricReader(
                        PeriodicMetricReader.builder(
                            OtlpGrpcMetricExporter.builder()
                                .setEndpoint("http://tempo:4317")
                                .build()
                        )
                        .setExportInterval(Duration.ofSeconds(30))
                        .build()
                    )
                    .build()
            )
            .setPropagators(ContextPropagators.create(
                W3CTraceContextPropagator.getInstance()))
            .build();
    }
    
    @Bean
    public SpanProcessor spanProcessor() {
        return BatchSpanProcessor.builder(
            LoggingSpanExporter.builder()
                .build()
        )
        .setMaxExportBatchSize(512)
        .setExportTimeout(Duration.ofSeconds(30))
        .build();
    }
}
```

## **Casos de Uso**

### 1. Monitoreo de Microservicios
- **Trazabilidad distribuida**: Seguir solicitudes a través de múltiples servicios
- **Análisis de latencia**: Identificar cuellos de botella en el sistema
- **Dependencias**: Visualizar interacciones entre servicios
- **Rendimiento**: Medir performance de operaciones específicas

```java
@Service
public class PedidoService {
    
    private final Tracer tracer;
    private final UsuarioService usuarioService;
    private final InventoryService inventoryService;
    
    public PedidoService(Tracer tracer,
                        UsuarioService usuarioService,
                        InventoryService inventoryService) {
        this.tracer = tracer;
        this.usuarioService = usuarioService;
        this.inventoryService = inventoryService;
    }
    
    public Pedido crearPedido(CrearPedidoRequest request) {
        Span span = tracer.spanBuilder("crear-pedido")
            .setAttribute("pedido.id", request.getPedidoId())
            .setAttribute("usuario.id", request.getUsuarioId())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Validar usuario
            span.addEvent("validando-usuario");
            Usuario usuario = usuarioService.validarUsuario(request.getUsuarioId());
            span.setAttribute("usuario.validado", true);
            
            // Verificar inventario
            span.addEvent("verificando-inventario");
            List<ItemPedido> items = inventoryService.verificarInventario(request.getItems());
            span.setAttribute("items.validados", items.size());
            
            // Procesar pago
            span.addEvent("procesando-pago");
            ResultadoPago resultado = procesarPago(request.getPago());
            span.setAttribute("pago.procesado", resultado.isExitoso());
            
            if (resultado.isExitoso()) {
                // Crear pedido
                span.addEvent("creando-pedido");
                Pedido pedido = persistirPedido(usuario, items);
                span.setAttribute("pedido.creado", pedido.getId());
                
                return pedido;
            } else {
                span.setStatus(StatusCode.ERROR, "Pago fallido");
                throw new PagoFallidoException("No se pudo procesar el pago");
            }
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### 2. Performance Monitoring
- **Métricas de negocio**: KPIs específicos de la aplicación
- **Métricas técnicas**: CPU, memoria, I/O, red
- **SLI/SLO tracking**: Medir objetivos de nivel de servicio
- **Capacity planning**: Planificar capacidad basada en tendencias

```java
@Component
public class BusinessMetrics {
    
    private final Meter meter;
    private final LongCounter ordersCounter;
    private final LongHistogram orderValueHistogram;
    private final DoubleGauge activeUsersGauge;
    
    public BusinessMetrics(Meter meter) {
        this.meter = meter;
        this.ordersCounter = meter.counterBuilder("orders_total")
            .setDescription("Total de pedidos creados")
            .build();
        
        this.orderValueHistogram = meter.histogramBuilder("order_value_dollars")
            .setDescription("Valor de pedidos en dólares")
            .setExplicitBucketBoundaries(0.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
            .build();
        
        this.activeUsersGauge = meter.gaugeBuilder("active_users")
            .setDescription("Usuarios activos actualmente")
            .setUnit("users")
            .buildObserver();
    }
    
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        ordersCounter.add(1,
            Attributes.of(
                AttributeKey.stringKey("region"), event.getRegion(),
                AttributeKey.stringKey("channel"), event.getChannel()
            ));
        
        orderValueHistogram.record(event.getTotalAmount().doubleValue(),
            Attributes.of(
                AttributeKey.stringKey("currency"), "USD"
            ));
    }
    
    @Scheduled(fixedRate = 60000) // Cada minuto
    public void updateActiveUsers() {
        int activeUsers = sessionManager.getActiveUsersCount();
        
        activeUsersGauge.record(activeUsers,
            Attributes.of(
                AttributeKey.stringKey("environment"), "production"
            ));
    }
}
```

### 3. Debugging y Troubleshooting
- **Root cause analysis**: Identificar la causa raíz de problemas
- **Error tracking**: Rastrear errores y excepciones
- **Performance profiling**: Perfilado detallado de operaciones
- **Flow analysis**: Análisis de flujo de ejecución

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final Tracer tracer;
    
    public GlobalExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception ex, HttpServletRequest request) {
        
        // Crear span específico para errores
        Span errorSpan = tracer.spanBuilder("error-handling")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("error.type", ex.getClass().getSimpleName())
            .setAttribute("error.message", ex.getMessage())
            .setAttribute("http.method", request.getMethod())
            .setAttribute("http.url", request.getRequestURL().toString())
            .setAttribute("http.status_code", 500)
            .startSpan();
        
        try (Scope scope = errorSpan.makeCurrent()) {
            // Obtener span actual para agregar información del error
            Span currentSpan = Span.current();
            if (currentSpan != null) {
                currentSpan.recordException(ex);
                currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());
            }
            
            logger.error("Error occurred: {}", ex.getMessage(), ex);
            
            ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(500)
                .error("Internal Server Error")
                .message("Ha ocurrido un error interno")
                .traceId(getTraceId())
                .build();
            
            return ResponseEntity.status(500).body(error);
        } finally {
            errorSpan.end();
        }
    }
    
    private String getTraceId() {
        Span currentSpan = Span.current();
        return currentSpan != null ? 
            currentSpan.getSpanContext().getTraceId() : "unknown";
    }
}
```

### 4. Compliance y Auditing
- **Audit trails**: Rastrear operaciones sensibles
- **Data lineage**: Seguimiento del flujo de datos
- **Access tracking**: Rastrear accesos a recursos
- **Regulatory compliance**: Cumplimiento de regulaciones

```java
@Service
public class AuditService {
    
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter auditEventsCounter;
    
    public AuditService(Tracer tracer, Meter meter) {
        this.tracer = tracer;
        this.meter = meter;
        this.auditEventsCounter = meter.counterBuilder("audit_events_total")
            .setDescription("Total de eventos de auditoría")
            .build();
    }
    
    public void logDataAccess(String userId, String resourceId, 
                            String operation, String outcome) {
        
        Span auditSpan = tracer.spanBuilder("data-access")
            .setSpanKind(SpanKind.INTERNAL)
            .setAttribute("audit.user_id", userId)
            .setAttribute("audit.resource_id", resourceId)
            .setAttribute("audit.operation", operation)
            .setAttribute("audit.outcome", outcome)
            .setAttribute("audit.timestamp", Instant.now().toString())
            .startSpan();
        
        try (Scope scope = auditSpan.makeCurrent()) {
            // Log to audit database
            AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .resourceId(resourceId)
                .operation(operation)
                .outcome(outcome)
                .timestamp(Instant.now())
                .traceId(auditSpan.getSpanContext().getTraceId())
                .build();
            
            auditRepository.save(auditLog);
            
            // Update metrics
            auditEventsCounter.add(1,
                Attributes.of(
                    AttributeKey.stringKey("operation"), operation,
                    AttributeKey.stringKey("outcome"), outcome,
                    AttributeKey.stringKey("user_type"), getUserType(userId)
                ));
            
        } finally {
            auditSpan.end();
        }
    }
}
```

## **Ejemplos en Java**

### Instrumentación Automática con Spring Boot

```java
@SpringBootApplication
@EnableOpenTelemetry
public class ObservabilityApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ObservabilityApplication.class, args);
    }
}

// Configuración de OpenTelemetry
@Configuration
@ConditionalOnClass(OpenTelemetry.class)
public class OpenTelemetryConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "otel.exporter.jaeger.enabled", havingValue = "true")
    public SpanProcessor jaegerSpanProcessor() {
        return BatchSpanProcessor.builder(
            JaegerGrpcSpanExporter.builder()
                .setEndpoint("http://jaeger:14250")
                .setTimeout(Duration.ofSeconds(10))
                .build()
        )
        .setMaxExportBatchSize(512)
        .setExportTimeout(Duration.ofSeconds(30))
        .build();
    }
    
    @Bean
    @ConditionalOnProperty(name = "otel.exporter.prometheus.enabled", havingValue = "true")
    public MetricReader prometheusMetricReader() {
        return PrometheusMetricReader.builder()
            .build();
    }
}

// Filtro para instrumentación HTTP automática
@Component
public class OpenTelemetryFilter implements Filter {
    
    private final Tracer tracer;
    
    public OpenTelemetryFilter(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // Crear span para request HTTP
        Span span = tracer.spanBuilder(httpRequest.getMethod() + " " + httpRequest.getRequestURI())
            .setSpanKind(SpanKind.SERVER)
            .setAttribute("http.method", httpRequest.getMethod())
            .setAttribute("http.url", httpRequest.getRequestURL().toString())
            .setAttribute("http.host", httpRequest.getHeader("Host"))
            .setAttribute("http.user_agent", httpRequest.getHeader("User-Agent"))
            .setAttribute("http.client_ip", getClientIpAddress(httpRequest))
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            chain.doFilter(request, response);
            
            // Agregar información de respuesta
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            span.setAttribute("http.status_code", httpResponse.getStatus());
            span.setAttribute("http.response_content_length", 
                response.getBufferSize());
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
```

### Instrumentación Manual de Servicios

```java
@Service
public class EmailService {
    
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter emailsSentCounter;
    private final DoubleHistogram emailDeliveryTimeHistogram;
    
    public EmailService(Tracer tracer, Meter meter) {
        this.tracer = tracer;
        this.meter = meter;
        this.emailsSentCounter = meter.counterBuilder("emails_sent_total")
            .setDescription("Total de emails enviados")
            .build();
        this.emailDeliveryTimeHistogram = meter.histogramBuilder("email_delivery_time")
            .setDescription("Tiempo de entrega de emails en milisegundos")
            .setExplicitBucketBoundaries(
                100.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0, 30000.0)
            .build();
    }
    
    public void enviarEmail(EmailRequest request) {
        Span span = tracer.spanBuilder("enviar-email")
            .setAttribute("email.recipient", request.getRecipient())
            .setAttribute("email.subject", request.getSubject())
            .setAttribute("email.template", request.getTemplate())
            .startSpan();
        
        long startTime = System.currentTimeMillis();
        
        try (Scope scope = span.makeCurrent()) {
            // Validar email
            span.addEvent("validando-email");
            validateEmail(request.getRecipient());
            
            // Aplicar template
            span.addEvent("aplicando-template");
            String contenido = applyTemplate(request);
            span.setAttribute("email.content_length", contenido.length());
            
            // Enviar via proveedor
            span.addEvent("enviando-via-proveedor");
            EmailResult result = sendViaProvider(request.getRecipient(), contenido);
            
            span.setAttribute("email.provider", result.getProvider());
            span.setAttribute("email.message_id", result.getMessageId());
            
            // Actualizar métricas
            emailsSentCounter.add(1,
                Attributes.of(
                    AttributeKey.stringKey("provider"), result.getProvider(),
                    AttributeKey.stringKey("template"), request.getTemplate(),
                    AttributeKey.stringKey("outcome"), "success"
                ));
            
        } catch (EmailException e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            
            // Métrica de fallo
            emailsSentCounter.add(1,
                Attributes.of(
                    AttributeKey.stringKey("outcome"), "failure",
                    AttributeKey.stringKey("error_type"), e.getClass().getSimpleName()
                ));
            
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            emailDeliveryTimeHistogram.record(duration);
            span.end();
        }
    }
    
    private EmailResult sendViaProvider(String recipient, String content) {
        // Simular llamada a proveedor
        Span providerSpan = tracer.spanBuilder("proveedor-email")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("peer.service", "email-provider")
            .setAttribute("peer.address", "email-provider:587")
            .startSpan();
        
        try (Scope scope = providerSpan.makeCurrent()) {
            // Llamada HTTP al proveedor
            try {
                // Simular latencia del proveedor
                Thread.sleep(500 + (long)(Math.random() * 1000));
                
                providerSpan.setAttribute("http.status_code", 200);
                providerSpan.setAttribute("email.provider_response", "success");
                
                return EmailResult.builder()
                    .provider("smtp-provider")
                    .messageId("msg-" + System.currentTimeMillis())
                    .build();
                    
            } catch (Exception e) {
                providerSpan.setAttribute("http.status_code", 500);
                providerSpan.recordException(e);
                providerSpan.setStatus(StatusCode.ERROR, e.getMessage());
                throw new EmailException("Proveedor no disponible", e);
            }
        } finally {
            providerSpan.end();
        }
    }
}
```

### Instrumentación de Base de Datos

```java
@Configuration
public class DatabaseTelemetryConfig {
    
    @Bean
    public DataSource dataSource(DataSource originalDataSource, 
                                Tracer tracer, Meter meter) {
        return DataSourceTelemetryProxy.builder(originalDataSource)
            .withTracer(tracer)
            .withMeter(meter)
            .withServiceName("user-service-db")
            .build()
            .proxy();
    }
}

@Component
public class DatabaseTelemetryProxy {
    
    private final DataSource originalDataSource;
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter queryCounter;
    private final DoubleHistogram queryDurationHistogram;
    
    public DatabaseTelemetryProxy(DataSource originalDataSource,
                                 Tracer tracer, Meter meter) {
        this.originalDataSource = originalDataSource;
        this.tracer = tracer;
        this.meter = meter;
        
        this.queryCounter = meter.counterBuilder("db_queries_total")
            .setDescription("Total de consultas a base de datos")
            .build();
        
        this.queryDurationHistogram = meter.histogramBuilder("db_query_duration")
            .setDescription("Duración de consultas a base de datos en milisegundos")
            .setExplicitBucketBoundaries(
                1.0, 5.0, 10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0)
            .build();
    }
    
    public Connection getConnection() throws SQLException {
        return new TelemetryConnection(originalDataSource.getConnection());
    }
    
    private class TelemetryConnection implements Connection {
        
        private final Connection delegate;
        
        public TelemetryConnection(Connection delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public PreparedStatement prepareStatement(String sql) throws SQLException {
            return new TelemetryPreparedStatement(delegate.prepareStatement(sql), sql);
        }
        
        // Implementar otros métodos de Connection...
    }
    
    private class TelemetryPreparedStatement implements PreparedStatement {
        
        private final PreparedStatement delegate;
        private final String sql;
        
        public TelemetryPreparedStatement(PreparedStatement delegate, String sql) {
            this.delegate = delegate;
            this.sql = sql;
        }
        
        @Override
        public ResultSet executeQuery() throws SQLException {
            Span span = tracer.spanBuilder("db.query")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.connection_string", getConnectionString())
                .setAttribute("db.statement", maskSql(sql))
                .setAttribute("db.operation", "SELECT")
                .startSpan();
            
            long startTime = System.currentTimeMillis();
            
            try (Scope scope = span.makeCurrent()) {
                ResultSet result = delegate.executeQuery();
                
                // Obtener información adicional del result set
                ResultSetMetaData metaData = result.getMetaData();
                span.setAttribute("db.columns", metaData.getColumnCount());
                
                return result;
                
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                
                queryDurationHistogram.record(duration);
                queryCounter.add(1,
                    Attributes.of(
                        AttributeKey.stringKey("db.operation"), "SELECT",
                        AttributeKey.stringKey("db.statement_type"), getStatementType(sql)
                    ));
                
                span.end();
            }
        }
        
        private String maskSql(String sql) {
            // Mask sensitive data in SQL queries
            return sql.replaceAll("'[^']*'", "'***'")
                     .replaceAll("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}", "****-****-****-****");
        }
        
        private String getStatementType(String sql) {
            return sql.substring(0, sql.indexOf(' ')).toUpperCase();
        }
        
        // Implementar otros métodos de PreparedStatement...
    }
}
```

### Instrumentación de Mensajería

```java
@Component
public class MessageQueueTelemetry {
    
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter messagesSentCounter;
    private final LongCounter messagesReceivedCounter;
    private final DoubleHistogram messageProcessingTimeHistogram;
    
    public MessageQueueTelemetry(Tracer tracer, Meter meter) {
        this.tracer = tracer;
        this.meter = meter;
        
        this.messagesSentCounter = meter.counterBuilder("messages_sent_total")
            .setDescription("Total de mensajes enviados")
            .build();
            
        this.messagesReceivedCounter = meter.counterBuilder("messages_received_total")
            .setDescription("Total de mensajes recibidos")
            .build();
            
        this.messageProcessingTimeHistogram = meter.histogramBuilder("message_processing_time")
            .setDescription("Tiempo de procesamiento de mensajes en milisegundos")
            .setExplicitBucketBoundaries(
                10.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 2500.0, 5000.0)
            .build();
    }
    
    public void sendMessage(String queue, Message message) {
        Span span = tracer.spanBuilder("message.send")
            .setSpanKind(SpanKind.PRODUCER)
            .setAttribute("messaging.system", "rabbitmq")
            .setAttribute("messaging.destination", queue)
            .setAttribute("messaging.message_id", message.getId())
            .setAttribute("messaging.message_size", message.getSize())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Propagar contexto en headers del mensaje
            injectTraceContext(message);
            
            // Enviar mensaje
            rabbitTemplate.convertAndSend(queue, message);
            
            span.setAttribute("messaging.delivery_mode", message.getDeliveryMode());
            
            // Métricas
            messagesSentCounter.add(1,
                Attributes.of(
                    AttributeKey.stringKey("queue"), queue,
                    AttributeKey.stringKey("message_type"), message.getType()
                ));
                
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @RabbitListener(queues = "user-events")
    public void handleMessage(Message message, @Header Map<String, Object> headers) {
        long startTime = System.currentTimeMillis();
        
        Span span = tracer.spanBuilder("message.receive")
            .setSpanKind(SpanKind.CONSUMER)
            .setAttribute("messaging.system", "rabbitmq")
            .setAttribute("messaging.destination", "user-events")
            .setAttribute("messaging.message_id", message.getMessageProperties().getMessageId())
            .startSpan();
        
        // Extraer contexto del mensaje
        extractTraceContext(message, headers);
        
        try (Scope scope = span.makeCurrent()) {
            // Procesar mensaje
            processMessage(message);
            
            span.setAttribute("messaging.message_processed", true);
            
            // Métricas
            messagesReceivedCounter.add(1,
                Attributes.of(
                    AttributeKey.stringKey("queue"), "user-events",
                    AttributeKey.stringKey("message_type"), message.getMessageProperties().getType()
                ));
                
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            messageProcessingTimeHistogram.record(duration);
            span.end();
        }
    }
    
    private void injectTraceContext(Message message) {
        Context context = Context.current();
        TextMapPropagator propagator = OpenTelemetryPropagators.getGlobalPropagators()
            .getTextMapPropagator();
        
        Map<String, Object> headers = new HashMap<>();
        propagator.inject(context, headers, Map::put);
        
        message.getMessageProperties().getHeaders().putAll(headers);
    }
    
    private void extractTraceContext(Message message, Map<String, Object> headers) {
        TextMapPropagator propagator = OpenTelemetryPropagators.getGlobalPropagators()
            .getTextMapPropagator();
        
        Context context = propagator.extract(Context.current(), headers, Map::get);
        context.makeCurrent();
    }
}
```

### Configuración Avanzada con Backends

```java
@Configuration
public class OpenTelemetryBackendConfig {
    
    @Bean
    @ConditionalOnProperty(name = "otel.exporter.jaeger.enabled")
    public JaegerGrpcSpanExporter jaegerExporter(
            @Value("${otel.exporter.jaeger.endpoint:http://jaeger:14250}") String endpoint) {
        
        return JaegerGrpcSpanExporter.builder()
            .setEndpoint(endpoint)
            .setTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Bean
    @ConditionalOnProperty(name = "otel.exporter.zipkin.enabled")
    public ZipkinSpanExporter zipkinExporter(
            @Value("${otel.exporter.zipkin.endpoint:http://zipkin:9411/api/v2/spans}") String endpoint) {
        
        return ZipkinSpanExporter.builder()
            .setEndpoint(endpoint)
            .build();
    }
    
    @Bean
    @ConditionalOnProperty(name = "otel.exporter.otlp.enabled")
    public OtlpGrpcSpanExporter otlpExporter(
            @Value("${otel.exporter.otlp.endpoint:http://tempo:4317}") String endpoint) {
        
        return OtlpGrpcSpanExporter.builder()
            .setEndpoint(endpoint)
            .setTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    @Bean
    public Resource resource(@Value("${spring.application.name}") String serviceName) {
        return Resource.create(
            Attributes.of(
                AttributeKey.stringKey("service.name"), serviceName,
                AttributeKey.stringKey("service.version"), getServiceVersion(),
                AttributeKey.stringKey("deployment.environment"), getEnvironment(),
                AttributeKey.stringKey("host.name"), InetAddress.getLocalHost().getHostName()
            ));
    }
    
    @Bean
    public SdkTracerProvider sdkTracerProvider(
            Resource resource,
            List<SpanProcessor> spanProcessors) {
        
        return SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(
                SpanProcessor.composite(
                    BatchSpanProcessor.builder(LoggingSpanExporter.builder().build())
                        .setMaxExportBatchSize(512)
                        .setExportTimeout(Duration.ofSeconds(30))
                        .build(),
                    ResourceEnrichingSpanProcessor.create()
                )
            )
            .build();
    }
    
    @Bean
    public SdkMeterProvider sdkMeterProvider(
            Resource resource,
            List<MetricReader> metricReaders) {
        
        return SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(
                PeriodicMetricReader.builder(
                    InMemoryMetricExporter.builder().build()
                ).setExportInterval(Duration.ofSeconds(30)).build()
            )
            .build();
    }
}
```

### Testing con OpenTelemetry

```java
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailServiceWithTelemetryTest {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private InMemorySpanExporter spanExporter;
    
    @Autowired
    private InMemoryMetricReader metricReader;
    
    @Test
    void testEmailSending_GeneratesTelemetry() {
        // Given
        EmailRequest request = EmailRequest.builder()
            .recipient("test@example.com")
            .subject("Test Email")
            .template("welcome")
            .build();
        
        // When
        assertDoesNotThrow(() -> emailService.enviarEmail(request));
        
        // Then - Verificar traces
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(2); // Email span + Provider span
        
        // Verificar span principal
        SpanData emailSpan = spans.stream()
            .filter(span -> span.getName().equals("enviar-email"))
            .findFirst()
            .orElseThrow();
            
        assertThat(emailSpan.getAttributes().get(AttributeKey.stringKey("email.recipient")))
            .isEqualTo("test@example.com");
        assertThat(emailSpan.getAttributes().get(AttributeKey.stringKey("email.subject")))
            .isEqualTo("Test Email");
            
        // Verificar métricas
        List<MetricData> metrics = metricReader.collectAllMetrics();
        assertThat(metrics).isNotEmpty();
        
        // Verificar que hay métricas de emails enviados
        MetricData emailsMetric = metrics.stream()
            .filter(metric -> metric.getName().equals("emails_sent_total"))
            .findFirst()
            .orElseThrow();
            
        assertThat(emailsMetric.getDoubleSumData().getPoints()).hasSize(1);
    }
    
    @Test
    void testEmailFailure_RecordsError() {
        // Given
        EmailRequest request = EmailRequest.builder()
            .recipient("invalid-email")
            .subject("Test")
            .template("test")
            .build();
        
        // When & Then
        assertThatThrownBy(() -> emailService.enviarEmail(request))
            .isInstanceOf(EmailException.class);
        
        // Verificar que se registró el error en el trace
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        SpanData emailSpan = spans.stream()
            .filter(span -> span.getName().equals("enviar-email"))
            .findFirst()
            .orElseThrow();
            
        assertThat(emailSpan.getStatus().getCode()).isEqualTo(StatusCode.ERROR);
        assertThat(emailSpan.getEvents()).hasSize(1);
    }
}
```

## **Ventajas y Desventajas**

### Ventajas
- **Vendor-agnostic**: No dependiente de un proveedor específico
- **Estándar unificado**: Unifica traces, metrics y logs
- **Ecosistema robusto**: Soporte para múltiples lenguajes y frameworks
- **Instrumentación automática**: Auto-instrumentation para frameworks populares
- **Escalabilidad**: Diseñado para sistemas distribuidos a gran escala
- **Open source**: Gratuito y con comunidad activa
- **API estable**: Especificaciones maduras y estables
- **Performance**: Overhead bajo para aplicaciones

### Desventajas
- **Complejidad inicial**: Curva de aprendizaje para configurar correctamente
- **Configuración extensa**: Requiere configuración detallada para casos complejos
- **Overhead**: Costo de performance por instrumentación
- **Storage**: Requiere backend de almacenamiento para telemetría
- **Vendor lock-in**: Aunque es vendor-agnostic, los backends pueden crear dependencia
- **Debugging**: Puede ser complejo debuggear problemas de instrumentación
- **Version compatibility**: Cambios de versión pueden requerir actualizaciones

## **Buenas Prácticas**

### Instrumentación Efectiva
- **Mínima instrumentación**: Instrumentar solo operaciones críticas
- **Atributos relevantes**: Agregar atributos útiles para debugging
- **Span naming**: Usar nombres descriptivos y consistentes
- **Eventos importantes**: Agregar eventos en puntos clave de negocio

```java
// Buen ejemplo
Span span = tracer.spanBuilder("procesar-pedido")
    .setAttribute("pedido.id", pedido.getId())
    .setAttribute("usuario.id", pedido.getUsuario().getId())
    .setAttribute("items.count", pedido.getItems().size())
    .startSpan();

try (Scope scope = span.makeCurrent()) {
    span.addEvent("iniciando-procesamiento", 
        Attributes.of(AttributeKey.stringKey("processor"), "main-thread"));
    
    // Operaciones de negocio
    
    span.addEvent("pago-procesado",
        Attributes.of(AttributeKey.stringKey("payment.method"), pago.getMetodo()));
        
} finally {
    span.end();
}

// Mal ejemplo
Span span = tracer.spanBuilder("operation");
span.startSpan();
// Sin atributos, sin eventos, sin manejo de errores
span.end();
```

### Performance y Optimización
- **Sampling**: Usar sampling para reducir volumen de datos
- **Batch processing**: Procesar datos en lotes
- **Async export**: Exportación asíncrona de telemetría
- **Resource management**: Limpiar recursos apropiadamente

```java
@Configuration
public class OptimizedTelemetryConfig {
    
    @Bean
    public SpanProcessor optimizedSpanProcessor() {
        return BatchSpanProcessor.builder(jaegerExporter)
            .setMaxExportBatchSize(512)     // Procesar en lotes
            .setExportTimeout(Duration.ofSeconds(30))
            .setScheduleDelay(Duration.ofSeconds(5))
            .build();
    }
    
    @Bean
    public TraceIdRatioBasedSampler sampler() {
        return TraceIdRatioBasedSampler.create(0.1); // Sample 10% de traces
    }
}
```

### Seguridad y Privacidad
- **Data masking**: Enmascarar datos sensibles en atributos
- **PII handling**: Manejar información personal identificable apropiadamente
- **Secure export**: Usar TLS para exportación de datos
- **Access control**: Controlar acceso a datos de telemetría

```java
@Component
public class SecureTelemetryFilter {
    
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "token", "ssn", "credit_card", "email"
    );
    
    public Span sanitizeSpan(Span span, Map<String, Object> attributes) {
        Map<String, Object> sanitized = new HashMap<>();
        
        attributes.forEach((key, value) -> {
            if (SENSITIVE_FIELDS.stream().anyMatch(key::contains)) {
                sanitized.put(key, "***MASKED***");
            } else {
                sanitized.put(key, value);
            }
        });
        
        return span.setAttributes(
            AttributeKey.stringKeyMap("attributes"), 
            sanitizeized
        );
    }
}
```

### Configuración y Deployment
- **Environment-specific**: Configuraciones diferentes por ambiente
- **Graceful degradation**: Degradación elegante cuando observabilidad falla
- **Monitoring telemetry**: Monitorear el propio sistema de telemetría
- **Health checks**: Health checks para sistemas de observabilidad

```yaml
# application-prod.yml
otel:
  exporter:
    jaeger:
      enabled: true
      endpoint: ${JAEGER_ENDPOINT:http://jaeger:14250}
    prometheus:
      enabled: true
      endpoint: ${PROMETHEUS_ENDPOINT:http://prometheus:9090}
  sampling:
    probability: 0.1  # 10% sampling en producción
  resource:
    service:
      name: ${SPRING_APPLICATION_NAME:user-service}
      version: ${APP_VERSION:1.0.0}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
```

### Monitoreo y Alerting
- **SLI/SLO tracking**: Monitorear objetivos de nivel de servicio
- **Custom dashboards**: Crear dashboards específicos del negocio
- **Alerting rules**: Configurar alertas basadas en métricas de negocio
- **Error tracking**: Rastrear y alertar sobre errores críticos

```java
@Component
public class BusinessAlerting {
    
    private final Meter meter;
    private final AlertingService alertingService;
    
    @EventListener
    public void handleOrderEvent(OrderEvent event) {
        if (event.getType() == OrderEvent.Type.FAILED) {
            // Alert crítico para pedidos fallidos
            alertingService.sendAlert(
                AlertLevel.CRITICAL,
                "High order failure rate",
                Map.of("failure_rate", calculateFailureRate(),
                       "affected_orders", event.getOrderIds())
            );
        }
    }
    
    @Scheduled(fixedRate = 60000)
    public void checkBusinessMetrics() {
        double errorRate = calculateErrorRate();
        if (errorRate > 0.05) { // 5% threshold
            alertingService.sendAlert(
                AlertLevel.WARNING,
                "High error rate detected",
                Map.of("error_rate", errorRate,
                       "threshold", 0.05)
            );
        }
    }
}
```

---

## **Referencias Oficiales**

1. **OpenTelemetry Official Documentation**: https://opentelemetry.io/docs/
2. **OpenTelemetry Java API**: https://opentelemetry.io/docs/instrumentation/java/
3. **OpenTelemetry Specification**: https://github.com/open-telemetry/opentelemetry-specification
4. **OpenTelemetry Java Examples**: https://github.com/open-telemetry/opentelemetry-java-examples
5. **CNCF OpenTelemetry Project**: https://www.cncf.io/projects/opentelemetry/