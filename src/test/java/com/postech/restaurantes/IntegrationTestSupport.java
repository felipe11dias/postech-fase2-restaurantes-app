package com.postech.restaurantes;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração sem canal HTTP: contexto Spring completo sobre um PostgreSQL
 * de verdade, migrado pelo Flyway.
 *
 * <p>Nada é mockado. Se o mapeamento JPA divergir do schema, o {@code ddl-auto: validate}
 * impede o contexto de subir e <em>todos</em> os testes desta hierarquia falham — é esse o
 * ponto.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = IntegrationTestProperties.JWT_SECRET)
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = SharedPostgres.INSTANCE;
}
