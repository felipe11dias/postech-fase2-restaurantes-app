package com.postech.restaurantes.infrastructure.persistence.jpa.user.password;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.TOKEN_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.tokenEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordResetTokenJpaEntityTest {

    @Test
    @DisplayName("Guarda o dono por identidade e só o hash do token")
    void deveGuardarOsCampos() {
        PasswordResetTokenJpaEntity token = tokenEntity();

        assertEquals(TOKEN_ID, token.getId());
        assertEquals(USER_ID, token.getUserId());
        assertEquals("hash-do-token", token.getTokenHash());
        assertEquals(NOW.plusMinutes(30), token.getExpiresAt());
        assertFalse(token.isUsed());
    }

    @Test
    @DisplayName("Marcar como usado é a única mudança de estado prevista")
    void deveMarcarUsado() {
        PasswordResetTokenJpaEntity token = tokenEntity();

        token.setUsed(true);

        assertTrue(token.isUsed());
    }
}
