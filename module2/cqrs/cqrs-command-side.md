# CQRS Command Side - Beneficio Service

## Aggregate de Dominio

### BeneficioAggregate.java

```java
package com.mutualidad.beneficio.command.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Aggregate Root para el beneficio.
 * Encapsula la lógica de negocio y protege los invariantes.
 */
@Getter
public class BeneficioAggregate {

    private final Beneficio beneficio;

    private BeneficioAggregate(Beneficio beneficio) {
        this.beneficio = beneficio;
    }

    /**
     * Factory method para crear un nuevo beneficio.
     */
    public static BeneficioAggregate crear(
            String afiliadoId,
            TipoBeneficio tipoBeneficio,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal monto,
            String descripcion,
            String solicitadoPor) {

        validarFechas(fechaInicio, fechaFin);
        validarMonto(tipoBeneficio, monto);

        Beneficio beneficio = Beneficio.builder()
            .id(UUID.randomUUID().toString())
            .afiliadoId(afiliadoId)
            .tipoBeneficio(tipoBeneficio)
            .estado(EstadoBeneficio.ACTIVO)
            .fechaInicio(fechaInicio)
            .fechaFin(fechaFin)
            .monto(monto)
            .descripcion(descripcion)
            .solicitadoPor(solicitadoPor)
            .fechaCreacion(LocalDate.now())
            .build();

        return new BeneficioAggregate(beneficio);
    }

    /**
     * Recarga un aggregate desde persistencia.
     */
    public static BeneficioAggregate fromExisting(Beneficio beneficio) {
        return new BeneficioAggregate(beneficio);
    }

    private static void validarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null) {
            throw new IllegalArgumentException("La fecha de inicio es obligatoria");
        }
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser anterior a hoy");
        }
        if (fechaFin != null && fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
    }

    private static void validarMonto(TipoBeneficio tipo, BigDecimal monto) {
        if (tipo.requiereMonto() && (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException(
                "El tipo de beneficio " + tipo + " requiere un monto positivo"
            );
        }
    }
}
```

### Beneficio.java (Entidad de Dominio)

```java
package com.mutualidad.beneficio.command.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa un beneficio asignado.
 */
@Getter
@Builder
@AllArgsConstructor
public class Beneficio {

    private final String id;
    private final String afiliadoId;
    private final TipoBeneficio tipoBeneficio;
    private EstadoBeneficio estado;
    private final LocalDate fechaInicio;
    private LocalDate fechaFin;
    private BigDecimal monto;
    private String descripcion;
    private final String solicitadoPor;
    private final LocalDate fechaCreacion;
    
    // Campos de auditoría
    private LocalDateTime fechaRevocacion;
    private String motivoRevocacion;
    private String revocadoPor;
    private LocalDateTime fechaSuspension;
    private String motivoSuspension;
    private String suspendidoPor;

    /**
     * Revoca el beneficio con un motivo.
     */
    public void revocar(String motivo, LocalDate fechaEfectiva, String usuario) {
        if (!esRevocable()) {
            throw new IllegalStateException("El beneficio no puede ser revocado en estado: " + estado);
        }
        
        this.estado = EstadoBeneficio.REVOCADO;
        this.fechaFin = fechaEfectiva;
        this.motivoRevocacion = motivo;
        this.revocadoPor = usuario;
        this.fechaRevocacion = LocalDateTime.now();
    }

    /**
     * Suspende temporalmente el beneficio.
     */
    public void suspender(String motivo, String usuario) {
        if (this.estado != EstadoBeneficio.ACTIVO) {
            throw new IllegalStateException("Solo se pueden suspender beneficios activos");
        }
        
        this.estado = EstadoBeneficio.SUSPENDIDO;
        this.motivoSuspension = motivo;
        this.suspendidoPor = usuario;
        this.fechaSuspension = LocalDateTime.now();
    }

    /**
     * Reactiva un beneficio suspendido.
     */
    public void reactivar() {
        if (this.estado != EstadoBeneficio.SUSPENDIDO) {
            throw new IllegalStateException("Solo se pueden reactivar beneficios suspendidos");
        }
        
        this.estado = EstadoBeneficio.ACTIVO;
        this.motivoSuspension = null;
        this.suspendidoPor = null;
        this.fechaSuspension = null;
    }

    /**
     * Actualiza el monto del beneficio.
     */
    public void actualizarMonto(BigDecimal nuevoMonto) {
        if (nuevoMonto == null || nuevoMonto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser positivo");
        }
        this.monto = nuevoMonto;
    }

    /**
     * Actualiza la fecha de fin.
     */
    public void actualizarFechaFin(LocalDate nuevaFechaFin) {
        if (nuevaFechaFin != null && nuevaFechaFin.isBefore(this.fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior al inicio");
        }
        this.fechaFin = nuevaFechaFin;
    }

    /**
     * Actualiza la descripción.
     */
    public void actualizarDescripcion(String nuevaDescripcion) {
        this.descripcion = nuevaDescripcion;
    }

    /**
     * Verifica si el beneficio puede ser modificado.
     */
    public boolean esModificable() {
        return this.estado == EstadoBeneficio.ACTIVO || 
               this.estado == EstadoBeneficio.SUSPENDIDO;
    }

    /**
     * Verifica si el beneficio puede ser revocado.
     */
    public boolean esRevocable() {
        return this.estado == EstadoBeneficio.ACTIVO || 
               this.estado == EstadoBeneficio.SUSPENDIDO;
    }

    /**
     * Verifica si el beneficio está vigente en una fecha dada.
     */
    public boolean estaVigente(LocalDate fecha) {
        if (this.estado != EstadoBeneficio.ACTIVO) {
            return false;
        }
        boolean despuesDeInicio = !fecha.isBefore(this.fechaInicio);
        boolean antesDeFinOIndefinido = this.fechaFin == null || !fecha.isAfter(this.fechaFin);
        return despuesDeInicio && antesDeFinOIndefinido;
    }

    /**
     * Serializa el beneficio a JSON para historial.
     */
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando beneficio", e);
        }
    }
}
```

