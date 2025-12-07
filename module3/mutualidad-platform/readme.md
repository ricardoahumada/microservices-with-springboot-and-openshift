# Modulo 3: Comunicacion Sincrona y Resiliencia

## Descripcion

Este modulo implementa patrones de resiliencia para comunicacion sincrona entre microservicios usando:

- **Feign Client**: Cliente HTTP declarativo
- **Resilience4j**: Circuit Breaker, Retry, Timeout
- **Idempotencia**: Garantia de operaciones seguras
- **Health Checks**: Monitoreo con Spring Boot Actuator

## Arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO DE ALTA DE AFILIADO                    │
│                                                                 │
│  ┌──────────────────┐    ┌──────────────────┐                  │
│  │ afiliado-service │───►│ validacion-serv. │                  │
│  │    :8081         │    │     :8084        │                  │
│  │                  │    │ (simula fallos)  │                  │
│  │  Circuit Breaker │    └──────────────────┘                  │
│  │  Retry           │                                          │
│  │  Timeout         │    ┌──────────────────┐                  │
│  │  Idempotencia    │───►│ beneficio-serv.  │                  │
│  │                  │    │     :8082        │                  │
│  │                  │    └──────────────────┘                  │
│  │                  │                                          │
│  │                  │    ┌──────────────────┐                  │
│  │                  │───►│ notificacion-s.  │                  │
│  │                  │    │     :8083        │                  │
│  └──────────────────┘    └──────────────────┘                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Proyectos Incluidos

| Servicio | Puerto | Descripcion |
|----------|--------|-------------|
| afiliado-service-resilient | 8081 | Orquestador con patrones de resiliencia |
| validacion-service | 8084 | Servicio de validacion con simulacion de fallos |
| beneficio-service | 8082 | Servicio de gestion de beneficios |
| notificacion-service | 8083 | Servicio de notificaciones |

---

## Compilacion y Ejecucion

### 1. Compilar todos los proyectos

```bash
cd module3/solutions/mutualidad-platform

# Compilar cada servicio
cd validacion-service && mvn clean package -DskipTests && cd ..
cd beneficio-service && mvn clean package -DskipTests && cd ..
cd notificacion-service && mvn clean package -DskipTests && cd ..
cd afiliado-service-resilient && mvn clean package -DskipTests && cd ..
```

### 2. Ejecutar los servicios (en terminales separadas)

```bash
# Terminal 1: Validacion Service
cd validacion-service
mvn spring-boot:run

# Terminal 2: Beneficio Service
cd beneficio-service
mvn spring-boot:run

# Terminal 3: Notificacion Service
cd notificacion-service
mvn spring-boot:run

# Terminal 4: Afiliado Service (el orquestador)
cd afiliado-service-resilient
mvn spring-boot:run
```

### 3. Verificar que todos estan corriendo

```bash
curl http://localhost:8084/api/validaciones/status
curl http://localhost:8082/api/beneficios/status
curl http://localhost:8083/api/notificaciones/status
curl http://localhost:8081/api/afiliados/status
```

**Resultado esperado** (para cada servicio):
```json
{"status":"UP","service":"<nombre-servicio>"}
```

---

## Guia de Ejercicios

### Ejercicio 1: Integracion con Feign Client (20 min)

**Objetivo**: Configurar comunicacion entre afiliado-service y validacion-service usando Feign.

**Archivos a revisar**:
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/client/ValidacionServiceClient.java`
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/config/FeignConfig.java`
- `afiliado-service-resilient/src/main/resources/application.yml`

**Prueba**:
```bash
# Alta de afiliado (caso exitoso)
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: test-feign-001" \
  -d '{
    "dni": "12345678A",
    "nombre": "Juan",
    "apellido": "Garcia",
    "email": "juan@email.com",
    "empresaId": "EMP001"
  }'
```

**Resultado esperado**:
```json
{
  "id": 1,
  "dni": "12345678A",
  "nombre": "Juan",
  "apellido": "Garcia",
  "email": "juan@email.com",
  "empresaId": "EMP001",
  "estado": "ACTIVO",
  "fechaAlta": "2025-12-08T01:30:00",
  "validacionLaboral": true,
  "beneficioAsignado": true,
  "notificacionEnviada": true
}
```

---

### Ejercicio 2: Configuracion de Resilience4j (25 min)

**Objetivo**: Implementar Circuit Breaker, Retry y Timeout.

**Configuracion en application.yml**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      validacionService:
        failureRateThreshold: 50
        slidingWindowSize: 10
        waitDurationInOpenState: 30s
  retry:
    instances:
      validacionService:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
  timelimiter:
    instances:
      validacionService:
        timeoutDuration: 3s
