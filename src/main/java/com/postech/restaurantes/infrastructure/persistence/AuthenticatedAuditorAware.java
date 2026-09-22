package com.postech.restaurantes.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem está gravando. Responde ao {@code AuditingEntityListener} com o login autenticado ou
 * com {@value #SYSTEM} quando não há ninguém — requisição pública, migration ou seed.
 *
 * <p>Saber quem é o usuário corrente é assunto do contexto de execução, não do domínio: por
 * isso a consulta ao {@code SecurityContextHolder} fica confinada à infraestrutura.
 */
@Component
public class AuthenticatedAuditorAware implements AuditorAware<String> {

    /** Autor registrado quando a operação não parte de um usuário autenticado. */
    public static final String SYSTEM = "system";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM);
        }
        return Optional.of(authentication.getName());
    }
}
