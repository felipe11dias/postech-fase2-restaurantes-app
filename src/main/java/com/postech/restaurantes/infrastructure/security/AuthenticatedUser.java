package com.postech.restaurantes.infrastructure.security;

import java.security.Principal;
import java.util.Set;
import java.util.UUID;

/**
 * Quem o token diz ser o portador. É o {@code principal} do contexto de segurança.
 *
 * <p>Implementa {@link Principal} de propósito: assim {@code Authentication.getName()} devolve
 * o login, que é o que a auditoria grava em {@code created_by} — e o id continua disponível
 * para a regra de posse, sem precisar de nova consulta ao banco.
 */
public record AuthenticatedUser(UUID id, String login, Set<String> roles) implements Principal {

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }

    @Override
    public String getName() {
        return login;
    }
}
