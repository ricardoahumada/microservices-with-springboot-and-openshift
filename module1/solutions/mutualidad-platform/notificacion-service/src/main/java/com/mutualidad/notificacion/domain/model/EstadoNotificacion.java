package com.mutualidad.notificacion.domain.model;

public enum EstadoNotificacion {
    PENDIENTE("Pendiente de envio"),
    ENVIADA("Enviada"),
    ENTREGADA("Entregada"),
    FALLIDA("Fallo en el envio"),
    LEIDA("Leida por el destinatario");

    private final String descripcion;

    EstadoNotificacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
