package com.postech.restaurantes.adapter.gateway;

import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.TOKEN_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.TOKEN_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.domain.entity.password.PasswordResetToken;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PasswordResetTokenGatewayTest {

    private final IPasswordResetTokenDataSource dataSource = mock(IPasswordResetTokenDataSource.class);
    private final PasswordResetTokenGateway gateway = PasswordResetTokenGateway.create(dataSource);

    @Test
    @DisplayName("Recusa origem de dados nula")
    void deveRecusarOrigemNula() {
        assertThrows(IllegalArgumentException.class, () -> PasswordResetTokenGateway.create(null));
    }

    @Test
    @DisplayName("Busca pelo hash reconstrói o token; ausência vira Optional vazio")
    void deveBuscarPeloHash() {
        when(dataSource.findByTokenHash("hash-do-token")).thenReturn(Optional.of(TOKEN_DATA));
        when(dataSource.findByTokenHash("outro")).thenReturn(Optional.empty());

        PasswordResetToken token = gateway.findByTokenHash("hash-do-token").orElseThrow();

        assertEquals(TOKEN_ID, token.getId());
        assertEquals(USER_ID, token.getUserId());
        assertEquals(NOW.plusMinutes(30), token.getExpiresAt());
        assertFalse(token.isUsed());
        assertTrue(gateway.findByTokenHash("outro").isEmpty());
    }

    @Test
    @DisplayName("Busca pelo dono reconstrói o token; ausência vira Optional vazio")
    void deveBuscarPeloDono() {
        when(dataSource.findByUserId(USER_ID)).thenReturn(Optional.of(TOKEN_DATA));

        assertEquals(TOKEN_ID, gateway.findByUserId(USER_ID).orElseThrow().getId());
        assertTrue(gateway.findByUserId(TOKEN_ID).isEmpty());
    }

    @Test
    @DisplayName("Inserção envia o token novo sem id e devolve o registro reconstruído")
    void deveTraduzirNaInsercao() {
        PasswordResetToken novo = PasswordResetToken.create(USER_ID, "hash-do-token", NOW.plusMinutes(30), NOW);
        when(dataSource.insert(any())).thenReturn(TOKEN_DATA);

        PasswordResetToken result = gateway.insert(novo);

        ArgumentCaptor<PasswordResetTokenData> captor = ArgumentCaptor.forClass(PasswordResetTokenData.class);
        verify(dataSource).insert(captor.capture());
        assertNull(captor.getValue().id());
        assertEquals("hash-do-token", captor.getValue().tokenHash());
        assertEquals(TOKEN_ID, result.getId());
    }

    @Test
    @DisplayName("Atualização envia o estado de uso e reconstrói")
    void deveTraduzirNaAtualizacao() {
        PasswordResetToken usado = PasswordResetTokenGateway.toEntity(TOKEN_DATA);
        usado.markUsed();
        when(dataSource.update(any())).thenReturn(new PasswordResetTokenData(TOKEN_ID, USER_ID, "hash-do-token",
                NOW.plusMinutes(30), true));

        PasswordResetToken result = gateway.update(usado);

        ArgumentCaptor<PasswordResetTokenData> captor = ArgumentCaptor.forClass(PasswordResetTokenData.class);
        verify(dataSource).update(captor.capture());
        assertTrue(captor.getValue().used());
        assertTrue(result.isUsed());
    }
}
