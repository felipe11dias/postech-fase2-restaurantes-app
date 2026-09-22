package com.postech.restaurantes.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Liga a auditoria do Spring Data JPA ao {@code AuditorAware} da infraestrutura. Puramente
 * declarativa: nenhuma regra mora aqui.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "authenticatedAuditorAware")
public class PersistenceConfig {
}
