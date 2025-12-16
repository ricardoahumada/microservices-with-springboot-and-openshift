# Patrones de Resiliencia

Los patrones de resiliencia son estrategias de diseño que permiten a las aplicaciones mantener su funcionamiento ante fallos, degradación de servicios o condiciones adversas. Estos patrones son fundamentales para construir sistemas distribuidos robustos y tolerantes a fallos.

## **Timeout (Tiempo Límite)**

### ¿Qué es?
El patrón Timeout establece un tiempo máximo de espera para operaciones, evitando que las aplicaciones esperen indefinidamente por respuestas de servicios externos.

### Conceptos Clave
- **Timeout configurado**: Tiempo máximo de espera
- **Timeout de conexión**: Tiempo para establecer conexión
- **Timeout de lectura**: Tiempo para recibir respuesta
- **Timeout configurable**: Ajustable según criticidad

### Ejemplo en Java
```java
@Configuration
public class TimeoutConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = 
            new HttpComponentsClientHttpRequestFactory();
        
        // Timeout de conexión: 3 segundos
        factory.setConnectTimeout(3000);
        // Timeout de lectura: 30 segundos
        factory.setReadTimeout(30000);
        
        return new RestTemplate(factory);
    }
}

// Uso en servicio
@Service
public class UsuarioService {
    
    private final RestTemplate restTemplate;
    
    public UsuarioService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public Usuario obtenerUsuario(Long id) {
        try {
            return restTemplate.getForObject(
                "http://external-service/api/usuarios/" + id, 
                Usuario.class
            );
        } catch (RestClientException e) {
            // Manejar timeout
            throw new ServicioNoDisponibleException("Timeout al obtener usuario");
        }
    }
}
```

## **Retry (Reintento)**

### ¿Qué es?
El patrón Retry ejecuta automáticamente una operación fallida múltiples veces antes de considerarla definitivamente fallida, basado en la suposición de que muchos fallos son temporales.

### Conceptos Clave
- **Reintentos configurables**: Número máximo de intentos
- **Backoff exponencial**: Incremento del tiempo entre reintentos
- **Condiciones de reintento**: Cuándo reintentar (5xx, timeouts)
- **Límite de reintentos**: Prevenir bucles infinitos

### Ejemplo en Java
```java
@Component
public class RetryableUsuarioService {
    
    @Retryable(
        value = {ServicioNoDisponibleException.class, TimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public Usuario obtenerUsuarioConReintento(Long id) {
        return restTemplate.getForObject(
            "http://external-service/api/usuarios/" + id, 
            Usuario.class
        );
    }
    
    @Recover
    public Usuario recuperarUsuario(Throwable t, Long id) {
        // Plan de recuperación cuando todos los reintentos fallan
        logger.warn("No se pudo obtener usuario después de reintentos: {}", id);
        return obtenerUsuarioCacheLocal(id);
    }
}

// Configuración personalizada
@Configuration
public class RetryConfig {
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Política de retry: máximo 3 intentos
        retryTemplate.setRetryPolicy(
            new SimpleRetryPolicy(3, 
                Map.of(
                    ServicioNoDisponibleException.class, true,
                    TimeoutException.class, true
                )
            )
        );
        
        // Backoff exponencial
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }
}
```

## **Circuit Breaker (Disyuntor)**

### ¿Qué es?
El patrón Circuit Breaker actúa como un interruptor automático que corta las llamadas a un servicio cuando detecta que está fallando repetidamente, permitiendo que se recupere sin ser sobrecargado.

### Estados del Circuit Breaker
- **CLOSED**: Funcionamiento normal, todas las llamadas pasan
- **OPEN**: Servicio considerado fallido, llamadas rechazadas inmediatamente
- **HALF-OPEN**: Permite algunas llamadas de prueba para verificar recuperación

