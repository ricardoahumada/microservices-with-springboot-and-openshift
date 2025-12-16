package com.mutualidad.beneficio.infrastructure.persistence;

import com.mutualidad.beneficio.domain.model.Beneficio;
import com.mutualidad.beneficio.domain.model.TipoBeneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficioJpaRepository extends JpaRepository<Beneficio, Long> {
    
    List<Beneficio> findByTipo(TipoBeneficio tipo);
    
    List<Beneficio> findByActivoTrue();
}
