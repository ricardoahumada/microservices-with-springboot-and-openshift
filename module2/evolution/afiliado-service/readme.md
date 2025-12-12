# Guía de Evolución: DDD Básico a Arquitectura Hexagonal

> **Objetivo**: Transformar el `afiliado-service` desde una estructura DDD clásica hacia Arquitectura Hexagonal (Ports & Adapters).

---

## Punto de Partida (Module 1)

Estructura DDD básica (`mutualidad-platform-complete/afiliado-service`):

```
com.mutualidad.afiliado/
├── AfiliadoServiceApplication.java
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

---

## Objetivo Final (Module 2)

Arquitectura Hexagonal completa (`afiliado-service-hexagonal`):

```
com.mutualidad.afiliado/
├── AfiliadoServiceApplication.java
├── domain/
│   ├── model/
│   │   ├── Afiliado.java
│   │   ├── Documento.java (Value Object)
│   │   ├── TipoDocumento.java
│   │   └── EstadoAfiliado.java
│   ├── event/
│   │   ├── AfiliadoEvent.java (Base)
│   │   ├── AfiliadoRegistradoEvent.java
│   │   ├── AfiliadoActivadoEvent.java
│   │   ├── AfiliadoDadoDeBajaEvent.java
│   │   └── AfiliadoReactivadoEvent.java
│   └── exception/
│       ├── AfiliadoException.java (Base)
│       ├── AfiliadoNoEncontradoException.java
│       ├── AfiliadoYaExisteException.java
│       ├── DocumentoInvalidoException.java
│       └── EstadoInvalidoException.java
├── application/
│   ├── port/
│   │   ├── input/
│   │   │   └── AfiliadoUseCase.java (Puerto de entrada)
│   │   └── output/
│   │       ├── AfiliadoRepository.java (Puerto de salida)
│   │       ├── NotificacionPort.java
│   │       ├── ValidacionExternaPort.java
│   │       └── EventPublisherPort.java
│   ├── service/
│   │   ├── AfiliadoApplicationService.java
│   │   └── AfiliadoMapper.java
│   └── dto/
│       ├── AfiliadoDTO.java
│       └── RegistrarAfiliadoCommand.java
└── infrastructure/
    ├── adapter/
    │   ├── input/
    │   │   └── rest/
    │   │       ├── AfiliadoController.java
    │   │       ├── RegistrarAfiliadoRequest.java
    │   │       ├── ActualizarContactoRequest.java
    │   │       └── DarDeBajaRequest.java
    │   └── output/
    │       ├── persistence/
    │       │   ├── AfiliadoEntity.java
    │       │   ├── AfiliadoJpaRepository.java
    │       │   ├── AfiliadoJpaAdapter.java
    │       │   └── AfiliadoPersistenceMapper.java
    │       ├── notification/
    │       │   └── LogNotificationAdapter.java
    │       ├── external/
    │       │   └── ValidacionRestAdapter.java
    │       └── event/
    │           └── InMemoryEventPublisher.java
    └── config/
        ├── GlobalExceptionHandler.java
        └── RestTemplateConfig.java
```

---

## Diagrama de Arquitectura Hexagonal

```
                    ┌─────────────────────────────────────────┐
                    │         ADAPTADORES DE ENTRADA          │
                    │  ┌─────────────────────────────────┐    │
                    │  │    AfiliadoController (REST)    │    │
                    │  └──────────────┬──────────────────┘    │
                    └─────────────────┼───────────────────────┘
                                      │ usa
                    ┌─────────────────▼───────────────────────┐
                    │         PUERTO DE ENTRADA               │
                    │  ┌─────────────────────────────────┐    │
                    │  │        AfiliadoUseCase          │    │
                    │  └─────────────────────────────────┘    │
                    └─────────────────┬───────────────────────┘
                                      │ implementa
