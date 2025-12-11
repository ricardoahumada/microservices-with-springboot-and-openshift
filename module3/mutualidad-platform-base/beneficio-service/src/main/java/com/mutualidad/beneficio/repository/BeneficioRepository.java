package com.mutualidad.beneficio.repository;

import com.mutualidad.beneficio.entity.Beneficio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficioRepository extends JpaRepository<Beneficio, Long> {
    List<Beneficio> findByAfiliadoId(Long afiliadoId);
    List<Beneficio> findByAfiliadoIdAndEstado(Long afiliadoId, String estado);
}