```

**Pruebas de resiliencia**:

#### 2.1 Prueba con TIMEOUT (servicio lento)
```bash
curl -X POST "http://localhost:8081/api/test/resiliencia/validacion?dni=12345678A&empresaId=EMP001&simulateError=TIMEOUT"
```

**Resultado esperado** (despues de ~3s por timeout):
```json
{
  "success": false,
  "message": "Timeout ejecutando validacion",
  "fallbackUsed": true,
  "errorType": "TIMEOUT"
}
```

#### 2.2 Prueba con ERROR 500
```bash
curl -X POST "http://localhost:8081/api/test/resiliencia/validacion?dni=12345678A&empresaId=EMP001&simulateError=ERROR_500"
```

**Resultado esperado** (despues de 3 reintentos):
```json
{
  "success": false,
  "message": "Servicio de validacion no disponible - usando fallback",
  "fallbackUsed": true,
  "errorType": "SERVICE_ERROR",
  "retryAttempts": 3
}
```

#### 2.3 Prueba con ERROR 503 (Service Unavailable)
```bash
curl -X POST "http://localhost:8081/api/test/resiliencia/validacion?dni=12345678A&empresaId=EMP001&simulateError=ERROR_503"
```

**Resultado esperado**:
```json
{
  "success": false,
  "message": "Servicio temporalmente no disponible",
  "fallbackUsed": true,
  "errorType": "SERVICE_UNAVAILABLE",
  "retryAttempts": 3
}
```

#### 2.4 Prueba con respuesta lenta (3s)
```bash
curl -X POST "http://localhost:8081/api/test/resiliencia/validacion?dni=12345678A&empresaId=EMP001&simulateError=SLOW"
```

**Resultado esperado** (puede completar o timeout dependiendo de configuracion):
```json
{
  "success": true,
  "message": "Validacion completada (respuesta lenta)",
  "durationMs": 3000,
  "validado": true
}
```

#### 2.5 Ver estado de Circuit Breakers
```bash
curl http://localhost:8081/api/test/resiliencia/circuit-breakers
```

**Resultado esperado**:
```json
{
  "circuitBreakers": {
    "validacionService": {
      "state": "CLOSED",
      "failureRate": 0.0,
      "numberOfFailedCalls": 0,
      "numberOfSuccessfulCalls": 5,
      "numberOfNotPermittedCalls": 0
    },
    "beneficioService": {
      "state": "CLOSED",
      "failureRate": 0.0
    },
    "notificacionService": {
      "state": "CLOSED",
      "failureRate": 0.0
    }
  }
}
```

#### 2.6 Forzar apertura de Circuit Breaker
```bash
curl -X POST http://localhost:8081/api/test/resiliencia/circuit-breakers/validacionService/force-open
```

**Resultado esperado**:
```json
{
  "circuitBreaker": "validacionService",
  "previousState": "CLOSED",
  "currentState": "FORCED_OPEN",
  "message": "Circuit breaker forzado a estado OPEN"
}
```

#### 2.7 Resetear Circuit Breaker
```bash
curl -X POST http://localhost:8081/api/test/resiliencia/circuit-breakers/validacionService/reset
```

**Resultado esperado**:
```json
{
  "circuitBreaker": "validacionService",
  "previousState": "FORCED_OPEN",
  "currentState": "CLOSED",
  "message": "Circuit breaker reseteado"
}
```

---

### Ejercicio 3: Implementacion de Idempotencia (20 min)

**Objetivo**: Garantizar operaciones seguras con X-Idempotency-Key.

**Archivos a revisar**:
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/idempotency/IdempotencyRecord.java`
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/idempotency/IdempotencyService.java`
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/controller/AfiliadoController.java`

**Pruebas**:

#### 3.1 Primera peticion con idempotency key
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-001" \
  -d '{
    "dni": "87654321B",
    "nombre": "Maria",
    "apellido": "Lopez",
    "email": "maria@email.com",
    "empresaId": "EMP002"
  }'
```

**Resultado esperado** (HTTP 201 Created):
```json
{
  "id": 2,
  "dni": "87654321B",
  "nombre": "Maria",
  "apellido": "Lopez",
  "email": "maria@email.com",
  "empresaId": "EMP002",
  "estado": "ACTIVO",
  "fechaAlta": "2025-12-08T01:35:00"
}
```

#### 3.2 Repetir con la misma key - debe devolver la respuesta cacheada
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-001" \
  -d '{
    "dni": "87654321B",
    "nombre": "Maria",
    "apellido": "Lopez",
    "email": "maria@email.com",
    "empresaId": "EMP002"
  }'
```

