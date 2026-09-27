package com.postech.restaurantes.infrastructure.web.security;

import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem está autenticado na requisição corrente, pelo login. Vazio quando não há ninguém:
 * requisição pública, anônima ou autenticação ainda não confirmada.
 *
 * <p>É o único lugar que consulta o {@code SecurityContextHolder} para isso. Quem precisa saber
 * o autor — hoje, a auditoria da persistência — recebe {@link #currentLogin()} ligado pelo módulo
 * {@code main}, sem conhecer o Spring Security.
 */
@Component
public class AuthenticatedActor {

    public Optional<String> currentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication.getName());
    }
}
