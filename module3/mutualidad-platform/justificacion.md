# Justificación de Patrones de Resiliencia

## Introducción

Este documento justifica la implementación de patrones de resiliencia en la plataforma de mutualidad, explicando el propósito, beneficios y casos de uso de cada patrón aplicado.

---

## 1. Feign Client con Fallbacks

### Clases involucradas
- `ValidacionServiceClient.java`
- `ValidacionServiceClientFallback.java`
- `NotificacionServiceClient.java`
- `NotificacionServiceClientFallback.java`
- `FeignConfig.java`
- `FeignErrorDecoder.java`

### Justificación

**Problema:** La comunicación directa entre microservicios mediante RestTemplate es propensa a errores, difícil de mantener y no proporciona mecanismos de recuperación ante fallos.

**Solución:** Feign Client proporciona:

| Beneficio | Descripción |
|-----------|-------------|
| **Declarativo** | Define contratos de API como interfaces Java |
| **Integración nativa** | Se integra con Spring Cloud y Resilience4j |
| **Fallbacks** | Permite definir comportamientos alternativos cuando el servicio destino falla |
| **Configuración centralizada** | Timeouts, reintentos y codificación de errores en un solo lugar |

**Ejemplo de uso:**
```java
@FeignClient(name = "validacion-service", 
             fallback = ValidacionServiceClientFallback.class)
public interface ValidacionServiceClient {
    @PostMapping("/validaciones/afiliado")
    ValidacionResponse validarAfiliado(@RequestBody ValidacionRequest request);
}
```

**Fallback en acción:** Cuando `validacion-service` no responde, el fallback devuelve un resultado degradado que permite al sistema continuar operando.

---

## 2. Circuit Breaker (Resilience4j)

### Clases involucradas
- `CircuitBreakersHealthIndicator.java`
- Configuración en `application.yml`

### Justificación

**Problema:** Cuando un servicio downstream falla, las peticiones continúan enviándose, consumiendo recursos y degradando el rendimiento general del sistema.

**Solución:** El patrón Circuit Breaker:

| Estado | Comportamiento |
|--------|----------------|
| **CLOSED** | Peticiones fluyen normalmente |
| **OPEN** | Peticiones fallan inmediatamente (fail-fast) |
| **HALF_OPEN** | Permite peticiones de prueba para verificar recuperación |

**Configuración típica:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      validacionService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 3
```

**Beneficios:**
- **Fail-fast:** Evita esperas innecesarias
- **Protección de recursos:** No satura conexiones hacia servicios caídos
- **Auto-recuperación:** Detecta automáticamente cuando el servicio vuelve a estar disponible

---

## 3. Retry Pattern

### Justificación

**Problema:** Fallos transitorios (timeouts de red, alta carga momentánea) pueden resolverse automáticamente si se reintenta la operación.

**Solución:** Reintentos configurables con backoff exponencial:

```yaml
resilience4j:
  retry:
    instances:
      validacionService:
        maxAttempts: 3
        waitDuration: 500ms
        retryExceptions:
          - java.io.IOException
          - feign.FeignException.ServiceUnavailable
```

**Consideraciones:**
- Solo reintentar operaciones **idempotentes**
- Usar **backoff exponencial** para evitar thundering herd
- Definir **excepciones específicas** que justifican reintento

---

## 4. Rate Limiter

### Justificación

**Problema:** Picos de tráfico pueden saturar servicios downstream, causando cascadas de fallos.

**Solución:** Limitar la tasa de peticiones por período:

```yaml
resilience4j:
  ratelimiter:
    instances:
      validacionService:
        limitForPeriod: 100
        limitRefreshPeriod: 1s
        timeoutDuration: 500ms