**Resultado esperado** (HTTP 200 OK - respuesta cacheada):
```json
{
  "id": 2,
  "dni": "87654321B",
  "nombre": "Maria",
  "apellido": "Lopez",
  "email": "maria@email.com",
  "empresaId": "EMP002",
  "estado": "ACTIVO",
  "fechaAlta": "2025-12-08T01:35:00",
  "_idempotent": true,
  "_cached": true
}
```

#### 3.3 Usar la misma key con datos diferentes - debe dar error
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: unique-key-001" \
  -d '{
    "dni": "99999999C",
    "nombre": "Otro",
    "apellido": "Nombre",
    "email": "otro@email.com",
    "empresaId": "EMP003"
  }'
```

**Resultado esperado** (HTTP 422 Unprocessable Entity):
```json
{
  "error": "IDEMPOTENCY_KEY_MISMATCH",
  "message": "La clave de idempotencia ya fue usada con datos diferentes",
  "idempotencyKey": "unique-key-001",
  "timestamp": "2025-12-08T01:36:00"
}
```

#### 3.4 Peticion sin idempotency key
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "11111111X",
    "nombre": "Sin",
    "apellido": "Key",
    "email": "sinkey@email.com",
    "empresaId": "EMP001"
  }'
```

**Resultado esperado** (HTTP 400 Bad Request):
```json
{
  "error": "MISSING_IDEMPOTENCY_KEY",
  "message": "Se requiere el header X-Idempotency-Key para operaciones POST",
  "timestamp": "2025-12-08T01:36:30"
}
```

---

### Ejercicio 4: Health Checks con Actuator (15 min)

**Objetivo**: Configurar health checks que reflejen el estado real del servicio.

**Archivos a revisar**:
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/health/ValidacionServiceHealthIndicator.java`
- `afiliado-service-resilient/src/main/java/com/mutualidad/afiliado/health/CircuitBreakersHealthIndicator.java`

**Pruebas**:

#### 4.1 Health general
```bash
curl http://localhost:8081/actuator/health | json_pp
```

**Resultado esperado** (todos los servicios UP):
```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "validacionService": "CLOSED",
        "beneficioService": "CLOSED",
        "notificacionService": "CLOSED"
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    },
    "validacionService": {
      "status": "UP",
      "details": {
        "url": "http://localhost:8084",
        "lastCheck": "2025-12-08T01:37:00"
      }
    }
  }
}
```

#### 4.2 Liveness probe
```bash
curl http://localhost:8081/actuator/health/liveness | json_pp
```

**Resultado esperado**:
```json
{
  "status": "UP"
}
```

#### 4.3 Readiness probe
```bash
curl http://localhost:8081/actuator/health/readiness | json_pp
```

**Resultado esperado** (cuando todo esta OK):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "readinessState": {
      "status": "UP"
    }
  }
}
```

**Resultado cuando Circuit Breaker esta abierto**:
```json
{
  "status": "DOWN",
  "components": {
    "circuitBreakers": {
      "status": "DOWN",
      "details": {
        "validacionService": "OPEN"
      }
    }
  }
}
```

#### 4.4 Metricas de Circuit Breakers
```bash
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state | json_pp
```

**Resultado esperado**:
```json
{
  "name": "resilience4j.circuitbreaker.state",
  "description": "The states of the circuit breaker",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 0.0
    }
  ],
  "availableTags": [
    {
      "tag": "name",
      "values": ["validacionService", "beneficioService", "notificacionService"]
    },
    {
      "tag": "state",
      "values": ["closed", "open", "half_open"]
    }
  ]
}
```

#### 4.5 Informacion de Circuit Breakers
```bash
curl http://localhost:8081/actuator/circuitbreakers | json_pp
```

**Resultado esperado**:
```json
{
  "circuitBreakers": {
    "validacionService": {
      "failureRate": "-1.0%",
      "slowCallRate": "-1.0%",
      "failureRateThreshold": "50.0%",
      "slowCallRateThreshold": "80.0%",
      "bufferedCalls": 0,
      "failedCalls": 0,
      "slowCalls": 0,
      "slowFailedCalls": 0,
      "notPermittedCalls": 0,
      "state": "CLOSED"
    }
  }
}
```

---

### Ejercicio Integrador (10 min)

**Criterios de aceptacion a verificar**:

#### 5.1 Alta de afiliado funciona en caso feliz
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: integrador-001" \
  -d '{"dni":"11111111A","nombre":"Test","apellido":"User","email":"test@email.com","empresaId":"EMP001"}'
