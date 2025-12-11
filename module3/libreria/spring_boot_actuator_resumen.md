# Spring Boot Actuator

## **¿Qué es Spring Boot Actuator?**

Spring Boot Actuator es un framework que proporciona endpoints de monitoreo y gestión listos para producción para aplicaciones Spring Boot. Permite acceso a información crítica de la aplicación como métricas, health checks, información de la aplicación, y capacidades de gestión remota.

**Características principales**:
- **Health Checks**: Verificación del estado de la aplicación y sus dependencias
- **Métricas**: Métricas de JVM, aplicación y sistema operativo
- **Info Endpoints**: Información sobre la aplicación, build y git
- **Management Endpoints**: Gestión remota de la aplicación
- **Auditing**: Sistema de auditoría para eventos de seguridad
- **Integration**: Integración con herramientas de monitoreo externas

## **Conceptos Fundamentales**

### Endpoints
Los endpoints son URLs especiales que exponen información o permiten gestión de la aplicación. Spring Boot proporciona múltiples endpoints preconfigurados.

```java
// Endpoints principales disponibles por defecto
// GET /actuator/health - Estado de la aplicación
// GET /actuator/info - Información de la aplicación
// GET /actuator/metrics - Métricas de la aplicación
// GET /actuator/loggers - Estado de los loggers
// GET /actuator/env - Propiedades del entorno
// GET /actuator/mappings - Mapeos de request handlers
```

### Health Indicators
Los health indicators verifican el estado de componentes específicos de la aplicación y dependencias externas.

```java
// Health indicator personalizado
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1)) {
                return Health.up()
                        .withDetail("database", "Available")
                        .withDetail("connection", connection.getMetaData().getURL())
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Unavailable")
                    .withException(e)
                    .build();
        }
        return Health.down().build();
    }
}
```

### Metrics
Las métricas proporcionan información cuantitativa sobre el comportamiento de la aplicación.

```java
// Métricas personalizadas
@RestController
public class UserController {
    
    private final Counter userCreationCounter;
    private final Timer userCreationTimer;
    
    public UserController(MeterRegistry meterRegistry) {
        this.userCreationCounter = Counter.builder("user.creation.total")
                .description("Total number of user creations")
                .register(meterRegistry);
        
        this.userCreationTimer = Timer.builder("user.creation.duration")
                .description("Duration of user creation process")
                .register(meterRegistry);
    }
    
    @PostMapping("/users")
    public User createUser(@RequestBody CreateUserRequest request) {
        return userCreationTimer.record(() -> {
            User user = userService.createUser(request);
            userCreationCounter.increment();
            return user;
        });
    }
}
```

## **Configuración y Setup**

### Dependencias Maven
```xml
<!-- Spring Boot Actuator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Para métricas avanzadas con Micrometer -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Para health checks de base de datos -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Para health checks de Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Para health checks de RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### Configuración en Application.yml
```yaml
# Configuración básica de Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  
  endpoint:
    health:
      show-details: when-authorized  # when-authorized | always | never
      show-components: when-authorized
      
  info:
    env:
      enabled: true
    build:
      enabled: true
    git:
      mode: full
    
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      sla:
        http.server.requests: 50ms,100ms,200ms,300ms,500ms,1s,2s,5s

# Configuración de seguridad para endpoints
spring:
  security:
    user:
      name: admin
      password: admin123

# Configuración específica por entorno
management:
  security:
    enabled: false  # Deshabilitar en desarrollo
    
  endpoints:
    web:
      exposure:
        include: "*"  # Todos los endpoints en desarrollo
```

### Configuración de Seguridad
```java
@Configuration
@EnableConfigurationProperties(ManagementProperties.class)
public class ActuatorSecurityConfig {
    
    @Bean
    @Order(ManagementServerProperties.BASIC_AUTH_ORDER)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests(requests -> requests
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .formLogin(form -> {});
        
        return http.build();
    }
}

