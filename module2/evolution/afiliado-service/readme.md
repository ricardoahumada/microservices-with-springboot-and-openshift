---
AIGC:
    ContentProducer: Minimax Agent AI
    ContentPropagator: Minimax Agent AI
    Label: AIGC
    ProduceID: "00000000000000000000000000000000"
    PropagateID: "00000000000000000000000000000000"
    ReservedCode1: 3046022100822cb3f03ad1759689c39be65fc305e95c7cf96195298799870ea23f651832a9022100bf4824eea60c695f19857e4a541d635f08aae63b79293eb81dca441782cf6c3b
    ReservedCode2: 3046022100ea6a7754fd6ba23f45b16811ffc19ff31b6f33af9a610ec583b8d27840069465022100b0f021a91473ea0bd87fce1f06b87850fd54cdbc09afb98c9118543f968af162
---

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

## Evolución Paso a Paso

### FASE 1: Enriquecer el Dominio

> **Principio Hexagonal**: El dominio es el núcleo puro, sin dependencias externas. Debe contener eventos y excepciones propias.

---

#### Paso 1.1: Crear `TipoDocumento.java` (Enum)

**Paquete**: `domain.model`

**¿Por qué?**
- Define los tipos de documento válidos: DNI, NIE, PASAPORTE
- Reemplaza el simple String del DNI por un modelo más rico
- Base para el nuevo Value Object `Documento`

---

#### Paso 1.2: Crear `Documento.java` (Value Object)

**Paquete**: `domain.model`

**¿Por qué?**
- Evolución del `DNI.java` original a un concepto más genérico
- Soporta múltiples tipos de documento (DNI, NIE, Pasaporte)
- Validación específica según el tipo

---

#### Paso 1.3: Actualizar `Afiliado.java`

**Paquete**: `domain.model`

**¿Por qué?**
- Usar `Documento` en lugar de `DNI`
- Añadir generación de Domain Events
- Eliminar anotaciones JPA (se mueven a Entity en infrastructure)

---

#### Paso 1.4: Crear paquete `domain.event`

**¿Por qué?** Los Domain Events permiten comunicación desacoplada entre bounded contexts.

---

#### Paso 1.5: Crear `AfiliadoEvent.java` (Clase base)

**Paquete**: `domain.event`

**¿Por qué?**
- Clase abstracta base para todos los eventos de afiliado
- Contiene campos comunes: id, timestamp, afiliadoId

---

#### Paso 1.6: Crear eventos específicos

**Paquete**: `domain.event`

| Clase | Descripción |
|-------|-------------|
| `AfiliadoRegistradoEvent.java` | Cuando se registra un nuevo afiliado |
| `AfiliadoActivadoEvent.java` | Cuando se activa un afiliado pendiente |
| `AfiliadoDadoDeBajaEvent.java` | Cuando se da de baja un afiliado |
| `AfiliadoReactivadoEvent.java` | Cuando se reactiva un afiliado |

**Orden**: Después de `AfiliadoEvent.java` porque heredan de él.

---

#### Paso 1.7: Crear paquete `domain.exception`

**¿Por qué?** Excepciones de dominio específicas permiten un manejo de errores más expresivo.

---

#### Paso 1.8: Crear excepciones de dominio

**Paquete**: `domain.exception`

| Clase | Orden | Descripción |
|-------|-------|-------------|
| `AfiliadoException.java` | 1 | Clase base para excepciones de afiliado |
| `AfiliadoNoEncontradoException.java` | 2 | Afiliado no existe |
| `AfiliadoYaExisteException.java` | 3 | Afiliado duplicado |
| `DocumentoInvalidoException.java` | 4 | Documento con formato incorrecto |
| `EstadoInvalidoException.java` | 5 | Transición de estado no permitida |

---

### FASE 2: Definir Puertos (Contratos)

> **Principio Hexagonal**: Los puertos son interfaces que definen cómo el núcleo se comunica con el exterior. Son parte de la aplicación, no de la infraestructura.

---

#### Paso 2.1: Crear paquete `application.port.input`

**¿Por qué?** Los puertos de entrada definen los casos de uso que el exterior puede invocar.

---

#### Paso 2.2: Crear `AfiliadoUseCase.java` (Puerto de entrada)

**Paquete**: `application.port.input`

**¿Por qué?**
- Define el contrato de los casos de uso disponibles
- Métodos: `registrar()`, `activar()`, `darDeBaja()`, `reactivar()`, `buscar()`
- Es una interfaz, la implementación está en el Application Service

**Importancia**: Este es el punto de entrada al hexágono desde el exterior.

---

#### Paso 2.3: Crear paquete `application.port.output`

