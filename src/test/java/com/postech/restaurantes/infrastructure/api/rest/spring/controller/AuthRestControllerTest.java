package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.NOW;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.controller.AuthController;
import com.postech.restaurantes.adapter.presenter.view.AuthView;
import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ForgotPasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.LoginRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ResetPasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.AuthResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuthRestControllerTest {

    private AuthController controller;
    private List<Runnable> fila;
    private AuthRestController restController;

    @BeforeEach
    void setUp() {
        controller = mock(AuthController.class);
        fila = new ArrayList<>();
        restController = new AuthRestController(controller, fila::add);
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
    @DisplayName("Esqueci minha senha responde sem esperar o processamento, que roda depois, fora da requisição")
    void deveDelegarARecuperacaoEmSegundoPlano() {
        restController.forgotPassword(new ForgotPasswordRequest("joao.silva@email.com"));

        verifyNoInteractions(controller);
        assertEquals(1, fila.size(), "o pedido foi para a fila");

        fila.get(0).run();

        verify(controller).forgotPassword("joao.silva@email.com");
    }

    @Test
    @DisplayName("Fila cheia: o pedido é descartado e a resposta continua a mesma")
    void naoDeveFalharComAFilaCheia() {
        AuthRestController comFilaCheia = new AuthRestController(controller, tarefa -> {
            throw new RejectedExecutionException("fila cheia");
        });

        assertDoesNotThrow(() -> comFilaCheia.forgotPassword(new ForgotPasswordRequest("joao.silva@email.com")));
        verifyNoInteractions(controller);
    }

    @Test
    @DisplayName("Falha no processamento em segundo plano vai para o log e não derruba a thread da fila")
    void naoDevePropagarFalhaDoProcessamento() {
        doThrow(new IllegalStateException("banco fora do ar")).when(controller).forgotPassword(anyString());
        restController.forgotPassword(new ForgotPasswordRequest("joao.silva@email.com"));

        assertDoesNotThrow(() -> fila.get(0).run());
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
