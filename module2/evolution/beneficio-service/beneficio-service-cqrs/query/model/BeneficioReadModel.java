package com.mutualidad.beneficio.query.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String montoFormateado;

    private String descripcion;

    @Column(name = "esta_vigente")
    private boolean estaVigente;

    @Column(name = "dias_restantes")
    private Integer diasRestantes;

    @Column(name = "fecha_creacion")
    private LocalDate fechaCreacion;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;
}
