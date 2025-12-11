package com.mutualidad.beneficio.infrastructure.persistence;

import com.mutualidad.beneficio.command.api.TipoBeneficio;
import com.mutualidad.beneficio.command.domain.Beneficio;
import com.mutualidad.beneficio.command.repository.BeneficioWriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BeneficioWriteJpaAdapter implements BeneficioWriteRepository {

    private final BeneficioJpaRepository jpaRepository;
    private final BeneficioEntityMapper mapper;

    @Override
    public Beneficio save(Beneficio beneficio) {
        BeneficioEntity entity = mapper.toEntity(beneficio);
        BeneficioEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Beneficio> findById(String id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Beneficio> findActivosByAfiliadoAndTipo(String afiliadoId, TipoBeneficio tipo) {
        return jpaRepository.findByAfiliadoIdAndTipoBeneficioAndEstado(afiliadoId, tipo.name(), "ACTIVO")
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
