package com.mutualidad.beneficio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarBeneficioRequest {
    
    @NotNull(message = "ID de afiliado es requerido")
    private Long afiliadoId;
    
    @NotNull(message = "Tipo de beneficio es requerido")
    private String tipoBeneficio;
    
    private BigDecimal monto;
    
    private LocalDate fechaInicio;
    
    private LocalDate fechaFin;
    
    private String observaciones;
}
