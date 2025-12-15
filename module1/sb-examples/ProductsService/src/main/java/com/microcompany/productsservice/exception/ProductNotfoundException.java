package com.microcompany.productsservice.exception;

public class ProductNotfoundException extends GlobalException {
    private static final long serialVersionUID = 2L;

    public ProductNotfoundException() {
    }

    public ProductNotfoundException(String message) {
        super(message);
    }

    public ProductNotfoundException(Long id) {
        super(id);
    }
}
