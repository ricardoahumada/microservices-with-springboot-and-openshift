package com.mutualidad.afiliado.domain.exception;

public class AfiliadoNoEncontradoException extends AfiliadoException {
    public AfiliadoNoEncontradoException(String id) {
        super("No se encontro el afiliado con ID: " + id);
    }
}
