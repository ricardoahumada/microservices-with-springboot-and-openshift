package com.mutualidad.beneficio.infrastructure.persistence;

import com.mutualidad.beneficio.domain.model.EstadoSolicitud;
import com.mutualidad.beneficio.domain.model.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudJpaRepository extends JpaRepository<Solicitud, Long> {
    
    List<Solicitud> findByAfiliadoId(Long afiliadoId);
    
    List<Solicitud> findByEstado(EstadoSolicitud estado);
    
    List<Solicitud> findByAfiliadoIdAndEstado(Long afiliadoId, EstadoSolicitud estado);
}
