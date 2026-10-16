package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Regra de posse do restaurante, usada nas expressões {@code @PreAuthorize}: o restaurante pedido é do
 * requisitante? É a {@link UserSecurity} aplicada ao restaurante — a mesma regra contra referência direta
 * a objeto (IDOR): ter o papel de dono não basta para alterar o restaurante de outro dono.
 *
 * <p>Diferente do usuário, o dono não está no caminho da URL: é preciso perguntar de quem é o restaurante
 * ({@link IRestaurantOwnerReader}). Restaurante que não existe não é de ninguém — o dono recebe 403, e
 * só o administrador chega ao 404.
 */
@Component("restaurantSecurity")
public class RestaurantSecurity {

    private final IRestaurantOwnerReader owners;

    public RestaurantSecurity(IRestaurantOwnerReader owners) {
        this.owners = owners;
    }

    public boolean isOwner(UUID restaurantId, Authentication authentication) {
        return restaurantId != null
                && authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && owners.ownerOf(restaurantId).filter(user.id()::equals).isPresent();
    }
}
