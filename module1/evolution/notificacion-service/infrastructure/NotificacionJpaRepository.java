package com.mutualidad.notificacion.infrastructure.persistence;

import com.mutualidad.notificacion.domain.model.Canal;
import com.mutualidad.notificacion.domain.model.EstadoNotificacion;
import com.mutualidad.notificacion.domain.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionJpaRepository extends JpaRepository<Notificacion, Long> {
    
    List<Notificacion> findByDestinatarioAfiliadoId(String afiliadoId);
    
    List<Notificacion> findByEstado(EstadoNotificacion estado);
    
    List<Notificacion> findByCanal(Canal canal);
    
    List<Notificacion> findByEstadoAndIntentosEnvioLessThan(EstadoNotificacion estado, Integer maxIntentos);
}
