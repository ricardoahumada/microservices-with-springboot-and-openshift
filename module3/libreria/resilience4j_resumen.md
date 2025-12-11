# Resilience4j

## ¿Qué es Resilience4j?

**Resilience4j** es una biblioteca de resiliencia y tolerancia a fallos para Java, diseñada específicamente para aplicaciones funcionales modernas y arquitecturas de microservicios. Proporciona implementaciones de patrones de resiliencia como Circuit Breaker, Retry, Rate Limiter, Bulkhead, Time Limiter y Cache, todas basadas en conceptos funcionales de programación reactiva.

### Conceptos Fundamentales

**Circuit Breaker**: Evita llamadas a servicios que han fallado repetidamente, proporcionando recuperación automática y evitando cascadas de fallos.

**Retry**: Maneja fallos temporales reintentando operaciones con backoff exponencial configurable.

**Rate Limiter**: Controla la frecuencia de llamadas a servicios externos para evitar sobrecarga.

**Bulkhead**: Aísla recursos para prevenir que un fallo en un servicio afecte otros servicios.

**Time Limiter**: Establece timeouts para operaciones que podrían colgarse indefinidamente.

**Cache**: Proporciona caching inteligente para reducir latencia y mejorar rendimiento.

---

## **Componentes Principales**

### **CircuitBreaker**
- **Closed**: Estado normal, las llamadas pasan a través del circuito
- **Open**: Circuit breaker abierto, las llamadas fallan inmediatamente
- **Half-Open**: Estado de prueba, permite algunas llamadas para verificar recuperación

### **Retry**
- Backoff exponencial
- Configuración de intentos máximos
- Manejo de excepciones específicas
- Jitter para evitar thundering herd

### **RateLimiter**
- Acquire permits
- Configuración de rate
- Sync y async support
- Permits por unidad de tiempo

### **Bulkhead**
- Thread pool isolation
- Semaphore isolation
- Configuración de pool sizes
- Rejection policies

---

## **Casos de Uso**

### **Microservicios**
- Protección contra fallos en cadena
- Aislación de servicios críticos
- Rate limiting para APIs externas

### **Integración con APIs Externas**
- Manejo de timeouts en servicios terceros
- Protección contra rate limiting de proveedores
- Reintentos inteligentes con backoff

### **Bases de Datos**
- Prevención de overload en conexiones
- Circuit breaker para servicios DB no disponibles
- Cache de consultas frecuentes

### **Sistemas de Alto Rendimiento**
- Control de throughput
- Aislación de recursos
- Bulkhead para evitar starvation

### **Sistemas de Misión Crítica**
- Graceful degradation
- Fallbacks automáticos
- Monitoreo de resiliencia

---

## **Ventajas y Desventajas**

### **Ventajas**

**Lightweight**
- Sin dependencias externas pesadas
- Minimal overhead
- Solo incluye funcionalidades necesarias

**Funcional**
- API funcional y reactiva
- Compatible con Java 8+
- CompletableFuture support

**Configuración Flexible**
- External configuration (YAML, Properties)
- Runtime reconfiguration
- Granular control per instance

**Observabilidad**
- Métricas integradas
- Event listeners
- Spring Boot Actuator integration

**Patrones Completos**
- Circuit Breaker, Retry, RateLimiter
- Bulkhead, TimeLimiter, Cache
- Combinar múltiples patrones

### **Desventajas**

**Curva de Aprendizaje**
- Conceptos avanzados de resiliencia
- Configuración compleja
- Testing de escenarios edge

**Performance Overhead**
- Overhead de interceptors
- Métricas collection cost
- Memory usage para tracking

**Configuración Compleja**
- Muchos parámetros para configurar
- Tuning específico por use case
- Dependencies entre componentes

---

## **Buenas Prácticas**

### **Configuración por Entorno**

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 3
  retry:
    instances:
      userService:
        max-attempts: 3
        wait-duration: 200ms
        retry-exceptions:
          - java.io.IOException
          - java.net.SocketTimeoutException
  ratelimiter:
    instances:
      externalApi:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0ms
```

### **Error Handling Strategy**

```java
@Component
public class ResilienceErrorHandler {
    
