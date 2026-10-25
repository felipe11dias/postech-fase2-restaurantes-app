package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.dto.restaurant.OfficeHourDTO;
import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
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

/**
 * Atualização de dados de restaurante existente. Sem {@code userId} no pedido, o dono continua o mesmo;
 * com ele, o indicado precisa existir e ter perfil de dono. Quem pode trocar o dono é decidido na entrada
 * (só um administrador). Sem {@code cuisineIds}, os tipos de cozinha continuam os mesmos; com eles, a lista
 * inteira é trocada (vazia tira todos), e todos precisam existir no catálogo.
 */
public final class UpdateRestaurantUseCase {

    private final IRestaurantGateway restaurantGateway;
    private final IUserGateway userGateway;
    private final ICuisineGateway cuisineGateway;

    private UpdateRestaurantUseCase(IRestaurantGateway restaurantGateway, IUserGateway userGateway,
                                    ICuisineGateway cuisineGateway) {
        this.restaurantGateway = restaurantGateway;
        this.userGateway = userGateway;
        this.cuisineGateway = cuisineGateway;
    }

    public static UpdateRestaurantUseCase create(IRestaurantGateway restaurantGateway, IUserGateway userGateway,
                                                 ICuisineGateway cuisineGateway) {
        return new UpdateRestaurantUseCase(restaurantGateway, userGateway, cuisineGateway);
    }

    public Restaurant run(UpdateRestaurantDTO dto) {
        Guard.requireNonNull(dto, "Dados do restaurante inválidos");
        Guard.requireNonNull(dto.id(), "Id do restaurante inválido");

        Restaurant existing = restaurantGateway.findById(dto.id())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));

        if (dto.userId() != null) {
            User user = userGateway.findById(dto.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Dono do restaurante não encontrado"));
            if (!user.hasRole(RoleName.ROLE_OWNER)) {
                throw new ForbiddenOperationException("O usuário informado não tem perfil de dono de restaurante");
            }
            existing.setUserId(dto.userId());
        }
        existing.setAddress(Guard.requireNonNull(dto.address(), "Endereço do restaurante inválido").toEntity());
        existing.setName(dto.name());
        existing.replaceOfficeHours(OfficeHourDTO.toEntities(dto.officeHours()));
        if (dto.cuisineIds() != null) {
            existing.replaceCuisines(ExistingCuisinesPolicy.create(cuisineGateway).requireAll(dto.cuisineIds()));
        }

        return restaurantGateway.update(existing);
    }
}
