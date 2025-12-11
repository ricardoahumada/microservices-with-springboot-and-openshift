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
