package com.postech.restaurantes.domain.entity.role;

/**
 * Papéis de autorização reconhecidos pelo sistema. Não são escolhidos nem gravados: cada papel é
 * consequência de um perfil do usuário (ver {@code UserProfiles#roles()}) — quem tem perfil de dono
 * é {@link #ROLE_OWNER}, e assim por diante. Os nomes são os que vão no token de acesso.
 */
public enum RoleName {
    ROLE_OWNER,
    ROLE_CLIENT,
    ROLE_COURIER,
    ROLE_ADMIN
}
