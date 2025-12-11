# Puertos de Entrada y Salida - Arquitectura Hexagonal

## Puertos de Entrada (Driving Ports)

### AfiliadoUseCase.java

```java
package com.mutualidad.afiliado.application.port.input;

import com.mutualidad.afiliado.application.dto.AfiliadoDTO;
import com.mutualidad.afiliado.application.dto.RegistrarAfiliadoCommand;

import java.util.Optional;

/**
 * Puerto de entrada principal para el servicio de afiliados.
 * Define los casos de uso disponibles para los adaptadores primarios.
 */
public interface AfiliadoUseCase {

    /**
     * Registra un nuevo afiliado.
     */
    AfiliadoDTO registrarAfiliado(RegistrarAfiliadoCommand command);

    /**
     * Consulta un afiliado por documento.
     */
    Optional<AfiliadoDTO> consultarPorDocumento(String tipoDocumento, String numeroDocumento);

    /**
     * Consulta un afiliado por ID.
     */
    Optional<AfiliadoDTO> consultarPorId(String afiliadoId);

    /**
     * Da de baja a un afiliado.
     */
    void darDeBaja(String afiliadoId, String motivo);

    /**
     * Reactiva un afiliado dado de baja.
     */
    void reactivar(String afiliadoId);

    /**
     * Actualiza datos de contacto.
     */
    AfiliadoDTO actualizarContacto(String afiliadoId, String email, String telefono);
}
```

## Puertos de Salida (Driven Ports)

### AfiliadoRepository.java

```java
package com.mutualidad.afiliado.application.port.output;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;

import java.util.Optional;

/**
 * Puerto de salida para persistencia de afiliados.
 * Será implementado por un adaptador de base de datos.
 */
public interface AfiliadoRepository {

    /**
     * Guarda o actualiza un afiliado.
     */
    Afiliado save(Afiliado afiliado);

    /**
     * Busca un afiliado por ID.
     */
    Optional<Afiliado> findById(String id);

    /**
     * Busca un afiliado por documento.
     */
    Optional<Afiliado> findByDocumento(Documento documento);

    /**
     * Verifica si existe un afiliado con el documento dado.
     */
    boolean existsByDocumento(Documento documento);

    /**
     * Elimina un afiliado por ID.
     */
    void deleteById(String id);
}
```

### ValidacionExternaPort.java

```java
package com.mutualidad.afiliado.application.port.output;

import com.mutualidad.afiliado.domain.model.Documento;

/**
 * Puerto de salida para validaciones externas.
 * Conecta con servicios gubernamentales o de terceros.
 */
public interface ValidacionExternaPort {

    /**
     * Valida un documento con servicios externos.
     * @return true si el documento es válido
     */
    boolean validarDocumento(Documento documento);

    /**
     * Verifica el estado laboral del afiliado.
     * @return true si el estado laboral está activo
     */
    boolean verificarEstadoLaboral(String codigoEmpresa, String numeroDocumento);
}
```

### EventPublisherPort.java

```java
package com.mutualidad.afiliado.application.port.output;

import com.mutualidad.afiliado.domain.event.AfiliadoEvent;

/**
 * Puerto de salida para publicación de eventos de dominio.
 */
public interface EventPublisherPort {

    /**
     * Publica un evento de dominio.
     */
    void publish(AfiliadoEvent event);

    /**
     * Publica múltiples eventos en orden.
     */
    void publishAll(Iterable<AfiliadoEvent> events);
}
```

### NotificacionPort.java

```java
package com.mutualidad.afiliado.application.port.output;

/**
 * Puerto de salida para envío de notificaciones.
 */
public interface NotificacionPort {

    /**
     * Envía notificación de bienvenida al afiliado.
     */
    void enviarBienvenida(String email, String nombre);

    /**
     * Notifica la baja del afiliado.
     */
    void notificarBaja(String email, String nombre, String motivo);

    /**
     * Notifica la reactivación.
     */
    void notificarReactivacion(String email, String nombre);
}
```

