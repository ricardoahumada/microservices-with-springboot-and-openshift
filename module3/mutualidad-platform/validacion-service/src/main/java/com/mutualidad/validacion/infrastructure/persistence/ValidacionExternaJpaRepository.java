package com.mutualidad.validacion.infrastructure.persistence;

import com.mutualidad.validacion.domain.model.ResultadoValidacion;
import com.mutualidad.validacion.domain.model.TipoValidacion;
import com.mutualidad.validacion.domain.model.ValidacionExterna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ValidacionExternaJpaRepository extends JpaRepository<ValidacionExterna, Long> {
    
    List<ValidacionExterna> findByAfiliadoId(Long afiliadoId);
    
    List<ValidacionExterna> findByResultado(ResultadoValidacion resultado);
    
    List<ValidacionExterna> findByTipo(TipoValidacion tipo);
    
    Optional<ValidacionExterna> findByAfiliadoIdAndTipoAndResultado(
            Long afiliadoId, TipoValidacion tipo, ResultadoValidacion resultado);
    
    @Query("SELECT v FROM ValidacionExterna v WHERE v.afiliadoId = :afiliadoId " +
           "AND v.tipo = :tipo AND v.resultado = 'APROBADO' " +
           "AND v.fechaExpiracion > :now")
    Optional<ValidacionExterna> findValidacionVigente(
            Long afiliadoId, TipoValidacion tipo, LocalDateTime now);
}
