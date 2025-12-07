package com.mutualidad.afiliado.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {
    private String id;
    private Long afiliadoId;
    private String tipo;
    private String estado;
    private String mensaje;
}
