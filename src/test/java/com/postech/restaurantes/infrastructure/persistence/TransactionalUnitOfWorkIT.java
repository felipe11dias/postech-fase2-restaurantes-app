package com.postech.restaurantes.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.IntegrationTestSupport;
import com.postech.restaurantes.adapter.datasource.IRoleDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A promessa da unidade de trabalho — "estas escritas acontecem juntas ou nenhuma" — só pode
 * ser verificada contra um banco de verdade: é o único lugar onde o desfazer é observável.
 */
class TransactionalUnitOfWorkIT extends IntegrationTestSupport {

    @Autowired
    private IUnitOfWork unitOfWork;

    @Autowired
    private IUserDataSource userDataSource;

    @Autowired
    private IRoleDataSource roleDataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Bloco concluído confirma todas as escritas")
    void deveConfirmarTudoNoSucesso() {
        String login = "uow." + UUID.randomUUID().toString().substring(0, 8);

        UUID id = unitOfWork.execute(() -> {
            UserData gravado = userDataSource.insert(novo(login, "Antes"));
            return userDataSource.update(new UserData(gravado.id(), "Depois", gravado.email(), gravado.login(),
                    gravado.passwordHash(), gravado.roles(), List.of(), gravado.createdAt(),
                    LocalDateTime.now())).id();
        });

        assertEquals("Depois", userDataSource.findById(id).orElseThrow().name());
    }

    @Test
    @DisplayName("Exceção no meio do bloco desfaz também o que já havia sido gravado")
    void deveDesfazerTudoNaFalha() {
        String login = "uow." + UUID.randomUUID().toString().substring(0, 8);

        assertThrows(IllegalStateException.class, () -> unitOfWork.execute(() -> {
            userDataSource.insert(novo(login, "Nunca Gravado"));
            throw new IllegalStateException("falha no meio da unidade de trabalho");
        }));

        assertTrue(userDataSource.findByLogin(login).isEmpty());
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE login = ?", Integer.class, login));
    }

    private UserData novo(String login, String nome) {
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        return new UserData(null, nome, login + "@email.com", login,
                "$2a$10$hashDeIntegracaoComTamanhoSuficiente",
                roleDataSource.findByNames(Set.of("ROLE_CUSTOMER")), List.of(), agora, agora);
    }
}
