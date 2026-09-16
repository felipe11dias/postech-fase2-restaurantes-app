package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLOCK;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.HASH;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.NOW;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.ResetPasswordDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IPasswordResetTokenGateway;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.PasswordResetToken;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResetPasswordUseCaseTest {

    private static final ResetPasswordDTO DTO = new ResetPasswordDTO("token-em-claro", "nova", "nova");

    private IUserGateway userGateway;
    private IPasswordResetTokenGateway tokenGateway;
    private ISecureTokenGenerator tokenGenerator;
    private IPasswordEncoder passwordEncoder;
    private ResetPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        tokenGateway = mock(IPasswordResetTokenGateway.class);
        tokenGenerator = mock(ISecureTokenGenerator.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        useCase = ResetPasswordUseCase.create(userGateway, tokenGateway, tokenGenerator, passwordEncoder, CLOCK);
        when(tokenGenerator.hash("token-em-claro")).thenReturn("hash-do-token");
    }

    private static PasswordResetToken usableToken() {
        return PasswordResetToken.restore(UUID.randomUUID(), USER_ID, "hash-do-token", NOW.plusMinutes(10), false);
    }

    @Test
    @DisplayName("Redefine a senha, grava o novo hash e marca o token como usado")
    void deveRedefinirQuandoTokenValido() {
        PasswordResetToken token = usableToken();
        User user = existingUser();
        when(tokenGateway.findByTokenHash("hash-do-token")).thenReturn(Optional.of(token));
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("nova")).thenReturn("novoHash");

        useCase.run(DTO);

        assertEquals("novoHash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(userGateway).update(user);
        verify(tokenGateway).update(token);
    }

    @Test
    @DisplayName("Token inexistente é rejeitado")
    void deveRecusarQuandoTokenInexistente() {
        when(tokenGateway.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidOrExpiredTokenException.class, () -> useCase.run(DTO));

        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Token expirado é rejeitado")
    void deveRecusarQuandoTokenExpirado() {
        PasswordResetToken expirado = PasswordResetToken.restore(UUID.randomUUID(), USER_ID, "hash-do-token", NOW, false);
        when(tokenGateway.findByTokenHash("hash-do-token")).thenReturn(Optional.of(expirado));

        assertThrows(InvalidOrExpiredTokenException.class, () -> useCase.run(DTO));
    }

    @Test
    @DisplayName("Token já usado é rejeitado")
    void deveRecusarQuandoTokenJaUsado() {
        PasswordResetToken usado = PasswordResetToken.restore(UUID.randomUUID(), USER_ID, "hash-do-token", NOW.plusHours(1), true);
        when(tokenGateway.findByTokenHash("hash-do-token")).thenReturn(Optional.of(usado));

        assertThrows(InvalidOrExpiredTokenException.class, () -> useCase.run(DTO));
    }

    @Test
    @DisplayName("Confirmação divergente é rejeitada antes de tocar no usuário")
    void deveRecusarQuandoConfirmacaoDiverge() {
        when(tokenGateway.findByTokenHash("hash-do-token")).thenReturn(Optional.of(usableToken()));

        assertThrows(InvalidPasswordException.class,
                () -> useCase.run(new ResetPasswordDTO("token-em-claro", "nova", "outra")));

        verify(userGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Nova senha em branco é rejeitada")
    void deveRecusarQuandoNovaSenhaEmBranco() {
        when(tokenGateway.findByTokenHash("hash-do-token")).thenReturn(Optional.of(usableToken()));

        assertThrows(IllegalArgumentException.class,
                () -> useCase.run(new ResetPasswordDTO("token-em-claro", " ", " ")));
    }

    @Test
    @DisplayName("Usuário do token inexistente resulta em ResourceNotFound")
    void deveFalharQuandoUsuarioDoTokenNaoExiste() {
        when(tokenGateway.findByTokenHash("hash-do-token")).thenReturn(Optional.of(usableToken()));
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(DTO));

        verify(tokenGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa dados nulos, token em branco e relógio nulo")
    void deveRecusarEntradaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(new ResetPasswordDTO(" ", "n", "n")));
        assertThrows(IllegalArgumentException.class, () -> ResetPasswordUseCase.create(
                userGateway, tokenGateway, tokenGenerator, passwordEncoder, null));
        assertEquals(HASH, existingUser().getPasswordHash());
    }
}
