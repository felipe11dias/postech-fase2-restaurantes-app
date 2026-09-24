package com.postech.restaurantes.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do token de acesso. O segredo precisa ter ao menos 256 bits porque a assinatura
 * é HMAC-SHA256: um segredo curto é recusado <strong>na subida da aplicação</strong>, e não na
 * primeira tentativa de login.
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    /** 256 bits — o mínimo exigido pela especificação do HMAC-SHA256. */
    static final int MINIMUM_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "Segredo do JWT deve ter ao menos " + MINIMUM_SECRET_BYTES + " bytes (256 bits)");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("Expiração do JWT deve ser positiva");
        }
    }
}