**¿Por qué?** Los puertos de salida definen lo que el núcleo necesita del exterior (persistencia, servicios externos, etc.)

---

#### Paso 2.4: Crear `AfiliadoRepository.java` (Puerto de salida)

**Paquete**: `application.port.output`

**¿Por qué?**
- Define el contrato de persistencia
- Métodos: `save()`, `findById()`, `findByDocumento()`, `existsByDocumento()`
- Es una interfaz, la implementación está en infrastructure

**Diferencia con Module 1**: Ahora es un puerto explícito, no un JpaRepository directamente.

---

#### Paso 2.5: Crear `NotificacionPort.java` (Puerto de salida)

**Paquete**: `application.port.output`

**¿Por qué?**
- Define contrato para envío de notificaciones
- Permite cambiar implementación sin afectar dominio (email, SMS, log)

---

#### Paso 2.6: Crear `ValidacionExternaPort.java` (Puerto de salida)

**Paquete**: `application.port.output`

**¿Por qué?**
- Define contrato para validaciones con servicios externos
- Ejemplo: validar documento contra sistema externo

---

#### Paso 2.7: Crear `EventPublisherPort.java` (Puerto de salida)

**Paquete**: `application.port.output`

**¿Por qué?**
- Define contrato para publicar Domain Events
- Permite cambiar implementación (memoria, Kafka, RabbitMQ)

---

### FASE 3: Application Layer (Orquestación)

> **Principio Hexagonal**: El Application Service implementa los puertos de entrada y usa los puertos de salida.

---

#### Paso 3.1: Crear DTOs de aplicación

**Paquete**: `application.dto`

| Clase | Descripción |
|-------|-------------|
| `AfiliadoDTO.java` | DTO genérico para transferir datos de afiliado |
| `RegistrarAfiliadoCommand.java` | Comando para registrar nuevo afiliado |

---

#### Paso 3.2: Crear `AfiliadoMapper.java`

**Paquete**: `application.service`

**¿Por qué?**
- Convierte entre Afiliado (dominio) y AfiliadoDTO
- Centraliza la lógica de mapeo

---

#### Paso 3.3: Crear `AfiliadoApplicationService.java`

**Paquete**: `application.service`

**¿Por qué?**
- Implementa `AfiliadoUseCase` (puerto de entrada)
- Inyecta los puertos de salida (repository, notificación, eventos)
- Orquesta los casos de uso

**Diferencia con Module 1**: 
- Implementa una interfaz explícita
- Usa puertos de salida en lugar de implementaciones concretas

---

### FASE 4: Adaptadores de Entrada (Driving Adapters)

> **Principio Hexagonal**: Los adaptadores de entrada traducen las peticiones externas a llamadas al puerto de entrada.

---

#### Paso 4.1: Crear paquete `infrastructure.adapter.input.rest`

---

#### Paso 4.2: Crear Request DTOs

**Paquete**: `infrastructure.adapter.input.rest`

| Clase | Descripción |
|-------|-------------|
| `RegistrarAfiliadoRequest.java` | Request para registrar afiliado |
| `ActualizarContactoRequest.java` | Request para actualizar contacto |
| `DarDeBajaRequest.java` | Request para dar de baja |

**¿Por qué separados de application.dto?** Son específicos del adaptador REST.

---

#### Paso 4.3: Crear `AfiliadoController.java`

**Paquete**: `infrastructure.adapter.input.rest`

**¿Por qué?**
- Adaptador REST que traduce HTTP a casos de uso
- Inyecta `AfiliadoUseCase` (interfaz), no la implementación
- Usa sus propios Request DTOs

---

### FASE 5: Adaptadores de Salida (Driven Adapters)

> **Principio Hexagonal**: Los adaptadores de salida implementan los puertos de salida con tecnologías concretas.

---

#### Paso 5.1: Crear paquete `infrastructure.adapter.output.persistence`

---

#### Paso 5.2: Crear `AfiliadoEntity.java`

**Paquete**: `infrastructure.adapter.output.persistence`

**¿Por qué?**
- Entidad JPA separada del modelo de dominio
- Contiene anotaciones de persistencia (@Entity, @Table, etc.)

**Diferencia con Module 1**: El dominio `Afiliado` ya no tiene anotaciones JPA.

---

#### Paso 5.3: Crear `AfiliadoJpaRepository.java`

**Paquete**: `infrastructure.adapter.output.persistence`

**¿Por qué?**
- Interfaz Spring Data JPA para `AfiliadoEntity`
- Es un detalle de implementación, no un puerto

---

#### Paso 5.4: Crear `AfiliadoPersistenceMapper.java`

**Paquete**: `infrastructure.adapter.output.persistence`

