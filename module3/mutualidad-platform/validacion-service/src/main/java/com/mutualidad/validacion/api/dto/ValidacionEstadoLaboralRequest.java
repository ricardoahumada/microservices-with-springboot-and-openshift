package com.mutualidad.validacion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionEstadoLaboralRequest {
    
    @NotBlank(message = "DNI es requerido")
    private String dni;
    
    @NotBlank(message = "ID de empresa es requerido")
    private String empresaId;
}
