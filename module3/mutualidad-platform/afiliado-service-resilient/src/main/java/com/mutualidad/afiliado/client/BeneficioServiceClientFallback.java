package com.mutualidad.afiliado.client;

import com.mutualidad.afiliado.dto.BeneficioDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class BeneficioServiceClientFallback implements BeneficioServiceClient {

    @Override
    public List<BeneficioDto> asignarBeneficiosBasicos(Long afiliadoId) {
        log.warn("FALLBACK: Servicio de beneficios no disponible. AfiliadoId: {}", afiliadoId);
        return Collections.emptyList();
    }

    @Override
    public List<BeneficioDto> obtenerBeneficiosPorAfiliado(Long afiliadoId) {
        log.warn("FALLBACK: No se pudieron obtener beneficios. AfiliadoId: {}", afiliadoId);
        return Collections.emptyList();
    }
}
