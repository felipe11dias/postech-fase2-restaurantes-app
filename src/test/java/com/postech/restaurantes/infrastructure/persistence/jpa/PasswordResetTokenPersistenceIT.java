package com.postech.restaurantes.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.Documentos;
import com.postech.restaurantes.IntegrationTestSupport;
import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class PasswordResetTokenPersistenceIT extends IntegrationTestSupport {

    @Autowired
    private IPasswordResetTokenDataSource tokenDataSource;

    @Autowired
    private IUserDataSource userDataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Token gravado é recuperado pelo hash, com dono e validade")
    void deveGravarERecuperarPeloHash() {
        UserData dono = inserirUsuario();
        String hash = "hash-" + UUID.randomUUID();
        LocalDateTime expiraEm = LocalDateTime.now().withNano(0).plusMinutes(30);

        PasswordResetTokenData gravado = tokenDataSource.insert(
                new PasswordResetTokenData(null, dono.id(), hash, expiraEm, false));

        PasswordResetTokenData lido = tokenDataSource.findByTokenHash(hash).orElseThrow();
        assertNotNull(gravado.id());
        assertEquals(gravado.id(), lido.id());
        assertEquals(dono.id(), lido.userId());
        assertEquals(expiraEm, lido.expiresAt());
        assertFalse(lido.used());
    }

    @Test
    @DisplayName("Hash inexistente devolve vazio")
    void deveDevolverVazioParaHashDesconhecido() {
        assertTrue(tokenDataSource.findByTokenHash("hash-que-nunca-existiu").isEmpty());
    }

    @Test
    @DisplayName("Consumir o token grava o uso, mantendo hash e validade")
    void deveGravarOConsumo() {
        UserData dono = inserirUsuario();
        String hash = "hash-" + UUID.randomUUID();
        PasswordResetTokenData gravado = tokenDataSource.insert(
                new PasswordResetTokenData(null, dono.id(), hash, LocalDateTime.now().plusMinutes(30), false));

        tokenDataSource.update(new PasswordResetTokenData(gravado.id(), gravado.userId(), gravado.tokenHash(),
                gravado.expiresAt(), true));

        PasswordResetTokenData lido = tokenDataSource.findByTokenHash(hash).orElseThrow();
        assertTrue(lido.used());
        assertEquals(hash, lido.tokenHash());
    }

    @Test
    @DisplayName("O mesmo hash não pode ser gravado duas vezes, nem para usuários diferentes")
    void deveRecusarHashDuplicado() {
        String hash = "hash-" + UUID.randomUUID();
        tokenDataSource.insert(new PasswordResetTokenData(null, inserirUsuario().id(), hash,
                LocalDateTime.now().plusMinutes(30), false));
        UUID outroDono = inserirUsuario().id();

        assertThrows(DataIntegrityViolationException.class, () -> tokenDataSource.insert(
                new PasswordResetTokenData(null, outroDono, hash, LocalDateTime.now().plusMinutes(30), false)));
    }

    @Test
    @DisplayName("Um usuário tem um token só: o banco recusa o segundo (V5)")
    void deveRecusarSegundoTokenDoMesmoUsuario() {
        UserData dono = inserirUsuario();
        tokenDataSource.insert(new PasswordResetTokenData(null, dono.id(), "hash-" + UUID.randomUUID(),
                LocalDateTime.now().plusMinutes(30), false));

        assertThrows(DataIntegrityViolationException.class, () -> tokenDataSource.insert(new PasswordResetTokenData(
                null, dono.id(), "hash-" + UUID.randomUUID(), LocalDateTime.now().plusMinutes(30), false)));
    }

    @Test
    @DisplayName("Reemitir grava hash, validade e uso novos na mesma linha; o hash antigo deixa de achar o token")
    void deveReemitirNaMesmaLinha() {
        UserData dono = inserirUsuario();
        String hashAntigo = "hash-" + UUID.randomUUID();
        String hashNovo = "hash-" + UUID.randomUUID();
        LocalDateTime novaValidade = LocalDateTime.now().withNano(0).plusHours(1);
        PasswordResetTokenData gravado = tokenDataSource.insert(new PasswordResetTokenData(null, dono.id(),
                hashAntigo, LocalDateTime.now().plusMinutes(30), true));

        tokenDataSource.update(new PasswordResetTokenData(gravado.id(), dono.id(), hashNovo, novaValidade, false));

        PasswordResetTokenData lido = tokenDataSource.findByUserId(dono.id()).orElseThrow();
        assertEquals(gravado.id(), lido.id());
        assertEquals(hashNovo, lido.tokenHash());
        assertEquals(novaValidade, lido.expiresAt());
        assertFalse(lido.used());
        assertTrue(tokenDataSource.findByTokenHash(hashAntigo).isEmpty());
    }

    @Test
    @DisplayName("Excluir o usuário apaga seus tokens em cascata")
    void deveApagarOsTokensComOUsuario() {
        UserData dono = inserirUsuario();
        String hash = "hash-" + UUID.randomUUID();
        tokenDataSource.insert(new PasswordResetTokenData(null, dono.id(), hash,
                LocalDateTime.now().plusMinutes(30), false));

        userDataSource.delete(dono.id());

        assertTrue(tokenDataSource.findByTokenHash(hash).isEmpty());
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT count(*) FROM password_reset_tokens WHERE user_id = ?", Integer.class, dono.id()));
    }

    private UserData inserirUsuario() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        return userDataSource.insert(new UserData(null, "Dono do Token", "token." + sufixo + "@email.com",
                "token." + sufixo, "$2a$10$hashDeIntegracaoComTamanhoSuficiente",
                null, new ClientData(Documentos.cpf(), "11912345678", null), null, null, List.of(), agora, agora));
    }
}
