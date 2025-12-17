# Guía de Testing de Patrones de Resiliencia

## Prerrequisitos

```bash
# Iniciar los servicios (desde mutualidad-platform-ddd-2)
cd module3/solutions/mutualidad-platform

# Terminal 1: Validación Service (puerto 8082)
cd validacion-service && mvn spring-boot:run

# Terminal 2: Notificación Service (puerto 8083)
cd notificacion-service && mvn spring-boot:run

# Terminal 3: Afiliado Service (puerto 8081)
cd afiliado-service && mvn spring-boot:run
```

---

## 1. Testing de Feign Client con Fallbacks

### 1.1 Escenario: Servicio disponible

```bash
# Validación funciona correctamente
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678D",
    "nombre": "Juan",
    "apellidos": "Garcia Lopez",
    "email": "juan.garcia@email.com",
    "fechaNacimiento": "1985-03-15"
  }' | json_pp
```

**Resultado esperado:** Afiliado creado con validación exitosa.

### 1.2 Escenario: Servicio caído (Fallback activado)

```bash
# 1. Detener validacion-service (Ctrl+C en terminal 1)

# 2. Intentar crear afiliado
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "87654321C",
    "nombre": "Ana",
    "apellidos": "Martinez",
    "email": "ana@email.com",
    "fechaNacimiento": "1985-03-20"
  }' | json_pp
```

**Resultado esperado:** Respuesta degradada indicando `SERVICE_DEGRADED` o similar.

---

## 2. Testing de Circuit Breaker

### 2.1 Verificar estado inicial

```bash
curl http://localhost:8081/api/v1/actuator/health | json_pp '.components.circuitBreakers' | json_pp
```

**Resultado esperado:** Estado `CLOSED` para todos los circuit breakers.

### 2.2 Forzar apertura del Circuit Breaker

```bash
# 1. Detener validacion-service

# 2. Ejecutar múltiples peticiones para superar el umbral de fallos
for i in {1..55}; do
  curl -s -X POST http://localhost:8081/api/v1/afiliados \
    -H "Content-Type: application/json" \
    -d '{"dni":"1234567'$i'A","nombre":"Test","apellidos":"User","email":"test@email.com","fechaNacimiento":"1990-01-01"}' &
done
wait

# 3. Verificar que el circuit breaker está OPEN
curl http://localhost:8081/api/v1/actuator/health | json_pp '.components.circuitBreakers'
```

**Resultado esperado:** Estado `OPEN` para `validacionService`.

### 2.3 Verificar transición a HALF_OPEN

```bash
# Esperar el tiempo configurado (waitDurationInOpenState)
sleep 30

# Verificar estado
curl http://localhost:8081/api/v1/actuator/health | json_pp '.components.circuitBreakers'
```

**Resultado esperado:** Estado `HALF_OPEN`.

### 2.4 Recuperación automática

```bash
# 1. Reiniciar validacion-service

# 2. Ejecutar peticiones de prueba
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{"dni":"99999999Z","nombre":"Recovery","apellidos":"Test","email":"recovery@email.com","fechaNacimiento":"1990-01-01"}'

# 3. Verificar que vuelve a CLOSED
curl http://localhost:8081/api/v1/actuator/health | json_pp '.components.circuitBreakers'
```

**Resultado esperado:** Estado `CLOSED` tras peticiones exitosas.

---

## 3. Testing de Retry

### 3.1 Simular fallo transitorio

```bash
# Usar el endpoint de test de resiliencia (si disponible)
curl http://localhost:8081/test/retry?failCount=2
```

**Verificar en logs:**
```
Intento 1 fallido, reintentando...
Intento 2 fallido, reintentando...
Intento 3 exitoso
```

### 3.2 Verificar métricas de retry

```bash
curl http://localhost:8081/api/v1/actuator/metrics/resilience4j.retry.calls | json_pp
```

---

## 4. Testing de Rate Limiter

