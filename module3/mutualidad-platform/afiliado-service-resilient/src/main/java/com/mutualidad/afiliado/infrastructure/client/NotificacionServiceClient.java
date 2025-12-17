package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.infrastructure.client.dto.NotificacionResponse;
import com.mutualidad.afiliado.infrastructure.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "notificacion-service",
    url = "${services.notificacion.url}",
    configuration = FeignConfig.class,
    fallback = NotificacionServiceClientFallback.class
)
public interface NotificacionServiceClient {

    @PostMapping("/api/notificaciones/bienvenida/{afiliadoId}")
    NotificacionResponse enviarBienvenida(
            @PathVariable("afiliadoId") Long afiliadoId,
            @RequestParam("email") String email);
}
