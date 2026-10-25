package com.postech.restaurantes.application.usecase.cuisine;

import com.postech.restaurantes.application.dto.cuisine.UpdateCuisineDTO;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.application.policy.cuisine.UniqueCuisineNamePolicy;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;

public final class UpdateCuisineUseCase {

    private final ICuisineGateway cuisineGateway;

    private UpdateCuisineUseCase(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static UpdateCuisineUseCase create(ICuisineGateway cuisineGateway) {
        return new UpdateCuisineUseCase(cuisineGateway);
    }

    public Cuisine run(UpdateCuisineDTO dto) {
        Guard.requireNonNull(dto, "Dados do tipo de cozinha inválidos");
        Guard.requireNonNull(dto.id(), "Id do tipo de cozinha inválido");

        Cuisine existing = cuisineGateway.findById(dto.id())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de cozinha não encontrado"));
        existing.setName(dto.name());

        UniqueCuisineNamePolicy.create(cuisineGateway).requireUnique(existing.getName(), existing.getId());

        return cuisineGateway.update(existing);
    }
}