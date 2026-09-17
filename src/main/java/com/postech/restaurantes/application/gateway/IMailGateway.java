package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.domain.vo.Email;

/** Envio de e-mail. O caso de uso entrega o token em claro; só o e-mail o carrega. */
public interface IMailGateway {

    void sendPasswordReset(Email to, String rawToken);
}
