package com.postech.restaurantes.infrastructure.mail.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.postech.restaurantes.IntegrationTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Sem limite de espera, o Jakarta Mail aguarda para sempre um servidor que aceita a conexão e não
 * responde — ou um que descarta os pacotes. Cada pedido de redefinição prenderia uma thread da fila
 * indefinidamente. O teste lê a configuração que a aplicação de fato montou, não o arquivo.
 */
class SmtpTimeoutIT extends IntegrationTestSupport {

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Test
    @DisplayName("O envio de e-mail tem limite de espera para conectar, ler e escrever")
    void deveLimitarAEsperaPeloServidorDeEmail() {
        for (String propriedade : List.of("mail.smtp.connectiontimeout", "mail.smtp.timeout", "mail.smtp.writetimeout")) {
            assertEquals("5000", mailSender.getJavaMailProperties().getProperty(propriedade), propriedade);
        }
    }
}
