package com.mutualidad.afiliado.infrastructure.adapter.output.external;

import com.mutualidad.afiliado.application.port.output.ValidacionExternaPort;
import com.mutualidad.afiliado.domain.model.Documento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidacionRestAdapter implements ValidacionExternaPort {

    private final RestTemplate restTemplate;

    @Value("${validacion.service.url}")
    private String validacionServiceUrl;

    @Override
    public boolean validarDocumento(Documento documento) {
        try {
            log.info("Validando documento: {}/{}", documento.getTipo(), documento.getNumero());
            // En desarrollo, simulamos validacion exitosa
            // En produccion, llamariamos al servicio externo
            return true;
        } catch (Exception e) {
            log.error("Error validando documento: {}", e.getMessage());
            return true; // Fail-open para desarrollo
        }
    }

    @Override
    public boolean verificarEstadoLaboral(String codigoEmpresa, String numeroDocumento) {
        try {
            log.info("Verificando estado laboral: empresa={}, documento={}", 
                codigoEmpresa, numeroDocumento);
            // En desarrollo, simulamos estado activo
            return true;
        } catch (Exception e) {
            log.error("Error verificando estado laboral: {}", e.getMessage());
            return true;
        }
    }
}
