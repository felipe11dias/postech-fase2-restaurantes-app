package com.postech.restaurantes.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IUnitOfWork;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * A unidade de trabalho é o único ponto onde o núcleo toca uma transação — e ainda assim
 * pela interface. Aqui se verifica que o bloco roda dentro de uma transação, que o sucesso
 * confirma e que a falha desfaz.
 */
class TransactionalUnitOfWorkTest {

    private PlatformTransactionManager transactionManager;
    private TransactionStatus status;
    private IUnitOfWork unitOfWork;

    @BeforeEach
    void setUp() {
        transactionManager = mock(PlatformTransactionManager.class);
        status = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(status);
        unitOfWork = new TransactionalUnitOfWork(transactionManager);
    }

    @Test
    @DisplayName("Recusa gerenciador de transação nulo")
    void deveRecusarGerenciadorNulo() {
        assertThrows(NullPointerException.class, () -> new TransactionalUnitOfWork(null));
    }

    @Test
    @DisplayName("Recusa trabalho nulo")
    void deveRecusarTrabalhoNulo() {
        assertThrows(NullPointerException.class, () -> unitOfWork.execute((java.util.function.Supplier<?>) null));
    }

    @Test
    @DisplayName("Trabalho bem-sucedido roda em transação, confirma e devolve o resultado")
    void deveConfirmarNoSucesso() {
        String resultado = unitOfWork.execute(() -> "pronto");

        assertEquals("pronto", resultado);
        verify(transactionManager).getTransaction(any());
        verify(transactionManager).commit(status);
    }

    @Test
    @DisplayName("Exceção no bloco sobe intacta e desfaz a transação")
    void deveDesfazerNaFalha() {
        IllegalStateException falha = new IllegalStateException("quebrou");

        IllegalStateException lancada = assertThrows(IllegalStateException.class,
                () -> unitOfWork.execute(() -> {
                    throw falha;
                }));

        assertEquals(falha, lancada);
        verify(transactionManager).rollback(status);
    }

    @Test
    @DisplayName("Trabalho sem retorno também roda dentro da transação")
    void deveExecutarTrabalhoSemRetorno() {
        AtomicBoolean executou = new AtomicBoolean(false);

        unitOfWork.execute(() -> executou.set(true));

        assertTrue(executou.get());
        verify(transactionManager).commit(status);
    }
}
