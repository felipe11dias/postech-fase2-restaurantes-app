package com.postech.restaurantes.domain.exception;

/** Usuário, papel ou outro recurso inexistente. */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
