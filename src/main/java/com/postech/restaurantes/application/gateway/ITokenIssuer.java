package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.domain.entity.user.User;

/** Emissão do token de acesso (JWT na infraestrutura) para um usuário autenticado. */
public interface ITokenIssuer {

    IssuedToken issue(User user);
}
