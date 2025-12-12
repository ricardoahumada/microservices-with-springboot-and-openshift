package com.mutualidad.validacion.application.service;

import com.mutualidad.validacion.domain.model.ResultadoValidacion;
import com.mutualidad.validacion.domain.model.TipoValidacion;
import com.mutualidad.validacion.domain.model.ValidacionExterna;
import com.mutualidad.validacion.infrastructure.persistence.ValidacionExternaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ValidacionService {

    private final ValidacionExternaJpaRepository repository;

    public ValidacionExterna crear(Long afiliadoId, TipoValidacion tipo, String datosEnviados) {
        ValidacionExterna validacion = ValidacionExterna.builder()
                .afiliadoId(afiliadoId)
                .tipo(tipo)
                .datosEnviados(datosEnviados)
                .build();
        
        return repository.save(validacion);
    }

    @Transactional(readOnly = true)
    public Optional<ValidacionExterna> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<ValidacionExterna> buscarPorAfiliado(Long afiliadoId) {
        return repository.findByAfiliadoId(afiliadoId);
    }

    @Transactional(readOnly = true)
    public List<ValidacionExterna> buscarPorResultado(ResultadoValidacion resultado) {
        return repository.findByResultado(resultado);
    }

    @Transactional(readOnly = true)
    public List<ValidacionExterna> buscarPorTipo(TipoValidacion tipo) {
        return repository.findByTipo(tipo);
    }

    @Transactional(readOnly = true)
    public Optional<ValidacionExterna> buscarValidacionVigente(Long afiliadoId, TipoValidacion tipo) {
        return repository.findValidacionVigente(afiliadoId, tipo, LocalDateTime.now());
    }

    public ValidacionExterna iniciarProceso(Long id, String proveedor, String referencia) {
        ValidacionExterna validacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Validacion no encontrada"));
        validacion.iniciarProceso(proveedor, referencia);
        return repository.save(validacion);
    }

    public ValidacionExterna aprobar(Long id, Integer puntuacion, String mensaje, String respuesta) {
        ValidacionExterna validacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Validacion no encontrada"));
        validacion.aprobar(puntuacion, mensaje, respuesta);
        return repository.save(validacion);
    }

    public ValidacionExterna rechazar(Long id, String mensaje, String respuesta) {
        ValidacionExterna validacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Validacion no encontrada"));
        validacion.rechazar(mensaje, respuesta);
        return repository.save(validacion);
    }

    public ValidacionExterna registrarError(Long id, String mensaje) {
        ValidacionExterna validacion = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Validacion no encontrada"));
        validacion.registrarError(mensaje);
        return repository.save(validacion);
    }

    @Transactional(readOnly = true)
    public boolean tieneValidacionVigente(Long afiliadoId, TipoValidacion tipo) {
        return buscarValidacionVigente(afiliadoId, tipo).isPresent();
    }
}
