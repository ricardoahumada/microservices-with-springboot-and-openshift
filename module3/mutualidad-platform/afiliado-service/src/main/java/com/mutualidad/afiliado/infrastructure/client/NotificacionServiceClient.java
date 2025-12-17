package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.infrastructure.client.dto.NotificacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.NotificacionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "notificacion-service",
    url = "${services.notificacion.url}",
    fallback = NotificacionServiceClientFallback.class
)
public interface NotificacionServiceClient {

    @PostMapping("/api/v1/notificaciones")
    NotificacionResponse enviarNotificacion(@RequestBody NotificacionRequest request);
}
