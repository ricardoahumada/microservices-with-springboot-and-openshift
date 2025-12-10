package com.mutualidad.validacion.api.dto;

import com.mutualidad.validacion.domain.model.TipoValidacion;
import lombok.Data;

import javax.validation.constraints.*;

@Data
public class ValidacionRequest {
    
    @NotNull(message = "El ID del afiliado es obligatorio")
    private Long afiliadoId;
    
    @NotNull(message = "El tipo de validacion es obligatorio")
    private TipoValidacion tipo;
    
    private String datosEnviados;
}
