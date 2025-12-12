# Guía de Evolución: DDD Básico a Arquitectura CQRS

> **Objetivo**: Transformar el `beneficio-service` desde una estructura DDD clásica hacia Arquitectura CQRS (Command Query Responsibility Segregation).

---

## Punto de Partida (Module 1)

Estructura DDD básica (`mutualidad-platform-complete/beneficio-service`):

```
com.mutualidad.beneficio/
├── BeneficioServiceApplication.java
├── api/
│   ├── controller/BeneficioController.java
│   └── dto/
│       ├── BeneficioRequest.java
│       └── BeneficioResponse.java
├── application/
│   └── service/
│       ├── BeneficioService.java
│       └── SolicitudService.java
├── domain/
│   └── model/
│       ├── Beneficio.java
│       ├── Solicitud.java
│       ├── TipoBeneficio.java
│       └── EstadoSolicitud.java
└── infrastructure/
    └── persistence/
        ├── BeneficioJpaRepository.java
        └── SolicitudJpaRepository.java
```

---

## Objetivo Final (Module 2)

Arquitectura CQRS completa (`beneficio-service-cqrs`):

```
com.mutualidad.beneficio/
├── BeneficioServiceCqrsApplication.java
│
├── command/                           # LADO DE ESCRITURA
│   ├── api/
│   │   ├── AsignarBeneficioCommand.java
│   │   ├── ModificarBeneficioCommand.java
│   │   ├── RevocarBeneficioCommand.java
│   │   ├── SuspenderBeneficioCommand.java
│   │   ├── ReactivarBeneficioCommand.java
│   │   ├── TipoBeneficio.java
│   │   └── CommandResult.java
│   ├── handler/
│   │   ├── AsignarBeneficioHandler.java
│   │   ├── ModificarBeneficioHandler.java
│   │   └── RevocarBeneficioHandler.java
│   ├── domain/
│   │   ├── Beneficio.java
│   │   ├── BeneficioAggregate.java
│   │   └── EstadoBeneficio.java
│   └── repository/
│       └── BeneficioWriteRepository.java
│
├── query/                             # LADO DE LECTURA
│   ├── api/
│   │   └── BuscarBeneficiosQuery.java
│   ├── handler/
│   │   ├── BuscarBeneficiosHandler.java
│   │   └── ResumenBeneficiosHandler.java
│   ├── model/
│   │   ├── BeneficioReadModel.java
│   │   └── ResumenBeneficiosReadModel.java
│   └── repository/
│       └── BeneficioReadRepository.java
│
├── event/                             # EVENTOS DE DOMINIO
│   ├── BeneficioEvent.java (Base)
│   ├── BeneficioAsignadoEvent.java
│   ├── BeneficioModificadoEvent.java
│   ├── BeneficioRevocadoEvent.java
│   └── EventPublisher.java
│
└── infrastructure/
    ├── rest/
    │   ├── BeneficioCommandController.java
    │   └── BeneficioQueryController.java
    ├── persistence/
    │   ├── BeneficioEntity.java
    │   ├── BeneficioJpaRepository.java
    │   ├── BeneficioEntityMapper.java
    │   └── BeneficioWriteJpaAdapter.java
    ├── event/
    │   ├── InMemoryEventPublisher.java
    │   └── BeneficioProjection.java
    └── config/
        └── GlobalExceptionHandler.java
```

---

## Evolución Paso a Paso

### FASE 1: Definir Eventos de Dominio

> **Principio CQRS**: Los eventos son el puente entre Commands y Queries. Los Commands generan eventos, las Queries se actualizan con esos eventos.

---

#### Paso 1.1: Crear paquete `event`

**¿Por qué?** En CQRS, los eventos son ciudadanos de primera clase. Merecen su propio paquete al nivel superior.

---

#### Paso 1.2: Crear `BeneficioEvent.java` (Clase base)

**Paquete**: `event`

**¿Por qué?**
- Clase abstracta base para todos los eventos
- Campos comunes: eventId, timestamp, beneficioId, afiliadoId

---

#### Paso 1.3: Crear eventos específicos

**Paquete**: `event`

