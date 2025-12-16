package com.mutualidad.validacion.domain.model;

public enum TipoValidacion {
    IDENTIDAD("Validacion de identidad"),
    DOMICILIO("Validacion de domicilio"),
    LABORAL("Validacion laboral"),
    BANCARIA("Validacion bancaria"),
    ANTECEDENTES("Validacion de antecedentes");

    private final String descripcion;

    TipoValidacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
