package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.dto.restaurant.OfficeHourDTO;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.application.policy.restaurant.ExistingCuisinesPolicy;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;

/**
 * Caso de uso de criação de restaurante.
 *
 * <p>Regras de aplicação: o dono deve existir e ter perfil de dono de restaurante
 * ({@code ROLE_OWNER}) — um administrador sem esse perfil não é dono. O endereço chega no pedido e é do
 * restaurante — não é escolhido entre os endereços do dono. Os tipos de cozinha (cuisineIds), quando informados,
 * precisam existir no catálogo; sem eles, o restaurante nasce sem nenhum.
 */
public final class CreateRestaurantUseCase {

    private final IRestaurantGateway restaurantGateway;
    private final IUserGateway userGateway;
    private final ICuisineGateway cuisineGateway;

    private CreateRestaurantUseCase(IRestaurantGateway restaurantGateway, IUserGateway userGateway,
                                    ICuisineGateway cuisineGateway) {
        this.restaurantGateway = restaurantGateway;
        this.userGateway = userGateway;
        this.cuisineGateway = cuisineGateway;
    }

    public static CreateRestaurantUseCase create(IRestaurantGateway restaurantGateway, IUserGateway userGateway,
                                                 ICuisineGateway cuisineGateway) {
        return new CreateRestaurantUseCase(restaurantGateway, userGateway, cuisineGateway);
    }

    public Restaurant run(CreateRestaurantDTO dto) {
        Guard.requireNonNull(dto, "Dados do restaurante inválidos");
        User user = userGateway.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Dono do restaurante não encontrado"));

        if (!user.hasRole(RoleName.ROLE_OWNER)) {
            throw new ForbiddenOperationException("O usuário informado não tem perfil de dono de restaurante");
        }

        Restaurant restaurant = Restaurant.create(
                dto.userId(),
                Guard.requireNonNull(dto.address(), "Endereço do restaurante inválido").toEntity(),
                dto.name(),
                OfficeHourDTO.toEntities(dto.officeHours()),
                dto.cuisineIds() == null
                        ? List.of()
                        : ExistingCuisinesPolicy.create(cuisineGateway).requireAll(dto.cuisineIds())
        );

        return restaurantGateway.insert(restaurant);
    }
}