### 4.1 Ejecutar ráfaga de peticiones

```bash
# Enviar 150 peticiones en paralelo (límite típico: 100/segundo)
for i in {1..150}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    http://localhost:8081/api/v1/afiliados/1 &
done | sort | uniq -c
```

**Resultado esperado:** 
- ~100 respuestas `200` (exitosas)
- ~50 respuestas `429` (rate limited)

### 4.2 Verificar métricas

```bash
curl http://localhost:8081/api/v1/actuator/metrics/resilience4j.ratelimiter.available.permissions | json_pp
```

---

## 5. Testing de Idempotencia

### 5.1 Enviar petición duplicada

```bash
# Primera petición con Idempotency-Key
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-001" \
  -d '{
    "dni": "11111111B",
    "nombre": "Idempotent",
    "apellidos": "Test",
    "email": "idem@email.com",
    "fechaNacimiento": "1990-01-01"
  }'

# Segunda petición con la misma key
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: test-key-001" \
  -d '{
    "dni": "11111111B",
    "nombre": "Idempotent",
    "apellidos": "Test",
    "email": "idem@email.com",
    "fechaNacimiento": "1990-01-01"
  }'
```

**Resultado esperado:** Ambas respuestas devuelven el mismo resultado, sin crear duplicados.

### 5.2 Verificar en base de datos

```bash
# Conectar a H2 Console (si habilitada)
# http://localhost:8081/h2-console

# Query: SELECT * FROM IDEMPOTENCY_RECORD WHERE KEY = 'test-key-001'
```

---

## 6. Testing de Correlation ID

### 6.1 Verificar propagación

```bash
# Enviar petición con Correlation ID
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: my-trace-12345" \
  -d '{
    "dni": "22222222B",
    "nombre": "Trace",
    "apellidos": "Test",
    "email": "trace@email.com",
    "fechaNacimiento": "1990-01-01"
  }' -v 2>&1 | grep -i correlation
```

**Resultado esperado:** Header `X-Correlation-ID` en la respuesta.

### 6.2 Verificar en logs

```bash
# Buscar en logs de cada servicio
grep "my-trace-12345" afiliado-service/logs/*.log
grep "my-trace-12345" validacion-service/logs/*.log
```

**Resultado esperado:** El mismo correlationId aparece en todos los servicios.

---

## 7. Testing de Health Indicators

### 7.1 Health general

```bash
curl http://localhost:8081/api/v1/actuator/health | json_pp
```

### 7.2 Health detallado

```bash
curl http://localhost:8081/api/v1/actuator/health/validacionService | json_pp
curl http://localhost:8081/api/v1/actuator/health/circuitBreakers | json_pp
```

### 7.3 Simular degradación

```bash
# 1. Detener validacion-service
# 2. Verificar health
curl http://localhost:8081/api/v1/actuator/health | json_pp
```

**Resultado esperado:** `"DEGRADED"` o `"DOWN"` dependiendo de configuración.

---

## 8. Testing de Manejo de Excepciones

### 8.1 Error de validación (400)

```bash
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{"dni": "INVALIDO", "nombre": "", "apellidos": ""}' | json_pp
```

**Resultado esperado:**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "DNI inválido",
  "timestamp": "..."
}
```

### 8.2 Recurso no encontrado (404)

```bash
curl http://localhost:8081/api/v1/afiliados/99999 | json_pp
```

**Resultado esperado:**
```json
{
  "code": "NOT_FOUND",
  "message": "Afiliado no encontrado"
}
```

### 8.3 Error de negocio (422)

```bash
# Intentar dar de baja un afiliado ya dado de baja
curl -X DELETE http://localhost:8081/api/v1/afiliados/1 
curl -X DELETE http://localhost:8081/api/v1/afiliados/1 | json_pp
```

**Resultado esperado:**
```json
{
  "code": "BUSINESS_ERROR",
  "message": "El afiliado ya está dado de baja"
}
```

