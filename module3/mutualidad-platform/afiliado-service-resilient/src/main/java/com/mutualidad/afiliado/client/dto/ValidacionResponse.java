package com.mutualidad.afiliado.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionResponse {
    private boolean valido;
    private String estado;
    private String mensaje;
    private String dni;
    private String empresaId;
}
