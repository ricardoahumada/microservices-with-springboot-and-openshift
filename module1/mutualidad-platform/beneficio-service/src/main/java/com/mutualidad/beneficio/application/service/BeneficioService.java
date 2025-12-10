package com.mutualidad.beneficio.application.service;

import com.mutualidad.beneficio.domain.model.Beneficio;
import com.mutualidad.beneficio.domain.model.TipoBeneficio;
import com.mutualidad.beneficio.infrastructure.persistence.BeneficioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BeneficioService {

    private final BeneficioJpaRepository repository;

    public Beneficio crear(Beneficio beneficio) {
        return repository.save(beneficio);
    }

    @Transactional(readOnly = true)
    public Optional<Beneficio> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Beneficio> listarTodos() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Beneficio> listarActivos() {
        return repository.findByActivoTrue();
    }

    @Transactional(readOnly = true)
    public List<Beneficio> buscarPorTipo(TipoBeneficio tipo) {
        return repository.findByTipo(tipo);
    }

    public Beneficio actualizar(Long id, Beneficio datos) {
        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beneficio no encontrado"));
        
        beneficio.setNombre(datos.getNombre());
        beneficio.setDescripcion(datos.getDescripcion());
        beneficio.setMontoMaximo(datos.getMontoMaximo());
        beneficio.setDiasCarencia(datos.getDiasCarencia());
        
        return repository.save(beneficio);
    }

    public Beneficio activar(Long id) {
        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beneficio no encontrado"));
        beneficio.setActivo(true);
        return repository.save(beneficio);
    }

    public Beneficio desactivar(Long id) {
        Beneficio beneficio = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beneficio no encontrado"));
        beneficio.setActivo(false);
        return repository.save(beneficio);
    }
}
