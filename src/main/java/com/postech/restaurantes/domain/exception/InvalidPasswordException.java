package com.postech.restaurantes.domain.exception;

/** Senha atual incorreta ou confirmação divergente. */
public class InvalidPasswordException extends DomainException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
