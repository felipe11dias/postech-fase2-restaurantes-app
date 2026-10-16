package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import java.util.Set;
import java.util.UUID;

/**
 * Os papéis que o usuário tem agora — derivados dos perfis gravados, e não os que o token trazia no
 * login. Porta declarada por quem a consome (o {@link BearerTokenAuthenticationFilter}), como a
 * {@link IAccessTokenReader}; quem a liga ao núcleo é a composição ({@code main}).
 *
 * <p>Contrato: usuário que não existe mais devolve conjunto vazio, nunca exceção.
 */
public interface ICurrentRolesReader {

    Set<String> currentRoles(UUID userId);
}
