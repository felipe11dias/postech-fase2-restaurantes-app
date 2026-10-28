package com.postech.restaurantes.application.usecase.cuisine;

import com.postech.restaurantes.application.dto.cuisine.CreateCuisineDTO;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.application.policy.cuisine.UniqueCuisineNamePolicy;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;

public final class CreateCuisineUseCase {

    private final ICuisineGateway cuisineGateway;

    private CreateCuisineUseCase(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static CreateCuisineUseCase create(ICuisineGateway cuisineGateway) {
        return  new CreateCuisineUseCase(cuisineGateway);
    }

    public Cuisine run(CreateCuisineDTO dto) {
        Guard.requireNonNull(dto, "Dados do tipo de cozinha inválidos");
        Cuisine cuisine = Cuisine.create(dto.name());
        UniqueCuisineNamePolicy.create(cuisineGateway).requireUnique(cuisine.getName(), null);

        return cuisineGateway.insert(cuisine);
    }
}
