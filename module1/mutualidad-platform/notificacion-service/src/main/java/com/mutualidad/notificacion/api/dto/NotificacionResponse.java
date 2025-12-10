package com.mutualidad.notificacion.api.dto;

import com.mutualidad.notificacion.domain.model.Notificacion;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificacionResponse {
    private Long id;
    private String afiliadoId;
    private String nombreDestinatario;
    private String asunto;
    private String canal;
    private String estado;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaEntrega;
    private Integer intentosEnvio;

    public static NotificacionResponse fromEntity(Notificacion notificacion) {
        return NotificacionResponse.builder()
                .id(notificacion.getId())
                .afiliadoId(notificacion.getDestinatario().getAfiliadoId())
                .nombreDestinatario(notificacion.getDestinatario().getNombre())
                .asunto(notificacion.getAsunto())
                .canal(notificacion.getCanal().name())
                .estado(notificacion.getEstado().name())
                .fechaEnvio(notificacion.getFechaEnvio())
                .fechaEntrega(notificacion.getFechaEntrega())
                .intentosEnvio(notificacion.getIntentosEnvio())
                .build();
    }
}
