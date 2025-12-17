package com.mutualidad.afiliado.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficioDto {
    private Long id;
    private String tipoBeneficio;
    private String estado;
    private BigDecimal monto;
    private LocalDate fechaInicio;
}
