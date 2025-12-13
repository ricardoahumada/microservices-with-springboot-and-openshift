# Guía de Evolución: Añadir Patrones de Resiliencia a Arquitectura DDD

## Objetivo
Esta guía muestra cómo evolucionar el proyecto `mutualidad-platform` desde una estructura DDD básica hacia una arquitectura DDD con patrones de resiliencia para comunicación entre microservicios.

---

## Estructura Inicial (Module 1)

El módulo 1 ya implementa una estructura DDD básica:

```
com.mutualidad.afiliado/
├── api/
│   ├── controller/
│   │   └── AfiliadoController.java
│   └── dto/
│       ├── AfiliadoRequest.java
│       └── AfiliadoResponse.java
├── application/
│   └── service/
│       └── AfiliadoService.java
├── domain/
│   └── model/
│       ├── Afiliado.java
│       ├── DNI.java
│       └── EstadoAfiliado.java
├── infrastructure/
│   └── persistence/
│       └── AfiliadoJpaRepository.java
└── AfiliadoServiceApplication.java
```

## Estructura Final con Resiliencia (Module 3)

El módulo 3 extiende la estructura DDD añadiendo componentes de resiliencia:

```
com.mutualidad.afiliado/
├── api/
│   ├── controller/
│   │   ├── AfiliadoController.java
│   │   └── ResilienciaTestController.java      # NUEVO
│   └── dto/
│       ├── AfiliadoResponse.java
│       ├── AltaAfiliadoRequest.java            # NUEVO
│       └── BeneficioDto.java                   # NUEVO
├── application/
│   └── service/
│       └── AfiliadoService.java                # MODIFICADO
├── domain/
│   ├── model/
│   │   └── Afiliado.java
│   └── exception/                              # NUEVO
│       └── BusinessException.java
└── infrastructure/
    ├── client/                                 # NUEVO
    │   ├── dto/
    │   │   ├── NotificacionResponse.java
    │   │   ├── ValidacionRequest.java
    │   │   └── ValidacionResponse.java
    │   ├── BeneficioServiceClient.java
    │   ├── BeneficioServiceClientFallback.java
    │   ├── NotificacionServiceClient.java
    │   ├── NotificacionServiceClientFallback.java
    │   ├── ValidacionServiceClient.java
    │   └── ValidacionServiceClientFallback.java
    ├── config/                                 # NUEVO
    │   ├── FeignConfig.java
    │   └── GlobalExceptionHandler.java
    ├── filter/                                 # NUEVO
    │   └── CorrelationIdFilter.java
    ├── health/                                 # NUEVO
    │   ├── CircuitBreakersHealthIndicator.java
    │   └── ValidacionServiceHealthIndicator.java
    ├── idempotency/                            # NUEVO
    │   ├── IdempotencyRecord.java
    │   ├── IdempotencyRepository.java
    │   └── IdempotencyService.java
    └── persistence/
        └── AfiliadoRepository.java
```


---

## Diagrama de Flujo con Resiliencia

```
┌─────────────┐    ┌─────────────────────────────────────────────────────────┐
│   Cliente   │───>│                  afiliado-service                       │
└─────────────┘    │  ┌─────────────┐   ┌──────────────────────────────────┐ │
                   │  │ Controller  │──>│         AfiliadoService          │ │
                   │  └─────────────┘   │  ┌────────────────────────────┐  │ │
                   │                    │  │    @CircuitBreaker         │  │ │
                   │                    │  │    @Retry                  │  │ │
                   │                    │  │    @TimeLimiter            │  │ │
                   │                    │  └────────────────────────────┘  │ │
                   │                    └──────────────┬───────────────────┘ │
                   └───────────────────────────────────┼─────────────────────┘
                                                       │
                   ┌───────────────────────────────────┤
                   │                                   ▼                      
         ┌─────────┴─────────┐           ┌─────────────┴─────────┐            
         ▼                   ▼           ▼                       ▼            
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐ ┌────────────────┐
│  validacion-    │ │   beneficio-    │ │  notificacion-  │ │   Fallback     │
│    service      │ │    service      │ │    service      │ │   Response     │
│   :8084         │ │    :8082        │ │    :8083        │ │  (si falla)    │
└─────────────────┘ └─────────────────┘ └─────────────────┘ └────────────────┘
```

---

## Beneficios de la Evolución

1. **Tolerancia a fallos**: El servicio sigue funcionando aunque fallen dependencias
2. **Degradación elegante**: Respuestas fallback en lugar de errores 500
3. **Protección de recursos**: Circuit Breaker evita saturar servicios en problemas
4. **Reintentos inteligentes**: Recuperación automática de fallos transitorios
5. **Observabilidad**: Health checks y métricas de resiliencia
6. **Trazabilidad**: Correlation IDs para debugging distribuido
7. **Idempotencia**: Seguridad ante reintentos duplicados

---

## Orden de Migración por Servicio

1. **afiliado-service** → `afiliado-service-resilient` (orquestador, requiere todos los patrones)
2. **beneficio-service** (consumido por afiliado)
3. **validacion-service** (consumido por afiliado)
4. **notificacion-service** (consumido por afiliado)

---
