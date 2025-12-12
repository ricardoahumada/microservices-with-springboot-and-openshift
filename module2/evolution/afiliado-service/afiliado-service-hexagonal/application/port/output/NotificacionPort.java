package com.mutualidad.afiliado.application.port.output;

public interface NotificacionPort {

    void enviarBienvenida(String email, String nombre);

    void notificarBaja(String email, String nombre, String motivo);

    void notificarReactivacion(String email, String nombre);
}
