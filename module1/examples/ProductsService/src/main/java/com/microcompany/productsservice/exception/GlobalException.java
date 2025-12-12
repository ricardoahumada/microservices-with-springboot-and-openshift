package com.microcompany.productsservice.exception;

public class GlobalException extends RuntimeException {
    protected static final long serialVersionUID = 1L;

    public GlobalException() {
    }

    public GlobalException(String message) {
        super(message);
    }

    public GlobalException(Long id) {
        super("GlobalException for element with id: " + id);
    }
}