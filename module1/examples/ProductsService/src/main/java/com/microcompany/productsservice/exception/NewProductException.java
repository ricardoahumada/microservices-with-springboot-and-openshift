package com.microcompany.productsservice.exception;


public class NewProductException extends GlobalException {
    private static final long serialVersionUID = 2L;

    public NewProductException(String message) {
        super(message);
    }
}