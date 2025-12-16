# Guia de Evolucion: Patrones de Resiliencia en Arquitectura DDD

## Objetivo

Evolucionar `module1/solutions/mutualidad-platform` hacia `module3/solutions/mutualidad-platform-ddd` anadiendo patrones de resiliencia para comunicacion entre microservicios.

---

## Estructura Inicial (Module 1)

```
com.mutualidad.afiliado/
├── api/
│   ├── controller/AfiliadoController.java
│   └── dto/
│       ├── AfiliadoRequest.java
│       └── AfiliadoResponse.java
├── application/
│   └── service/AfiliadoService.java
├── domain/
│   └── model/
│       ├── Afiliado.java
│       ├── DNI.java
│       └── EstadoAfiliado.java
└── infrastructure/
    └── persistence/AfiliadoJpaRepository.java
```

## Estructura Final (Module 3)

```
com.mutualidad.afiliado/
├── api/
│   ├── controller/
│   │   ├── AfiliadoController.java
│   │   └── ResilienciaTestController.java      # Ejercicio 2
│   └── dto/
│       ├── AfiliadoResponse.java
│       ├── AltaAfiliadoRequest.java            # Ejercicio 1
│       └── BeneficioDto.java                   # Ejercicio 1
├── application/
│   └── service/AfiliadoService.java            # Modificado en cada ejercicio
├── domain/
│   ├── model/Afiliado.java
│   └── exception/BusinessException.java        # Ejercicio 2
└── infrastructure/
    ├── client/                                 # Ejercicio 1
    │   ├── dto/*.java
    │   ├── *ServiceClient.java
    │   └── *ServiceClientFallback.java
    ├── config/                                 # Ejercicios 1-2
    │   ├── FeignConfig.java
    │   └── GlobalExceptionHandler.java
    ├── filter/CorrelationIdFilter.java         # Ejercicio 1
    ├── health/                                 # Ejercicio 4
    │   ├── CircuitBreakersHealthIndicator.java
    │   └── ValidacionServiceHealthIndicator.java
    ├── idempotency/                            # Ejercicio 3
    │   ├── IdempotencyRecord.java
    │   ├── IdempotencyRepository.java
    │   └── IdempotencyService.java
    └── persistence/AfiliadoRepository.java
```

---

# Ejercicio 1: Integracion con Feign Client

## Objetivo
Comunicar afiliado-service con servicios externos usando clientes declarativos.

---

### Paso 1.1: Actualizar Dependencias (`pom.xml`)

```xml
<properties>
    <spring-cloud.version>2021.0.8</spring-cloud.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Feign Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>
</dependencies>
```

**Verificar:** `mvn compile` debe completar sin errores.

---

### Paso 1.2: Habilitar Feign en Application

```java
@SpringBootApplication
@EnableFeignClients  // ANADIR
public class AfiliadoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AfiliadoServiceApplication.class, args);
    }
}
```

---

### Paso 1.3: Configurar URLs de Servicios (`application.yml`)

```yaml
services:
  validacion:
    url: http://localhost:8084
  beneficio:
    url: http://localhost:8082
  notificacion:
    url: http://localhost:8083
```

---

### Paso 1.4: Crear DTOs de Cliente (`infrastructure/client/dto/`)

```java
// ValidacionRequest.java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ValidacionRequest {
    private String dni;
    private String nombre;
    private String apellidos;
}

// ValidacionResponse.java
@Data @NoArgsConstructor @AllArgsConstructor
public class ValidacionResponse {
    private boolean valido;
    private String mensaje;
    private List<String> errores;
}

// NotificacionResponse.java
@Data @NoArgsConstructor @AllArgsConstructor
public class NotificacionResponse {
    private String notificacionId;
    private String estado;
}
```

---

### Paso 1.5: Crear Cliente Feign (`infrastructure/client/`)

```java
@FeignClient(
    name = "validacion-service",
    url = "${services.validacion.url}"
)
public interface ValidacionServiceClient {

    @PostMapping("/api/validaciones/afiliado")
    ValidacionResponse validarAfiliado(@RequestBody ValidacionRequest request);
}
```

**Verificar:** Iniciar aplicacion y verificar que arranca sin errores.