// Configuración más granular
@Configuration
public class CustomActuatorSecurityConfig {
    
    @Bean
    public SecurityFilterChain customActuatorSecurity(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.to("health", "info"))
            .authorizeRequests(requests -> requests
                .anyRequest().permitAll()  // Público para health e info
            )
            .requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests(requests -> requests
                .anyRequest().hasRole("ACTUATOR")  // Requiere rol para otros endpoints
            )
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
}
```

## **Casos de Uso Principales**

### 1. Health Checks y Monitoring
```java
// Health indicator para base de datos
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public Health health() {
        try {
            // Verificar conexión
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(1)) {
                    return Health.down()
                            .withDetail("connection", "Invalid connection")
                            .build();
                }
            }
            
            // Verificar que我们可以执行查询
            String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
            
            return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("version", version)
                    .withDetail("connection_pool", getConnectionPoolInfo())
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
    
    private Map<String, Object> getConnectionPoolInfo() {
        // Información del pool de conexiones (HikariCP)
        return Map.of(
            "active", getHikariPoolMXBean().getActiveConnections(),
            "idle", getHikariPoolMXBean().getIdleConnections(),
            "total", getHikariPoolMXBean().getTotalConnections(),
            "max", getHikariPoolMXBean().getMaximumPoolSize()
        );
    }
}

// Health indicator para servicios externos
@Component
public class ExternalApiHealthIndicator implements HealthIndicator {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Override
    public Health health() {
        try {
            ResponseEntity<ApiStatus> response = restTemplate.getForEntity(
                "https://api.external-service.com/status", 
                ApiStatus.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && 
                "UP".equals(response.getBody().getStatus())) {
                
                return Health.up()
                        .withDetail("external_api", "Available")
                        .withDetail("response_time", response.getHeaders().getFirst("X-Response-Time"))
                        .build();
            }
            
            return Health.down()
                    .withDetail("external_api", "Degraded")
                    .withDetail("status", response.getStatusCode().getReasonPhrase())
                    .build();
                    
        } catch (Exception e) {
            return Health.down()
                    .withDetail("external_api", "Unavailable")
                    .withException(e)
                    .build();
        }
    }
}
```

### 2. Métricas de Negocio
```java
// Servicio con métricas de negocio
@Service
public class OrderService {
    
    private final Counter orderCreationCounter;
    private final Timer orderProcessingTimer;
    private final DistributionSummary orderAmountSummary;
    private final Gauge activeOrdersGauge;
    
    public OrderService(MeterRegistry meterRegistry, OrderRepository orderRepository) {
        this.orderCreationCounter = Counter.builder("order.creation.total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.orderProcessingTimer = Timer.builder("order.processing.duration")
                .description("Time taken to process orders")
                .register(meterRegistry);
        
        this.orderAmountSummary = DistributionSummary.builder("order.amount")
                .description("Order amounts distribution")
                .register(meterRegistry);
        
        this.activeOrdersGauge = Gauge.builder("order.active.count")
                .description("Number of active orders")
                .register(meterRegistry, orderRepository, OrderRepository::countActiveOrders);
    }
    
    public Order createOrder(CreateOrderRequest request) {
        return orderProcessingTimer.record(() -> {
            Order order = Order.builder()
                    .customerId(request.getCustomerId())
                    .items(request.getItems())
                    .totalAmount(request.getTotalAmount())
                    .status(OrderStatus.PENDING)
                    .build();
            
            Order savedOrder = orderRepository.save(order);
            
            // Registrar métricas
            orderCreationCounter.increment();
            orderAmountSummary.record(request.getTotalAmount().doubleValue());
            
            return savedOrder;
        });
    }
}

// Métricas de performance específicas
@RestController
public class ApiMetricsController {
    
    private final Counter requestCounter;
    private final Timer requestTimer;
    
