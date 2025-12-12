# Compilacion y Testing

```bash
# Compilar todo el proyecto
mvn clean install

# Solo compilar (sin tests)
mvn clean install -DskipTests

# Ejecutar tests de un servicio especifico
cd afiliado-service
mvn clean test
```

## Ejecucion

### Ejecutar un servicio individual

```bash
cd afiliado-service
mvn spring-boot:run
```

### Ejecutar todos los servicios (terminales separadas)

```bash
# Terminal 1
cd afiliado-service && mvn spring-boot:run

# Terminal 2
cd beneficio-service && mvn spring-boot:run

# Terminal 3
cd notificacion-service && mvn spring-boot:run

# Terminal 4
cd validacion-service && mvn spring-boot:run
```

## Endpoints de Health

```bash
curl http://localhost:8081/api/v1/actuator/health
curl http://localhost:8082/api/v1/actuator/health
curl http://localhost:8083/api/v1/actuator/health
curl http://localhost:8084/api/v1/actuator/health
```

## Consola H2

Cada servicio tiene una consola H2 disponible:
- http://localhost:8081/api/v1/h2-console
- http://localhost:8082/api/v1/h2-console
- http://localhost:8083/api/v1/h2-console
- http://localhost:8084/api/v1/h2-console

---

## API de Afiliados (puerto 8081)

### Crear afiliado

```bash
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{
    "dni": "12345678A",
    "nombre": "Juan",
    "apellidos": "Garcia Lopez",
    "fechaNacimiento": "1985-03-15",
    "email": "juan.garcia@email.com",
    "telefono": "+34612345678"
  }'
```

### Listar afiliados

```bash
curl http://localhost:8081/api/v1/afiliados
```

### Buscar por ID

```bash
curl http://localhost:8081/api/v1/afiliados/1
```

### Buscar por DNI

```bash
curl http://localhost:8081/api/v1/afiliados/dni/12345678A
```

### Activar afiliado

```bash
curl -X POST http://localhost:8081/api/v1/afiliados/1/activar
```

### Dar de baja

```bash
curl -X POST "http://localhost:8081/api/v1/afiliados/1/baja?motivo=Solicitud%20voluntaria"
```

---

## API de Beneficios (puerto 8082)

### Crear beneficio

```bash
curl -X POST http://localhost:8082/api/v1/beneficios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Subsidio por enfermedad",
    "descripcion": "Ayuda economica durante baja por enfermedad",
    "tipo": "SUBSIDIO_ENFERMEDAD",
    "montoMaximo": 1500.00,
    "diasCarencia": 30
  }'
```

### Listar beneficios

```bash
curl http://localhost:8082/api/v1/beneficios
```

### Listar solo activos

```bash
curl "http://localhost:8082/api/v1/beneficios?soloActivos=true"
```

### Buscar por tipo

```bash
curl "http://localhost:8082/api/v1/beneficios?tipo=SUBSIDIO_ENFERMEDAD"
```

### Activar/Desactivar beneficio

```bash
curl -X POST http://localhost:8082/api/v1/beneficios/1/activar
curl -X POST http://localhost:8082/api/v1/beneficios/1/desactivar
```

---

## API de Notificaciones (puerto 8083)

### Crear notificacion

```bash
curl -X POST http://localhost:8083/api/v1/notificaciones \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": "1",
    "nombreDestinatario": "Juan Garcia",
    "email": "juan.garcia@email.com",
    "asunto": "Bienvenido a la Mutualidad",
    "contenido": "Su solicitud de afiliacion ha sido aprobada.",
    "canal": "EMAIL"
  }'
```

### Listar notificaciones por afiliado

```bash
curl "http://localhost:8083/api/v1/notificaciones?afiliadoId=1"
```

### Listar por estado

```bash
curl "http://localhost:8083/api/v1/notificaciones?estado=PENDIENTE"
```

### Enviar notificacion

```bash
curl -X POST http://localhost:8083/api/v1/notificaciones/1/enviar
```

### Confirmar entrega

```bash
curl -X POST http://localhost:8083/api/v1/notificaciones/1/confirmar-entrega
```

### Confirmar lectura

```bash
curl -X POST http://localhost:8083/api/v1/notificaciones/1/confirmar-lectura
```

---

## API de Validaciones (puerto 8084)

### Crear validacion

```bash
curl -X POST http://localhost:8084/api/v1/validaciones \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": 1,
    "tipo": "IDENTIDAD",
    "datosEnviados": "{\"dni\": \"12345678A\", \"nombre\": \"Juan Garcia\"}"
  }'
```

### Listar validaciones por afiliado

```bash
curl "http://localhost:8084/api/v1/validaciones?afiliadoId=1"
```

### Listar por resultado

```bash
curl "http://localhost:8084/api/v1/validaciones?resultado=PENDIENTE"
```

### Buscar validacion vigente

```bash
curl "http://localhost:8084/api/v1/validaciones/vigente?afiliadoId=1&tipo=IDENTIDAD"
```

### Iniciar proceso de validacion

```bash
curl -X POST "http://localhost:8084/api/v1/validaciones/1/iniciar?proveedor=ValidadorDNI&referencia=REF-001"
```

### Aprobar validacion

```bash
curl -X POST "http://localhost:8084/api/v1/validaciones/1/aprobar?puntuacion=95&mensaje=Identidad%20verificada"
```

### Rechazar validacion

```bash
curl -X POST "http://localhost:8084/api/v1/validaciones/1/rechazar?mensaje=Datos%20incorrectos"
```
