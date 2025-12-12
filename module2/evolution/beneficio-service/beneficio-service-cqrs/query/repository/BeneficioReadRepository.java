package com.mutualidad.beneficio.query.repository;

import com.mutualidad.beneficio.query.model.BeneficioReadModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface BeneficioReadRepository extends JpaRepository<BeneficioReadModel, String> {

    Page<BeneficioReadModel> findByAfiliadoId(String afiliadoId, Pageable pageable);

    Page<BeneficioReadModel> findByAfiliadoIdAndEstado(String afiliadoId, String estado, Pageable pageable);

    Page<BeneficioReadModel> findByAfiliadoIdAndTipoBeneficio(String afiliadoId, String tipoBeneficio, Pageable pageable);

    List<BeneficioReadModel> findByAfiliadoIdAndEstaVigenteTrue(String afiliadoId);

    @Query("SELECT COUNT(b) FROM BeneficioReadModel b WHERE b.afiliadoId = :afiliadoId AND b.estado = :estado")
    long countByAfiliadoIdAndEstado(String afiliadoId, String estado);

    @Query("SELECT COALESCE(SUM(b.monto), 0) FROM BeneficioReadModel b WHERE b.afiliadoId = :afiliadoId AND b.estado = 'ACTIVO'")
    BigDecimal sumMontoActivoByAfiliado(String afiliadoId);
}
