package com.postech.restaurantes.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do token de acesso. Tudo é conferido <strong>na subida da aplicação</strong>, e
 * não na primeira tentativa de login:
 *
 * <ul>
 *   <li>o segredo precisa ter ao menos 256 bits, porque a assinatura é HMAC-SHA256;</li>
 *   <li>o segredo não pode ser um dos valores de exemplo que estão versionados no repositório.
 *       Quem conhece o segredo assina qualquer token — inclusive um com {@code ROLE_ADMIN} —, e
 *       um valor que está no Git é conhecido por definição. Tamanho suficiente não o salva.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    /** 256 bits — o mínimo exigido pela especificação do HMAC-SHA256. */
    static final int MINIMUM_SECRET_BYTES = 32;

    /** Marcadores publicados em {@code .env.example} e em versões anteriores da configuração. */
    static final Set<String> KNOWN_EXAMPLE_SECRETS = Set.of(
            "troque-este-segredo-por-um-valor-grande-de-no-minimo-256-bits");

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "Segredo do JWT ausente ou curto: defina JWT_SECRET com ao menos " + MINIMUM_SECRET_BYTES
                            + " bytes (256 bits) — ex.: openssl rand -base64 48");
        }
        if (KNOWN_EXAMPLE_SECRETS.contains(secret.strip())) {
            throw new IllegalArgumentException(
                    "Segredo do JWT é o valor de exemplo publicado no repositório; gere um próprio "
                            + "(ex.: openssl rand -base64 48) e defina JWT_SECRET");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("Expiração do JWT deve ser positiva");
        }
    }
}
