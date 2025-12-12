package com.mutualidad.beneficio.domain.model;

public enum EstadoSolicitud {
    PENDIENTE("Pendiente de revision"),
    EN_REVISION("En revision"),
    APROBADA("Aprobada"),
    RECHAZADA("Rechazada"),
    PAGADA("Pagada");

    private final String descripcion;

    EstadoSolicitud(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
