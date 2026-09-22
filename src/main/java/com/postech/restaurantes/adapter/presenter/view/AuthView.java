package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDateTime;

/** Resultado do login: token de acesso, esquema e expiração. */
public record AuthView(String token, String type, LocalDateTime expiresAt) {
}