---

### Paso 1.6: Crear Configuracion Feign (`infrastructure/config/`)

```java
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return template -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                template.header("X-Correlation-ID", correlationId);
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
```

---

### Paso 1.7: Crear Filtro de Correlacion (`infrastructure/filter/`)

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

---

### Paso 1.8: Integrar Cliente en Servicio (`application/service/`)

```java
@Service
@RequiredArgsConstructor
public class AfiliadoService {

    private final AfiliadoRepository repository;
    private final ValidacionServiceClient validacionClient;  // NUEVO

    public AfiliadoResponse registrar(AfiliadoRequest request) {
        // Validar con servicio externo
        ValidacionResponse validacion = validacionClient.validarAfiliado(
            ValidacionRequest.builder()
                .dni(request.getDni())
                .nombre(request.getNombre())
                .apellidos(request.getApellidos())
                .build()
        );
        
        if (!validacion.isValido()) {
            throw new IllegalArgumentException(validacion.getMensaje());
        }
        
        // Continuar con registro...
    }
}
```

---

### Paso 1.9: Verificar Ejercicio 1

```bash
# Iniciar validacion-service (puerto 8084)
cd validacion-service && mvn spring-boot:run

# Iniciar afiliado-service (puerto 8081)
cd afiliado-service && mvn spring-boot:run

# Probar llamada
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -d '{"dni":"12345678Z","nombre":"Juan","apellidos":"Garcia"}'

# Verificar header de correlacion en respuesta
```

---

# Ejercicio 2: Configuracion de Resilience4j

## Objetivo
Anadir Circuit Breaker, Retry y TimeLimiter para tolerancia a fallos.

---

### Paso 2.1: Anadir Dependencias (`pom.xml`)

```xml
<dependencies>
    <!-- Resilience4j -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot2</artifactId>
        <version>2.1.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-feign</artifactId>
        <version>2.1.0</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
</dependencies>
```

---

### Paso 2.2: Crear Excepcion de Dominio (`domain/exception/`)

```java
public class BusinessException extends RuntimeException {
    
    private final String code;
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
```

---

### Paso 2.3: Configurar Resilience4j (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    instances:
      validacionService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
        ignoreExceptions:
          - com.mutualidad.afiliado.domain.exception.BusinessException
      beneficioService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      notificacionService:
        slidingWindowSize: 5
        failureRateThreshold: 60
        waitDurationInOpenState: 60s

  retry:
    instances:
      validacionService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        retryExceptions:
          - java.io.IOException
          - feign.FeignException$ServiceUnavailable

  timelimiter:
    instances:
      validacionService:
        timeoutDuration: 5s
        cancelRunningFuture: true
```

---

### Paso 2.4: Crear Fallback para Cliente (`infrastructure/client/`)

```java
@Component
@Slf4j
public class ValidacionServiceClientFallback implements ValidacionServiceClient {

    @Override
    public ValidacionResponse validarAfiliado(ValidacionRequest request) {
        log.warn("Fallback activado para validacion de afiliado: {}", request.getDni());
        return ValidacionResponse.builder()
            .valido(false)
            .mensaje("Servicio de validacion no disponible. Intente mas tarde.")
            .errores(List.of("SERVICE_UNAVAILABLE"))
            .build();
    }
}
```

---

### Paso 2.5: Actualizar Cliente con Fallback

```java
@FeignClient(
    name = "validacion-service",
    url = "${services.validacion.url}",
    fallback = ValidacionServiceClientFallback.class  // ANADIR
)
public interface ValidacionServiceClient {
    @PostMapping("/api/validaciones/afiliado")
    ValidacionResponse validarAfiliado(@RequestBody ValidacionRequest request);
}
```

---

### Paso 2.6: Aplicar Anotaciones en Servicio

```java
@Service
@RequiredArgsConstructor
public class AfiliadoService {

    @CircuitBreaker(name = "validacionService", fallbackMethod = "registrarFallback")
    @Retry(name = "validacionService")
    @TimeLimiter(name = "validacionService")
    public CompletableFuture<AfiliadoResponse> registrarAsync(AfiliadoRequest request) {
        return CompletableFuture.supplyAsync(() -> registrar(request));
    }

