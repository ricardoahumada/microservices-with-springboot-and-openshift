package com.mutualidad.afiliado.domain.model;

public enum TipoDocumento {
    DNI("Documento Nacional de Identidad"),
    NIE("Numero de Identidad de Extranjero"),
    PASAPORTE("Pasaporte");

    private final String descripcion;

    TipoDocumento(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
