package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.infrastructure.client.dto.NotificacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.NotificacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificacionServiceClientFallback implements NotificacionServiceClient {

    @Override
    public NotificacionResponse enviarNotificacion(NotificacionRequest request) {
        log.warn("Fallback activado para notificacion a: {}", request.getDestinatario());
        return new NotificacionResponse("FALLBACK-" + System.currentTimeMillis(), "PENDIENTE");
    }
}