    public ApiMetricsController(MeterRegistry meterRegistry) {
        this.requestCounter = Counter.builder("api.requests.total")
                .description("Total API requests")
                .tag("service", "api-gateway")
                .register(meterRegistry);
        
        this.requestTimer = Timer.builder("api.request.duration")
                .description("API request duration")
                .register(meterRegistry);
    }
    
    @GetMapping("/api/users")
    public List<User> getUsers() {
        return requestTimer.record(() -> {
            requestCounter.increment();
            return userService.getAllUsers();
        });
    }
}
```

### 3. Info Endpoint Personalizado
```java
// Contributor personalizado para info endpoint
@Component
public class CustomInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("application", Map.of(
                "name", "User Management Service",
                "version", getApplicationVersion(),
                "environment", getActiveProfile(),
                "startupTime", System.currentTimeMillis()
            ))
            .withDetail("database", Map.of(
                "type", "PostgreSQL",
                "version", getDatabaseVersion(),
                "poolSize", getConnectionPoolSize()
            ))
            .withDetail("cache", Map.of(
                "provider", "Redis",
                "nodes", getRedisNodes(),
                "memoryUsage", getRedisMemoryUsage()
            ));
    }
    
    private String getApplicationVersion() {
        try {
            Package pkg = getClass().getPackage();
            return pkg.getImplementationVersion() != null ? 
                pkg.getImplementationVersion() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}

// Info endpoint para información de build
@Configuration
@AutoConfigurationPackage
public class BuildInfoAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnEnabledInfoContributor("build")
    public BuildProperties buildProperties() {
        return new BuildProperties();
    }
}

// Configuración para incluir información de Git
management:
  info:
    git:
      mode: full
      properties:
        git:
          branch: "@git.branch@"
          commit:
            id: "@git.commit.id.abbrev@"
            message:
              short: "@git.commit.message.short@"
```

### 4. Loggers Management
```java
// Controller para gestión de loggers
@RestController
@RequestMapping("/actuator/loggers")
public class LoggersController {
    
    @Autowired
    private LoggerContext loggerContext;
    
    @GetMapping
    public Map<String, Object> getLoggers() {
        Map<String, Object> loggers = new HashMap<>();
        
        for (Logger logger : loggerContext.getLoggerList()) {
            if (logger.getName().startsWith("com.example")) {
                loggers.put(logger.getName(), Map.of(
                    "level", logger.getEffectiveLevel().toString(),
                    "configuredLevel", logger.getLevel() != null ? logger.getLevel().toString() : null
                ));
            }
        }
        
        return loggers;
    }
    
    @PostMapping("/{loggerName}")
    public Map<String, String> setLoggerLevel(@PathVariable String loggerName, 
                                             @RequestBody Map<String, String> request) {
        String level = request.get("level");
        Level newLevel = Level.valueOf(level.toUpperCase());
        
        Logger logger = loggerContext.getLogger(loggerName);
        logger.setLevel(newLevel);
        
        return Map.of(
            "logger", loggerName,
            "oldLevel", logger.getEffectiveLevel().toString(),
            "newLevel", newLevel.toString()
        );
    }
}
```

## **Ejemplos de Código Completo**

### 1. Configuración Completa de Actuator
```java
// Configuración principal de Actuator
@Configuration
@EnableConfigurationProperties({ManagementProperties.class})
public class ActuatorConfig {
    
