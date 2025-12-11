# CQRS Query Side - Beneficio Service

## Queries

### BuscarBeneficiosQuery.java

```java
package com.mutualidad.beneficio.query.api;

import com.mutualidad.beneficio.command.domain.EstadoBeneficio;
import com.mutualidad.beneficio.command.domain.TipoBeneficio;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

/**
 * Query para buscar beneficios de un afiliado.
 */
@Value
@Builder
public class BuscarBeneficiosQuery {

    @NotBlank(message = "El ID del afiliado es obligatorio")
    String afiliadoId;

    EstadoBeneficio estado; // Opcional, filtra por estado

    TipoBeneficio tipo; // Opcional, filtra por tipo

    @Min(0)
    @Builder.Default
    int page = 0;

    @Min(1)
    @Max(100)
    @Builder.Default
    int size = 20;
}
```

### ObtenerHistorialQuery.java

```java
package com.mutualidad.beneficio.query.api;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Query para obtener el historial de cambios de un beneficio.
 */
@Value
@Builder
public class ObtenerHistorialQuery {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;

    LocalDateTime fechaDesde; // Opcional

    LocalDateTime fechaHasta; // Opcional

    @Builder.Default
    int page = 0;

    @Builder.Default
    int size = 50;
}
```

### ObtenerBeneficioDetalleQuery.java

```java
package com.mutualidad.beneficio.query.api;

import lombok.Value;

import javax.validation.constraints.NotBlank;

/**
 * Query para obtener el detalle completo de un beneficio.
 */
@Value
public class ObtenerBeneficioDetalleQuery {

    @NotBlank(message = "El ID del beneficio es obligatorio")
    String beneficioId;
}
```

### ResumenBeneficiosQuery.java

```java
package com.mutualidad.beneficio.query.api;

import lombok.Value;

import javax.validation.constraints.NotBlank;

/**
 * Query para obtener un resumen de beneficios por afiliado.
 */
@Value
public class ResumenBeneficiosQuery {

    @NotBlank(message = "El ID del afiliado es obligatorio")
    String afiliadoId;
}
```

## Read Models

### BeneficioReadModel.java

```java
package com.mutualidad.beneficio.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modelo de lectura optimizado para consultas.
 * Desnormalizado para evitar joins.
 */
@Entity
@Table(name = "beneficios_read")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficioReadModel {

    @Id
    private String id;

    @Column(name = "afiliado_id", nullable = false)
    private String afiliadoId;

    // Datos desnormalizados del afiliado
    @Column(name = "afiliado_nombre")
    private String afiliadoNombre;

    @Column(name = "afiliado_documento")
    private String afiliadoDocumento;

    @Column(name = "tipo_beneficio", nullable = false)
    private String tipoBeneficio;

    @Column(name = "tipo_beneficio_descripcion")
    private String tipoBeneficioDescripcion;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_formateado")
    private String montoFormateado; // Ej: "1.500,00 EUR"

    private String descripcion;

    @Column(name = "esta_vigente")
    private boolean estaVigente;

    @Column(name = "dias_restantes")
    private Integer diasRestantes;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;

    // Índices para consultas frecuentes
    @Table(indexes = {
        @Index(name = "idx_afiliado_estado", columnList = "afiliado_id, estado"),
        @Index(name = "idx_afiliado_tipo", columnList = "afiliado_id, tipo_beneficio"),
        @Index(name = "idx_vigente", columnList = "esta_vigente, fecha_fin")
    })
    private static class Indexes {}
}
```

### CambioHistoricoReadModel.java

```java
package com.mutualidad.beneficio.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Modelo de lectura para historial de cambios.
 */
@Entity
@Table(name = "cambios_historico_read")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CambioHistoricoReadModel {

    @Id
    private String id;

    @Column(name = "beneficio_id", nullable = false)
    private String beneficioId;

    @Column(name = "tipo_cambio", nullable = false)
    private String tipoCambio; // CREACION, MODIFICACION, SUSPENSION, REACTIVACION, REVOCACION

    @Column(columnDefinition = "TEXT")
    private String descripcionCambio; // Descripción legible del cambio

    @Column(name = "campo_modificado")
    private String campoModificado;

    @Column(name = "valor_anterior")
    private String valorAnterior;

    @Column(name = "valor_nuevo")
    private String valorNuevo;

    private String motivo;

    @Column(name = "realizado_por", nullable = false)
    private String realizadoPor;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;
}
```

### ResumenBeneficiosReadModel.java