    public <T> T executeWithResilience(Supplier<T> supplier) {
        try {
            return executeSupplier(supplier);
        } catch (CircuitBreakerOpenException e) {
            log.warn("Circuit breaker open for service", e);
            return handleCircuitBreakerOpen();
        } catch (TimeoutException e) {
            log.warn("Operation timed out", e);
            return handleTimeout();
        } catch (RateLimiterExceededException e) {
            log.warn("Rate limit exceeded", e);
            return handleRateLimitExceeded();
        } catch (Exception e) {
            log.error("Unexpected error in resilience execution", e);
            throw new ServiceUnavailableException("Service temporarily unavailable", e);
        }
    }
    
    private <T> T handleCircuitBreakerOpen() {
        // Return cached data or default response
        return getCachedResponseOrDefault();
    }
    
    private <T> T handleTimeout() {
        // Return fallback response
        return getFallbackResponse();
    }
    
    private <T> T handleRateLimitExceeded() {
        // Return queued response or reject
        return getQueuedResponseOrReject();
    }
}
```

### **Monitoring y Observabilidad**

```java
@Component
public class ResilienceMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Counter circuitBreakerCalls;
    private final Timer serviceCallTimer;
    
    public ResilienceMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.circuitBreakerCalls = Counter.builder("resilience4j.circuitbreaker.calls")
            .description("Circuit breaker call count")
            .register(meterRegistry);
        this.serviceCallTimer = Timer.builder("resilience4j.service.call.time")
            .description("Service call duration")
            .register(meterRegistry);
    }
    
    public void recordCircuitBreakerCall(String serviceName, String state) {
        circuitBreakerCalls.increment(
            Tags.of("service", serviceName, "state", state));
    }
    
    public Timer.Sample startServiceCallTimer() {
        return Timer.start(meterRegistry);
    }
}
```

---

## **Ejemplos en Java**

### **Circuit Breaker**

```java
// Configuración del Circuit Breaker
@Configuration
public class CircuitBreakerConfig {
    
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // 50% failure rate
            .waitDurationInOpenState(Duration.ofSeconds(60)) // 60s en estado open
            .slidingWindowSize(10) // Ventana de 10 llamadas
            .minimumNumberOfCalls(5) // Mínimo 5 llamadas antes de evaluar
            .permittedNumberOfCallsInHalfOpenState(3) // 3 llamadas en half-open
            .slowCallRateThreshold(100) // 100% de llamadas lentas
            .slowCallDurationThreshold(Duration.ofSeconds(2)) // > 2s es lenta
            .build();
    }
    
    @Bean
    public CircuitBreaker userServiceCircuitBreaker() {
        return CircuitBreaker.of("userService", circuitBreakerConfig());
    }
}

// Servicio con Circuit Breaker
@Service
public class UserServiceClient {
    
    @Autowired
    private CircuitBreaker userServiceCircuitBreaker;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public User getUser(String userId) {
        Supplier<User> userSupplier = () -> {
            ResponseEntity<User> response = restTemplate.getForEntity(
                "http://user-service/api/users/{id}", User.class, userId);
            return response.getBody();
        };
        
        // Ejecutar con Circuit Breaker
        return CircuitBreaker.decorateSupplier(userServiceCircuitBreaker, userSupplier)
            .get();
    }
    
    // Con fallback
    public User getUserWithFallback(String userId) {
        Supplier<User> userSupplier = () -> {
            ResponseEntity<User> response = restTemplate.getForEntity(
                "http://user-service/api/users/{id}", User.class, userId);
            return response.getBody();
        };
        
        Supplier<User> fallbackSupplier = () -> {
            log.warn("Circuit breaker open, returning cached user for ID: {}", userId);
            return userCache.get(userId); // Cache o default user
        };
        
        return userServiceCircuitBreaker.executeSupplier(userSupplier)
            .recover(throwable -> fallbackSupplier.get());
    }
}

// Event listeners para monitoreo
@Component
public class CircuitBreakerEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerEventListener.class);
    
    @EventListener
    public void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker circuitBreaker = event.getCircuitBreaker();
        State fromState = event.getStateTransition().getFromState();
        State toState = event.getStateTransition().getToState();
        
        log.info("Circuit breaker {} transitioned from {} to {}", 
                circuitBreaker.getName(), fromState, toState);
        
        // Enviar alerta si el circuito se abre
        if (toState == State.OPEN) {
            alertService.sendCircuitBreakerOpenAlert(circuitBreaker.getName());
        }
    }
    
    @EventListener
    public void onCallNotPermitted(CircuitBreakerOnCallNotPermittedEvent event) {
        log.warn("Call not permitted by circuit breaker {}", 
                event.getCircuitBreaker().getName());
        
        // Métricas
        metricsService.incrementRejectedCalls(event.getCircuitBreaker().getName());
    }
}
```

### **Retry Pattern**

```java
// Configuración de Retry
@Configuration
public class RetryConfig {
    
