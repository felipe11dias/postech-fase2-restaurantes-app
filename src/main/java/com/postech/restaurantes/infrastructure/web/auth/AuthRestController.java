package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.adapter.controller.AuthController;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta HTTP de autenticação e recuperação de senha. Os três endpoints são públicos — exigir
 * autenticação para fazer login seria circular, e para recuperar a senha, impossível.
 */
@RestController
@RequestMapping(AuthRestController.BASE_PATH)
public class AuthRestController {

    public static final String BASE_PATH = "/api/v1/auth";

    private final AuthController controller;

    public AuthRestController(AuthController controller) {
        this.controller = controller;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthResponse.from(controller.login(request.toDTO()));
    }

    /**
     * Responde {@code 202 Accepted} — e o mesmo {@code 202} — exista ou não o e-mail. Um
     * {@code 404} para endereço desconhecido transformaria este endpoint em um verificador de
     * quem tem conta no sistema.
     */
    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        controller.forgotPassword(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        controller.resetPassword(request.toDTO());
    }
}
