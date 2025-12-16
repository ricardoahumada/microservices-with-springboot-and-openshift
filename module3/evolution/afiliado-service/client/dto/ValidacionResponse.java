package com.mutualidad.afiliado.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionResponse {
    private boolean valido;
    private String mensaje;
    private List<String> errores;
}