```

**Resultado esperado** (HTTP 201):
```json
{
  "id": 3,
  "dni": "11111111A",
  "nombre": "Test",
  "apellido": "User",
  "email": "test@email.com",
  "empresaId": "EMP001",
  "estado": "ACTIVO",
  "validacionLaboral": true,
  "beneficioAsignado": true,
  "notificacionEnviada": true
}
```

#### 5.2 Verificar que el afiliado fue creado
```bash
curl http://localhost:8081/api/afiliados/dni/11111111A
```

**Resultado esperado**:
```json
{
  "id": 3,
  "dni": "11111111A",
  "nombre": "Test",
  "apellido": "User",
  "email": "test@email.com",
  "empresaId": "EMP001",
  "estado": "ACTIVO",
  "fechaAlta": "2025-12-08T01:37:00"
}
```

#### 5.3 Simular multiples fallos para abrir Circuit Breaker
```bash
for i in {1..10}; do
  curl -s -X POST "http://localhost:8081/api/test/resiliencia/validacion?dni=test&empresaId=EMP&simulateError=ERROR_500"
  echo ""
done
```

**Resultado esperado** (ultimas llamadas):
```json
{
  "success": false,
  "message": "Circuit breaker OPEN - llamada no permitida",
  "fallbackUsed": true,
  "circuitBreakerState": "OPEN"
}
```

#### 5.4 Verificar que Circuit Breaker esta abierto
```bash
curl http://localhost:8081/api/test/resiliencia/circuit-breakers
```

**Resultado esperado**:
```json
{
  "circuitBreakers": {
    "validacionService": {
      "state": "OPEN",
      "failureRate": 100.0,
      "numberOfFailedCalls": 10,
      "numberOfNotPermittedCalls": 5
    }
  }
}
```

#### 5.5 Verificar que el fallback permite continuar
```bash
curl -X POST http://localhost:8081/api/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Idempotency-Key: fallback-test-001" \
  -d '{"dni":"22222222B","nombre":"Fallback","apellido":"Test","email":"fb@email.com","empresaId":"EMP001"}'
```

**Resultado esperado** (operacion exitosa con fallback):
```json
{
  "id": 4,
  "dni": "22222222B",
  "nombre": "Fallback",
  "apellido": "Test",
  "email": "fb@email.com",
  "empresaId": "EMP001",
  "estado": "PENDIENTE_VALIDACION",
  "validacionLaboral": false,
  "validacionFallback": true,
  "message": "Afiliado creado con validacion pendiente (fallback activo)"
}
```

#### 5.6 Health check refleja estado del Circuit Breaker
```bash
curl http://localhost:8081/actuator/health/readiness | json_pp
```

**Resultado esperado** (readiness DOWN por CB abierto):
```json
{
  "status": "DOWN",
  "components": {
    "circuitBreakers": {
      "status": "DOWN",
      "details": {
        "validacionService": "OPEN",
        "reason": "Circuit breaker abierto - servicio degradado"
      }
    }
  }
}
```

#### 5.7 Resetear para siguientes pruebas
```bash
curl -X POST http://localhost:8081/api/test/resiliencia/circuit-breakers/validacionService/reset
```

**Resultado esperado**:
```json
{
  "circuitBreaker": "validacionService",
  "previousState": "OPEN",
  "currentState": "CLOSED",
  "message": "Circuit breaker reseteado exitosamente"
}
```

---

## Endpoints de Validacion Service (Simulacion de Fallos)

El servicio de validacion permite simular diferentes escenarios:

| simulateError | Comportamiento | Resultado HTTP |
|---------------|----------------|----------------|
| `NONE` o vacio | Respuesta normal exitosa | 200 OK |
| `TIMEOUT` | Espera 10 segundos (provoca timeout) | Timeout del cliente |
| `ERROR_500` | Error interno del servidor | 500 Internal Server Error |
| `ERROR_503` | Servicio no disponible | 503 Service Unavailable |
| `ERROR_400` | Peticion invalida (no se reintenta) | 400 Bad Request |
| `SLOW` | Respuesta lenta (3 segundos) | 200 OK (lento) |
| `INTERMITTENT` | Falla cada 3 peticiones | 500/200 alternados |

---

## Configuracion de Resilience4j Explicada

```yaml
resilience4j:
  circuitbreaker:
    instances:
      validacionService:
        slidingWindowSize: 10          # Ventana de 10 llamadas para calcular metricas
        minimumNumberOfCalls: 5        # Minimo de llamadas antes de evaluar
        failureRateThreshold: 50       # Se abre si >50% fallan
        slowCallRateThreshold: 80      # Se abre si >80% son lentas
        slowCallDurationThreshold: 2s  # Define "lenta" como >2s
        waitDurationInOpenState: 30s   # Tiempo en estado OPEN antes de HALF-OPEN
        permittedNumberOfCallsInHalfOpenState: 3  # Llamadas de prueba en HALF-OPEN
        automaticTransitionFromOpenToHalfOpenEnabled: true
        
  retry:
    instances:
      validacionService:
        maxAttempts: 3                 # Maximo 3 intentos
        waitDuration: 1s               # Espera inicial entre intentos
        enableExponentialBackoff: true # Backoff exponencial
        exponentialBackoffMultiplier: 2 # 1s -> 2s -> 4s
        retryExceptions:               # Solo reintentar estos errores
          - java.io.IOException
          - feign.FeignException.ServiceUnavailable
        ignoreExceptions:              # NO reintentar estos errores
          - feign.FeignException.BadRequest
          
  timelimiter:
    instances:
      validacionService:
        timeoutDuration: 3s            # Timeout de 3 segundos
        cancelRunningFuture: true      # Cancelar si excede timeout
