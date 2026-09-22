package com.postech.restaurantes;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração: contexto Spring completo sobre um PostgreSQL de verdade,
 * criado pelo Testcontainers e migrado pelo Flyway.
 *
 * <p>Nada é mockado. Se o mapeamento JPA divergir do schema, o {@code ddl-auto: validate}
 * impede o contexto de subir e <em>todos</em> os testes desta hierarquia falham — é esse o
 * ponto.
 *
 * <p>Container <strong>único</strong>, iniciado no carregamento da classe e nunca parado: com
 * {@code @Testcontainers}/{@code @Container} a extensão do JUnit encerraria o container ao fim
 * da primeira classe, e as seguintes reaproveitariam o mesmo contexto Spring apontando para um
 * banco morto. Quem remove o container ao fim da execução é o Ryuk do próprio Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public abstract class IntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }
}
