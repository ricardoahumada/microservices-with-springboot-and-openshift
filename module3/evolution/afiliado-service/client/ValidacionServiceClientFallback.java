package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ValidacionServiceClientFallback implements ValidacionServiceClient {

    @Override
    public ValidacionResponse validarAfiliado(ValidacionRequest request) {
        log.warn("Fallback activado para validacion de afiliado: {}", request.getDni());
        return ValidacionResponse.builder()
            .valido(false)
            .mensaje("Servicio de validacion no disponible. Intente mas tarde.")
            .errores(List.of("SERVICE_UNAVAILABLE"))
            .build();
    }
}
