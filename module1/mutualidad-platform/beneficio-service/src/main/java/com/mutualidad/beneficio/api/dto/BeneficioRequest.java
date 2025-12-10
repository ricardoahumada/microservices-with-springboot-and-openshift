package com.mutualidad.beneficio.api.dto;

import com.mutualidad.beneficio.domain.model.TipoBeneficio;
import lombok.Data;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class BeneficioRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;
    
    @Size(max = 500)
    private String descripcion;
    
    @NotNull(message = "El tipo es obligatorio")
    private TipoBeneficio tipo;
    
    @NotNull(message = "El monto maximo es obligatorio")
    @DecimalMin(value = "0.01")
    private BigDecimal montoMaximo;
    
    @Min(0)
    private Integer diasCarencia = 0;
}
