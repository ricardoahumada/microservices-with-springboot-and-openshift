package com.mutualidad.notificacion.domain.model;

public enum Canal {
    EMAIL("Correo electronico"),
    SMS("Mensaje de texto"),
    PUSH("Notificacion push"),
    POSTAL("Correo postal");

    private final String descripcion;

    Canal(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
