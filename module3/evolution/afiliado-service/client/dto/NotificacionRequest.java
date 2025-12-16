package com.mutualidad.afiliado.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificacionRequest {
    private String destinatario;
    private String asunto;
    private String mensaje;
    private String canal;
}
