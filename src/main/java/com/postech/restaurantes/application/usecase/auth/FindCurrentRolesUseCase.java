package com.postech.restaurantes.application.usecase.auth;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import java.util.Set;
import java.util.UUID;

/**
 * Os papéis que o usuário tem <em>agora</em>, derivados dos perfis gravados. A autorização de cada
 * requisição usa este resultado, e não os papéis que o token trazia no login: perfil removido deixa de
 * autorizar na hora, e não só quando o token vence. Usuário que não existe mais não tem papel nenhum.
 */
public final class FindCurrentRolesUseCase {

    private final IUserGateway userGateway;

    private FindCurrentRolesUseCase(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static FindCurrentRolesUseCase create(IUserGateway userGateway) {
        return new FindCurrentRolesUseCase(userGateway);
    }

    public Set<RoleName> run(UUID id) {
        return userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .map(User::getRoles)
                .orElse(Set.of());
    }
}
