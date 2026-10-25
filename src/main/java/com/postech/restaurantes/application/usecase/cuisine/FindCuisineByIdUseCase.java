package com.postech.restaurantes.application.usecase.cuisine;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;

import java.util.UUID;

public final class FindCuisineByIdUseCase {

    private final ICuisineGateway cuisineGateway;

    private FindCuisineByIdUseCase(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static FindCuisineByIdUseCase create(ICuisineGateway cuisineGateway) {
        return new FindCuisineByIdUseCase(cuisineGateway);
    }

    public Cuisine run(UUID id) {
        Guard.requireNonNull(id, "Id do tipo de cozinha inválido");
        return cuisineGateway.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cozinha não encontrado"));
    }
}
