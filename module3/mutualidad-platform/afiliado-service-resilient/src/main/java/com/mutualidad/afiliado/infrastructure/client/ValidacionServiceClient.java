package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionResponse;
import com.mutualidad.afiliado.infrastructure.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "validacion-service",
    url = "${services.validacion.url}",
    configuration = FeignConfig.class,
    fallback = ValidacionServiceClientFallback.class
)
public interface ValidacionServiceClient {

    @PostMapping("/api/validaciones/estado-laboral")
    ValidacionResponse validarEstadoLaboral(
            @RequestBody ValidacionRequest request,
            @RequestParam(value = "simulateError", required = false) String simulateError);
}