| Clase | Orden | Descripción |
|-------|-------|-------------|
| `BeneficioAsignadoEvent.java` | 2 | Cuando se asigna un nuevo beneficio |
| `BeneficioModificadoEvent.java` | 3 | Cuando se modifica un beneficio |
| `BeneficioRevocadoEvent.java` | 4 | Cuando se revoca un beneficio |

---

#### Paso 1.4: Crear `EventPublisher.java` (Interfaz)

**Paquete**: `event`

**¿Por qué?**
- Define el contrato para publicar eventos
- Permite diferentes implementaciones (memoria, Kafka, RabbitMQ)

---

### FASE 2: Lado de Comandos (Write Side)

> **Principio CQRS**: El lado de comandos se encarga de las operaciones de escritura. Recibe Commands, ejecuta lógica de negocio y genera Events.

---

#### Paso 2.1: Crear paquete `command.api`

**¿Por qué?** Los Commands son objetos inmutables que representan intenciones de cambio.

---

#### Paso 2.2: Crear `TipoBeneficio.java` (Enum)

**Paquete**: `command.api`

**¿Por qué?**
- Define tipos de beneficio: MEDICO, DENTAL, FARMACIA, HOSPITALIZACION, OPTICA
- Movido desde domain.model al lado de comandos

---

#### Paso 2.3: Crear Commands

**Paquete**: `command.api`

| Clase | Orden | Descripción |
|-------|-------|-------------|
| `AsignarBeneficioCommand.java` | 1 | Comando para asignar nuevo beneficio |
| `ModificarBeneficioCommand.java` | 2 | Comando para modificar beneficio existente |
| `RevocarBeneficioCommand.java` | 3 | Comando para revocar beneficio |
| `SuspenderBeneficioCommand.java` | 4 | Comando para suspender temporalmente |
| `ReactivarBeneficioCommand.java` | 5 | Comando para reactivar beneficio |

**Características de un Command**:
- Inmutable
- Representa una intención de cambio
- Puede ser rechazado si viola reglas de negocio

---

#### Paso 2.4: Crear `CommandResult.java`

**Paquete**: `command.api`

**¿Por qué?**
- Respuesta estándar de un command handler
- Contiene: success, id generado, mensaje de error

---

#### Paso 2.5: Crear paquete `command.domain`

**¿Por qué?** El modelo de dominio del lado de escritura.

---

#### Paso 2.6: Crear `EstadoBeneficio.java` (Enum)

**Paquete**: `command.domain`

**¿Por qué?**
- Estados del beneficio: ACTIVO, SUSPENDIDO, REVOCADO
- Específico del modelo de escritura

---

#### Paso 2.7: Crear `Beneficio.java` (Entity)

**Paquete**: `command.domain`

**¿Por qué?**
- Entidad de dominio del lado de escritura
- Contiene la lógica de negocio para modificar el beneficio
- Genera eventos cuando cambia de estado

---

#### Paso 2.8: Crear `BeneficioAggregate.java`

**Paquete**: `command.domain`

**¿Por qué?**
- Aggregate Root que encapsula la lógica de negocio
- Coordina entidades dentro del agregado
- Genera eventos de dominio

**Diferencia con Module 1**: Ahora genera eventos explícitamente.

---

#### Paso 2.9: Crear paquete `command.repository`

---

#### Paso 2.10: Crear `BeneficioWriteRepository.java` (Interfaz)

**Paquete**: `command.repository`

**¿Por qué?**
- Repositorio específico para escritura
- Métodos: `save()`, `findById()`, `findByAfiliadoId()`
- Optimizado para operaciones de escritura

---

#### Paso 2.11: Crear paquete `command.handler`

**¿Por qué?** Los handlers procesan commands y aplican lógica de negocio.

---

#### Paso 2.12: Crear Command Handlers

**Paquete**: `command.handler`

| Clase | Orden | Descripción |
|-------|-------|-------------|
| `AsignarBeneficioHandler.java` | 1 | Procesa AsignarBeneficioCommand |
| `ModificarBeneficioHandler.java` | 2 | Procesa ModificarBeneficioCommand |
| `RevocarBeneficioHandler.java` | 3 | Procesa RevocarBeneficioCommand |

