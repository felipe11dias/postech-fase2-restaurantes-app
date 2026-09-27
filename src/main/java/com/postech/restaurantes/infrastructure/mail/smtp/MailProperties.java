package com.postech.restaurantes.infrastructure.mail.smtp;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Remetente dos e-mails. Só o que é do transporte: a validade do token de redefinição é política
 * de autenticação, decidida pelo caso de uso e configurada em {@code main}.
 */
@ConfigurationProperties(prefix = "mail")
public record MailProperties(String from) {

    public MailProperties {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("Remetente de e-mail deve ser informado");
        }
    }
}
