package com.postech.restaurantes.application.usecase.cuisine;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.ResourceInUseException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

public final class DeleteCuisineUseCase {

    private final ICuisineGateway cuisineGateway;

    private DeleteCuisineUseCase(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static DeleteCuisineUseCase create(ICuisineGateway cuisineGateway) {
        return new DeleteCuisineUseCase(cuisineGateway);
    }

    public void run(UUID id) {
        Guard.requireNonNull(id, "Id do tipo de cozinha inválido");
        cuisineGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cozinha não encontrado"));
        if (cuisineGateway.isUsedByRestaurants(id)) {
            throw new ResourceInUseException("Tipo de cozinha em uso por restaurantes");
        }
        cuisineGateway.delete(id);
    }
}