### Ejemplo en Java
```java
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreaker circuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% de fallos para abrir circuito
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Esperar 30s en OPEN
            .slidingWindowSize(10) // Ventana de 10 llamadas
            .minimumNumberOfCalls(5) // Mínimo 5 llamadas para evaluar
            .build();
        
        return CircuitBreakerRegistry.of(config).circuitBreaker("usuario-service");
    }
}

@Service
public class UsuarioConCircuitBreakerService {
    
    private final CircuitBreaker circuitBreaker;
    private final RestTemplate restTemplate;
    
    public UsuarioConCircuitBreakerService(CircuitBreaker circuitBreaker, 
                                          RestTemplate restTemplate) {
        this.circuitBreaker = circuitBreaker;
        this.restTemplate = restTemplate;
    }
    
    public Usuario obtenerUsuario(Long id) {
        return circuitBreaker.executeSupplier(() -> {
            return restTemplate.getForObject(
                "http://external-service/api/usuarios/" + id, 
                Usuario.class
            );
        });
    }
}

// Con Resilience4j
@Service
@CircuitBreaker(name = "usuario-service", fallbackMethod = "obtenerUsuarioFallback")
public class UsuarioResilienceService {
    
    public Usuario obtenerUsuario(Long id) {
        return restTemplate.getForObject(
            "http://external-service/api/usuarios/" + id, 
            Usuario.class
        );
    }
    
    public Usuario obtenerUsuarioFallback(Long id, Exception ex) {
        logger.warn("Circuit breaker activado para usuario: {}", id);
        return obtenerUsuarioCacheLocal(id);
    }
}
```

## **Bulkhead (Compartimentos Estancos)**

### ¿Qué es?
El patrón Bulkhead aísla recursos críticos en compartimentos separados, de manera que si uno falla, los otros continúan funcionando sin verse afectados.

### Tipos de Bulkhead
- **Thread Pool Isolation**: Pools de hilos separados
- **Connection Pool Isolation**: Pools de conexión separados
- **Memory Isolation**: Separación de memoria
- **Resource Isolation**: Aislamiento de recursos específicos

### Ejemplo en Java
```java
@Configuration
public class BulkheadConfig {
    
    @Bean
    public ThreadPoolBulkhead bulkhead() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .coreThreadPoolSize(10) // Hilos principales
            .maxThreadPoolSize(50)  // Hilos máximos
            .queueCapacity(100)     // Capacidad de cola
            .keepAliveTime(Duration.ofSeconds(30))
            .build();
        
        return ThreadPoolBulkheadRegistry.of(config).bulkhead("usuario-bulkhead");
    }
    
    @Bean
    public SemaphoreBulkhead semaphoreBulkhead() {
        SemaphoreBulkheadConfig config = SemaphoreBulkheadConfig.custom()
            .maxConcurrentCalls(5) // Máximo 5 llamadas concurrentes
            .build();
        
        return SemaphoreBulkheadRegistry.of(config).bulkhead("usuario-semaphore");
    }
}

@Service
public class UsuarioBulkheadService {
    
    private final ThreadPoolBulkhead threadPoolBulkhead;
    private final RestTemplate restTemplate;
    
    public UsuarioBulkheadService(ThreadPoolBulkhead threadPoolBulkhead,
                                 RestTemplate restTemplate) {
        this.threadPoolBulkhead = threadPoolBulkhead;
        this.restTemplate = restTemplate;
    }
    
    @Bulkhead(name = "usuario-bulkhead")
    public CompletableFuture<Usuario> obtenerUsuarioAsync(Long id) {
        return threadPoolBulkhead.executeSupplier(() -> {
            return restTemplate.getForObject(
                "http://external-service/api/usuarios/" + id, 
                Usuario.class
            );
        });
    }
}

// Pools de conexión separados
@Configuration
public class ConnectionPoolsConfig {
    
    @Bean
    @Qualifier("critical-pool")
    public DataSource criticalDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://critical-db:3306/app");
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(10);
        return new HikariDataSource(config);
    }
    
    @Bean
    @Qualifier("analytics-pool")
    public DataSource analyticsDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://analytics-db:3306/app");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        return new HikariDataSource(config);
    }
}
```

## **Fallback (Plan B)**

### ¿Qué es?
El patrón Fallback proporciona una respuesta alternativa cuando una operación principal falla, asegurando que la aplicación mantenga funcionalidad básica.

### Tipos de Fallback
- **Fallback estático**: Valor predeterminado
- **Fallback dinámico**: Valor calculado
- **Fallback de cache**: Valor desde cache
- **Fallback de servicio alternativo**: Servicio secundario

