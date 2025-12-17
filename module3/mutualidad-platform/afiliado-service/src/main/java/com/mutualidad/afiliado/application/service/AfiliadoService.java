package com.mutualidad.afiliado.application.service;

import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.DNI;
import com.mutualidad.afiliado.domain.model.EstadoAfiliado;
import com.mutualidad.afiliado.infrastructure.client.ValidacionServiceClient;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionRequest;
import com.mutualidad.afiliado.infrastructure.client.dto.ValidacionResponse;
import com.mutualidad.afiliado.infrastructure.persistence.AfiliadoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AfiliadoService {

    private final AfiliadoJpaRepository repository;
    private final ValidacionServiceClient validacionClient;  // NUEVO

    public Afiliado crear(Afiliado afiliado) {
        // Validar con servicio externo
        ValidacionResponse validacion = validacionClient.validarAfiliado(
                ValidacionRequest.builder()
                        .dni(afiliado.getDni().getValor())
                        .nombre(afiliado.getNombre())
                        .apellidos(afiliado.getApellidos())
                        .build()
        );

        System.out.println("***"+validacion);

        if (!validacion.isValido()) {
            throw new IllegalArgumentException(validacion.getMensaje());
        }

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
