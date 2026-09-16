package com.postech.restaurantes.application.dto.auth;

import com.postech.restaurantes.domain.Guard;
import java.time.LocalDateTime;

/** Token de acesso emitido após autenticação, com o instante em que expira. */
public record IssuedToken(String token, LocalDateTime expiresAt) {

    public IssuedToken {
        token = Guard.requireNonBlank(token, "Token inválido");
        Guard.requireNonNull(expiresAt, "Expiração do token inválida");
    }
}
