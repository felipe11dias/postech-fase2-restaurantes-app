package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.adapter.presenter.view.AuthView;
import java.time.LocalDateTime;

/**
 * Resultado do login. O instante de expiração vem pronto do presenter — é ele quem decide o
 * que o cliente vê, e um instante absoluto não depende de o relógio do cliente estar certo
 * nem de quanto tempo a resposta levou para chegar.
 */
public record AuthResponse(String token, String type, LocalDateTime expiresAt) {

    public static AuthResponse from(AuthView view) {
        return new AuthResponse(view.token(), view.type(), view.expiresAt());
    }
}