    @Bean
    public RetryConfig retryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3) // Máximo 3 intentos
            .waitDuration(Duration.ofMillis(200)) // Esperar 200ms entre intentos
            .retryExceptions(
                IOException.class,
                SocketTimeoutException.class,
                ConnectException.class
            )
            .ignoreExceptions(
                IllegalArgumentException.class,
                UserNotFoundException.class
            )
            .retryOnResultCheck(result -> {
                // Reintentar si el resultado indica error
                return result instanceof ApiResponse && 
                       ((ApiResponse) result).hasError();
            })
            .build();
    }
    
    @Bean
    public Retry paymentServiceRetry() {
        return Retry.of("paymentService", retryConfig());
    }
}

// Servicio con Retry
@Service
public class PaymentServiceClient {
    
    @Autowired
    private Retry paymentServiceRetry;
    
    @Autowired
    private PaymentApi paymentApi;
    
    public PaymentResult processPayment(PaymentRequest request) {
        Callable<PaymentResult> paymentCallable = () -> {
            return paymentApi.processPayment(request);
        };
        
        // Ejecutar con retry
        return paymentServiceRetry.executeCallable(paymentCallable);
    }
    
    // Con configuración específica por método
    @CircuitBreaker(name = "externalApi", fallbackMethod = "getApiFallback")
    @Retry(name = "externalApi", maxAttempts = 3, waitDuration = @Duration("500ms"))
    public ExternalApiResponse callExternalApi(ExternalApiRequest request) {
        return externalApiClient.call(request);
    }
    
    public ExternalApiResponse getApiFallback(ExternalApiRequest request, Exception ex) {
        log.warn("External API call failed, returning fallback response", ex);
        return ExternalApiResponse.fallbackResponse();
    }
}

// Custom Retry Logic
@Component
public class CustomRetryHandler {
    
    public <T> T executeWithCustomRetry(Supplier<T> supplier, RetryContext context) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < context.getMaxAttempts()) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt >= context.getMaxAttempts()) {
                    break;
                }
                
                // Backoff exponencial con jitter
                long delay = calculateExponentialBackoff(attempt, context.getBaseDelay());
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry", ie);
                }
            }
        }
        
        throw new RetryExhaustedException("Max retry attempts reached", lastException);
    }
    
    private long calculateExponentialBackoff(int attempt, long baseDelay) {
        long exponentialDelay = baseDelay * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(exponentialDelay / 2);
        return exponentialDelay + jitter;
    }
}
```

### **Rate Limiter**

```java
// Configuración de Rate Limiter
@Configuration
public class RateLimiterConfig {
    
    @Bean
    public RateLimiterConfig rateLimiterConfig() {
        return RateLimiterConfig.custom()
            .limitForPeriod(100) // 100 permits por período
            .limitRefreshPeriod(Duration.ofSeconds(1)) // Período de 1 segundo
            .timeoutDuration(Duration.ofMillis(0)) // No esperar si no hay permits
            .build();
    }
    
    @Bean
    public RateLimiter externalApiRateLimiter() {
        return RateLimiter.of("externalApi", rateLimiterConfig());
    }
}

// Servicio con Rate Limiter
@Service
public class ExternalApiClient {
    
    @Autowired
    private RateLimiter externalApiRateLimiter;
    
    @Autowired
    private RestTemplate restTemplate;
    
    public ExternalApiResponse callApi(ExternalApiRequest request) {
        // Decorar el callable con Rate Limiter
        Callable<ExternalApiResponse> decoratedCallable = 
            RateLimiter.decorateCallable(externalApiRateLimiter, () -> {
                return restTemplate.postForEntity(
                    "https://external-api.com/endpoint", 
                    request, 
                    ExternalApiResponse.class
                ).getBody();
            });
        
        try {
            return decoratedCallable.call();
        } catch (RateLimiterExceededException e) {
            log.warn("Rate limit exceeded for external API call");
            return handleRateLimitExceeded(request);
        } catch (Exception e) {
            log.error("External API call failed", e);
            throw new ApiCallFailedException("External API call failed", e);
        }
    }
    
    private ExternalApiResponse handleRateLimitExceeded(ExternalApiRequest request) {
        // Opción 1: Fallback a cache
        return getCachedResponse(request);
        
        // Opción 2: Queue para procesamiento posterior
        queueForLaterProcessing(request);
        
        // Opción 3: Return default response
        return ExternalApiResponse.defaultResponse();
    }
}