**¿Por qué?**
- Convierte entre `Afiliado` (dominio) y `AfiliadoEntity` (JPA)
- Aísla el mapeo de persistencia

---

#### Paso 5.5: Crear `AfiliadoJpaAdapter.java`

**Paquete**: `infrastructure.adapter.output.persistence`

**¿Por qué?**
- Implementa `AfiliadoRepository` (puerto de salida)
- Usa `AfiliadoJpaRepository` internamente
- Traduce entre dominio y entidad JPA

---

#### Paso 5.6: Crear adaptadores de notificación

**Paquete**: `infrastructure.adapter.output.notification`

| Clase | Descripción |
|-------|-------------|
| `LogNotificationAdapter.java` | Implementa `NotificacionPort` escribiendo a log |

---

#### Paso 5.7: Crear adaptadores externos

**Paquete**: `infrastructure.adapter.output.external`

| Clase | Descripción |
|-------|-------------|
| `ValidacionRestAdapter.java` | Implementa `ValidacionExternaPort` via REST |

---

#### Paso 5.8: Crear adaptadores de eventos

**Paquete**: `infrastructure.adapter.output.event`

| Clase | Descripción |
|-------|-------------|
| `InMemoryEventPublisher.java` | Implementa `EventPublisherPort` en memoria |

---

### FASE 6: Configuración

---

#### Paso 6.1: Crear `GlobalExceptionHandler.java`

**Paquete**: `infrastructure.config`

**¿Por qué?**
- Maneja excepciones de dominio y las traduce a respuestas HTTP
- Captura `AfiliadoNoEncontradoException` → 404
- Captura `AfiliadoYaExisteException` → 409

---

#### Paso 6.2: Crear `RestTemplateConfig.java`

**Paquete**: `infrastructure.config`

**¿Por qué?**
- Configuración de RestTemplate para llamadas a servicios externos
- Usado por `ValidacionRestAdapter`

---

### FASE 7: Tests

---

#### Paso 7.1: Crear `DocumentoTest.java`

**Paquete**: `domain.model` (en src/test)

**¿Por qué?**
- Valida el nuevo Value Object `Documento`
- Tests para DNI, NIE, Pasaporte

---

---

## Resumen del Orden de Creación

| Fase | Orden | Paquete | Clase |
|------|-------|---------|-------|
| 1 | 1 | domain.model | TipoDocumento.java |
| 1 | 2 | domain.model | Documento.java |
| 1 | 3 | domain.model | Afiliado.java (actualizar) |
| 1 | 4 | domain.event | AfiliadoEvent.java |
| 1 | 5-8 | domain.event | *Event.java (4 eventos) |
| 1 | 9 | domain.exception | AfiliadoException.java |
| 1 | 10-13 | domain.exception | *Exception.java (4 excepciones) |
| 2 | 14 | application.port.input | AfiliadoUseCase.java |
| 2 | 15 | application.port.output | AfiliadoRepository.java |
| 2 | 16-18 | application.port.output | *Port.java (3 puertos) |
| 3 | 19-20 | application.dto | AfiliadoDTO, Command |
| 3 | 21 | application.service | AfiliadoMapper.java |
| 3 | 22 | application.service | AfiliadoApplicationService.java |
| 4 | 23-25 | infrastructure.adapter.input.rest | *Request.java (3 requests) |
| 4 | 26 | infrastructure.adapter.input.rest | AfiliadoController.java |
| 5 | 27 | infrastructure.adapter.output.persistence | AfiliadoEntity.java |
| 5 | 28 | infrastructure.adapter.output.persistence | AfiliadoJpaRepository.java |
| 5 | 29 | infrastructure.adapter.output.persistence | AfiliadoPersistenceMapper.java |
| 5 | 30 | infrastructure.adapter.output.persistence | AfiliadoJpaAdapter.java |
| 5 | 31 | infrastructure.adapter.output.notification | LogNotificationAdapter.java |
| 5 | 32 | infrastructure.adapter.output.external | ValidacionRestAdapter.java |
| 5 | 33 | infrastructure.adapter.output.event | InMemoryEventPublisher.java |
| 6 | 34 | infrastructure.config | GlobalExceptionHandler.java |
| 6 | 35 | infrastructure.config | RestTemplateConfig.java |
| 7 | 36 | test | DocumentoTest.java |

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
│                              NÚCLEO                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                    APPLICATION SERVICE                               │  │
│  │  ┌─────────────────────────────────────────────────────────────┐    │  │
│  │  │              AfiliadoApplicationService                      │    │  │
│  │  └─────────────────────────────────────────────────────────────┘    │  │
│  └─────────────────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────────────────┐  │
│  │                         DOMINIO                                      │  │
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
