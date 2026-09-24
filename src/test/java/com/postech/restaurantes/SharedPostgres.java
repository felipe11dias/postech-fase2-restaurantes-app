package com.postech.restaurantes;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Um único PostgreSQL para toda a execução dos testes de integração, iniciado no carregamento
 * desta classe e nunca parado — quem o remove ao fim é o Ryuk do Testcontainers.
 *
 * <p>Não use {@code @Testcontainers}/{@code @Container}: a extensão do JUnit encerraria o
 * container ao fim da primeira classe, enquanto o Spring reaproveita o contexto em cache, e as
 * classes seguintes passariam a apontar para um banco morto.
 */
public final class SharedPostgres {

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        INSTANCE.start();
    }

    private SharedPostgres() {
    }
}
