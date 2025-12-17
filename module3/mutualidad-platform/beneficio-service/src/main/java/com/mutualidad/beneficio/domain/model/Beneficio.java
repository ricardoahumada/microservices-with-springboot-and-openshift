package com.mutualidad.beneficio.domain.model;

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
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long afiliadoId;

    @Column(nullable = false)
    private String tipoBeneficio; // SALUD, EDUCACION, VIVIENDA, RECREACION

    @Column(nullable = false)
    private String estado; // ACTIVO, SUSPENDIDO, REVOCADO

    private BigDecimal monto;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    private String observaciones;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        if (estado == null) {
            estado = "ACTIVO";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