```

---

## Orden de Aplicacion de Patrones

```
Peticion
    │
    ▼
┌──────────────────┐
│ CIRCUIT BREAKER  │ ← Primero: ¿Esta abierto? Si -> Fallback
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│     RETRY        │ ← Segundo: Si falla, reintentar
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│    TIMEOUT       │ ← Tercero: Limite de tiempo por intento
└────────┬─────────┘
         │
         ▼
    Llamada HTTP
```

---

## Verificacion de Logs

Los logs incluyen el Correlation ID para trazabilidad:

```bash
# Ver logs con correlacion
tail -f logs/afiliado-service.log | grep correlationId
```

Formato de log esperado:
```
2025-12-08 10:30:45 [abc12345] INFO  - Iniciando alta de afiliado DNI: 12345678A
2025-12-08 10:30:45 [abc12345] INFO  - Llamando a servicio de validacion...
2025-12-08 10:30:46 [abc12345] INFO  - Validacion exitosa para DNI: 12345678A
2025-12-08 10:30:46 [abc12345] INFO  - Asignando beneficio...
2025-12-08 10:30:47 [abc12345] INFO  - Enviando notificacion...
2025-12-08 10:30:47 [abc12345] INFO  - Alta completada exitosamente
```

Log con fallback:
```
2025-12-08 10:30:45 [def67890] INFO  - Iniciando alta de afiliado DNI: 87654321B
2025-12-08 10:30:45 [def67890] INFO  - Llamando a servicio de validacion...
2025-12-08 10:30:46 [def67890] WARN  - FALLBACK validacion - servicio no disponible
2025-12-08 10:30:46 [def67890] INFO  - Continuando con validacion pendiente
2025-12-08 10:30:47 [def67890] INFO  - Alta completada con estado PENDIENTE_VALIDACION
```

---

## Troubleshooting

### Circuit Breaker siempre abierto
```bash
# Resetear el circuit breaker
curl -X POST http://localhost:8081/api/test/resiliencia/circuit-breakers/validacionService/reset
```
**Resultado esperado**: `{"currentState": "CLOSED"}`

### Servicio no responde
```bash
# Verificar que todos los servicios estan corriendo
curl http://localhost:8084/api/validaciones/status  # Validacion
curl http://localhost:8082/api/beneficios/status    # Beneficio
curl http://localhost:8083/api/notificaciones/status # Notificacion
curl http://localhost:8081/api/afiliados/status     # Afiliado
```
**Resultado esperado**: `{"status":"UP"}` para cada servicio

### Ver metricas de Resilience4j
```bash
curl http://localhost:8081/actuator/metrics | json_pp '.names | map(select(contains("resilience4j")))'
```
**Resultado esperado**:
```json
[
  "resilience4j.circuitbreaker.calls",
  "resilience4j.circuitbreaker.failure.rate",
  "resilience4j.circuitbreaker.state",
  "resilience4j.retry.calls"
]
```

### Puerto ya en uso
```bash
# Encontrar proceso usando el puerto
netstat -ano | findstr :8081  # Windows
lsof -i :8081                 # Linux/Mac

# Matar proceso
taskkill /PID <pid> /F        # Windows
kill -9 <pid>                 # Linux/Mac
```

---

## Autor

**Curso de Microservicios con Spring Boot 2.7 y OpenShift**
