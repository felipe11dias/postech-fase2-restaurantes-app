package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.application.policy.user.LastAdminPolicy;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.ProfileType;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceInUseException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/**
 * Remove um perfil do usuário. "Ao menos um perfil" é invariante do domínio — tirar o último é
 * recusado pelo próprio {@code UserProfiles}. As regras de aplicação ficam aqui: o perfil precisa
 * existir; o de dono não sai enquanto houver restaurante do usuário (o restaurante perderia o dono); e o de
 * administrador não sai do último administrador ({@link LastAdminPolicy}).
 */
public final class RemoveUserProfileUseCase {

    private final IUserGateway userGateway;
    private final IRestaurantGateway restaurantGateway;
    private final LastAdminPolicy lastAdmin;

    private RemoveUserProfileUseCase(IUserGateway userGateway, IRestaurantGateway restaurantGateway) {
        this.userGateway = userGateway;
        this.restaurantGateway = restaurantGateway;
        this.lastAdmin = LastAdminPolicy.create(userGateway);
    }

    public static RemoveUserProfileUseCase create(IUserGateway userGateway, IRestaurantGateway restaurantGateway) {
        return new RemoveUserProfileUseCase(userGateway, restaurantGateway);
    }

    public User run(UUID id, String type) {
        ProfileType profileType = ProfileType.from(type);
        User user = userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (!user.getProfiles().has(profileType)) {
            throw new ResourceNotFoundException("O usuário não tem perfil de " + profileType.description());
        }
        if (profileType == ProfileType.OWNER && restaurantGateway.existsByUserId(id)) {
            throw new ResourceInUseException(
                    "O perfil de dono não pode ser removido enquanto o usuário tiver restaurantes");
        }
        if (profileType == ProfileType.ADMIN) {
            lastAdmin.requireNotLastAdmin(user, "O último administrador não pode deixar de ser administrador");
        }
        user.replaceProfiles(user.getProfiles().without(profileType));
        return userGateway.update(user);
    }
}
