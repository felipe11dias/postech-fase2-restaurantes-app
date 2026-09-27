package com.postech.restaurantes.infrastructure.main;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Validade do token de redefinição de senha. É política de autenticação — o caso de uso a usa para
 * calcular o vencimento e a repassa ao e-mail —, e não configuração de transporte: por isso mora
 * na composição, e não no módulo de e-mail.
 */
@ConfigurationProperties(prefix = "password-reset")
public record PasswordResetProperties(int tokenExpirationMinutes) {

    public PasswordResetProperties {
        if (tokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("Validade do token de redefinição deve ser positiva");
        }
    }

    public Duration tokenValidity() {
        return Duration.ofMinutes(tokenExpirationMinutes);
    }
}
