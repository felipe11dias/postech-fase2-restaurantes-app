package com.postech.restaurantes.domain.exception;

/**
 * Recurso que não pode ser removido enquanto outro depender dele — como o perfil de dono de quem
 * ainda tem restaurantes. Conflito com o estado atual, não pedido malformado nem falta de permissão.
 */
public class ResourceInUseException extends DomainException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
