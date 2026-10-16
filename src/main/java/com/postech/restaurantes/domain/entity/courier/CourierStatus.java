package com.postech.restaurantes.domain.entity.courier;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.InvariantViolationException;

/** Disponibilidade do entregador — o tipo {@code courier_status} do banco. Todo entregador nasce {@link #OFFLINE}. */
public enum CourierStatus {
    OFFLINE,
    AVAILABLE,
    BUSY;

    /** Converte o nome textual, recusando valores desconhecidos com mensagem de domínio. */
    public static CourierStatus from(String name) {
        String normalized = Guard.requireNonBlank(name, "Status do entregador inválido").toUpperCase();
        for (CourierStatus candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new InvariantViolationException("Status do entregador inválido: " + name);
    }
}