### Ejemplo en Java
```java
@Service
public class UsuarioFallbackService {
    
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Usuario> redisTemplate;
    
    public UsuarioFallbackService(RestTemplate restTemplate,
                                 RedisTemplate<String, Usuario> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }
    
    @Fallback(fallbackMethod = "obtenerUsuarioCache")
    public Usuario obtenerUsuario(Long id) {
        return restTemplate.getForObject(
            "http://external-service/api/usuarios/" + id, 
            Usuario.class
        );
    }
    
    public Usuario obtenerUsuarioCache(Long id) {
        // Intentar desde cache
        Usuario usuarioCache = redisTemplate.opsForValue().get("usuario:" + id);
        if (usuarioCache != null) {
            return usuarioCache;
        }
        
        // Fallback estático para casos críticos
        return Usuario.builder()
            .id(id)
            .nombre("Usuario Temporal")
            .email("temporal@empresa.com")
            .estado(EstadoUsuario.TEMPORAL)
            .build();
    }
    
    @Fallback(fallbackMethod = "obtenerPedidosPorDefecto")
    public List<Pedido> obtenerPedidos(Long usuarioId) {
        return restTemplate.getForObject(
            "http://external-service/api/pedidos?usuario=" + usuarioId,
            List.class
        );
    }
    
    public List<Pedido> obtenerPedidosPorDefecto(Long usuarioId) {
        logger.warn("Usando fallback para pedidos del usuario: {}", usuarioId);
        return Collections.emptyList(); // Lista vacía como fallback
    }
}

// Fallback con decoradores
@Component
public class UsuarioFallbackDecorator {
    
    private final UsuarioService usuarioService;
    
    public UsuarioFallbackDecorator(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    
    public Usuario obtenerUsuarioConFallback(Long id) {
        try {
            return usuarioService.obtenerUsuario(id);
        } catch (Exception e) {
            logger.warn("Servicio de usuario falló, usando fallback para ID: {}", id);
            return obtenerUsuarioFallback(id);
        }
    }
    
    private Usuario obtenerUsuarioFallback(Long id) {
        return Usuario.builder()
            .id(id)
            .nombre("Usuario Desconocido")
            .email("desconocido@empresa.com")
            .estado(EstadoUsuario.DESCONOCIDO)
            .fechaUltimoAcceso(LocalDateTime.now())
            .build();
    }
}
```

## **Rate Limiting & Throttling (Límite de Ritmo)**

### ¿Qué es?
El patrón Rate Limiting controla la cantidad de solicitudes que pueden ser procesadas en un período de tiempo específico, previniendo sobrecarga de sistemas.

### Estrategias
- **Fixed Window**: Ventana fija de tiempo
- **Sliding Window**: Ventana deslizante
- **Token Bucket**: Cubo de tokens
- **Leaky Bucket**: Cubo con fuga

### Ejemplo en Java
```java
@Configuration
public class RateLimitingConfig {
    
    @Bean
    public RateLimiter rateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(100) // Máximo 100 requests
            .limitRefreshPeriod(Duration.ofSeconds(1)) // Por segundo
            .timeoutDuration(Duration.ofSeconds(5)) // Timeout de espera
            .build();
        
        return RateLimiterRegistry.of(config).rateLimiter("usuario-rate-limiter");
    }
}

@RestController
public class UsuarioRateLimitedController {
    
    private final RateLimiter rateLimiter;
    private final UsuarioService usuarioService;
    
    public UsuarioRateLimitedController(RateLimiter rateLimiter,
                                       UsuarioService usuarioService) {
        this.rateLimiter = rateLimiter;
        this.usuarioService = usuarioService;
    }
    
    @GetMapping("/api/usuarios/{id}")
    @RateLimiter(name = "usuario-rate-limiter")
    public ResponseEntity<Usuario> obtenerUsuario(@PathVariable Long id) {
        Usuario usuario = usuarioService.obtenerUsuario(id);
        return ResponseEntity.ok(usuario);
    }
}

// Implementación manual con Token Bucket
@Component
public class TokenBucketRateLimiter {
    
    private final int capacity;
    private final int tokensPerSecond;
    private double tokens;
    private long lastRefillTime;
    
    public TokenBucketRateLimiter(int capacity, int tokensPerSecond) {
        this.capacity = capacity;
        this.tokensPerSecond = tokensPerSecond;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }
    
    public boolean allowRequest() {
        long now = System.currentTimeMillis();
        double timePassed = (now - lastRefillTime) / 1000.0;
        
        // Refill tokens
        tokens = Math.min(capacity, tokens + timePassed * tokensPerSecond);
        lastRefillTime = now;
        
        if (tokens >= 1) {
            tokens--;
            return true;
        }
        
        return false;
    }
}

// Middleware para rate limiting
@Component
public class RateLimitingMiddleware {
    
    private final TokenBucketRateLimiter rateLimiter;
    
    public RateLimitingMiddleware() {
        this.rateLimiter = new TokenBucketRateLimiter(100, 10); // 100 tokens, 10 por segundo
    }
    
    public boolean checkRateLimit(String clientId) {
        if (!rateLimiter.allowRequest()) {
            logger.warn("Rate limit exceeded for client: {}", clientId);
            return false;
        }
        return true;
    }
}
```

