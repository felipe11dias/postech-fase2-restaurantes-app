package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Busca de restaurante por ID. */
public final class FindRestaurantByIdUseCase {

    private final IRestaurantGateway restaurantGateway;

    private FindRestaurantByIdUseCase(IRestaurantGateway restaurantGateway) {
        this.restaurantGateway = restaurantGateway;
    }

    public static FindRestaurantByIdUseCase create(IRestaurantGateway restaurantGateway) {
        return new FindRestaurantByIdUseCase(restaurantGateway);
    }

    public Restaurant run(UUID id) {
        Guard.requireNonNull(id, "Id do restaurante inválido");
        return restaurantGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
    }
}
