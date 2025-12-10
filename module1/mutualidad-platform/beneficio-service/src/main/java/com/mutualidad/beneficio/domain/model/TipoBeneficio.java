package com.mutualidad.beneficio.domain.model;

public enum TipoBeneficio {
    SUBSIDIO_ENFERMEDAD("Subsidio por enfermedad"),
    SUBSIDIO_ACCIDENTE("Subsidio por accidente"),
    AYUDA_DEFUNCION("Ayuda por defuncion"),
    PRESTAMO_PERSONAL("Prestamo personal"),
    AYUDA_ESCOLAR("Ayuda escolar");

    private final String descripcion;

    TipoBeneficio(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
