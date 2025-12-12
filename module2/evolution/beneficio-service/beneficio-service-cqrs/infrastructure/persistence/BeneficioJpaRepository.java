package com.mutualidad.beneficio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficioJpaRepository extends JpaRepository<BeneficioEntity, String> {
    List<BeneficioEntity> findByAfiliadoIdAndTipoBeneficioAndEstado(String afiliadoId, String tipoBeneficio, String estado);
    List<BeneficioEntity> findByAfiliadoId(String afiliadoId);
    List<BeneficioEntity> findByAfiliadoIdAndEstado(String afiliadoId, String estado);
}
