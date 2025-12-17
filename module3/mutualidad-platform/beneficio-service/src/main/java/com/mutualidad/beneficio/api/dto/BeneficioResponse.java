package com.mutualidad.beneficio.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficioResponse {
    private Long id;
    private Long afiliadoId;
    private String tipoBeneficio;
    private String estado;
    private BigDecimal monto;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observaciones;
    private LocalDateTime fechaCreacion;
}
