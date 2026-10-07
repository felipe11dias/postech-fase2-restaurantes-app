package com.postech.restaurantes.application.policy.user;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.user.ProfileType;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceInUseException;

/**
 * O sistema não fica sem administrador: o perfil de administrador só é concedido por outro administrador
 * (Etapa 22), e sem nenhum ninguém mais o concederia. Vale para quem tira o perfil e para quem exclui o
 * cadastro — dois casos de uso, uma regra só.
 */
public final class LastAdminPolicy {

    private final IUserGateway userGateway;

    private LastAdminPolicy(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static LastAdminPolicy create(IUserGateway userGateway) {
        return new LastAdminPolicy(userGateway);
    }

    /**
     * Recusa a operação quando o usuário é o último administrador; quem não é administrador passa sem consulta.
     *
     * @param message o que o usuário lê, dito pela operação recusada
     */
    public void requireNotLastAdmin(User user, String message) {
        if (user.getProfiles().has(ProfileType.ADMIN) && userGateway.countAdmins() <= 1) {
            throw new ResourceInUseException(message);
        }
    }
}