    public CompletableFuture<AfiliadoResponse> registrarFallback(
            AfiliadoRequest request, Throwable t) {
        log.error("Fallback ejecutado: {}", t.getMessage());
        throw new BusinessException("SERVICE_DEGRADED", 
            "Servicio temporalmente degradado. Su solicitud sera procesada.");
    }
}
```

---

### Paso 2.7: Crear Controller de Pruebas (`api/controller/`)

```java
@RestController
@RequestMapping("/api/test/resiliencia")
@RequiredArgsConstructor
public class ResilienciaTestController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/circuit-breakers")
    public Map<String, String> getCircuitBreakersStatus() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> cb.getState().name()
            ));
    }

    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String name) {
        circuitBreakerRegistry.circuitBreaker(name).reset();
        return ResponseEntity.ok("Circuit breaker " + name + " reseteado");
    }
}
```

---

### Paso 2.8: Crear GlobalExceptionHandler (`infrastructure/config/`)

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", e.getCode(),
            "message", e.getMessage()
        ));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<Map<String, Object>> handleCircuitBreakerOpen(CallNotPermittedException e) {
        log.warn("Circuit breaker abierto: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
            "error", "CIRCUIT_OPEN",
            "message", "Servicio temporalmente no disponible"
        ));
    }
}
```

---

### Paso 2.9: Verificar Ejercicio 2

```bash
# Iniciar afiliado-service SIN validacion-service activo
mvn spring-boot:run

# Hacer varias llamadas para abrir el circuit breaker
for i in {1..15}; do
  curl -X POST http://localhost:8081/api/afiliados \
    -H "Content-Type: application/json" \
    -d '{"dni":"12345678Z","nombre":"Juan","apellidos":"Garcia"}'
  sleep 0.5
done

# Verificar estado del circuit breaker
curl http://localhost:8081/api/test/resiliencia/circuit-breakers

# Deberia mostrar: {"validacionService":"OPEN"}
```

---

# Ejercicio 3: Implementacion de Idempotencia

## Objetivo
Prevenir efectos duplicados cuando hay reintentos automaticos.

---

### Paso 3.1: Crear Entidad de Idempotencia (`infrastructure/idempotency/`)

```java
@Entity
@Table(name = "idempotency_records")
@Getter @Setter @NoArgsConstructor
public class IdempotencyRecord {

    @Id
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public IdempotencyRecord(String key, String response, Duration ttl) {
        this.idempotencyKey = key;
        this.response = response;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plus(ttl);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}
```

---

### Paso 3.2: Crear Repositorio

```java
@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {
    
    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
```

---

### Paso 3.3: Crear Servicio de Idempotencia

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    public <T> Optional<T> getExistingResponse(String key, Class<T> responseType) {
        return repository.findById(key)
            .filter(record -> !record.isExpired())
            .map(record -> {
                try {
                    return objectMapper.readValue(record.getResponse(), responseType);
                } catch (Exception e) {
                    log.error("Error deserializando respuesta idempotente", e);
                    return null;
                }
            });
    }

    public <T> void saveResponse(String key, T response) {
        try {
            String json = objectMapper.writeValueAsString(response);
            repository.save(new IdempotencyRecord(key, json, DEFAULT_TTL));
        } catch (Exception e) {
            log.error("Error guardando respuesta idempotente", e);
        }
    }

    @Scheduled(fixedRate = 3600000) // Cada hora
    public void cleanupExpired() {
        repository.deleteExpired(LocalDateTime.now());
    }
}
```

---

### Paso 3.4: Integrar en Servicio

```java
@Service
@RequiredArgsConstructor
public class AfiliadoService {

    private final IdempotencyService idempotencyService;

