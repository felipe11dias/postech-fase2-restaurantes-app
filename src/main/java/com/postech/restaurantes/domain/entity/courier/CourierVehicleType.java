package com.postech.restaurantes.domain.entity.courier;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.InvariantViolationException;

/** Meio de transporte do entregador — o tipo {@code courier_vehicle_type} do banco. */
public enum CourierVehicleType {
    ON_FOOT,
    BICYCLE,
    MOTORCYCLE,
    CAR;

    /** Converte o nome textual, recusando valores desconhecidos com mensagem de domínio. */
    public static CourierVehicleType from(String name) {
        String normalized = Guard.requireNonBlank(name, "Tipo de veículo inválido").toUpperCase();
        for (CourierVehicleType candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new InvariantViolationException("Tipo de veículo inválido: " + name);
    }

    /** Veículo motorizado: exige CNH e placa (o que o modelo de dados registra em comentário de coluna). */
    public boolean requiresLicense() {
        return this == MOTORCYCLE || this == CAR;
    }
}
