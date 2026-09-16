package com.postech.restaurantes.domain.exception;

/** Operação não permitida no contexto, como autocadastro solicitando papel privilegiado. */
public class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
