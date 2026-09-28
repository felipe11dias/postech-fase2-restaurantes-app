package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import java.util.Optional;

/**
 * Lê o token de acesso recebido no cabeçalho {@code Authorization: Bearer}.
 *
 * <p>Porta declarada por quem a consome — o {@link BearerTokenAuthenticationFilter} —, e não por
 * quem a implementa: é a inversão de dependência aplicada dentro do anel externo. A cadeia HTTP
 * não sabe qual é o formato do token; o módulo de token (hoje, {@code token/jwt}) é quem sabe.
 *
 * <p>Contrato: qualquer motivo de recusa — assinatura inválida, expirado, malformado,
 * reivindicação ausente — devolve vazio, nunca exceção, e nunca diz o motivo.
 */
public interface IAccessTokenReader {

    Optional<AuthenticatedUser> read(String token);
}