// Async Rate Limiter
@Service
public class AsyncApiClient {
    
    @Autowired
    private RateLimiter apiRateLimiter;
    
    public CompletableFuture<ExternalApiResponse> callApiAsync(ExternalApiRequest request) {
        // Decorar con rate limiter
        Callable<ExternalApiResponse> decoratedCallable = 
            RateLimiter.decorateCallable(apiRateLimiter, () -> {
                return externalApiClient.call(request);
            });
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return decoratedCallable.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }
    
    // Rate limiting con permisos específicos
    public CompletableFuture<ExternalApiResponse> callApiWithPermits(
            ExternalApiRequest request, int permits) {
        
        return CompletableFuture.supplyAsync(() -> {
            // Acquire permits before calling
            boolean acquired = apiRateLimiter.acquirePermission(permits);
            
            if (!acquired) {
                throw new RateLimitExceededException(
                    "Could not acquire {} permits", permits);
            }
            
            try {
                return externalApiClient.call(request);
            } finally {
                // Release permits (optional, depends on use case)
                apiRateLimiter.acquirePermission(0); // No-op release
            }
        });
    }
}
```

### **Bulkhead Pattern**

```java
// Thread Pool Bulkhead
@Configuration
public class BulkheadConfig {
    
    @Bean
    public ThreadPoolBulkheadConfig threadPoolBulkheadConfig() {
        return ThreadPoolBulkheadConfig.custom()
            .maxThreadCount(10) // Máximo 10 threads
            .coreThreadCount(5) // 5 threads core
            .queueCapacity(20) // Cola de 20 elementos
            .keepAliveDuration(Duration.ofSeconds(20)) // 20s keep alive
            .build();
    }
    
    @Bean
    public ThreadPoolBulkhead threadPoolBulkhead() {
        return ThreadPoolBulkhead.of("serviceBulkhead", threadPoolBulkheadConfig());
    }
    
    // Semaphore Bulkhead
    @Bean
    public BulkheadConfig semaphoreBulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(10) // Máximo 10 llamadas concurrentes
            .maxWaitDuration(Duration.ofMillis(5000)) // 5s máximo de espera
            .build();
    }
    
    @Bean
    public Bulkhead semaphoreBulkhead() {
        return Bulkhead.of("serviceBulkhead", semaphoreBulkheadConfig());
    }
}

// Servicio con Bulkhead
@Service
public class ServiceClientWithBulkhead {
    
    @Autowired
    private ThreadPoolBulkhead threadPoolBulkhead;
    
    @Autowired
    private Bulkhead semaphoreBulkhead;
    
    @Autowired
    private RestTemplate restTemplate;
    
    // Thread Pool Bulkhead para operaciones async
    public CompletableFuture<ServiceResponse> callServiceAsync(ServiceRequest request) {
        Supplier<ServiceResponse> serviceSupplier = () -> {
            ResponseEntity<ServiceResponse> response = restTemplate.postForEntity(
                "http://service/api/endpoint", request, ServiceResponse.class);
            return response.getBody();
        };
        
        return threadPoolBulkhead.executeSupplier(serviceSupplier);
    }
    
    // Semaphore Bulkhead para operaciones sync
    public ServiceResponse callServiceSync(ServiceRequest request) {
        Callable<ServiceResponse> serviceCallable = () -> {
            ResponseEntity<ServiceResponse> response = restTemplate.postForEntity(
                "http://service/api/endpoint", request, ServiceResponse.class);
            return response.getBody();
        };
        
        return semaphoreBulkhead.executeCallable(serviceCallable);
    }
    
    // Combinando Bulkhead con Circuit Breaker y Retry
    @Bulkhead(name = "combinedService", fallbackMethod = "serviceFallback")
    @CircuitBreaker(name = "combinedService", fallbackMethod = "serviceFallback")
    @Retry(name = "combinedService", maxAttempts = 3)
    public ServiceResponse callCombinedService(ServiceRequest request) {
        return restTemplate.postForEntity(
            "http://service/api/endpoint", 
            request, 
            ServiceResponse.class
        ).getBody();
    }
    
    public ServiceResponse serviceFallback(ServiceRequest request, Exception ex) {
        log.warn("Service call failed, returning fallback response", ex);
        return ServiceResponse.fallbackResponse();
    }
}
```

### **Time Limiter**

```java
// Configuración de Time Limiter
@Configuration
public class TimeLimiterConfig {
    
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3)) // 3 segundos timeout
            .build();
    }
    
    @Bean
    public TimeLimiter slowServiceTimeLimiter() {
        return TimeLimiter.of("slowService", timeLimiterConfig());
    }
}

