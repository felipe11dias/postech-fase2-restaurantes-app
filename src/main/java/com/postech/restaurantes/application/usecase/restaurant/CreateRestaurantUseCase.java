package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;

/**
 * Caso de uso de criação de restaurante.
 *
 * <p>Regras de aplicação: o dono deve existir, possuir papel de dono de restaurante
 * ({@code ROLE_OWNER}) ou admin ({@code ROLE_ADMIN}), e o endereço associado deve pertencer ao usuário.
 */
public final class CreateRestaurantUseCase {

    private final IRestaurantGateway restaurantGateway;
    private final IUserGateway userGateway;

    private CreateRestaurantUseCase(IRestaurantGateway restaurantGateway, IUserGateway userGateway) {
        this.restaurantGateway = restaurantGateway;
        this.userGateway = userGateway;
    }

    public static CreateRestaurantUseCase create(IRestaurantGateway restaurantGateway, IUserGateway userGateway) {
        return new CreateRestaurantUseCase(restaurantGateway, userGateway);
    }

    public Restaurant run(CreateRestaurantDTO dto) {
        Guard.requireNonNull(dto, "Dados do restaurante inválidos");
        User user = userGateway.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Dono do restaurante não encontrado"));

        if (!user.hasRole(RoleName.ROLE_OWNER) && !user.hasRole(RoleName.ROLE_ADMIN)) {
            throw new ForbiddenOperationException("O usuário informado não possui papel de dono de restaurante");
        }

        boolean addressBelongsToUser = user.getAddresses().stream()
                .anyMatch(addr -> addr.getId().equals(dto.addressId()));
        if (!addressBelongsToUser) {
            throw new ResourceNotFoundException("Endereço não encontrado ou não pertence ao usuário");
        }

        Restaurant restaurant = Restaurant.create(
                dto.userId(),
                dto.addressId(),
                dto.name(),
                dto.officeHourStart(),
                dto.officeHourEnd()
        );

        return restaurantGateway.insert(restaurant);
    }
}
