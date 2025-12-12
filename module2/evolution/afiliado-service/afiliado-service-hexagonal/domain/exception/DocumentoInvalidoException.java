package com.mutualidad.afiliado.domain.exception;

public class DocumentoInvalidoException extends AfiliadoException {
    public DocumentoInvalidoException(String documento) {
        super("El documento no pudo ser validado: " + documento);
    }
}
