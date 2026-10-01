package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Exclusão de restaurante existente. */
public final class DeleteRestaurantUseCase {

    private final IRestaurantGateway restaurantGateway;

    private DeleteRestaurantUseCase(IRestaurantGateway restaurantGateway) {
        this.restaurantGateway = restaurantGateway;
    }

    public static DeleteRestaurantUseCase create(IRestaurantGateway restaurantGateway) {
        return new DeleteRestaurantUseCase(restaurantGateway);
    }

    public void run(UUID id) {
        Guard.requireNonNull(id, "Id do restaurante inválido");
        restaurantGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        restaurantGateway.delete(id);
    }
}
