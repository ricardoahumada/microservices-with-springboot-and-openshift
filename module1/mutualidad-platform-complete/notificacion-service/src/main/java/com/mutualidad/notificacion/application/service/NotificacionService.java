package com.mutualidad.notificacion.application.service;

import com.mutualidad.notificacion.domain.model.Canal;
import com.mutualidad.notificacion.domain.model.EstadoNotificacion;
import com.mutualidad.notificacion.domain.model.Notificacion;
import com.mutualidad.notificacion.infrastructure.persistence.NotificacionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificacionService {

    private final NotificacionJpaRepository repository;

    public Notificacion crear(Notificacion notificacion) {
        return repository.save(notificacion);
    }

    @Transactional(readOnly = true)
    public Optional<Notificacion> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> buscarPorAfiliado(String afiliadoId) {
        return repository.findByDestinatarioAfiliadoId(afiliadoId);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> buscarPorEstado(EstadoNotificacion estado) {
        return repository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> buscarPorCanal(Canal canal) {
        return repository.findByCanal(canal);
    }

    @Transactional(readOnly = true)
    public List<Notificacion> buscarPendientesDeReintento() {
        return repository.findByEstadoAndIntentosEnvioLessThan(EstadoNotificacion.FALLIDA, 3);
    }

    public Notificacion enviar(Long id) {
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        
        // Simulacion de envio
        notificacion.marcarComoEnviada();
        return repository.save(notificacion);
    }

    public Notificacion confirmarEntrega(Long id) {
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        notificacion.marcarComoEntregada();
        return repository.save(notificacion);
    }

    public Notificacion confirmarLectura(Long id) {
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        notificacion.marcarComoLeida();
        return repository.save(notificacion);
    }

    public Notificacion registrarFallo(Long id, String error) {
        Notificacion notificacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notificacion no encontrada"));
        notificacion.marcarComoFallida(error);
        return repository.save(notificacion);
    }
}