    public AfiliadoResponse registrar(String idempotencyKey, AfiliadoRequest request) {
        // Verificar si ya existe respuesta
        Optional<AfiliadoResponse> existing = idempotencyService
            .getExistingResponse(idempotencyKey, AfiliadoResponse.class);
        
        if (existing.isPresent()) {
            log.info("Retornando respuesta idempotente para key: {}", idempotencyKey);
            return existing.get();
        }

        // Procesar normalmente
        AfiliadoResponse response = procesarRegistro(request);
        
        // Guardar para idempotencia
        idempotencyService.saveResponse(idempotencyKey, response);
        
        return response;
    }
}
```

---

### Paso 3.5: Actualizar Controller

```java
@PostMapping
public ResponseEntity<AfiliadoResponse> registrar(
        @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody AfiliadoRequest request) {
    
    if (idempotencyKey == null) {
        idempotencyKey = UUID.randomUUID().toString();
    }
    
    AfiliadoResponse response = afiliadoService.registrar(idempotencyKey, request);
    return ResponseEntity.ok(response);
}
```

---

### Paso 3.6: Verificar Ejercicio 3

```bash
# Primera llamada con idempotency key
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: test-123" \
  -d '{"dni":"12345678Z","nombre":"Juan","apellidos":"Garcia"}'

# Segunda llamada con misma key (debe retornar igual sin crear duplicado)
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: test-123" \
  -d '{"dni":"12345678Z","nombre":"Juan","apellidos":"Garcia"}'

# Verificar en BD que solo hay 1 registro
```

---

# Ejercicio 4: Health Checks con Actuator

## Objetivo
Exponer estado de salud del servicio y sus dependencias.

---

### Paso 4.1: Anadir Dependencia (`pom.xml`)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

### Paso 4.2: Configurar Actuator (`application.yml`)

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,circuitbreakers,retries,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    circuitbreakers:
      enabled: true
```

---

### Paso 4.3: Crear Health Indicator de Servicio Externo (`infrastructure/health/`)

```java
@Component
@RequiredArgsConstructor
public class ValidacionServiceHealthIndicator implements HealthIndicator {

    private final RestTemplate restTemplate;

    @Value("${services.validacion.url}")
    private String validacionUrl;

    @Override
    public Health health() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                validacionUrl + "/actuator/health", String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                    .withDetail("service", "validacion-service")
                    .withDetail("url", validacionUrl)
                    .build();
            }
            return Health.down()
                .withDetail("service", "validacion-service")
                .withDetail("status", response.getStatusCode())
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("service", "validacion-service")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

---

### Paso 4.4: Crear Health Indicator de Circuit Breakers

```java
@Component
@RequiredArgsConstructor
public class CircuitBreakersHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry registry;

    @Override
    public Health health() {
        Map<String, String> states = registry.getAllCircuitBreakers().stream()
            .collect(Collectors.toMap(
                CircuitBreaker::getName,
                cb -> cb.getState().name()
            ));

        boolean allClosed = states.values().stream()
            .allMatch(state -> "CLOSED".equals(state));

        Health.Builder builder = allClosed ? Health.up() : Health.down();
        return builder.withDetails(states).build();
    }
}
```

---

### Paso 4.5: Verificar Ejercicio 4

```bash
# Health general
curl http://localhost:8081/actuator/health | jq

# Health de circuit breakers
curl http://localhost:8081/actuator/health/circuitBreakers | jq

# Metricas de resilience4j
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state

# Liveness y Readiness (para Kubernetes)
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness
```

---

# Resumen de Archivos por Ejercicio

| Ejercicio | Archivos Nuevos/Modificados |
|-----------|---------------------------|
| **1. Feign** | `pom.xml`, `application.yml`, `*ServiceClient.java`, `FeignConfig.java`, `CorrelationIdFilter.java`, DTOs cliente |
| **2. Resilience4j** | `pom.xml`, `application.yml`, `BusinessException.java`, `*Fallback.java`, `GlobalExceptionHandler.java`, `ResilienciaTestController.java` |
| **3. Idempotencia** | `IdempotencyRecord.java`, `IdempotencyRepository.java`, `IdempotencyService.java`, Controller modificado |
| **4. Actuator** | `pom.xml`, `application.yml`, `*HealthIndicator.java` |

---

# Comandos de Verificacion Final

```bash
# Compilar todo
mvn clean compile

# Ejecutar tests
mvn test

# Iniciar aplicacion
mvn spring-boot:run

# Verificar todos los endpoints
curl http://localhost:8081/actuator/health | jq
curl http://localhost:8081/api/test/resiliencia/circuit-breakers
```
