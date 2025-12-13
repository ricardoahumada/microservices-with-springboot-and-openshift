package com.mutualidad.beneficio.infrastructure.persistence;

import com.mutualidad.beneficio.domain.model.Beneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {
    List<Beneficio> findByAfiliadoId(Long afiliadoId);
    List<Beneficio> findByAfiliadoIdAndEstado(Long afiliadoId, String estado);
}