## Eventos de Dominio

### AfiliadoEvent.java

```java
package com.mutualidad.afiliado.domain.event;

import java.time.Instant;

/**
 * Interfaz base para eventos de dominio del afiliado.
 */
public interface AfiliadoEvent {
    String getAfiliadoId();
    Instant getOcurridoEn();
    String getTipoEvento();
}
```

### Implementaciones de Eventos

```java
package com.mutualidad.afiliado.domain.event;

import lombok.Value;
import java.time.Instant;

@Value
public class AfiliadoRegistradoEvent implements AfiliadoEvent {
    String afiliadoId;
    String tipoDocumento;
    String numeroDocumento;
    String nombre;
    String codigoEmpresa;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "AFILIADO_REGISTRADO";
    }
}

@Value
public class AfiliadoActivadoEvent implements AfiliadoEvent {
    String afiliadoId;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "AFILIADO_ACTIVADO";
    }
}

@Value
public class AfiliadoDadoDeBajaEvent implements AfiliadoEvent {
    String afiliadoId;
    String motivo;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "AFILIADO_DADO_DE_BAJA";
    }
}

@Value
public class AfiliadoReactivadoEvent implements AfiliadoEvent {
    String afiliadoId;
    Instant ocurridoEn = Instant.now();

    @Override
    public String getTipoEvento() {
        return "AFILIADO_REACTIVADO";
    }
}
```

## Diagrama de Dependencias de Puertos

```
┌─────────────────────────────────────────────────────────────────┐
│                    ADAPTADORES PRIMARIOS                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │
│  │REST         │  │GraphQL      │  │Event        │              │
│  │Controller   │  │Resolver     │  │Listener     │              │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │
│         │                │                │                     │
│         └────────────────┼────────────────┘                     │
│                          │                                      │
│                          ▼                                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  PUERTOS DE ENTRADA                       │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │              AfiliadoUseCase                        │  │  │
│  │  │  - registrarAfiliado()                              │  │  │
│  │  │  - consultarPorDocumento()                          │  │  │
│  │  │  - darDeBaja()                                      │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                      DOMINIO                              │  │
│  │  ┌─────────────┐  ┌──────────────┐  ┌─────────────┐       │  │
│  │  │  Afiliado   │  │  Documento   │  │   Estado    │       │  │
│  │  │  (Entity)   │  │(Value Object)│  │   (Enum)    │       │  │
│  │  └─────────────┘  └──────────────┘  └─────────────┘       │  │
│  └───────────────────────────────────────────────────────────┘  │
│                          │                                      │
│                          ▼                                      │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                  PUERTOS DE SALIDA                        │  │
│  │  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐  │  │
│  │  │Afiliado       │  │Validacion     │  │Event          │  │  │
│  │  │Repository     │  │ExternaPort    │  │PublisherPort  │  │  │
│  │  └───────┬───────┘  └───────┬───────┘  └───────┬───────┘  │  │
│  └──────────┼──────────────────┼──────────────────┼──────────┘  │
│             │                  │                  │             │
│             ▼                  ▼                  ▼             │ 
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐        │
│  │JPA Adapter    │  │REST Client    │  │Kafka Producer │        │
│  │(PostgreSQL)   │  │(Validacion)   │  │(Events)       │        │
│  └───────────────┘  └───────────────┘  └───────────────┘        │
│                    ADAPTADORES SECUNDARIOS                      │
└─────────────────────────────────────────────────────────────────┘
```

## Principios de Diseño de Puertos

| Principio | Descripción |
|-----------|-------------|
| **Interfaz pura** | Solo tipos de Java y DTOs del dominio |
| **Sin frameworks** | Ninguna anotación de Spring o similar |
| **Nombrado semántico** | Nombres que reflejan el negocio |
| **Granularidad correcta** | Ni muy amplios ni muy específicos |
| **Documentación** | Javadoc claro con pre/postcondiciones |