## **Health Checks & Graceful Degradation**

### ¿Qué es?
Health Checks monitorean el estado de servicios y componentes, mientras que Graceful Degradation permite que el sistema continúe funcionando con funcionalidad reducida cuando algunos servicios no están disponibles.

### Health Check Types
- **Liveness**: Si el servicio está vivo
- **Readiness**: Si el servicio está listo para recibir tráfico
- **Startup**: Si el servicio completó el inicio
- **Dependencies**: Estado de servicios dependientes

### Ejemplo en Java
```java
@Configuration
public class HealthCheckConfig {
    
    @Bean
    public HealthIndicator usuarioServiceHealthIndicator() {
        return () -> {
            try {
                // Verificar conectividad con servicio externo
                Usuario usuario = usuarioService.obtenerUsuario(1L);
                return Health.up()
                    .withDetail("service", "up")
                    .withDetail("response_time", "50ms")
                    .build();
            } catch (Exception e) {
                return Health.down()
                    .withDetail("service", "down")
                    .withException(e)
                    .build();
            }
        };
    }
    
    @Bean
    public HealthIndicator databaseHealthIndicator(@Qualifier("primary-ds") DataSource dataSource) {
        return () -> {
            try (Connection connection = dataSource.getConnection()) {
                return Health.up()
                    .withDetail("database", "accessible")
                    .withDetail("url", connection.getMetaData().getURL())
                    .build();
            } catch (SQLException e) {
                return Health.down()
                    .withDetail("database", "unreachable")
                    .withException(e)
                    .build();
            }
        };
    }
}

// Graceful degradation service
@Service
public class GracefulDegradationService {
    
    private final Map<String, Boolean> serviceHealthStatus = new ConcurrentHashMap<>();
    
    @Scheduled(fixedRate = 30000) // Verificar cada 30 segundos
    public void checkServiceHealth() {
        try {
            usuarioService.obtenerUsuario(1L);
            serviceHealthStatus.put("usuario-service", true);
        } catch (Exception e) {
            serviceHealthStatus.put("usuario-service", false);
        }
    }
    
    @EventListener
    public void handleServiceFailure(ServiceFailureEvent event) {
        serviceHealthStatus.put(event.getServiceName(), false);
        logger.warn("Servicio falló: {}", event.getServiceName());
    }
    
    public boolean isServiceHealthy(String serviceName) {
        return serviceHealthStatus.getOrDefault(serviceName, true);
    }
    
    public Usuario obtenerUsuarioConDegradacion(Long id) {
        if (isServiceHealthy("usuario-service")) {
            try {
                return usuarioService.obtenerUsuario(id);
            } catch (Exception e) {
                // Marcar servicio como no saludable
                serviceHealthStatus.put("usuario-service", false);
                return obtenerUsuarioFallback(id);
            }
        } else {
            logger.info("Servicio usuario no saludable, usando degradación graciosa");
            return obtenerUsuarioFallback(id);
        }
    }
    
    @ConditionalOnProperty(name = "app.degradation.enabled", havingValue = "true")
    @Service
    public class DegradedUsuarioService {
        
        private final GracefulDegradationService degradationService;
        
        public DegradedUsuarioService(GracefulDegradationService degradationService) {
            this.degradationService = degradationService;
        }
        
        @Async
        public CompletableFuture<Usuario> obtenerUsuarioAsync(Long id) {
            if (degradationService.isServiceHealthy("usuario-service")) {
                return CompletableFuture.supplyAsync(() -> 
                    usuarioService.obtenerUsuario(id)
                ).orTimeout(5, TimeUnit.SECONDS)
                 .exceptionally(throwable -> {
                     degradationService.handleServiceFailure(
                         new ServiceFailureEvent("usuario-service", throwable)
                     );
                     return obtenerUsuarioFallback(id);
                 });
            } else {
                return CompletableFuture.completedFuture(obtenerUsuarioFallback(id));
            }
        }
    }
}

// Endpoint de health check
@RestController
public class HealthCheckController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        // Verificar servicios críticos
        boolean usuarioServiceHealthy = checkUsuarioService();
        boolean databaseHealthy = checkDatabase();
        boolean cacheHealthy = checkCache();
        
        health.put("timestamp", LocalDateTime.now());
        health.put("usuario-service", usuarioServiceHealthy ? "UP" : "DOWN");
        health.put("database", databaseHealthy ? "UP" : "DOWN");
        health.put("cache", cacheHealthy ? "UP" : "DOWN");
        
        boolean overallHealth = usuarioServiceHealthy && databaseHealthy;
        
        return ResponseEntity.status(overallHealth ? 200 : 503)
                .body(health);
    }
    
    @GetMapping("/ready")
    public ResponseEntity<String> readinessCheck() {
        // Verificar si el servicio está listo para recibir tráfico
        if (isReadyForTraffic()) {
            return ResponseEntity.ok("Ready");
        } else {
            return ResponseEntity.status(503).body("Not Ready");
        }
    }
    
    @GetMapping("/live")
    public ResponseEntity<String> livenessCheck() {
        return ResponseEntity.ok("Alive");
    }
}
```