### EstadoBeneficio.java

```java
package com.mutualidad.beneficio.command.domain;

/**
 * Estados posibles de un beneficio.
 */
public enum EstadoBeneficio {
    ACTIVO,      // Beneficio vigente
    SUSPENDIDO,  // Suspendido temporalmente
    REVOCADO,    // Revocado definitivamente
    EXPIRADO     // Venció la fecha de fin
}
```

## Write Repository

### BeneficioWriteRepository.java

```java
package com.mutualidad.beneficio.command.repository;

import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.TipoBeneficio;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de escritura.
 */
public interface BeneficioWriteRepository {

    Beneficio save(Beneficio beneficio);

    Optional<Beneficio> findById(String id);

    List<Beneficio> findActivosByAfiliadoAndTipo(String afiliadoId, TipoBeneficio tipo);

    void deleteById(String id);
}
```

### BeneficioWriteJpaAdapter.java

```java
package com.mutualidad.beneficio.infrastructure.persistence;

import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.domain.TipoBeneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BeneficioWriteJpaAdapter implements BeneficioWriteRepository {

    private final BeneficioJpaRepository jpaRepository;
    private final BeneficioEntityMapper mapper;

    @Override
    public Beneficio save(Beneficio beneficio) {
        BeneficioEntity entity = mapper.toEntity(beneficio);
        BeneficioEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Beneficio> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Beneficio> findActivosByAfiliadoAndTipo(String afiliadoId, TipoBeneficio tipo) {
        return jpaRepository.findByAfiliadoIdAndTipoBeneficioAndEstado(
                afiliadoId, 
                tipo.name(), 
                "ACTIVO"
            )
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
```

### BeneficioEntity.java

```java
package com.mutualidad.beneficio.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficioEntity {

    @Id
    private String id;

    @Column(name = "afiliado_id", nullable = false)
    private String afiliadoId;

    @Column(name = "tipo_beneficio", nullable = false)
    private String tipoBeneficio;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    private String descripcion;

    @Column(name = "solicitado_por", nullable = false)
    private String solicitadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_revocacion")
    private LocalDateTime fechaRevocacion;

    @Column(name = "motivo_revocacion")
    private String motivoRevocacion;

    @Column(name = "revocado_por")
    private String revocadoPor;

    @Column(name = "fecha_suspension")
    private LocalDateTime fechaSuspension;

    @Column(name = "motivo_suspension")
    private String motivoSuspension;

    @Column(name = "suspendido_por")
    private String suspendidoPor;
}
```

## Historial de Cambios (Auditoría)

### CambioHistorico.java

```java
package com.mutualidad.beneficio.command.domain;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Registro de cambio para auditoría.
 */
@Value
@Builder
public class CambioHistorico {
    String id;
    String beneficioId;
    String estadoAnterior;  // JSON del estado antes del cambio
    String estadoNuevo;     // JSON del estado después del cambio
    String motivo;
    String modificadoPor;
    LocalDateTime fechaModificacion;
}
```

### CambioHistoricoRepository.java

```java
package com.mutualidad.beneficio.command.repository;

import com.mutualidad.beneficio.command.domain.CambioHistorico;

public interface CambioHistoricoRepository {
    CambioHistorico save(CambioHistorico cambio);
}
```

## Diagrama Command Side

```
┌─────────────────────────────────────────────────────────────────┐
│                     COMMAND SIDE (WRITE)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                    REST CONTROLLER                      │    │
│  │    POST /beneficios                                     │    │
│  │    PUT /beneficios/{id}                                 │    │
│  │    POST /beneficios/{id}/revocar                        │    │
│  └────────────────────────┬───────────────────────────────┘    │
│                           │                                     │
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                   COMMAND BUS                           │    │
│  │    Enruta commands a handlers correspondientes          │    │
│  └────────────────────────┬───────────────────────────────┘    │
│                           │                                     │
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                 COMMAND HANDLERS                        │    │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │    │
│  │  │   Asignar    │ │  Modificar   │ │   Revocar    │   │    │
│  │  │   Handler    │ │   Handler    │ │   Handler    │   │    │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘   │    │
│  └─────────┼────────────────┼────────────────┼───────────┘    │
│            │                │                │                  │
│            ▼                ▼                ▼                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                    DOMAIN MODEL                         │    │
│  │  ┌──────────────┐  ┌──────────────┐                    │    │
│  │  │ Beneficio    │  │  CambioHist. │                    │    │
│  │  │ Aggregate    │  │  (Auditoría) │                    │    │
│  │  └──────────────┘  └──────────────┘                    │    │
│  └────────────────────────┬───────────────────────────────┘    │
│                           │                                     │
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                WRITE REPOSITORY                         │    │
│  │               (PostgreSQL / JPA)                        │    │
│  └────────────────────────┬───────────────────────────────┘    │
│                           │                                     │
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │                 EVENT PUBLISHER                         │    │
│  │           Publica eventos a Kafka/RabbitMQ              │    │
│  │          para sincronizar Read Model                    │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```