**Responsabilidades de un Handler**:
1. Validar el command
2. Cargar el agregado del repositorio
3. Ejecutar la lógica de negocio
4. Persistir los cambios
5. Publicar eventos generados

---

### FASE 3: Lado de Consultas (Read Side)

> **Principio CQRS**: El lado de queries se encarga de las lecturas. Usa modelos optimizados para consulta, actualizados por eventos.

---

#### Paso 3.1: Crear paquete `query.api`

**¿Por qué?** Los Queries representan solicitudes de información.

---

#### Paso 3.2: Crear `BuscarBeneficiosQuery.java`

**Paquete**: `query.api`

**¿Por qué?**
- Representa una consulta de beneficios
- Parámetros: afiliadoId, tipo, estado, fechas
- Inmutable

---

#### Paso 3.3: Crear paquete `query.model`

**¿Por qué?** Los Read Models son DTOs optimizados para consulta.

---

#### Paso 3.4: Crear Read Models

**Paquete**: `query.model`

| Clase | Orden | Descripción |
|-------|-------|-------------|
| `BeneficioReadModel.java` | 1 | DTO plano para un beneficio |
| `ResumenBeneficiosReadModel.java` | 2 | DTO agregado con estadísticas |

**Diferencia con Write Model**:
- Sin lógica de negocio
- Campos desnormalizados para consulta rápida
- Puede incluir datos precalculados

---

#### Paso 3.5: Crear paquete `query.repository`

---

#### Paso 3.6: Crear `BeneficioReadRepository.java` (Interfaz)

**Paquete**: `query.repository`

**¿Por qué?**
- Repositorio específico para lectura
- Métodos de consulta optimizados
- Puede usar base de datos diferente al Write

---

#### Paso 3.7: Crear paquete `query.handler`

---

#### Paso 3.8: Crear Query Handlers

**Paquete**: `query.handler`

| Clase | Orden | Descripción |
|-------|-------|-------------|
| `BuscarBeneficiosHandler.java` | 1 | Procesa BuscarBeneficiosQuery |
| `ResumenBeneficiosHandler.java` | 2 | Genera resúmenes y estadísticas |

**Diferencia con Command Handler**:
- Solo lectura, nunca modifica estado
- Retorna Read Models
- Puede usar caché

---

### FASE 4: Infraestructura - Persistencia

---

#### Paso 4.1: Crear paquete `infrastructure.persistence`

---

#### Paso 4.2: Crear `BeneficioEntity.java`

**Paquete**: `infrastructure.persistence`

**¿Por qué?**
- Entidad JPA separada del modelo de dominio
- Sirve tanto para Write como para Read (en este ejemplo)

---

#### Paso 4.3: Crear `BeneficioJpaRepository.java`

**Paquete**: `infrastructure.persistence`

**¿Por qué?**
- Spring Data JPA Repository
- Métodos de acceso a datos

---

#### Paso 4.4: Crear `BeneficioEntityMapper.java`

**Paquete**: `infrastructure.persistence`

**¿Por qué?**
- Convierte entre Beneficio (dominio) y BeneficioEntity (JPA)
- Convierte a BeneficioReadModel para consultas

---

#### Paso 4.5: Crear `BeneficioWriteJpaAdapter.java`

**Paquete**: `infrastructure.persistence`

**¿Por qué?**
- Implementa `BeneficioWriteRepository`
- Adapta JPA al contrato del dominio

---

### FASE 5: Infraestructura - Eventos

---

#### Paso 5.1: Crear paquete `infrastructure.event`

---

#### Paso 5.2: Crear `InMemoryEventPublisher.java`

**Paquete**: `infrastructure.event`

**¿Por qué?**
- Implementa `EventPublisher`
- Publicación síncrona en memoria (para desarrollo)
- En producción: Kafka, RabbitMQ

---

#### Paso 5.3: Crear `BeneficioProjection.java`

**Paquete**: `infrastructure.event`

**¿Por qué?**
- Escucha eventos y actualiza el Read Model
- Mantiene sincronizado el lado de lectura
- Patrón Projection en CQRS

**Funcionamiento**:
1. Recibe `BeneficioAsignadoEvent`
2. Crea/actualiza entrada en Read Model
3. El Query Handler lee datos actualizados

