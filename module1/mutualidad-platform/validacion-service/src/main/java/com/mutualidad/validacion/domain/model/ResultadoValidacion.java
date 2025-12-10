package com.mutualidad.validacion.domain.model;

public enum ResultadoValidacion {
    PENDIENTE("Pendiente de validacion"),
    EN_PROCESO("En proceso de validacion"),
    APROBADO("Validacion aprobada"),
    RECHAZADO("Validacion rechazada"),
    ERROR("Error en la validacion"),
    EXPIRADO("Validacion expirada");

    private final String descripcion;

    ResultadoValidacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
