package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/**
 * Exclusão de usuário. Os restaurantes dele saem antes, pela porta do restaurante, na mesma unidade de
 * trabalho: assim o endereço de cada restaurante sai junto, o que o {@code ON DELETE CASCADE} do banco
 * não faria (a chave aponta para {@code addresses}, e não o contrário). Perfis, endereços e tokens do
 * usuário caem pela persistência.
 */
public final class DeleteUserUseCase {

    private final IUserGateway userGateway;
    private final IRestaurantGateway restaurantGateway;

    private DeleteUserUseCase(IUserGateway userGateway, IRestaurantGateway restaurantGateway) {
        this.userGateway = userGateway;
        this.restaurantGateway = restaurantGateway;
    }

    public static DeleteUserUseCase create(IUserGateway userGateway, IRestaurantGateway restaurantGateway) {
        return new DeleteUserUseCase(userGateway, restaurantGateway);
    }

    public void run(UUID id) {
        Guard.requireNonNull(id, "Id inválido");
        if (userGateway.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        restaurantGateway.deleteByUserId(id);
        userGateway.delete(id);
    }
}
