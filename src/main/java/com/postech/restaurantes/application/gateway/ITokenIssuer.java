package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.IssuedToken;
import com.postech.restaurantes.domain.entity.User;

/** Emissão do token de acesso (JWT na infraestrutura) para um usuário autenticado. */
public interface ITokenIssuer {

    IssuedToken issue(User user);
}