// Servicio con Time Limiter
@Service
public class SlowServiceClient {
    
    @Autowired
    private TimeLimiter slowServiceTimeLimiter;
    
    @Autowired
    private SlowService slowService;
    
    public ServiceResult callSlowService(ServiceRequest request) {
        Callable<ServiceResult> serviceCallable = () -> {
            return slowService.process(request); // Esta operación puede ser lenta
        };
        
        // Aplicar time limiter
        return slowServiceTimeLimiter.executeSupplier(serviceCallable::call);
    }
    
    // Con timeout específico
    public ServiceResult callServiceWithCustomTimeout(ServiceRequest request, Duration timeout) {
        TimeLimiterConfig customConfig = TimeLimiterConfig.custom()
            .timeoutDuration(timeout)
            .build();
            
        TimeLimiter customTimeLimiter = TimeLimiter.of("customTimeout", customConfig);
        
        Callable<ServiceResult> serviceCallable = () -> {
            return slowService.process(request);
        };
        
        return customTimeLimiter.executeSupplier(serviceCallable::call);
    }
    
    // Para CompletableFuture
    public CompletableFuture<ServiceResult> callServiceAsync(ServiceRequest request) {
        Callable<ServiceResult> serviceCallable = () -> {
            return slowService.process(request);
        };
        
        return slowServiceTimeLimiter.executeCompletionStage(serviceCallable::call)
            .toCompletableFuture();
    }
}
```

### **Cache**

```java
// Configuración de Cache
@Configuration
public class CacheConfig {
    
    @Bean
    public Cache cache() {
        return Cache.of("userCache", cacheConfig());
    }
    
    private CacheConfig cacheConfig() {
        return CacheConfig.custom()
            .expireAfterAccess(Duration.ofMinutes(5)) // Expira después de 5 min sin acceso
            .expireAfterWrite(Duration.ofMinutes(10)) // Expira después de 10 min de escritura
            .maxSize(1000) // Máximo 1000 entries
            .build();
    }
}

// Servicio con Cache
@Service
public class UserCacheService {
    
    @Autowired
    private Cache userCache;
    
    @Autowired
    private UserService userService;
    
    public User getUser(String userId) {
        return userCache.computeIfAbsent(userId, id -> {
            log.debug("Cache miss for user {}", id);
            return userService.getUserById(id);
        });
    }
    
    // Cache con diferentes TTL por tipo de dato
    public UserProfile getUserProfile(String userId) {
        return userCache.computeIfAbsent("profile:" + userId, id -> {
            return userService.getUserProfile(userId);
        }, Duration.ofMinutes(15)); // 15 min TTL para profiles
    }
    
    public List<User> searchUsers(String query) {
        // Cache temporal para búsquedas
        String cacheKey = "search:" + query;
        return userCache.computeIfAbsent(cacheKey, key -> {
            return userService.searchUsers(query);
        }, Duration.ofMinutes(2)); // 2 min TTL para búsquedas
    }
    
    // Invalidate cache
    public void invalidateUserCache(String userId) {
        userCache.invalidate("user:" + userId);
        userCache.invalidate("profile:" + userId);
    }
    
    public void invalidateAllUserCache() {
        userCache.invalidateAll();
    }
}
```

### **Spring Boot Integration**

```java
// Application.yml configuration
resilience4j:
  circuitbreaker:
    instances:
      userService:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
        minimum-number-of-calls: 5
        permitted-number-of-calls-in-half-open-state: 3
      paymentService:
        failure-rate-threshold: 30
        wait-duration-in-open-state: 120s
        sliding-window-size: 20
  retry:
    instances:
      userService:
        max-attempts: 3
        wait-duration: 200ms
        retry-exceptions:
          - java.io.IOException
      paymentService:
        max-attempts: 5
        wait-duration: 500ms
        retry-exceptions:
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException
  ratelimiter:
    instances:
      externalApi:
        limit-for-period: 100
        limit-refresh-period: 1s
        timeout-duration: 0ms
  timelimiter:
    instances:
      slowService:
        timeout-duration: 5s
  cache:
    caches:
      userCache:
        expire-after-write: 5m
        expire-after-access: 2m
        max-size: 1000

