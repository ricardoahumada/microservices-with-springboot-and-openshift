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

## Diagrama de Arquitectura CQRS

```
                         ┌──────────────────────────────────────────┐
                         │           REST CONTROLLERS               │
                         │  ┌──────────────────┐ ┌───────────────┐  │
                         │  │ CommandController│ │QueryController│  │
                         │  └───────┬──────────┘ └───┬───────────┘  │
                         └──────────┼────────────────┼──────────────┘
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