┌───────────────────────────────────────────────────────────────────────────┐
│                              NÚCLEO                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    APPLICATION SERVICE                              │  │
│  │  ┌─────────────────────────────────────────────────────────────┐    │  │
│  │  │              AfiliadoApplicationService                     │    │  │
│  │  └─────────────────────────────────────────────────────────────┘    │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                         DOMINIO                                     │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐   │  │
│  │  │   Afiliado   │  │  Documento   │  │    Domain Events         │   │  │
│  │  │  (Aggregate) │  │    (VO)      │  │  (Registrado, Activado)  │   │  │
│  │  └──────────────┘  └──────────────┘  └──────────────────────────┘   │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────┘
                                      │ usa
                    ┌─────────────────▼───────────────────────┐
                    │         PUERTOS DE SALIDA               │
                    │  ┌─────────────┐ ┌─────────────────┐    │
                    │  │ Repository  │ │ NotificacionPort│    │
                    │  └─────────────┘ └─────────────────┘    │
                    │  ┌─────────────┐ ┌─────────────────┐    │
                    │  │ EventPort   │ │ ValidacionPort  │    │
                    │  └─────────────┘ └─────────────────┘    │
                    └─────────────────┬───────────────────────┘
                                      │ implementan
                    ┌─────────────────▼───────────────────────┐
                    │         ADAPTADORES DE SALIDA           │
                    │  ┌─────────────────────────────────┐    │
                    │  │   AfiliadoJpaAdapter (BD)       │    │
                    │  │   LogNotificationAdapter        │    │
                    │  │   ValidacionRestAdapter (HTTP)  │    │
                    │  │   InMemoryEventPublisher        │    │
                    │  └─────────────────────────────────┘    │
                    └─────────────────────────────────────────┘
```

---

## Principios Clave Aplicados

### 1. **Dependency Inversion**
- El núcleo define interfaces (puertos)
- La infraestructura implementa esas interfaces (adaptadores)
- Las dependencias apuntan hacia adentro

### 2. **Dominio Puro**
- Sin anotaciones de frameworks
- Sin dependencias de infraestructura
- Solo lógica de negocio

### 3. **Testeabilidad**
- Puertos permiten mocks fácilmente
- Dominio testeable sin Spring
- Adaptadores testeables de forma aislada

### 4. **Flexibilidad**
- Cambiar base de datos: solo el adaptador de persistencia
- Cambiar notificaciones: solo el adaptador de notificación
- Añadir nuevo canal: crear nuevo adaptador de entrada

---

## Ejecución

### Desarrollo (H2 en memoria)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Tests
```bash
mvn test -Dspring.profiles.active=test
```

### Producción
```bash
java -jar target/afiliado-service-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DB_HOST=db.mutualidad.com \
  --DB_USER=afiliado_user \
  --DB_PASSWORD=secret
```

## API Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/afiliados` | Registrar nuevo afiliado |
| GET | `/api/v1/afiliados/{id}` | Consultar por ID |
| GET | `/api/v1/afiliados/documento/{tipo}/{numero}` | Consultar por documento |
| POST | `/api/v1/afiliados/{id}/baja` | Dar de baja |
| POST | `/api/v1/afiliados/{id}/reactivar` | Reactivar |
| PUT | `/api/v1/afiliados/{id}/contacto` | Actualizar contacto |

## Ejemplos de Uso

### Registrar Afiliado

```bash
curl -X POST http://localhost:8080/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{
    "tipoDocumento": "DNI",
    "numeroDocumento": "12345678Z",
    "nombre": "Juan",
    "primerApellido": "García",
    "segundoApellido": "López",
    "fechaNacimiento": "1990-05-15",
    "email": "juan@email.com",
    "telefono": "600123456",
    "direccion": "Calle Mayor 1",
    "codigoPostal": "28001",
    "provincia": "Madrid",
    "codigoEmpresa": "EMP001"
  }'
```

### Consultar por Documento

```bash
curl http://localhost:8080/api/v1/afiliados/documento/DNI/12345678Z
```

### Dar de Baja

```bash
curl -X POST http://localhost:8080/api/v1/afiliados/{id}/baja \
  -H "Content-Type: application/json" \
  -d '{"motivo": "Cambio de mutualidad"}'
```