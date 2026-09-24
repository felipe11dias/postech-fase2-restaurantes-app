package com.postech.restaurantes.infrastructure.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Regra de posse usada nas expressões {@code @PreAuthorize}: o recurso pedido é do próprio
 * requisitante?
 *
 * <p>Fecha a referência direta a objeto (IDOR): sem ela, conhecer o id de outro usuário
 * bastaria para lê-lo ou alterá-lo. O UUID aleatório dificulta descobrir ids, mas dificultar
 * não é impedir — a verificação é que impede.
 */
@Component("userSecurity")
public class UserSecurity {

    public boolean isSelf(UUID id, Authentication authentication) {
        return id != null
                && authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && id.equals(user.id());
    }
}