```

**Beneficios:**
- **Protección proactiva:** Evita saturación antes de que ocurra
- **Fairness:** Distribuye capacidad equitativamente
- **Predecibilidad:** Comportamiento consistente bajo carga

---

## 5. Idempotencia

### Clases involucradas
- `IdempotencyService.java`
- `IdempotencyRepository.java`
- `IdempotencyRecord.java`

### Justificación

**Problema:** Los reintentos y la comunicación asíncrona pueden causar procesamiento duplicado de operaciones (doble cobro, doble registro, etc.).

**Solución:** Garantizar que operaciones repetidas produzcan el mismo resultado:

```java
@Service
public class IdempotencyService {
    public <T> T executeIdempotent(String key, Supplier<T> operation) {
        Optional<IdempotencyRecord> existing = repository.findByKey(key);
        if (existing.isPresent()) {
            return deserialize(existing.get().getResponse());
        }
        T result = operation.get();
        repository.save(new IdempotencyRecord(key, serialize(result)));
        return result;
    }
}
```

**Casos de uso críticos:**
| Operación | Riesgo sin idempotencia |
|-----------|-------------------------|
| Registro de afiliado | Duplicación de registros |
| Envío de notificación | Spam al usuario |
| Procesamiento de solicitud | Doble procesamiento |

---

## 6. Correlation ID (Trazabilidad Distribuida)

### Clases involucradas
- `CorrelationIdFilter.java`

### Justificación

**Problema:** En sistemas distribuidos, una petición atraviesa múltiples servicios, dificultando el diagnóstico de problemas.

**Solución:** Propagar un identificador único a través de toda la cadena:

```java
@Component
public class CorrelationIdFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) {
        String correlationId = getOrCreateCorrelationId(request);
        MDC.put("correlationId", correlationId);
        // Propagar en headers para llamadas downstream
    }
}
```

**Beneficios:**
- **Trazabilidad end-to-end:** Seguir una petición a través de todos los servicios
- **Debugging eficiente:** Filtrar logs por correlationId
- **Análisis de latencia:** Identificar cuellos de botella

---

## 7. Health Indicators Personalizados

### Clases involucradas
- `ValidacionServiceHealthIndicator.java`
- `CircuitBreakersHealthIndicator.java`

### Justificación

**Problema:** Los health checks básicos (UP/DOWN) no reflejan el estado real de las dependencias ni la capacidad de procesar peticiones.

**Solución:** Health indicators que verifican:

| Indicador | Verificación |
|-----------|--------------|
| `ValidacionServiceHealthIndicator` | Conectividad con servicio de validación |
| `CircuitBreakersHealthIndicator` | Estado de circuit breakers (CLOSED/OPEN) |

```java
@Component
public class CircuitBreakersHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        Map<String, String> states = getCircuitBreakerStates();
        boolean allClosed = states.values().stream()
            .allMatch(s -> s.equals("CLOSED"));
        return allClosed ? Health.up().build() 
                         : Health.status("DEGRADED").withDetails(states).build();
    }
}
```

**Uso en orquestación:**
- **Kubernetes:** Liveness/Readiness probes
- **Load Balancers:** Remover instancias degradadas del pool
- **Alerting:** Notificar cuando servicios entran en estado degradado

---

## 8. Manejo Global de Excepciones

### Clases involucradas
- `GlobalExceptionHandler.java`
- `BusinessException.java`

### Justificación

**Problema:** Excepciones no controladas exponen detalles internos y generan respuestas inconsistentes.

**Solución:** Centralizar el manejo de errores:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "Error interno del servidor"));
    }
}
```

**Beneficios:**
- **Consistencia:** Formato de error uniforme
- **Seguridad:** No expone stack traces ni detalles internos
- **Observabilidad:** Logging centralizado de errores

---

## Resumen de Patrones

| Patrón | Problema que resuelve | Beneficio principal |
|--------|----------------------|---------------------|
| Feign + Fallback | Comunicación entre servicios | Degradación elegante |
| Circuit Breaker | Cascada de fallos | Fail-fast y auto-recuperación |
| Retry | Fallos transitorios | Recuperación automática |
| Rate Limiter | Saturación | Protección proactiva |
| Idempotencia | Duplicación | Consistencia de datos |
| Correlation ID | Trazabilidad | Debugging distribuido |
| Health Indicators | Monitoreo | Observabilidad avanzada |
| Exception Handler | Errores inconsistentes | Respuestas uniformes |

---

## Conclusión

La implementación de estos patrones transforma un sistema de microservicios frágil en una arquitectura resiliente capaz de:

1. **Degradarse elegantemente** ante fallos parciales
2. **Recuperarse automáticamente** de fallos transitorios
3. **Proteger recursos** evitando cascadas de fallos
4. **Mantener consistencia** incluso con reintentos
5. **Facilitar el diagnóstico** mediante trazabilidad completa

Estos patrones son fundamentales para sistemas de producción que requieren alta disponibilidad y confiabilidad.
