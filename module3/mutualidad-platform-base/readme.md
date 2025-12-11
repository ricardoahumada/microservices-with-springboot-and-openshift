---
AIGC:
    ContentProducer: Minimax Agent AI
    ContentPropagator: Minimax Agent AI
    Label: AIGC
    ProduceID: "00000000000000000000000000000000"
    PropagateID: "00000000000000000000000000000000"
    ReservedCode1: 3046022100c309753bc7ec12978d57db539143c34d1748f150762c7069074c5229827a4c67022100c5d5bfd6cabfb19bf7a4fd7a1c8cc9399d766473a5eb45002dc88042d7a0b3d7
    ReservedCode2: 304502203dd4dd94d9853b45b31c57a5ba822fd52575ebe5b1dd6a04f8f9b635f72a4885022100e74e9d1715cb513f59b6674bbe6ff7bf38bb5a910f28d026cd98e4b8fe687844
---

# Mutualidad Platform - Version Base

> **Plataforma base sin patrones de resiliencia**
> 
> Este proyecto es el punto de partida para evolucionar hacia una arquitectura resiliente.

## Servicios Incluidos

| Servicio | Puerto | Descripcion |
|----------|--------|-------------|
| afiliado-service | 8081 | Gestion de afiliados (base) |
| beneficio-service | 8082 | Gestion de beneficios |
| notificacion-service | 8083 | Envio de notificaciones |
| validacion-service | 8084 | Validaciones externas |

## Estructura del afiliado-service (Base)

```
com.mutualidad.afiliado/
├── AfiliadoServiceApplication.java
├── controller/
│   └── AfiliadoController.java
├── service/
│   └── AfiliadoService.java
├── dto/
│   ├── AltaAfiliadoRequest.java
│   └── AfiliadoResponse.java
├── entity/
│   └── Afiliado.java
├── repository/
│   └── AfiliadoRepository.java
├── config/
│   └── GlobalExceptionHandler.java
└── exception/
    └── BusinessException.java
```

## Caracteristicas de la Version Base

- Servicio aislado (sin comunicacion con otros servicios)
- Sin Feign Clients
- Sin Circuit Breaker / Retry / Timeout
- Sin idempotencia
- Sin Correlation ID
- Sin Health Indicators avanzados

## Evolucion a Version Resiliente

Consultar la guia de evolucion en:
`module3/solutions/evolution/readme.md`

## Ejecucion

```bash
# Compilar
cd afiliado-service
mvn clean package

# Ejecutar
mvn spring-boot:run

# Probar
curl http://localhost:8081/api/afiliados/status
```

## Endpoints Disponibles

```
POST   /api/afiliados           - Alta de afiliado
GET    /api/afiliados/{id}      - Obtener por ID
GET    /api/afiliados/dni/{dni} - Obtener por DNI
PUT    /api/afiliados/{id}/activar - Activar afiliado
GET    /api/afiliados/status    - Estado del servicio
```
