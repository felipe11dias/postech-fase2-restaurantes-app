package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Exclusão de usuário. Endereços, tokens e vínculos de papel caem por cascade na persistência. */
public final class DeleteUserUseCase {

    private final IUserGateway userGateway;

    private DeleteUserUseCase(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static DeleteUserUseCase create(IUserGateway userGateway) {
        return new DeleteUserUseCase(userGateway);
    }

    public void run(UUID id) {
        Guard.requireNonNull(id, "Id inválido");
        if (userGateway.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("Usuário não encontrado");
        }
        userGateway.delete(id);
    }
}
