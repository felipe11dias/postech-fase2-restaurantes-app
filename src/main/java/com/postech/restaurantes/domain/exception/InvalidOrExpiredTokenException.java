package com.postech.restaurantes.domain.exception;

/** Token de redefinição inexistente, expirado ou já usado. */
public class InvalidOrExpiredTokenException extends DomainException {

    public InvalidOrExpiredTokenException(String message) {
        super(message);
    }
}
