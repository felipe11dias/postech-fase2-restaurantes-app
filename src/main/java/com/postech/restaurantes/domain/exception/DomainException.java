package com.postech.restaurantes.domain.exception;

/**
 * Base das exceções de domínio. Não verificadas e sem anotações: quem as traduz para HTTP é
 * a infraestrutura.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
