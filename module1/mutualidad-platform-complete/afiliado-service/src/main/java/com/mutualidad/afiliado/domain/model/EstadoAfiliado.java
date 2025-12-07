package com.mutualidad.afiliado.domain.model;

public enum EstadoAfiliado {
    PENDIENTE("Pendiente de validación"),
    ACTIVO("Activo"),
    SUSPENDIDO("Suspendido temporalmente"),
    BAJA("Baja definitiva");

    private final String descripcion;

    EstadoAfiliado(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean permiteOperaciones() {
        return this == ACTIVO;
    }
}
