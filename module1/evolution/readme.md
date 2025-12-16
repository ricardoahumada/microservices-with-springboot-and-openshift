# Evolución: mutualidad-platform-base → mutualidad-platform

## Resumen

Esta carpeta contiene las clases necesarias para evolucionar el proyecto base hacia la versión completa con DDD.

```
mutualidad-platform-base/          →          mutualidad-platform/
(solo Application + HealthController)         (estructura DDD completa)
```

## Estructura de Evolución

```
evolution/
├── readme.md                      # Esta guía
├── afiliado-service/              # Clases para afiliado
│   ├── api/
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── test/
├── beneficio-service/             # Clases para beneficio
│   ├── api/
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── test/
├── notificacion-service/          # Clases para notificacion
│   ├── api/
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── test/
└── validacion-service/            # Clases para validacion
    ├── api/
    ├── application/
    ├── domain/
    ├── infrastructure/
    └── test/
```

## Resumen de Clases por Servicio

| Servicio | Base | Completa | Clases a Añadir |
|----------|------|----------|-----------------|
| afiliado-service | 2 | 11 | 9 |
| beneficio-service | 2 | 12 | 10 |
| notificacion-service | 2 | 11 | 9 |
| validacion-service | 2 | 10 | 8 |

## Arquitectura DDD Aplicada

```
┌─────────────────────────────────────────────────────────────┐
│                      Servicio                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ api/                          (Adaptadores Entrada)  │   │
│  │  ├── controller/  → REST endpoints                   │   │
│  │  └── dto/         → Request/Response objects         │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                │
│                            ▼                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ application/              (Casos de Uso)             │   │
│  │  └── service/  → Orquestación de lógica              │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                │
│                            ▼                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ domain/                   (Núcleo del Negocio)       │   │
│  │  └── model/    → Entidades, Value Objects, Enums     │   │
│  └──────────────────────────────────────────────────────┘   │
│                            │                                │
│                            ▼                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ infrastructure/           (Adaptadores Salida)       │   │
│  │  └── persistence/ → Repositorios JPA                 │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Orden de Implementación Recomendado

1. **domain/** - Primero las entidades y value objects
2. **infrastructure/** - Repositorios para persistir
3. **application/** - Servicios con lógica de negocio
4. **api/** - Controllers y DTOs para exponer funcionalidad
5. **test/** - Tests para verificar