```java
package com.mutualidad.beneficio.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resumen de beneficios para dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenBeneficiosReadModel {

    private String afiliadoId;
    private String afiliadoNombre;
    
    private int totalBeneficios;
    private int beneficiosActivos;
    private int beneficiosSuspendidos;
    private int beneficiosRevocados;
    private int beneficiosExpirados;
    
    private BigDecimal montoTotalActivo;
    private String montoTotalFormateado;
    
    private String proximoVencimiento; // Fecha del próximo beneficio a vencer
    private String beneficioProximoVencer; // Tipo del beneficio
}
```

## Read Repositories

### BeneficioReadRepository.java

```java
package com.mutualidad.beneficio.query.repository;

import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficioReadRepository extends JpaRepository<BeneficioReadModel, String> {

    Page<BeneficioReadModel> findByAfiliadoId(String afiliadoId, Pageable pageable);

    Page<BeneficioReadModel> findByAfiliadoIdAndEstado(
        String afiliadoId, 
        String estado, 
        Pageable pageable
    );

    Page<BeneficioReadModel> findByAfiliadoIdAndTipoBeneficio(
        String afiliadoId, 
        String tipoBeneficio, 
        Pageable pageable
    );

    List<BeneficioReadModel> findByAfiliadoIdAndEstaVigenteTrue(String afiliadoId);

    @Query("SELECT b FROM BeneficioReadModel b " +
           "WHERE b.afiliadoId = :afiliadoId " +
           "AND b.estaVigente = true " +
           "AND b.fechaFin IS NOT NULL " +
           "ORDER BY b.fechaFin ASC")
    List<BeneficioReadModel> findProximosAVencer(String afiliadoId);

    @Query("SELECT COUNT(b) FROM BeneficioReadModel b " +
           "WHERE b.afiliadoId = :afiliadoId AND b.estado = :estado")
    long countByAfiliadoIdAndEstado(String afiliadoId, String estado);

    @Query("SELECT SUM(b.monto) FROM BeneficioReadModel b " +
           "WHERE b.afiliadoId = :afiliadoId AND b.estado = 'ACTIVO'")
    BigDecimal sumMontoActivoByAfiliado(String afiliadoId);
}
```

### CambioHistoricoReadRepository.java

```java
package com.mutualidad.beneficio.query.repository;

import com.mutualidad.beneficio.query.model.CambioHistoricoReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface CambioHistoricoReadRepository 
        extends JpaRepository<CambioHistoricoReadModel, String> {

    Page<CambioHistoricoReadModel> findByBeneficioId(
        String beneficioId, 
        Pageable pageable
    );

    Page<CambioHistoricoReadModel> findByBeneficioIdAndFechaCambioBetween(
        String beneficioId,
        LocalDateTime fechaDesde,
        LocalDateTime fechaHasta,
        Pageable pageable
    );

    Page<CambioHistoricoReadModel> findByRealizadoPor(
        String usuario,
        Pageable pageable
    );
}
```

## Query Handlers Completos

### BuscarBeneficiosHandler.java

```java
package com.mutualidad.beneficio.query.handler;

import com.mutualidad.beneficio.query.api.BuscarBeneficiosQuery;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BuscarBeneficiosHandler {

    private final BeneficioReadRepository repository;

    public Page<BeneficioReadModel> handle(BuscarBeneficiosQuery query) {
        log.debug("Buscando beneficios para afiliado: {}", query.getAfiliadoId());

        PageRequest pageRequest = PageRequest.of(
            query.getPage(),
            query.getSize(),
            Sort.by(Sort.Direction.DESC, "fechaInicio")
        );

        // Aplicar filtros según parámetros
        if (query.getEstado() != null && query.getTipo() != null) {
            return repository.findByAfiliadoIdAndEstadoAndTipoBeneficio(
                query.getAfiliadoId(),
                query.getEstado().name(),
                query.getTipo().name(),
                pageRequest
            );
        }

        if (query.getEstado() != null) {
            return repository.findByAfiliadoIdAndEstado(
                query.getAfiliadoId(),
                query.getEstado().name(),
                pageRequest
            );
        }

        if (query.getTipo() != null) {
            return repository.findByAfiliadoIdAndTipoBeneficio(
                query.getAfiliadoId(),
                query.getTipo().name(),
                pageRequest
            );
        }

        return repository.findByAfiliadoId(query.getAfiliadoId(), pageRequest);
    }
}
```

### ResumenBeneficiosHandler.java

