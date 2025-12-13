package com.mutualidad.validacion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionEstadoLaboralResponse {
    private boolean valido;
    private String estado; // ACTIVO, INACTIVO
    private String mensaje;
    private String dni;
    private String empresaId;
}
