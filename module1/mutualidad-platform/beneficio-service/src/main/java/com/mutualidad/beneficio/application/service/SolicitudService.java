package com.mutualidad.beneficio.application.service;

import com.mutualidad.beneficio.domain.model.Beneficio;
import com.mutualidad.beneficio.domain.model.EstadoSolicitud;
import com.mutualidad.beneficio.domain.model.Solicitud;
import com.mutualidad.beneficio.infrastructure.persistence.BeneficioJpaRepository;
import com.mutualidad.beneficio.infrastructure.persistence.SolicitudJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class SolicitudService {

    private final SolicitudJpaRepository solicitudRepository;
    private final BeneficioJpaRepository beneficioRepository;

    public Solicitud crear(Long afiliadoId, Long beneficioId, BigDecimal montoSolicitado) {
        Beneficio beneficio = beneficioRepository.findById(beneficioId)
                .orElseThrow(() -> new IllegalArgumentException("Beneficio no encontrado"));
        
        if (!beneficio.estaDisponible()) {
            throw new IllegalStateException("El beneficio no esta disponible");
        }
        
        if (montoSolicitado.compareTo(beneficio.getMontoMaximo()) > 0) {
            throw new IllegalArgumentException("El monto solicitado excede el maximo permitido");
        }
        
        Solicitud solicitud = Solicitud.builder()
                .afiliadoId(afiliadoId)
                .beneficio(beneficio)
                .montoSolicitado(montoSolicitado)
                .build();
        
        return solicitudRepository.save(solicitud);
    }

    @Transactional(readOnly = true)
    public Optional<Solicitud> buscarPorId(Long id) {
        return solicitudRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Solicitud> buscarPorAfiliado(Long afiliadoId) {
        return solicitudRepository.findByAfiliadoId(afiliadoId);
    }

    @Transactional(readOnly = true)
    public List<Solicitud> buscarPorEstado(EstadoSolicitud estado) {
        return solicitudRepository.findByEstado(estado);
    }

    public Solicitud aprobar(Long id, BigDecimal montoAprobado) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        solicitud.aprobar(montoAprobado);
        return solicitudRepository.save(solicitud);
    }

    public Solicitud rechazar(Long id, String motivo) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        solicitud.rechazar(motivo);
        return solicitudRepository.save(solicitud);
    }

    public Solicitud marcarComoPagada(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        solicitud.marcarComoPagada();
        return solicitudRepository.save(solicitud);
    }
}