```java
package com.mutualidad.beneficio.query.handler;

import com.mutualidad.beneficio.query.api.ResumenBeneficiosQuery;
import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import com.mutualidad.beneficio.query.model.ResumenBeneficiosReadModel;
import com.mutualidad.beneficio.query.repository.BeneficioReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ResumenBeneficiosHandler {

    private final BeneficioReadRepository repository;

    public ResumenBeneficiosReadModel handle(ResumenBeneficiosQuery query) {
        log.debug("Generando resumen de beneficios para: {}", query.getAfiliadoId());

        String afiliadoId = query.getAfiliadoId();

        // Contadores por estado
        long activos = repository.countByAfiliadoIdAndEstado(afiliadoId, "ACTIVO");
        long suspendidos = repository.countByAfiliadoIdAndEstado(afiliadoId, "SUSPENDIDO");
        long revocados = repository.countByAfiliadoIdAndEstado(afiliadoId, "REVOCADO");
        long expirados = repository.countByAfiliadoIdAndEstado(afiliadoId, "EXPIRADO");

        // Monto total activo
        BigDecimal montoTotal = repository.sumMontoActivoByAfiliado(afiliadoId);
        if (montoTotal == null) {
            montoTotal = BigDecimal.ZERO;
        }

        // Próximo a vencer
        List<BeneficioReadModel> proximosVencer = repository.findProximosAVencer(afiliadoId);
        String proximoVencimiento = null;
        String beneficioProximo = null;
        
        if (!proximosVencer.isEmpty()) {
            BeneficioReadModel proximo = proximosVencer.get(0);
            proximoVencimiento = proximo.getFechaFin().toString();
            beneficioProximo = proximo.getTipoBeneficioDescripcion();
        }

        // Obtener nombre del afiliado del primer registro
        String afiliadoNombre = repository.findByAfiliadoId(
                afiliadoId, 
                PageRequest.of(0, 1)
            )
            .getContent()
            .stream()
            .findFirst()
            .map(BeneficioReadModel::getAfiliadoNombre)
            .orElse("");

        // Formatear monto
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        String montoFormateado = formatter.format(montoTotal);

        return ResumenBeneficiosReadModel.builder()
            .afiliadoId(afiliadoId)
            .afiliadoNombre(afiliadoNombre)
            .totalBeneficios((int)(activos + suspendidos + revocados + expirados))
            .beneficiosActivos((int) activos)
            .beneficiosSuspendidos((int) suspendidos)
            .beneficiosRevocados((int) revocados)
            .beneficiosExpirados((int) expirados)
            .montoTotalActivo(montoTotal)
            .montoTotalFormateado(montoFormateado)
            .proximoVencimiento(proximoVencimiento)
            .beneficioProximoVencer(beneficioProximo)
            .build();
    }
}
```

## Diagrama Query Side

```
┌─────────────────────────────────────────────────────────────────┐
│                      QUERY SIDE (READ)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌────────────────────────────────────────────────────────┐     │
│  │                    REST CONTROLLER                     │     │
│  │    GET /beneficios?afiliadoId=X&estado=ACTIVO          │     │
│  │    GET /beneficios/{id}                                │     │
│  │    GET /beneficios/{id}/historial                      │     │
│  │    GET /afiliados/{id}/beneficios/resumen              │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │                                     │
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐     │
│  │                   QUERY HANDLERS                       │     │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │     │
│  │  │   Buscar     │ │  Historial   │ │   Resumen    │    │     │
│  │  │   Handler    │ │   Handler    │ │   Handler    │    │     │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘    │     │
│  └─────────┼────────────────┼────────────────┼────────────┘     │
│            │                │                │                  │
│            ▼                ▼                ▼                  │
│  ┌────────────────────────────────────────────────────────┐     │
│  │                    READ MODEL                          │     │
│  │  ┌───────────────────┐  ┌───────────────────┐          │     │
│  │  │ BeneficioReadModel│  │CambioHistoricoRead│          │     │
│  │  │ (Desnormalizado)  │  │     Model         │          │     │
│  │  └───────────────────┘  └───────────────────┘          │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │                                     │
│                           ▼                                     │
│  ┌────────────────────────────────────────────────────────┐     │
│  │                 READ REPOSITORY                        │     │
│  │   Optimizado para consultas rápidas                    │     │
│  │   (Puede ser PostgreSQL, Elasticsearch, Redis)         │     │
│  └────────────────────────────────────────────────────────┘     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```
