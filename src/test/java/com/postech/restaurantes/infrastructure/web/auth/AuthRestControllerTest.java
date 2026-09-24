package com.postech.restaurantes.infrastructure.web.auth;

import static com.postech.restaurantes.infrastructure.web.WebFixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.controller.AuthController;
import com.postech.restaurantes.adapter.presenter.view.AuthView;
import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthRestControllerTest {

    private AuthController controller;
    private AuthRestController restController;

    @BeforeEach
    void setUp() {
        controller = mock(AuthController.class);
        restController = new AuthRestController(controller);
    }

    @Test
    @DisplayName("Login devolve o token, o esquema e o instante de expiração vindos do presenter")
    void deveResponderOLogin() {
        when(controller.login(any())).thenReturn(new AuthView("jwt", "Bearer", NOW.plusHours(1)));

        AuthResponse resposta = restController.login(new LoginRequest("joao.silva", "senhaSegura123"));

        assertEquals("jwt", resposta.token());
        assertEquals("Bearer", resposta.type());
        assertEquals(NOW.plusHours(1), resposta.expiresAt());
        ArgumentCaptor<CredentialsDTO> captor = ArgumentCaptor.forClass(CredentialsDTO.class);
        verify(controller).login(captor.capture());
        assertEquals("joao.silva", captor.getValue().login());
        assertEquals("senhaSegura123", captor.getValue().password());
    }

    @Test
    @DisplayName("Esqueci minha senha repassa o e-mail e não devolve corpo")
    void deveDelegarARecuperacao() {
        restController.forgotPassword(new ForgotPasswordRequest("joao.silva@email.com"));

        verify(controller).forgotPassword("joao.silva@email.com");
    }

    @Test
    @DisplayName("Redefinir senha repassa token e as duas senhas, sem devolver corpo")
    void deveDelegarARedefinicao() {
        restController.resetPassword(new ResetPasswordRequest("token", "novaSenha123", "novaSenha123"));

        ArgumentCaptor<ResetPasswordDTO> captor = ArgumentCaptor.forClass(ResetPasswordDTO.class);
        verify(controller).resetPassword(captor.capture());
        assertEquals("token", captor.getValue().token());
        assertEquals("novaSenha123", captor.getValue().newPassword());
        assertEquals("novaSenha123", captor.getValue().confirmPassword());
    }
}
