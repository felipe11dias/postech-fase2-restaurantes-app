package com.postech.restaurantes.application.gateway;

import java.util.function.Supplier;

/**
 * Unidade de trabalho: executa um bloco de forma atômica. A transação em si é detalhe da
 * infraestrutura; a <em>demarcação</em> — "estas escritas acontecem juntas ou nenhuma" — é
 * regra de aplicação, e por isso a porta é declarada aqui e usada pelo controller de
 * adaptação ao invocar um caso de uso.
 */
public interface IUnitOfWork {

    <T> T execute(Supplier<T> work);

    default void execute(Runnable work) {
        execute(() -> {
            work.run();
            return null;
        });
    }
}
