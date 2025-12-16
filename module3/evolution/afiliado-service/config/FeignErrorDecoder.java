package com.mutualidad.afiliado.infrastructure.config;

import com.mutualidad.afiliado.domain.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Error en llamada Feign: {} - Status: {}", methodKey, response.status());
        
        if (response.status() == 400) {
            return new BusinessException("VALIDATION_ERROR", "Error de validacion en servicio externo");
        }
        
        if (response.status() == 404) {
            return new BusinessException("NOT_FOUND", "Recurso no encontrado en servicio externo");
        }
        
        return defaultDecoder.decode(methodKey, response);
    }
}
