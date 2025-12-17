# Beneficio Service - CQRS Pattern

Implementación del patrón CQRS (Command Query Responsibility Segregation) para el servicio de beneficios.

## Arquitectura CQRS

```
┌─────────────────────────────────────────────────────────────────┐
│                        CQRS ARCHITECTURE                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────┐         ┌────────────────────┐          │
│  │   COMMAND SIDE     │         │    QUERY SIDE      │          │
│  │   (Write Model)    │         │   (Read Model)     │          │
│  ├────────────────────┤         ├────────────────────┤          │
│  │ • Commands         │         │ • Queries          │          │
│  │ • Command Handlers │ ──────> │ • Query Handlers   │          │
│  │ • Domain Model     │  Events │ • Read Models      │          │
│  │ • Write Repository │         │ • Read Repository  │          │
│  └────────────────────┘         └────────────────────┘          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Estructura del Proyecto

```
beneficio-service-cqrs/
├── src/main/java/com/mutualidad/beneficio/
│   ├── command/
│   │   ├── api/           # Commands (AsignarBeneficio, Revocar, etc.)
│   │   ├── domain/        # Aggregate, Entidades, Estados
│   │   ├── handler/       # Command Handlers
│   │   └── repository/    # Write Repository
│   ├── query/
│   │   ├── api/           # Query DTOs
│   │   ├── handler/       # Query Handlers
│   │   ├── model/         # Read Models (desnormalizados)
│   │   └── repository/    # Read Repository
│   ├── event/             # Domain Events
│   └── infrastructure/
│       ├── persistence/   # JPA Entities y Adapters
│       ├── event/         # Event Publisher, Projection
│       ├── rest/          # Controllers (Command y Query)
│       └── config/        # Exception Handler
└── src/test/java/
```

## Compilación y Ejecución

```bash
# Compilar
cd module2/code/beneficio-service-cqrs
mvn clean compile

# Ejecutar tests
mvn test

# Ejecutar aplicación
mvn spring-boot:run
```

**Puerto:** 8085

## Endpoints

### Command Endpoints (Escritura)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/beneficios/commands/asignar` | Asignar nuevo beneficio |
| POST | `/api/v1/beneficios/commands/revocar` | Revocar beneficio |
| POST | `/api/v1/beneficios/commands/modificar` | Modificar beneficio |

### Query Endpoints (Lectura)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/v1/beneficios/queries?afiliadoId=X` | Buscar beneficios |
| GET | `/api/v1/beneficios/queries/{id}` | Obtener por ID |
| GET | `/api/v1/beneficios/queries/resumen/{afiliadoId}` | Resumen por afiliado |

## Ejemplos con curl

### Asignar Beneficio (Command)

```bash
curl -X POST http://localhost:8085/api/v1/beneficios/commands/asignar \
  -H "Content-Type: application/json" \
  -d '{
    "afiliadoId": "afiliado-001",
    "tipoBeneficio": "SALUD",
    "fechaInicio": "2025-01-01",
    "fechaFin": "2025-12-31",
    "monto": 500.00,
    "descripcion": "Cobertura de salud completa",
    "solicitadoPor": "admin",
    "motivo": "Alta de beneficio anual"
  }'
```

### Consultar Beneficios (Query)

```bash
curl "http://localhost:8085/api/v1/beneficios/queries?afiliadoId=afiliado-001"
```

### Obtener Resumen

```bash
curl http://localhost:8085/api/v1/beneficios/queries/resumen/afiliado-001
```

### Revocar Beneficio

```bash
curl -X POST http://localhost:8085/api/v1/beneficios/commands/revocar \
  -H "Content-Type: application/json" \
  -d '{
    "beneficioId": "UUID_DEL_BENEFICIO",
    "motivo": "Baja voluntaria del afiliado",
    "revocadoPor": "admin"
  }'
```

### Modificar Beneficio

```bash
curl -X POST http://localhost:8085/api/v1/beneficios/commands/modificar \
  -H "Content-Type: application/json" \
  -d '{
    "beneficioId": "UUID_DEL_BENEFICIO",
    "nuevoMonto": 750.00,
    "nuevaDescripcion": "Cobertura ampliada",
    "motivo": "Actualizacion anual",
    "modificadoPor": "admin"
  }'
```

## Tipos de Beneficio

| Tipo | Descripcion | Requiere Monto |
|------|-------------|----------------|
| SALUD | Cobertura de salud | Si |
| SUBSIDIO_DESEMPLEO | Subsidio por desempleo | Si |
| SUBSIDIO_INCAPACIDAD | Subsidio por incapacidad | Si |
| DESCUENTO_FARMACIA | Descuento en farmacias | No |
| DESCUENTO_OPTICA | Descuento en opticas | No |
| AYUDA_FAMILIAR | Ayuda por nacimiento/adopcion | Si |
| FORMACION | Ayuda para formacion | Si |
| JUBILACION_COMPLEMENTARIA | Complemento de jubilacion | Si |

## Estados de Beneficio

- **ACTIVO**: Beneficio vigente
- **SUSPENDIDO**: Suspendido temporalmente
- **REVOCADO**: Revocado definitivamente
- **EXPIRADO**: Vencio la fecha de fin

## Consola H2

```
URL: http://localhost:8085/h2-console
JDBC URL: jdbc:h2:mem:beneficiodb
User: sa
Password: (vacío)
```

## Health Check

```bash
curl http://localhost:8085/actuator/health
```
