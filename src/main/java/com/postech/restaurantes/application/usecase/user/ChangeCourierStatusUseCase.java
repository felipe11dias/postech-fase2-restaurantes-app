package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/**
 * O entregador informa a disponibilidade dele (fora de serviço, disponível, ocupado). O status vem como
 * texto e passa por {@code CourierStatus.from}, para um valor desconhecido produzir a mensagem do domínio.
 */
public final class ChangeCourierStatusUseCase {

    private final IUserGateway userGateway;

    private ChangeCourierStatusUseCase(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static ChangeCourierStatusUseCase create(IUserGateway userGateway) {
        return new ChangeCourierStatusUseCase(userGateway);
    }

    public User run(UUID id, String status) {
        CourierStatus newStatus = CourierStatus.from(status);
        User user = userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        CourierProfile courier = user.getProfiles().courier();
        if (courier == null) {
            throw new ResourceNotFoundException("O usuário não tem perfil de entregador");
        }
        courier.changeStatus(newStatus);
        return userGateway.update(user);
    }
}