## **Combinando Patrones**

### Ejemplo de Implementación Completa
```java
@Component
public class ResilientUsuarioService {
    
    private final CircuitBreaker circuitBreaker;
    private final RateLimiter rateLimiter;
    private final ThreadPoolBulkhead bulkhead;
    private final RestTemplate restTemplate;
    
    public ResilientUsuarioService(CircuitBreaker circuitBreaker,
                                  RateLimiter rateLimiter,
                                  ThreadPoolBulkhead bulkhead,
                                  RestTemplate restTemplate) {
        this.circuitBreaker = circuitBreaker;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
        this.restTemplate = restTemplate;
    }
    
    @Retryable(
        value = {ServicioNoDisponibleException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CircuitBreaker(name = "usuario-service", fallbackMethod = "obtenerUsuarioFallback")
    @RateLimiter(name = "usuario-rate-limiter")
    @Bulkhead(name = "usuario-bulkhead")
    public Usuario obtenerUsuarioCompleto(Long id) {
        return circuitBreaker.executeSupplier(() -> {
            return rateLimiter.executeSupplier(() -> {
                return bulkhead.executeSupplier(() -> {
                    return restTemplate.getForObject(
                        "http://external-service/api/usuarios/" + id, 
                        Usuario.class
                    );
                });
            });
        });
    }
    
    public Usuario obtenerUsuarioFallback(Long id, Exception ex) {
        logger.warn("Todos los patrones de resiliencia activados para usuario: {}", id);
        return obtenerUsuarioCacheLocal(id);
    }
}
```

## **Patrones de Resiliencia en Spring Boot 2.7**

### Configuración con Resilience4j
```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      usuario-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
  retry:
    instances:
      usuario-service:
        max-attempts: 3
        wait-duration: 1s
        retry-exceptions:
          - com.empresa.exceptions.ServicioNoDisponibleException
  ratelimiter:
    instances:
      usuario-service:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 5s
  bulkhead:
    threadpool:
      instances:
        usuario-service:
          core-thread-pool-size: 10
          max-thread-pool-size: 50
          queue-capacity: 100
```

## **Buenas Prácticas**

### Selección de Patrones
- **Timeout**: Siempre implementar en servicios externos
- **Retry**: Para operaciones idempotentes con fallos temporales
- **Circuit Breaker**: Para servicios críticos con alta latencia
- **Bulkhead**: Para sistemas con múltiples dependencias
- **Fallback**: Para funcionalidades no críticas
- **Rate Limiting**: Para APIs públicas y servicios limitados
- **Health Checks**: Para monitoreo proactivo

### Configuración
- **Valores conservador**: Empezar con valores conservadores
- **Observabilidad**: Monitorear métricas de patrones
- **Testing**: Probar escenarios de fallo
- **Gradual**: Implementar patrones gradualmente
- **Documentación**: Documentar configuraciones de resiliencia

### Monitoreo
- **Métricas**: Monitorear tasas de éxito/fallo
- **Alertas**: Configurar alertas para patrones activados
- **Dashboard**: Visualizar estado de servicios
- **Logs**: Logging detallado de patrones activados