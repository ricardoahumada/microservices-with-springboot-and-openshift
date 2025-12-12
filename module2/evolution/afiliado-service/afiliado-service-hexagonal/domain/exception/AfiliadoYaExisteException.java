package com.mutualidad.afiliado.domain.exception;

public class AfiliadoYaExisteException extends AfiliadoException {
    public AfiliadoYaExisteException(String documento) {
        super("Ya existe un afiliado con el documento: " + documento);
    }
}
