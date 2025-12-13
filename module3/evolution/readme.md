# Guia de Evolucion: Servicio Base a Servicio Resiliente

> **Objetivo**: Transformar el `afiliado-service` base hacia una version con patrones de resiliencia completos.

---

## Punto de Partida

Servicio base aislado (`mutualidad-platform-base/afiliado-service`):

```
com.mutualidad.afiliado/
├── AfiliadoServiceApplication.java
├── controller/
│   └── AfiliadoController.java
├── service/
│   └── AfiliadoService.java
├── dto/
│   ├── AltaAfiliadoRequest.java
│   └── AfiliadoResponse.java
├── entity/
│   └── Afiliado.java
├── repository/
│   └── AfiliadoRepository.java
├── config/
│   └── GlobalExceptionHandler.java
└── exception/
    └── BusinessException.java
```

**Caracteristicas**: Servicio aislado, sin comunicacion externa, sin resiliencia.

---

## Objetivo Final

Servicio resiliente (`mutualidad-platform/afiliado-service-resilient`):

```
com.mutualidad.afiliado/
├── AfiliadoServiceApplication.java        # + @EnableFeignClients
├── controller/
│   ├── AfiliadoController.java            # + Idempotencia
│   └── ResilienciaTestController.java     # NUEVO
├── service/
│   └── AfiliadoService.java               # + Feign + Resiliencia
├── dto/
│   ├── AltaAfiliadoRequest.java
│   ├── AfiliadoResponse.java              # + List<BeneficioDto>
│   └── BeneficioDto.java                  # NUEVO
├── entity/
│   └── Afiliado.java
├── repository/
│   └── AfiliadoRepository.java
│
├── client/                                # NUEVO - Comunicacion
│   ├── ValidacionServiceClient.java
│   ├── ValidacionServiceClientFallback.java
│   ├── NotificacionServiceClient.java
│   ├── NotificacionServiceClientFallback.java
│   ├── BeneficioServiceClient.java
│   ├── BeneficioServiceClientFallback.java
│   └── dto/
│       ├── ValidacionRequest.java
│       ├── ValidacionResponse.java
│       └── NotificacionResponse.java
│
├── idempotency/                           # NUEVO - Idempotencia
│   ├── IdempotencyService.java
│   ├── IdempotencyRepository.java
│   └── IdempotencyRecord.java
│
├── health/                                # NUEVO - Observabilidad
│   ├── CircuitBreakersHealthIndicator.java
│   └── ValidacionServiceHealthIndicator.java
│
├── filter/                                # NUEVO - Trazabilidad
│   └── CorrelationIdFilter.java
│
├── config/                                # Actualizado
│   ├── FeignConfig.java                   # NUEVO
│   └── GlobalExceptionHandler.java        # + Circuit Breaker
│
└── exception/
    └── BusinessException.java
```

---

## Diagrama de Arquitectura Resiliente

```
                                    ┌─────────────────┐
                                    │   Load Balancer │
                                    └────────┬────────┘
                                             │
                    ┌────────────────────────▼────────────────────────┐
                    │              AFILIADO-SERVICE-RESILIENT         │
                    │                                                 │
                    │  ┌──────────────────────────────────────────┐   │
                    │  │           CorrelationIdFilter            │   │
                    │  │         (X-Correlation-ID)               │   │
                    │  └──────────────────┬───────────────────────┘   │
                    │                     │                           │
                    │  ┌──────────────────▼───────────────────────┐   │
                    │  │           AfiliadoController             │   │
                    │  │         + IdempotencyService             │   │
                    │  └──────────────────┬───────────────────────┘   │
                    │                     │                           │
                    │  ┌──────────────────▼───────────────────────┐   │
                    │  │            AfiliadoService               │   │
                    │  │    @CircuitBreaker @Retry @TimeLimiter   │   │
                    │  └──────────────────┬───────────────────────┘   │
                    │                     │                           │
                    │         ┌───────────┼───────────┐               │
                    │         │           │           │               │
                    │         ▼           ▼           ▼               │
                    │  ┌───────────┐ ┌─────────┐ ┌────────────┐       │
                    │  │Validacion │ │Beneficio│ │Notificacion│       │
                    │  │  Client   │ │ Client  │ │   Client   │       │
                    │  │+Fallback  │ │+Fallback│ │ +Fallback  │       │
                    │  └─────┬─────┘ └────┬────┘ └──────┬─────┘       │
                    │        │            │             │             │
                    └────────┼────────────┼─────────────┼─────────────┘
                             │            │             │
              ┌──────────────▼──┐   ┌─────▼─────┐  ┌────▼───────────┐
              │   Validacion    │   │ Beneficio │  │  Notificacion  │
              │    Service      │   │  Service  │  │    Service     │
              │    :8084        │   │   :8082   │  │     :8083      │
              └─────────────────┘   └───────────┘  └────────────────┘
```

---

## Patrones de Resiliencia Implementados

### 1. **Circuit Breaker**
```
Estado CLOSED → Llamadas normales
     ↓ (fallos > umbral)
Estado OPEN → Fallback inmediato (sin llamar)
     ↓ (tiempo espera)
Estado HALF_OPEN → Prueba con algunas llamadas
     ↓ (exito)
Estado CLOSED
```

### 2. **Retry**
- Reintentos automaticos ante fallos transitorios
- Backoff exponencial entre reintentos
- Maximo configurable de intentos

### 3. **Timeout (TimeLimiter)**
- Tiempo maximo de espera por respuesta
- Evita bloqueos por servicios lentos
- Dispara fallback al expirar

### 4. **Fallback**
- Respuesta alternativa cuando falla el servicio
- Degradacion elegante del sistema
- Sistema sigue funcionando con datos por defecto

### 5. **Idempotencia**
- Operaciones seguras de reintentar
- Cache de respuestas por X-Idempotency-Key
- Evita duplicados en operaciones criticas

### 6. **Correlation ID**
- Trazabilidad end-to-end
- Propagacion entre servicios
- Facilita debugging distribuido
