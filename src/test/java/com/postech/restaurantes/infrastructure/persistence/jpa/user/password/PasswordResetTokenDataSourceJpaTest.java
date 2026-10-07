package com.postech.restaurantes.infrastructure.persistence.jpa.user.password;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.TOKEN_DATA;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.TOKEN_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.tokenEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PasswordResetTokenDataSourceJpaTest {

    private final SpringDataPasswordResetTokenRepository repository =
            mock(SpringDataPasswordResetTokenRepository.class);
    private final PasswordResetTokenDataSourceJpa dataSource = new PasswordResetTokenDataSourceJpa(repository);

    @Test
    @DisplayName("Busca pelo hash traduz o registro; ausência vira vazio")
    void deveBuscarPeloHash() {
        when(repository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(tokenEntity()));
        when(repository.findByTokenHash("outro")).thenReturn(Optional.empty());

        PasswordResetTokenData data = dataSource.findByTokenHash("hash-do-token").orElseThrow();

        assertEquals(TOKEN_ID, data.id());
        assertEquals(USER_ID, data.userId());
        assertEquals("hash-do-token", data.tokenHash());
        assertEquals(NOW.plusMinutes(30), data.expiresAt());
        assertFalse(data.used());
        assertTrue(dataSource.findByTokenHash("outro").isEmpty());
    }

    @Test
    @DisplayName("Busca pelo dono traduz o registro; ausência vira vazio")
    void deveBuscarPeloDono() {
        when(repository.findByUserId(USER_ID)).thenReturn(Optional.of(tokenEntity()));

        assertEquals(TOKEN_ID, dataSource.findByUserId(USER_ID).orElseThrow().id());
        assertTrue(dataSource.findByUserId(TOKEN_ID).isEmpty());
    }

    @Test
    @DisplayName("Inserção grava uma linha nova, sem id, e devolve o registro emitido")
    void deveInserir() {
        when(repository.save(any())).thenReturn(tokenEntity());

        PasswordResetTokenData emitido = dataSource.insert(
                new PasswordResetTokenData(null, USER_ID, "hash-do-token", NOW.plusMinutes(30), false));

        ArgumentCaptor<PasswordResetTokenJpaEntity> captor =
                ArgumentCaptor.forClass(PasswordResetTokenJpaEntity.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals("hash-do-token", captor.getValue().getTokenHash());
        assertFalse(captor.getValue().isUsed());
        assertEquals(TOKEN_ID, emitido.id());
    }

    @Test
    @DisplayName("Atualização grava hash, validade e uso — o consumo e a reemissão —, mas não o dono")
    void deveAtualizarOEstadoDoToken() {
        PasswordResetTokenJpaEntity existente = tokenEntity();
        when(repository.findById(TOKEN_ID)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PasswordResetTokenData atualizado = dataSource.update(
                new PasswordResetTokenData(TOKEN_ID, USER_ID, "outro-hash", NOW.plusYears(1), true));

        assertTrue(existente.isUsed());
        assertEquals("outro-hash", existente.getTokenHash());
        assertEquals(NOW.plusYears(1), existente.getExpiresAt());
        assertEquals(USER_ID, existente.getUserId());
        assertTrue(atualizado.used());
    }

    @Test
    @DisplayName("Atualizar token inexistente é falha de estado")
    void deveRecusarTokenInexistente() {
        when(repository.findById(TOKEN_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> dataSource.update(TOKEN_DATA));
    }
}
