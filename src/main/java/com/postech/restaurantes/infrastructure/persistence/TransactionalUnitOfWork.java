package com.postech.restaurantes.infrastructure.persistence;

import com.postech.restaurantes.application.gateway.IUnitOfWork;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementação da unidade de trabalho com o {@code TransactionTemplate} do Spring.
 *
 * <p>A <em>demarcação</em> — "estas operações formam um todo" — é regra de aplicação e vive
 * no controlador de adaptação, que chama {@link IUnitOfWork#execute}. O <em>mecanismo</em> —
 * transação JDBC, propagação, rollback — é detalhe, e vive aqui. É por isso que não existe
 * {@code @Transactional} em caso de uso: a anotação arrastaria o Spring para dentro do núcleo.
 */
@Component
public class TransactionalUnitOfWork implements IUnitOfWork {

    private final TransactionTemplate template;

    public TransactionalUnitOfWork(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "Gerenciador de transação inválido"));
    }

    /** Exceção lançada dentro do bloco sobe e desfaz a transação — nada é capturado aqui. */
    @Override
    public <T> T execute(Supplier<T> work) {
        Objects.requireNonNull(work, "Trabalho inválido");
        return template.execute(status -> work.get());
    }
}
