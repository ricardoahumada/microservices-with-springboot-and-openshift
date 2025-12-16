package com.mutualidad.afiliado.infrastructure.adapter.output.persistence;

import com.mutualidad.afiliado.application.port.output.AfiliadoRepository;
import com.mutualidad.afiliado.domain.model.Afiliado;
import com.mutualidad.afiliado.domain.model.Documento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AfiliadoJpaAdapter implements AfiliadoRepository {

    private final AfiliadoJpaRepository jpaRepository;
    private final AfiliadoPersistenceMapper mapper;

    @Override
    public Afiliado save(Afiliado afiliado) {
        log.debug("Guardando afiliado: {}", afiliado.getId());
        AfiliadoEntity entity = mapper.toEntity(afiliado);
        AfiliadoEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Afiliado> findById(String id) {
        log.debug("Buscando afiliado por ID: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Afiliado> findByDocumento(Documento documento) {
        log.debug("Buscando afiliado por documento: {}", documento);
        return jpaRepository.findByTipoDocumentoAndNumeroDocumento(
                documento.getTipo().name(),
                documento.getNumero()
            )
            .map(mapper::toDomain);
    }

    @Override
    public boolean existsByDocumento(Documento documento) {
        return jpaRepository.existsByTipoDocumentoAndNumeroDocumento(
            documento.getTipo().name(),
            documento.getNumero()
        );
    }

    @Override
    public void deleteById(String id) {
        log.debug("Eliminando afiliado: {}", id);
        jpaRepository.deleteById(id);
    }
}