    @Bean
    @ConditionalOnMissingBean
    public HealthEndpointGroups healthEndpointGroups(
            ObjectProvider<HealthEndpointGroup> groups) {
        HealthEndpointGroups healthGroups = new HealthEndpointGroups();
        
        // Grupo de health checks para uso interno
        HealthEndpointGroup internalGroup = HealthEndpointGroup.builder()
                .include("dbHealth", "redisHealth", "externalApiHealth")
                .build();
        healthGroups.add("internal", internalGroup);
        
        // Grupo para sistemas de monitoreo externos
        HealthEndpointGroup monitoringGroup = HealthEndpointGroup.builder()
                .includeAll()
                .exclude("sensitiveData")
                .build();
        healthGroups.add("monitoring", monitoringGroup);
        
        return healthGroups;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MetricsEndpoint metricsEndpoint(MeterRegistry meterRegistry) {
        return new MetricsEndpoint(meterRegistry);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public InfoEndpoint infoEndpoint(ObjectProvider<InfoContributor> contributors) {
        return new InfoEndpoint(contributors.orderedStream().collect(Collectors.toList()));
    }
}

// Health indicators personalizados
@Component
public class ComprehensiveHealthChecker {
    
    @Autowired
    private List<HealthIndicator> healthIndicators;
    
    @EventListener
    public void handleHealthCheckEvent(HealthCheckEvent event) {
        Map<String, Health> healthStatuses = new HashMap<>();
        
        for (HealthIndicator indicator : healthIndicators) {
            String indicatorName = indicator.getClass().getSimpleName()
                    .replace("HealthIndicator", "");
            Health health = indicator.health();
            healthStatuses.put(indicatorName, health);
            
            // Log health status changes
            if (health.getStatus().equals(Status.DOWN)) {
                log.error("Health check failed for {}: {}", 
                    indicatorName, health.getDetails());
            }
        }
        
        // Publish health status event
        eventPublisher.publishEvent(new HealthStatusChangedEvent(
            event.getApplicationName(), 
            healthStatuses
        ));
    }
}
```

### 2. Métricas Avanzadas con Micrometer
```java
// Configuración de métricas personalizadas
@Configuration
public class CustomMetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> {
            registry.config()
                .commonTags("application", "user-service")
                .commonTags("environment", getEnvironment())
                .commonTags("version", getVersion());
        };
    }
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> distributionSLA() {
        return registry -> {
            registry.config().meterFilter(
                MeterFilter.nameMatcher("http.server.requests")
                    .transform(builder -> builder
                        .tag("outcome", TagsUtil.outcomeTag(builder)))
            );
        };
    }
    
    private String getEnvironment() {
        return System.getenv("SPRING_PROFILES_ACTIVE") != null ? 
            System.getenv("SPRING_PROFILES_ACTIVE") : "development";
    }
    
    private String getVersion() {
        Package pkg = getClass().getPackage();
        return pkg.getImplementationVersion() != null ? 
            pkg.getImplementationVersion() : "unknown";
    }
}

// Métricas de negocio específicas
@Service
public class BusinessMetricsService {
    
    private final Timer businessTransactionTimer;
    private final Counter businessTransactionCounter;
    private final DistributionSummary transactionAmountSummary;
    private final Gauge activeUsersGauge;
    private final Counter errorCounter;
    
    public BusinessMetricsService(MeterRegistry meterRegistry, 
                                UserRepository userRepository,
                                TransactionService transactionService) {
        
        this.businessTransactionTimer = Timer.builder("business.transaction.duration")
                .description("Business transaction processing time")
                .register(meterRegistry);
        
        this.businessTransactionCounter = Counter.builder("business.transaction.total")
                .description("Total business transactions")
                .register(meterRegistry);
        
        this.transactionAmountSummary = DistributionSummary.builder("business.transaction.amount")
                .description("Transaction amounts")
                .register(meterRegistry);
        
        this.activeUsersGauge = Gauge.builder("business.active.users")
                .description("Number of active users")
                .register(meterRegistry, userRepository, 
                    repo -> repo.countActiveUsersInLast24Hours());
        
        this.errorCounter = Counter.builder("business.errors.total")
                .description("Total business errors")
                .register(meterRegistry);
    }
    
    public <T> T executeBusinessTransaction(Supplier<T> transaction, String transactionType) {
        return businessTransactionTimer.record(() -> {
            try {
                T result = transaction.get();
                businessTransactionCounter.increment(Tags.of("type", transactionType, "status", "success"));
                return result;
            } catch (Exception e) {
                errorCounter.increment(Tags.of("type", transactionType, "error", e.getClass().getSimpleName()));
                throw e;
            }
        });
    }
    
