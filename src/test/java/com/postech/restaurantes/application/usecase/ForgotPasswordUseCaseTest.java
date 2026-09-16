package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLOCK;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.NOW;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.application.gateway.IPasswordResetTokenGateway;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.PasswordResetToken;
import com.postech.restaurantes.domain.vo.Email;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ForgotPasswordUseCaseTest {

    private static final Duration VALIDITY = Duration.ofMinutes(30);

    private IUserGateway userGateway;
    private IPasswordResetTokenGateway tokenGateway;
    private ISecureTokenGenerator tokenGenerator;
    private IMailGateway mailGateway;
    private ForgotPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        tokenGateway = mock(IPasswordResetTokenGateway.class);
        tokenGenerator = mock(ISecureTokenGenerator.class);
        mailGateway = mock(IMailGateway.class);
        useCase = ForgotPasswordUseCase.create(userGateway, tokenGateway, tokenGenerator, mailGateway, VALIDITY, CLOCK);
    }

    @Test
    @DisplayName("E-mail existente: persiste só o hash do token e envia o valor em claro por e-mail")
    void deveGerarTokenEEnviarQuandoEmailExiste() {
        when(userGateway.findByEmail(Email.of("joao.silva@email.com"))).thenReturn(Optional.of(existingUser()));
        when(tokenGenerator.generate()).thenReturn("token-em-claro");
        when(tokenGenerator.hash("token-em-claro")).thenReturn("hash-do-token");

        useCase.run("Joao.Silva@Email.com");

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenGateway).insert(captor.capture());
        PasswordResetToken token = captor.getValue();
        assertEquals(USER_ID, token.getUserId());
        assertEquals("hash-do-token", token.getTokenHash());
        assertEquals(NOW.plus(VALIDITY), token.getExpiresAt());
        assertFalse(token.isUsed());
        verify(mailGateway).sendPasswordReset(Email.of("joao.silva@email.com"), "token-em-claro");
    }

    @Test
    @DisplayName("E-mail inexistente: não gera token, não envia e-mail e não lança")
    void deveSilenciarQuandoEmailNaoExiste() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());

        useCase.run("ninguem@email.com");

        verifyNoInteractions(tokenGateway, mailGateway);
        verify(tokenGenerator, never()).generate();
    }

    @Test
    @DisplayName("E-mail com formato inválido é rejeitado antes de qualquer consulta")
    void deveRecusarEmailInvalido() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run("nao-e-email"));

        verify(userGateway, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Recusa validade nula, zero ou negativa e relógio nulo na criação")
    void deveRecusarConfiguracaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> ForgotPasswordUseCase.create(
                userGateway, tokenGateway, tokenGenerator, mailGateway, null, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> ForgotPasswordUseCase.create(
                userGateway, tokenGateway, tokenGenerator, mailGateway, Duration.ZERO, CLOCK));
        assertThrows(IllegalArgumentException.class, () -> ForgotPasswordUseCase.create(
                userGateway, tokenGateway, tokenGenerator, mailGateway, Duration.ofMinutes(-1), CLOCK));
        assertThrows(IllegalArgumentException.class, () -> ForgotPasswordUseCase.create(
                userGateway, tokenGateway, tokenGenerator, mailGateway, VALIDITY, null));
    }

    @Test
    @DisplayName("Falha no envio do e-mail se propaga (o token já persistido fica sem uso)")
    void devePropagarFalhaDeEnvio() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.of(existingUser()));
        when(tokenGenerator.generate()).thenReturn("t");
        when(tokenGenerator.hash(anyString())).thenReturn("h");
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp fora")).when(mailGateway).sendPasswordReset(any(), anyString());

        assertThrows(IllegalStateException.class, () -> useCase.run("joao.silva@email.com"));
    }
}
