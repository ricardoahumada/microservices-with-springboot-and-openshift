package com.mutualidad.afiliado.infrastructure.client;

import com.mutualidad.afiliado.api.dto.BeneficioDto;
import com.mutualidad.afiliado.infrastructure.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(
    name = "beneficio-service",
    url = "${services.beneficio.url}",
    configuration = FeignConfig.class,
    fallback = BeneficioServiceClientFallback.class
)
public interface BeneficioServiceClient {

    @PostMapping("/api/beneficios/basicos/{afiliadoId}")
    List<BeneficioDto> asignarBeneficiosBasicos(@PathVariable("afiliadoId") Long afiliadoId);

    @GetMapping("/api/beneficios/afiliado/{afiliadoId}")
    List<BeneficioDto> obtenerBeneficiosPorAfiliado(@PathVariable("afiliadoId") Long afiliadoId);
}