    public void recordTransactionAmount(BigDecimal amount) {
        transactionAmountSummary.record(amount.doubleValue());
    }
}
```

### 3. Endpoint de Gestión Personalizado
```java
// Endpoint personalizado de gestión
@RestController
@Endpoint(id = "custom-management")
public class CustomManagementEndpoint {
    
    @ReadOperation
    public Map<String, Object> getApplicationStatus() {
        return Map.of(
            "applicationName", getApplicationName(),
            "uptime", getUptime(),
            "memoryUsage", getMemoryUsage(),
            "activeConnections", getActiveConnections(),
            "cacheStats", getCacheStatistics(),
            "queueStats", getQueueStatistics()
        );
    }
    
    @WriteOperation
    public Map<String, String> clearCache(@Selector String cacheName) {
        CacheManager cacheManager = getCacheManager();
        cacheManager.getCache(cacheName).clear();
        
        return Map.of(
            "action", "cache_cleared",
            "cacheName", cacheName,
            "timestamp", Instant.now().toString()
        );
    }
    
    @WriteOperation
    public Map<String, String> reloadConfiguration() {
        // Recargar configuración de la aplicación
        ConfigurableApplicationContext context = getApplicationContext();
        context.refresh();
        
        return Map.of(
            "action", "configuration_reloaded",
            "timestamp", Instant.now().toString()
        );
    }
    
    @WriteOperation
    public Map<String, Object> performHealthCheck() {
        List<HealthCheckResult> results = new ArrayList<>();
        
        // Health checks específicos de negocio
        results.add(checkDatabaseHealth());
        results.add(checkExternalServiceHealth());
        results.add(checkCacheHealth());
        
        boolean allHealthy = results.stream()
                .allMatch(HealthCheckResult::isHealthy);
        
        return Map.of(
            "overallStatus", allHealthy ? "UP" : "DOWN",
            "checks", results,
            "timestamp", Instant.now().toString()
        );
    }
    
    private HealthCheckResult checkDatabaseHealth() {
        try {
            // Verificar base de datos
            return new HealthCheckResult("database", true, "Connection healthy");
        } catch (Exception e) {
            return new HealthCheckResult("database", false, e.getMessage());
        }
    }
}

// Extensión de Actuator para funcionalidades adicionales
@ManagementContextConfiguration
public class ExtendedActuatorConfig {
    
    @Bean
    @ConditionalOnAvailableEndpoint
    public ThreadDumpEndpoint threadDumpEndpoint() {
        return new ThreadDumpEndpoint();
    }
    
    @Bean
    @ConditionalOnAvailableEndpoint
    public HeapDumpEndpoint heapDumpEndpoint() {
        return new HeapDumpEndpoint();
    }
    
    @Bean
    @ConditionalOnAvailableEndpoint
    public HealthEndpoint healthEndpoint(List<HealthIndicator> healthIndicators) {
        return new HealthEndpoint(new HealthEndpointProperties(), healthIndicators);
    }
}
```

### 4. Integración con Sistemas de Monitoreo
```java
// Configuración para Prometheus
@Configuration
public class PrometheusConfig {
    
    @Bean
    public MeterRegistryCustomizer<PrometheusMeterRegistry> prometheusConfig() {
        return registry -> {
            registry.config()
                .enableTimeSeriesSampling();
        };
    }
    
    @Bean
    public HistogramTimerConfig histogramTimerConfig() {
        return HistogramTimerConfig.builder()
            .serviceLevelObjectives(Duration.ofMillis(50), 
                                  Duration.ofMillis(100), 
                                  Duration.ofMillis(200))
            .percentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram(true)
            .build();
    }
}

// Configuración para Datadog
@Configuration
@ConditionalOnClass(DatadogMeterRegistry.class)
public class DatadogConfig {
    
