package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.domain.vo.Email;

/** Envio de e-mail. O caso de uso entrega o token em claro; só o e-mail o carrega. */
public interface IMailGateway {

    /**
     * Envia o token de redefinição.
     *
     * <p><strong>Contrato:</strong> falha de transporte não se propaga — a implementação
     * registra e segue. O "esqueci minha senha" só envia quando o e-mail existe; se uma falha
     * de envio chegasse ao chamador, a resposta passaria a diferir entre e-mail cadastrado e
     * desconhecido, e o endpoint revelaria quem tem conta. A obrigação é declarada aqui, na
     * porta, e honrada pela infraestrutura.
     */
    void sendPasswordReset(Email to, String rawToken);
}
