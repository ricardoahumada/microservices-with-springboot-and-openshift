package com.mutualidad.validacion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidarAfiliadoRequest {
    private String dni;
    private String nombre;
    private String apellidos;
}
