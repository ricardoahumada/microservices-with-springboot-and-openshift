package com.mutualidad.notificacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {
    private String id;
    private Long afiliadoId;
    private String tipo;
    private String asunto;
    private String estado; // ENVIADO, PENDIENTE, ERROR
    private LocalDateTime fechaEnvio;
    private String mensaje;
}