// Spring Boot Service con annotations
@Service
public class ResilientUserService {
    
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Retry(name = "userService")
    @RateLimiter(name = "userService")
    public User getUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
    
    public User getUserFallback(String userId, Exception ex) {
        log.warn("User service unavailable, returning cached/default user", ex);
        return userCache.getCachedUser(userId);
    }
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "paymentService", maxAttempts = 3, waitDuration = @Duration("500ms"))
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentGateway.process(request);
    }
    
    public PaymentResult processPaymentFallback(PaymentRequest request, Exception ex) {
        log.warn("Payment service failed, queuing for later processing", ex);
        paymentQueue.add(request);
        return PaymentResult.queued();
    }
    
    @RateLimiter(name = "externalApi")
    public ExternalApiResponse callExternalApi(ExternalApiRequest request) {
        return externalApiClient.call(request);
    }
}

// Monitoring con Actuator
@RestController
@RequestMapping("/actuator/resilience4j")
public class ResilienceActuatorController {
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RetryRegistry retryRegistry;
    
    @GetMapping("/circuitbreakers")
    public Map<String, CircuitBreakerConfig> getCircuitBreakers() {
        return circuitBreakerRegistry.getAllCircuitBreakers()
            .stream()
            .collect(Collectors.toMap(
                cb -> cb.getName(),
                cb -> cb.getCircuitBreakerConfig()
            ));
    }
    
    @GetMapping("/retries")
    public Map<String, RetryConfig> getRetries() {
        return retryRegistry.getAllRetries()
            .stream()
            .collect(Collectors.toMap(
                retry -> retry.getName(),
                retry -> retry.getRetryConfig()
            ));
    }
}
```

---

## **Patrones Avanzados**

### **Combinando Múltiples Patrones**

```java
@Component
public class AdvancedResilienceHandler {
    
    public <T> T executeWithAllPatterns(
            String operationName,
            Supplier<T> operation,
            ResilienceConfig config) {
        
        // Crear circuit breaker
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(
            operationName, config.getCircuitBreakerConfig());
        
        // Crear retry
        Retry retry = retryRegistry.retry(operationName, config.getRetryConfig());
        
        // Crear rate limiter
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(
            operationName, config.getRateLimiterConfig());
        
        // Crear time limiter
        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(
            operationName, config.getTimeLimiterConfig());
        
        // Combinar todos los decorators
        Supplier<T> decorated = SupplierUtils.andThen(
            SupplierUtils.andThen(
                SupplierUtils.andThen(
                    SupplierUtils.andThen(
                        operation,
                        circuitBreaker
                    ),
                    retry
                ),
                rateLimiter
            ),
            timeLimiter
        );
        
        try {
            return decorated.get();
        } catch (Exception e) {
            return handleResilienceFailure(operationName, e, config);
        }
    }
    
    private <T> T handleResilienceFailure(String operationName, Exception e, ResilienceConfig config) {
        if (config.isFallbackEnabled()) {
            return config.getFallback().apply(e);
        }
        
        log.error("Resilience patterns failed for operation: {}", operationName, e);
        throw new ResilienceOperationFailedException(operationName, e);
    }
}
```

### **Dynamic Configuration**

```java
@Component
public class DynamicResilienceConfig {
    
    @Value("${resilience4j.circuitbreaker.failure-rate-threshold}")
    private double defaultFailureRate;
    
    public void updateCircuitBreakerConfig(String serviceName, double failureRate) {
        CircuitBreakerConfig newConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRate)
            .build();
        
        circuitBreakerRegistry.replaceCircuitBreakerConfig(serviceName, newConfig);
        
        log.info("Updated circuit breaker config for {}: failure rate = {}", 
                serviceName, failureRate);
    }
    
    public void enableCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        circuitBreaker.reset();
        log.info("Enabled circuit breaker for {}", serviceName);
    }
    
    public void disableCircuitBreaker(String serviceName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(serviceName);
        circuitBreaker.transitionToForcedOpenState();
        log.info("Disabled circuit breaker for {}", serviceName);
    }
}
```

---

## **Referencias Oficiales**

1. **Resilience4j Documentation**  
   https://resilience4j.readme.io/docs

2. **Resilience4j GitHub Repository**  
   https://github.com/resilience4j/resilience4j

3. **Spring Boot Resilience4j Starter**  
   https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.resilience4j

4. **Resilience4j Circuit Breaker Pattern**  
   https://resilience4j.readme.io/docs/circuitbreaker

5. **Resilience4j Retry Pattern**  
   https://resilience4j.readme.io/docs/retry