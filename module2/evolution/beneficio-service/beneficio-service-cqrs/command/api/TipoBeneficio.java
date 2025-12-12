package com.mutualidad.beneficio.command.api;

public enum TipoBeneficio {
    SALUD("Cobertura de salud", true),
    SUBSIDIO_DESEMPLEO("Subsidio por desempleo", true),
    SUBSIDIO_INCAPACIDAD("Subsidio por incapacidad", true),
    DESCUENTO_FARMACIA("Descuento en farmacias", false),
    DESCUENTO_OPTICA("Descuento en opticas", false),
    AYUDA_FAMILIAR("Ayuda por nacimiento/adopcion", true),
    FORMACION("Ayuda para formacion", true),
    JUBILACION_COMPLEMENTARIA("Complemento de jubilacion", true);

    private final String descripcion;
    private final boolean requiereMonto;

    TipoBeneficio(String descripcion, boolean requiereMonto) {
        this.descripcion = descripcion;
        this.requiereMonto = requiereMonto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean requiereMonto() {
        return requiereMonto;
    }
}
