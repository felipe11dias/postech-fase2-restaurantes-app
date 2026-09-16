package com.postech.restaurantes.domain.exception;

/** E-mail ou login já cadastrado. */
public class DuplicateResourceException extends DomainException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
