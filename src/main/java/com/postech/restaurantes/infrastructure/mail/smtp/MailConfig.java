package com.postech.restaurantes.infrastructure.mail.smtp;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** O módulo SMTP habilita a própria configuração: removê-lo não deixa referência em outro lugar. */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
}
