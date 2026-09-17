package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordResetTokenTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 16, 12, 0);
    private static final LocalDateTime FUTURO = NOW.plusMinutes(30);

    @Test
    @DisplayName("Cria token novo, não usado, com expiração futura")
    void deveCriarQuandoExpiracaoFutura() {
        PasswordResetToken token = PasswordResetToken.create(USER_ID, "hash", FUTURO, NOW);

        assertNull(token.getId());
        assertEquals(USER_ID, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(FUTURO, token.getExpiresAt());
        assertFalse(token.isUsed());
        assertTrue(token.isUsable(NOW));
    }

    @Test
    @DisplayName("Recusa token cuja expiração é igual ou anterior ao instante atual")
    void deveRecusarQuandoExpiracaoNaoFutura() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetToken.create(USER_ID, "hash", NOW, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> PasswordResetToken.create(USER_ID, "hash", NOW.minusSeconds(1), NOW));
    }

    @Test
    @DisplayName("Recusa criação sem instante de referência")
    void deveRecusarCriarSemInstante() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetToken.create(USER_ID, "hash", FUTURO, null));
    }

    @Test
    @DisplayName("Recusa token sem usuário")
    void deveRecusarSemUsuario() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetToken.create(null, "hash", FUTURO, NOW));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa token com hash em branco")
    void deveRecusarHashEmBranco(String hash) {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetToken.create(USER_ID, hash, FUTURO, NOW));
    }

    @Test
    @DisplayName("Recusa token sem expiração")
    void deveRecusarSemExpiracao() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetToken.create(USER_ID, "hash", null, NOW));
    }

    @Test
    @DisplayName("Restaura token com id e estado de uso")
    void deveRestaurarComEstado() {
        UUID id = UUID.randomUUID();

        PasswordResetToken token = PasswordResetToken.restore(id, USER_ID, "hash", NOW.minusHours(1), true);

        assertEquals(id, token.getId());
        assertTrue(token.isUsed());
        assertTrue(token.isExpired(NOW));
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarSemId() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordResetToken.restore(null, USER_ID, "hash", FUTURO, false));
    }

    @Test
    @DisplayName("Está expirado quando o instante atual alcança a expiração")
    void deveExpirarQuandoInstanteAlcancaExpiracao() {
        PasswordResetToken token = PasswordResetToken.create(USER_ID, "hash", FUTURO, NOW);

        assertFalse(token.isExpired(FUTURO.minusSeconds(1)));
        assertTrue(token.isExpired(FUTURO));
        assertTrue(token.isExpired(FUTURO.plusSeconds(1)));
    }

    @Test
    @DisplayName("Recusa consulta de expiração sem instante de referência")
    void deveRecusarConsultarExpiracaoSemInstante() {
        PasswordResetToken token = PasswordResetToken.create(USER_ID, "hash", FUTURO, NOW);

        assertThrows(IllegalArgumentException.class, () -> token.isExpired(null));
    }

    @Test
    @DisplayName("Marca como usado uma única vez")
    void deveMarcarUsadoUmaVez() {
        PasswordResetToken token = PasswordResetToken.create(USER_ID, "hash", FUTURO, NOW);

        token.markUsed();

        assertTrue(token.isUsed());
        assertFalse(token.isUsable(NOW));
        assertThrows(IllegalStateException.class, token::markUsed);
    }

    @Test
    @DisplayName("Token expirado não é utilizável mesmo sem ter sido usado")
    void naoDeveSerUtilizavelQuandoExpirado() {
        PasswordResetToken token = PasswordResetToken.create(USER_ID, "hash", FUTURO, NOW);

        assertFalse(token.isUsable(FUTURO));
    }
}
