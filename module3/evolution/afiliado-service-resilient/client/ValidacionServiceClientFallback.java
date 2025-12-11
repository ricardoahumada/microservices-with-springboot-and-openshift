package com.mutualidad.afiliado.client;

import com.mutualidad.afiliado.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.client.dto.ValidacionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ValidacionServiceClientFallback implements ValidacionServiceClient {

    @Override
    public ValidacionResponse validarEstadoLaboral(ValidacionRequest request, String simulateError) {
        log.warn("FALLBACK: Servicio de validacion no disponible. DNI: {}", request.getDni());
        
        return ValidacionResponse.builder()
                .valido(true) // Permitir continuar con advertencia
                .estado("PENDIENTE_VERIFICACION")
                .mensaje("Validacion pendiente - servicio temporalmente no disponible")
                .dni(request.getDni())
                .empresaId(request.getEmpresaId())
                .build();
    }
}
