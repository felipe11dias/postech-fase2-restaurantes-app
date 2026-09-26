package com.postech.restaurantes;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Um único PostgreSQL para toda a execução dos testes de integração, iniciado no carregamento
 * desta classe e nunca parado — quem o remove ao fim é o Ryuk do Testcontainers.
 *
 * <p>Não use {@code @Testcontainers}/{@code @Container}: a extensão do JUnit encerraria o
 * container ao fim da primeira classe, enquanto o Spring reaproveita o contexto em cache, e as
 * classes seguintes passariam a apontar para um banco morto.
 *
 * <p>A imagem é a mesma, na mesma versão exata, do serviço {@code db} do
 * {@code docker-compose.yml}: o banco dos testes é o banco em que a aplicação roda.
 */
public final class SharedPostgres {

    public static final PostgreSQLContainer<?> INSTANCE = new PostgreSQLContainer<>("postgres:16.15-alpine3.24");

    static {
        INSTANCE.start();
    }

    private SharedPostgres() {
    }
}
