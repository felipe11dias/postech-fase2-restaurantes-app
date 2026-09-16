package com.postech.restaurantes.domain.exception;

/** Login ou senha incorretos. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
