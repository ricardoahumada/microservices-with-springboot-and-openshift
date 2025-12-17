package com.mutualidad.beneficio.application.service;

import com.mutualidad.beneficio.api.dto.AsignarBeneficioRequest;
import com.mutualidad.beneficio.api.dto.BeneficioResponse;
import com.mutualidad.beneficio.domain.model.Beneficio;
import com.mutualidad.beneficio.infrastructure.persistence.BeneficioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficioService {

    private final BeneficioRepository beneficioRepository;

    @Transactional
    public BeneficioResponse asignarBeneficio(AsignarBeneficioRequest request) {
        log.info("Asignando beneficio {} al afiliado {}", request.getTipoBeneficio(), request.getAfiliadoId());
        
        Beneficio beneficio = Beneficio.builder()
                .afiliadoId(request.getAfiliadoId())
                .tipoBeneficio(request.getTipoBeneficio())
                .estado("ACTIVO")
                .monto(request.getMonto())
                .fechaInicio(request.getFechaInicio() != null ? request.getFechaInicio() : LocalDate.now())
                .fechaFin(request.getFechaFin())
                .observaciones(request.getObservaciones())
                .build();

        beneficio = beneficioRepository.save(beneficio);
        log.info("Beneficio asignado con ID: {}", beneficio.getId());

        return mapToResponse(beneficio);
    }

    @Transactional
    public List<BeneficioResponse> asignarBeneficiosBasicos(Long afiliadoId) {
        log.info("Asignando beneficios basicos al afiliado {}", afiliadoId);
        
        // Beneficios basicos por defecto
        String[] tiposBasicos = {"SALUD", "EDUCACION"};
        
        return java.util.Arrays.stream(tiposBasicos)
                .map(tipo -> {
                    AsignarBeneficioRequest req = AsignarBeneficioRequest.builder()
                            .afiliadoId(afiliadoId)
                            .tipoBeneficio(tipo)
                            .observaciones("Beneficio basico asignado automaticamente")
                            .build();
                    return asignarBeneficio(req);
                })
                .collect(Collectors.toList());
    }

    public List<BeneficioResponse> obtenerBeneficiosPorAfiliado(Long afiliadoId) {
        return beneficioRepository.findByAfiliadoId(afiliadoId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BeneficioResponse obtenerBeneficioPorId(Long id) {
        return beneficioRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Beneficio no encontrado: " + id));
    }

    private BeneficioResponse mapToResponse(Beneficio beneficio) {
        return BeneficioResponse.builder()
                .id(beneficio.getId())
                .afiliadoId(beneficio.getAfiliadoId())
                .tipoBeneficio(beneficio.getTipoBeneficio())
                .estado(beneficio.getEstado())
                .monto(beneficio.getMonto())
                .fechaInicio(beneficio.getFechaInicio())
                .fechaFin(beneficio.getFechaFin())
                .observaciones(beneficio.getObservaciones())
                .fechaCreacion(beneficio.getFechaCreacion())
                .build();
    }
}
