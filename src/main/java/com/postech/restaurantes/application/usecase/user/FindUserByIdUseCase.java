package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Consulta de usuário por id. */
public final class FindUserByIdUseCase {

    private final IUserGateway userGateway;

    private FindUserByIdUseCase(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static FindUserByIdUseCase create(IUserGateway userGateway) {
        return new FindUserByIdUseCase(userGateway);
    }

    public User run(UUID id) {
        return userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