    @Bean
    @ConditionalOnProperty(name = "management.metrics.export.datadog.enabled", 
                         havingValue = "true")
    public DatadogMeterRegistry datadogMeterRegistry(DatadogConfig.Builder builder) {
        return DatadogMeterRegistry.builder(builder
                .apiKey(System.getenv("DATADOG_API_KEY"))
                .applicationId(System.getenv("DATADOG_APPLICATION_ID"))
                .build())
            .build();
    }
}

// Exportador de métricas personalizado
@Component
public class CustomMetricsExporter {
    
    private final MeterRegistry meterRegistry;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public CustomMetricsExporter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Exportar métricas cada minuto
        scheduler.scheduleWithFixedDelay(this::exportMetrics, 0, 1, TimeUnit.MINUTES);
    }
    
    private void exportMetrics() {
        try {
            // Recopilar métricas de negocio
            Map<String, Object> businessMetrics = collectBusinessMetrics();
            
            // Enviar a sistema de monitoreo externo
            sendToExternalMonitoring(businessMetrics);
            
        } catch (Exception e) {
            log.error("Failed to export metrics", e);
        }
    }
    
    private Map<String, Object> collectBusinessMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Métricas de usuarios activos
        meterRegistry.getMeters().stream()
            .filter(meter -> meter.getId().getName().equals("business.active.users"))
            .forEach(meter -> {
                if (meter instanceof Gauge) {
                    Number value = ((Gauge) meter).value();
                    metrics.put("active_users", value);
                }
            });
        
        // Métricas de transacciones
        meterRegistry.getMeters().stream()
            .filter(meter -> meter.getId().getName().equals("business.transaction.total"))
            .forEach(meter -> {
                if (meter instanceof Counter) {
                    Counter counter = (Counter) meter;
                    metrics.put("total_transactions", counter.count());
                }
            });
        
        return metrics;
    }
}
```

## **Ventajas y Desventajas**

### Ventajas

1. **Monitoreo en Tiempo Real**
   - Acceso inmediato al estado de la aplicación
   - Métricas detalladas de rendimiento y negocio
   - Health checks automáticos de dependencias

2. **Gestión Remota**
   - Capacidad de gestionar aplicaciones sin acceso directo
   - Modificación de log levels en tiempo real
   - Recarga de configuraciones sin restart

3. **Integración Empresarial**
   - Compatibilidad con herramientas de monitoreo estándar
   - Soporte para Prometheus, Grafana, Datadog
   - Exportación automática de métricas

4. **Detección Temprana de Problemas**
   - Health checks proactivos
   - Alertas automáticas basadas en métricas
   - Identificación rápida de cuellos de botella

5. **Configuración Flexible**
   - Granular control sobre qué endpoints exponer
   - Configuración de seguridad personalizable
   - Métricas personalizables por negocio

### Desventajas

1. **Superficie de Ataque**
   - Expone información sensible si no se configura correctamente
   - Requiere configuración de seguridad adicional
   - Potencial exposición de datos internos

2. **Complejidad de Configuración**
   - Configuración puede volverse compleja en entornos grandes
   - Múltiples opciones pueden confundir a desarrolladores
   - Requiere conocimiento de métricas y monitoring

3. **Overhead de Performance**
   - Métricas pueden impactar ligeramente el rendimiento
   - Recolección continua de datos consume recursos
   - Exportación de métricas puede generar tráfico adicional

4. **Gestión de Seguridad**
   - Requiere configuración de autenticación y autorización
   - Diferentes requisitos de seguridad por entorno
   - Posible exposición de información sensible

5. **Curva de Aprendizaje**
   - Requiere entendimiento de métricas y monitoring
   - Conceptos avanzados como distribución y percentiles
   - Integración con sistemas externos puede ser compleja

## **Buenas Prácticas**

### 1. Configuración de Seguridad
```java
// ✅ BUENA PRÁCTICA: Configuración de seguridad granular
@Configuration
public class ActuatorSecurityConfig {
    
