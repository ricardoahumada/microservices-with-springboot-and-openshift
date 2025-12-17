package com.mutualidad.afiliado.application.service;

import com.mutualidad.afiliado.api.dto.AfiliadoResponse;
import com.mutualidad.afiliado.domain.exception.BusinessException;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import com.mutualidad.afiliado.infrastructure.client.NotificacionServiceClient;
import com.mutualidad.afiliado.infrastructure.client.ValidacionServiceClient;
import com.mutualidad.afiliado.infrastructure.client.dto.NotificacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionResponse;
import com.mutualidad.afiliado.infrastructure.idempotency.IdempotencyService;
import com.mutualidad.afiliado.infrastructure.persistence.AfiliadoJpaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AfiliadoService {

    private final AfiliadoJpaRepository repository;
    private final ValidacionServiceClient validacionClient;
    private final NotificacionServiceClient notificacionClient;
    private final IdempotencyService idempotencyService;

    @CircuitBreaker(name = "validacionService", fallbackMethod = "crearFallback")
    @Retry(name = "validacionService")
    public Afiliado crear(String idempotencyKey, Afiliado afiliado) {
        // Verificar idempotencia
        Optional<AfiliadoResponse> existing = idempotencyService
            .getExistingResponse(idempotencyKey, AfiliadoResponse.class);
        
        if (existing.isPresent()) {
            log.info("Retornando respuesta idempotente para key: {}", idempotencyKey);
            return repository.findById(existing.get().getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Afiliado no encontrado"));
        }

        // Validar con servicio externo
        ValidacionResponse validacion = validacionClient.validarAfiliado(
            ValidacionRequest.builder()
                .dni(afiliado.getDni().getValor())
                .nombre(afiliado.getNombre())
                .apellidos(afiliado.getApellidos())
                .build()
        );
        
        if (!validacion.isValido()) {
            throw new BusinessException("VALIDATION_FAILED", validacion.getMensaje());
        }

        if (repository.findByDni(afiliado.getDni().getValor()).isPresent()) {
            throw new BusinessException("DUPLICATE_DNI", "Ya existe un afiliado con ese DNI");
        }
        
        Afiliado creado = repository.save(afiliado);
        
        // Guardar para idempotencia
        idempotencyService.saveResponse(idempotencyKey, AfiliadoResponse.fromEntity(creado));
        
        // Notificar (fire and forget)
        try {
            notificacionClient.enviarNotificacion(
                NotificacionRequest.builder()
                    .destinatario(creado.getEmail())
                    .asunto("Bienvenido a Mutualidad")
                    .mensaje("Su registro ha sido completado exitosamente")
                    .canal("EMAIL")
                    .build()
            );
        } catch (Exception e) {
            log.warn("No se pudo enviar notificacion de bienvenida: {}", e.getMessage());
        }
        
        return creado;
    }

    public Afiliado crearFallback(String idempotencyKey, Afiliado afiliado, Throwable t) {
        log.error("Fallback ejecutado para crear afiliado: {}", t.getMessage());
        throw new BusinessException("SERVICE_DEGRADED", 
            "Servicio de validacion temporalmente no disponible. Intente mas tarde.");
    }

    // Metodo sin validacion externa (para compatibilidad)
    public Afiliado crear(Afiliado afiliado) {
        if (repository.findByDni(afiliado.getDni().getValor()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un afiliado con ese DNI");
        }
        return repository.save(afiliado);
    }

    @Transactional(readOnly = true)
    public Optional<Afiliado> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Afiliado> buscarPorDni(String dni) {
        return repository.findByDni(dni);
    }

    @Transactional(readOnly = true)
    public List<Afiliado> buscarPorEstado(EstadoAfiliado estado) {
        return repository.findByEstado(estado);
    }

    @Transactional(readOnly = true)
    public List<Afiliado> listarTodos() {
        return repository.findAll();
    }

    public Afiliado actualizar(Long id, Afiliado datosActualizados) {
        Afiliado afiliado = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado"));
        
        afiliado.setNombre(datosActualizados.getNombre());
        afiliado.setApellidos(datosActualizados.getApellidos());
        afiliado.setEmail(datosActualizados.getEmail());
        afiliado.setTelefono(datosActualizados.getTelefono());
        
        return repository.save(afiliado);
    }

    public Afiliado activar(Long id) {
        Afiliado afiliado = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado"));
        afiliado.activar();
        return repository.save(afiliado);
    }

    public Afiliado darDeBaja(Long id, String motivo) {
        Afiliado afiliado = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Afiliado no encontrado"));
        afiliado.darDeBaja(motivo);
        return repository.save(afiliado);
    }
}
