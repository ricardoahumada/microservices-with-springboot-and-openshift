package com.mutualidad.afiliado.infrastructure.adapter.output.notification;

import com.mutualidad.afiliado.application.port.output.NotificacionPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogNotificationAdapter implements NotificacionPort {

    @Override
    public void enviarBienvenida(String email, String nombre) {
        log.info("NOTIFICACION: Enviando bienvenida a {} ({})", nombre, email);
    }

    @Override
    public void notificarBaja(String email, String nombre, String motivo) {
        log.info("NOTIFICACION: Notificando baja a {} ({}). Motivo: {}", nombre, email, motivo);
    }

    @Override
    public void notificarReactivacion(String email, String nombre) {
        log.info("NOTIFICACION: Notificando reactivacion a {} ({})", nombre, email);
    }
}