---

### FASE 6: Infraestructura - REST Controllers

---

#### Paso 6.1: Crear paquete `infrastructure.rest`

---

#### Paso 6.2: Crear `BeneficioCommandController.java`

**Paquete**: `infrastructure.rest`

**¿Por qué?**
- Controller específico para operaciones de escritura
- Endpoints POST, PUT, DELETE
- Traduce HTTP a Commands

**Endpoints**:
- `POST /api/beneficios/commands/asignar`
- `PUT /api/beneficios/commands/{id}/modificar`
- `DELETE /api/beneficios/commands/{id}/revocar`

---

#### Paso 6.3: Crear `BeneficioQueryController.java`

**Paquete**: `infrastructure.rest`

**¿Por qué?**
- Controller específico para operaciones de lectura
- Endpoints GET
- Traduce HTTP a Queries

**Endpoints**:
- `GET /api/beneficios/queries/afiliado/{afiliadoId}`
- `GET /api/beneficios/queries/{id}`
- `GET /api/beneficios/queries/resumen/{afiliadoId}`

---

### FASE 7: Configuración

---

#### Paso 7.1: Crear `GlobalExceptionHandler.java`

**Paquete**: `infrastructure.config`

**¿Por qué?**
- Manejo centralizado de excepciones
- Traduce excepciones de dominio a respuestas HTTP

---

---

## Resumen del Orden de Creación

| Fase | Orden | Paquete | Clase |
|------|-------|---------|-------|
| 1 | 1 | event | BeneficioEvent.java |
| 1 | 2-4 | event | *Event.java (3 eventos) |
| 1 | 5 | event | EventPublisher.java |
| 2 | 6 | command.api | TipoBeneficio.java |
| 2 | 7-11 | command.api | *Command.java (5 commands) |
| 2 | 12 | command.api | CommandResult.java |
| 2 | 13 | command.domain | EstadoBeneficio.java |
| 2 | 14 | command.domain | Beneficio.java |
| 2 | 15 | command.domain | BeneficioAggregate.java |
| 2 | 16 | command.repository | BeneficioWriteRepository.java |
| 2 | 17-19 | command.handler | *Handler.java (3 handlers) |
| 3 | 20 | query.api | BuscarBeneficiosQuery.java |
| 3 | 21-22 | query.model | *ReadModel.java (2 modelos) |
| 3 | 23 | query.repository | BeneficioReadRepository.java |
| 3 | 24-25 | query.handler | *Handler.java (2 handlers) |
| 4 | 26 | infrastructure.persistence | BeneficioEntity.java |
| 4 | 27 | infrastructure.persistence | BeneficioJpaRepository.java |
| 4 | 28 | infrastructure.persistence | BeneficioEntityMapper.java |
| 4 | 29 | infrastructure.persistence | BeneficioWriteJpaAdapter.java |
| 5 | 30 | infrastructure.event | InMemoryEventPublisher.java |
| 5 | 31 | infrastructure.event | BeneficioProjection.java |
| 6 | 32 | infrastructure.rest | BeneficioCommandController.java |
| 6 | 33 | infrastructure.rest | BeneficioQueryController.java |
| 7 | 34 | infrastructure.config | GlobalExceptionHandler.java |

---

## Diagrama de Arquitectura CQRS

