package com.postech.restaurantes.infrastructure.token.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** O módulo JWT habilita a própria configuração: removê-lo não deixa referência em outro lugar. */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
