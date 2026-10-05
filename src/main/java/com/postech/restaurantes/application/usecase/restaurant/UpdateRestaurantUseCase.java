package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;

/** Atualização de dados de restaurante existente. */
public final class UpdateRestaurantUseCase {

    private final IRestaurantGateway restaurantGateway;
    private final IUserGateway userGateway;

    private UpdateRestaurantUseCase(IRestaurantGateway restaurantGateway, IUserGateway userGateway) {
        this.restaurantGateway = restaurantGateway;
        this.userGateway = userGateway;
    }

    public static UpdateRestaurantUseCase create(IRestaurantGateway restaurantGateway, IUserGateway userGateway) {
        return new UpdateRestaurantUseCase(restaurantGateway, userGateway);
    }

    public Restaurant run(UpdateRestaurantDTO dto) {
        Guard.requireNonNull(dto, "Dados do restaurante inválidos");
        Guard.requireNonNull(dto.id(), "Id do restaurante inválido");

        Restaurant existing = restaurantGateway.findById(dto.id())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));

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

        existing.setUserId(dto.userId());
        existing.setAddressId(dto.addressId());
        existing.setName(dto.name());
        existing.setOfficeHours(dto.officeHourStart(), dto.officeHourEnd());

        return restaurantGateway.update(existing);
    }
}
