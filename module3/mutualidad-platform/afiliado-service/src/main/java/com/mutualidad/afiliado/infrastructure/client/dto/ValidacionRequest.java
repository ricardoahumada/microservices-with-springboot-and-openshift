package com.mutualidad.afiliado.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidacionRequest {
    private String dni;
    private String nombre;
    private String apellidos;
}
