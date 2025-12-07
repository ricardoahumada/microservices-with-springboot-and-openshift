package com.mutualidad.afiliado.client;

import com.mutualidad.afiliado.client.dto.NotificacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificacionServiceClientFallback implements NotificacionServiceClient {

    @Override
    public NotificacionResponse enviarBienvenida(Long afiliadoId, String email) {
        log.warn("FALLBACK: Servicio de notificaciones no disponible. AfiliadoId: {}", afiliadoId);
        
        return NotificacionResponse.builder()
                .afiliadoId(afiliadoId)
                .estado("PENDIENTE")
                .mensaje("Notificacion pendiente - servicio temporalmente no disponible")
                .build();
    }
}
