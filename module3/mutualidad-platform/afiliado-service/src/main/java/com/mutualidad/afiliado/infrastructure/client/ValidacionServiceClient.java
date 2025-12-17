package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "validacion-service",
    url = "${services.validacion.url}",
    fallback = ValidacionServiceClientFallback.class
)
public interface ValidacionServiceClient {

    @PostMapping("/api/v1/validaciones/afiliado")
    ValidacionResponse validarAfiliado(@RequestBody ValidacionRequest request);
}