    @Bean
    @Order(ManagementServerProperties.BASIC_AUTH_ORDER)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests(requests -> requests
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                .requestMatchers(EndpointRequest.to("metrics")).hasRole("MONITORING")
                .requestMatchers(EndpointRequest.to("loggers", "env", "configprops")).hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());
        
        return http.build();
    }
}

// ✅ Configuración por entorno
@Profile("development")
@Configuration
public class DevActuatorSecurityConfig {
    
    @Bean
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}

// ❌ MALA PRÁCTICA: Sin seguridad
# management:
#   security:
#     enabled: false  # ❌ Nunca hacer esto en producción
```

### 2. Health Checks Efectivos
```java
// ✅ BUENA PRÁCTICA: Health checks informativos
@Component
public class DetailedHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        Health.Builder healthBuilder = Health.up();
        
        try {
            // Verificar base de datos
            DatabaseHealth dbHealth = checkDatabase();
            healthBuilder.withDetail("database", Map.of(
                "status", dbHealth.getStatus(),
                "response_time", dbHealth.getResponseTime() + "ms",
                "active_connections", dbHealth.getActiveConnections()
            ));
            
            // Verificar servicios externos
            ExternalServiceHealth serviceHealth = checkExternalServices();
            healthBuilder.withDetail("external_services", Map.of(
                "payment_gateway", serviceHealth.isPaymentGatewayUp(),
                "email_service", serviceHealth.isEmailServiceUp()
            ));
            
            // Verificar cache
            CacheHealth cacheHealth = checkCache();
            healthBuilder.withDetail("cache", Map.of(
                "status", cacheHealth.getStatus(),
                "hit_rate", cacheHealth.getHitRate() + "%",
                "memory_usage", cacheHealth.getMemoryUsage() + "MB"
            ));
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }
}

// ❌ MALA PRÁCTICA: Health checks básicos
@Component
public class BasicHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        return Health.up().build(); // ❌ No proporciona información útil
    }
}
```

### 3. Métricas Significativas
```java
// ✅ BUENA PRÁCTICA: Métricas con contexto
@Service
public class OrderServiceWithMetrics {
    
    private final Timer orderProcessingTimer;
    private final Counter orderSuccessCounter;
    private final Counter orderFailureCounter;
    private final DistributionSummary orderValueDistribution;
    
    public OrderServiceWithMetrics(MeterRegistry meterRegistry) {
        this.orderProcessingTimer = Timer.builder("order.processing.time")
                .description("Time taken to process orders")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.orderSuccessCounter = Counter.builder("order.success.total")
                .description("Total successful orders")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.orderFailureCounter = Counter.builder("order.failure.total")
                .description("Total failed orders")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        this.orderValueDistribution = DistributionSummary.builder("order.value")
                .description("Order value distribution")
                .tag("service", "order-service")
                .register(meterRegistry);
    }
    
    public Order processOrder(OrderRequest request) {
        return orderProcessingTimer.record(() -> {
            try {
                Order order = doProcessOrder(request);
                orderSuccessCounter.increment();
                orderValueDistribution.record(order.getTotal().doubleValue());
                return order;
            } catch (Exception e) {
                orderFailureCounter.increment(Tags.of("error_type", e.getClass().getSimpleName()));
                throw e;
            }
        });
    }
}

// ❌ MALA PRÁCTICA: Métricas sin contexto
// Métricas genéricas sin información de negocio
Counter genericCounter = Counter.builder("requests.total").register(meterRegistry);
```

### 4. Configuración por Entorno
```yaml
# ✅ BUENA PRÁCTICA: Configuración específica por entorno
---
spring:
  config:
    activate:
      on-profile: development
      
management:
  endpoints:
    web:
      exposure:
        include: "*"  # Todos los endpoints en desarrollo
  endpoint:
    health:
      show-details: always
  security:
    enabled: false

---
spring:
  config:
    activate:
      on-profile: staging
      
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  security:
    enabled: true

