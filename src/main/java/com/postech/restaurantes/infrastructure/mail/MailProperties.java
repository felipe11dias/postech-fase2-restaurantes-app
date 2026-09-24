package com.postech.restaurantes.infrastructure.mail;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Remetente e validade do token de redefinição. */
@ConfigurationProperties(prefix = "mail")
public record MailProperties(String from, int resetTokenExpirationMinutes) {

    public MailProperties {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Remetente de e-mail deve ser informado");
        }
        if (resetTokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("Validade do token de redefinição deve ser positiva");
        }
    }

    public Duration resetTokenValidity() {
        return Duration.ofMinutes(resetTokenExpirationMinutes);
    }
}