```
                         ┌─────────────────────────────────────┐
                         │           REST CONTROLLERS          │
                         │  ┌───────────────┐ ┌─────────────┐  │
                         │  │ CommandController│ │QueryController│ │
                         │  └───────┬───────┘ └──────┬──────┘  │
                         └──────────┼────────────────┼─────────┘
                                    │                │
                    ┌───────────────▼───┐        ┌───▼───────────────┐
                    │                   │        │                   │
                    │   COMMAND SIDE    │        │    QUERY SIDE     │
                    │                   │        │                   │
                    │  ┌─────────────┐  │        │  ┌─────────────┐  │
                    │  │  Commands   │  │        │  │   Queries   │  │
                    │  └──────┬──────┘  │        │  └──────┬──────┘  │
                    │         │         │        │         │         │
                    │  ┌──────▼──────┐  │        │  ┌──────▼──────┐  │
                    │  │  Handlers   │  │        │  │  Handlers   │  │
                    │  └──────┬──────┘  │        │  └──────┬──────┘  │
                    │         │         │        │         │         │
                    │  ┌──────▼──────┐  │        │  ┌──────▼──────┐  │
                    │  │  Aggregate  │  │        │  │ Read Models │  │
                    │  │  (Domain)   │  │        │  │   (DTOs)    │  │
                    │  └──────┬──────┘  │        │  └──────▲──────┘  │
                    │         │         │        │         │         │
                    │  ┌──────▼──────┐  │        │  ┌──────┴──────┐  │
                    │  │ Write Repo  │  │        │  │  Read Repo  │  │
                    │  └──────┬──────┘  │        │  └──────▲──────┘  │
                    │         │         │        │         │         │
                    └─────────┼─────────┘        └─────────┼─────────┘
                              │                            │
                              │    ┌─────────────────┐     │
                              │    │                 │     │
                              └────►     EVENTS      ├─────┘
                                   │                 │
                                   │ ┌─────────────┐ │
                                   │ │ Projection  │ │
                                   │ │ (Sincroniza)│ │
                                   │ └─────────────┘ │
                                   └─────────────────┘
                                          │
                              ┌───────────▼───────────┐
                              │      BASE DE DATOS    │
                              │  ┌──────┐  ┌───────┐  │
                              │  │Write │  │ Read  │  │
                              │  │Model │  │ Model │  │
                              │  └──────┘  └───────┘  │
                              └───────────────────────┘
```

---

## Flujo de Operaciones

### Flujo de Escritura (Command)

```
1. POST /api/beneficios/commands/asignar
        │
        ▼
2. BeneficioCommandController
        │ crea
        ▼
3. AsignarBeneficioCommand
        │ procesa
        ▼
4. AsignarBeneficioHandler
        │ usa
        ▼
5. BeneficioAggregate.asignar()
        │ genera
        ▼
6. BeneficioAsignadoEvent
        │ persiste          │ publica
        ▼                   ▼
7. WriteRepository     EventPublisher
        │                   │
        ▼                   ▼
8. Base de Datos       BeneficioProjection
                            │ actualiza
                            ▼
                       Read Model
```

### Flujo de Lectura (Query)

```
1. GET /api/beneficios/queries/afiliado/{id}
        │
        ▼
2. BeneficioQueryController
        │ crea
        ▼
3. BuscarBeneficiosQuery
        │ procesa
        ▼
4. BuscarBeneficiosHandler
        │ usa
        ▼
5. BeneficioReadRepository
        │ retorna
        ▼
6. List<BeneficioReadModel>
        │
        ▼
7. JSON Response
```

---

## Principios Clave Aplicados

### 1. **Separación de Responsabilidades**
- Commands: Solo escritura y lógica de negocio
- Queries: Solo lectura, sin lógica de negocio
- Events: Comunicación entre ambos lados

### 2. **Modelos Optimizados**
- Write Model: Rico en comportamiento, normalizado
- Read Model: Plano, desnormalizado, optimizado para consultas

### 3. **Consistencia Eventual**
- El Read Model se actualiza vía eventos
- Puede haber un pequeño delay entre escritura y lectura
- En este ejemplo es síncrono, pero puede ser asíncrono

### 4. **Escalabilidad**
- Command y Query pueden escalar independientemente
- Pueden usar bases de datos diferentes
- Queries pueden usar caché agresivo

---

## Comparación DDD Básico vs CQRS

| Aspecto | DDD Básico (Module 1) | CQRS (Module 2) |
|---------|----------------------|-----------------|
| **Modelo** | Único | Separado (Write/Read) |
| **Service** | BeneficioService | CommandHandlers + QueryHandlers |
| **Controller** | Uno unificado | Separados (Command/Query) |
| **Repository** | Uno | WriteRepository + ReadRepository |
| **Eventos** | Opcionales | Centrales para sincronización |
| **Escalabilidad** | Limitada | Alta (independiente por lado) |
| **Complejidad** | Baja | Media-Alta |
| **Caso de uso** | CRUD simple | Alto volumen, lecturas complejas |
