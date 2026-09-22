package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.AuthView;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.domain.Guard;

/** Prepara a saída do login. O esquema de portador é uma convenção de apresentação, não do núcleo. */
public final class AuthPresenter {

    static final String TOKEN_TYPE = "Bearer";

    private AuthPresenter() {
    }

    public static AuthView toView(IssuedToken token) {
        Guard.requireNonNull(token, "Token inválido");
        return new AuthView(token.token(), TOKEN_TYPE, token.expiresAt());
    }
}
