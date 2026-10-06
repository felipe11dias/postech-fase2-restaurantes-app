package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.util.Optional;
import java.util.UUID;

/**
 * Quem é o dono de um restaurante — o que a regra de posse precisa saber antes de deixar alterar ou
 * excluir. Restaurante inexistente não tem dono (vazio, não exceção): quem pergunta só quer saber se o
 * requisitante é o dono, e a resposta é "não".
 */
public final class FindRestaurantOwnerUseCase {

    private final IRestaurantGateway restaurantGateway;

    private FindRestaurantOwnerUseCase(IRestaurantGateway restaurantGateway) {
        this.restaurantGateway = restaurantGateway;
    }

    public static FindRestaurantOwnerUseCase create(IRestaurantGateway restaurantGateway) {
        return new FindRestaurantOwnerUseCase(restaurantGateway);
    }

    public Optional<UUID> run(UUID restaurantId) {
        return restaurantGateway.findById(Guard.requireNonNull(restaurantId, "Id do restaurante inválido"))
                .map(Restaurant::getUserId);
    }
}
