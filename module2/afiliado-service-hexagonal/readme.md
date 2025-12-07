# Afiliado Service - Arquitectura Hexagonal

Microservicio de gestion de afiliados implementado con arquitectura hexagonal (Ports & Adapters).

## Estructura del Proyecto

```
src/main/java/com/mutualidad/afiliado/
├── domain/                          # Nucleo del dominio (sin dependencias externas)
│   ├── model/                       # Entidades y Value Objects
│   │   ├── Afiliado.java           # Entidad principal
│   │   ├── Documento.java          # Value Object
│   │   ├── EstadoAfiliado.java     # Enum con transiciones
│   │   └── TipoDocumento.java      # Enum
│   ├── exception/                   # Excepciones de dominio
│   └── event/                       # Eventos de dominio
├── application/                     # Capa de aplicacion
│   ├── port/
│   │   ├── input/                  # Puertos de entrada (casos de uso)
│   │   │   └── AfiliadoUseCase.java
│   │   └── output/                 # Puertos de salida
│   │       ├── AfiliadoRepository.java
│   │       ├── ValidacionExternaPort.java
│   │       ├── EventPublisherPort.java
│   │       └── NotificacionPort.java
│   ├── service/                    # Servicios de aplicacion
│   │   └── AfiliadoApplicationService.java
│   └── dto/                        # DTOs y Commands
└── infrastructure/                  # Adaptadores
    ├── adapter/
    │   ├── input/rest/             # Adaptador REST (primario)
    │   │   └── AfiliadoController.java
    │   └── output/                 # Adaptadores secundarios
    │       ├── persistence/        # JPA Adapter
    │       ├── external/           # Cliente REST externo
    │       ├── event/              # Publicador de eventos
    │       └── notification/       # Notificaciones
    └── config/                     # Configuracion Spring
```

## Compilacion y Testing

```bash
# Compilar proyecto
mvn clean install

# Solo compilar sin tests
mvn clean install -DskipTests

# Ejecutar tests
mvn test
```

## Ejecucion

```bash
mvn spring-boot:run
```

El servicio estara disponible en: http://localhost:8081

## Endpoints

### Health Check
```bash
curl http://localhost:8081/actuator/health
```

### Registrar Afiliado
```bash
curl -X POST http://localhost:8081/api/v1/afiliados \
  -H "Content-Type: application/json" \
  -d '{
    "tipoDocumento": "DNI",
    "numeroDocumento": "12345678Z",
    "nombre": "Juan",
    "primerApellido": "Garcia",
    "segundoApellido": "Lopez",
    "fechaNacimiento": "1985-03-15",
    "email": "juan.garcia@email.com",
    "telefono": "+34612345678",
    "direccion": "Calle Mayor 1",
    "codigoPostal": "28001",
    "provincia": "Madrid",
    "codigoEmpresa": "EMP001"
  }'
```

### Consultar por ID
```bash
curl http://localhost:8081/api/v1/afiliados/{id}
```

### Consultar por Documento
```bash
curl http://localhost:8081/api/v1/afiliados/documento/DNI/12345678Z
```

### Dar de Baja
```bash
curl -X POST http://localhost:8081/api/v1/afiliados/{id}/baja \
  -H "Content-Type: application/json" \
  -d '{
    "motivo": "Solicitud voluntaria del afiliado"
  }'
```

### Reactivar Afiliado
```bash
curl -X POST http://localhost:8081/api/v1/afiliados/{id}/reactivar
```

### Actualizar Contacto
```bash
curl -X PUT http://localhost:8081/api/v1/afiliados/{id}/contacto \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nuevo.email@email.com",
    "telefono": "+34699888777"
  }'
```

## Consola H2
http://localhost:8081/h2-console

- JDBC URL: `jdbc:h2:mem:afiliadodb`
- Username: `sa`
- Password: (vacio)

## Principios de Arquitectura Hexagonal

| Capa | Responsabilidad |
|------|-----------------|
| **Domain** | Logica de negocio pura, sin dependencias de frameworks |
| **Application** | Orquestacion de casos de uso, define puertos |
| **Infrastructure** | Implementa adaptadores para conectar con el exterior |

### Flujo de Dependencias
```
Adaptadores Primarios → Puertos de Entrada → Aplicacion → Dominio
                                                ↓
Adaptadores Secundarios ← Puertos de Salida ←──┘
```

## Estados del Afiliado

| Estado | Transiciones Validas |
|--------|---------------------|
| PENDIENTE | ACTIVO, RECHAZADO |
| ACTIVO | BAJA, SUSPENDIDO |
| SUSPENDIDO | ACTIVO, BAJA |
| BAJA | ACTIVO (reactivacion) |
| RECHAZADO | (estado final) |

## Tipos de Documento

- DNI: Formato `12345678A`
- NIE: Formato `X1234567A`
- PASAPORTE: Formato `AA123456`