---
spring:
  config:
    activate:
      on-profile: production
      
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: never
  security:
    enabled: true
    # Configuración adicional de seguridad
```

### 5. Monitoring y Alertas
```java
// ✅ BUENA PRÁCTICA: Sistema de alertas integrado
@Component
public class HealthAlertingService {
    
    @EventListener
    public void handleHealthStatusChange(HealthStatusChangedEvent event) {
        Map<String, Health> statuses = event.getHealthStatuses();
        
        // Verificar si algún componente crítico está DOWN
        Map<String, Health> criticalComponents = statuses.entrySet().stream()
            .filter(entry -> isCriticalComponent(entry.getKey()))
            .filter(entry -> entry.getValue().getStatus().equals(Status.DOWN))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (!criticalComponents.isEmpty()) {
            sendAlert("CRITICAL_COMPONENT_DOWN", criticalComponents);
        }
        
        // Verificar degradación de performance
        statuses.entrySet().stream()
            .filter(entry -> isPerformanceDegraded(entry.getValue()))
            .forEach(entry -> {
                sendAlert("PERFORMANCE_DEGRADED", Map.of(entry.getKey(), entry.getValue()));
            });
    }
    
    private boolean isCriticalComponent(String componentName) {
        return List.of("database", "cache", "payment_gateway").contains(componentName);
    }
    
    private boolean isPerformanceDegraded(Health health) {
        Map<String, Object> details = health.getDetails();
        return details.containsKey("response_time") && 
               Integer.parseInt(details.get("response_time").toString()) > 5000;
    }
    
    private void sendAlert(String alertType, Map<String, ?> data) {
        // Enviar alerta a sistema de monitoreo
        Alert alert = Alert.builder()
            .type(alertType)
            .severity(Alert.Severity.CRITICAL)
            .data(data)
            .timestamp(Instant.now())
            .build();
            
        alertService.sendAlert(alert);
    }
}
```

### 6. Testing de Actuator Endpoints
```java
// ✅ BUENA PRÁCTICA: Testing integral de endpoints
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActuatorIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private TestDatabase database;
    
    @Test
    @WithMockUser(roles = "ACTUATOR")
    void shouldExposeHealthEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Health health = parseHealthResponse(response.getBody());
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("database");
    }
    
    @Test
    @WithMockUser(roles = "ACTUATOR")
    void shouldExposeMetricsEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/metrics", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        MetricsResponse metrics = parseMetricsResponse(response.getBody());
        assertThat(metrics.getNames()).contains("http.server.requests", "jvm.memory.used");
    }
    
    @Test
    void healthEndpointShouldBePublic() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // No debe requerir autenticación
    }
}

// ✅ Testing específico de health indicators
@SpringBootTest
class HealthIndicatorTest {
    
    @Autowired
    private HealthIndicatorRegistry healthIndicatorRegistry;
    
    @Test
    void shouldRegisterAllHealthIndicators() {
        List<String> indicatorNames = healthIndicatorRegistry.getAll().stream()
            .map(HealthIndicatorRegistration::getName)
            .collect(Collectors.toList());
        
        assertThat(indicatorNames).containsExactlyInAnyOrder(
            "dbHealth", "cacheHealth", "externalApiHealth"
        );
    }
}
```

## **Referencias Oficiales**

1. **Spring Boot Actuator Documentation**
   - https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
   - Documentación oficial completa de Spring Boot Actuator

2. **Spring Boot Actuator API Reference**
   - https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/actuate/package-summary.html
   - Referencia API completa de Actuator

3. **Micrometer Documentation**
   - https://micrometer.io/docs
   - Documentación de Micrometer para métricas

4. **Spring Boot Production Ready Features**
   - https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready.html
   - Guía de características de producción de Spring Boot

5. **Spring Boot Actuator GitHub Repository**
   - https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-actuator
   - Código fuente y ejemplos oficiales