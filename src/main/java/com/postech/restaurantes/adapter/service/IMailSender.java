package com.postech.restaurantes.adapter.service;

/**
 * Envia uma mensagem de texto já pronta. Não sabe o que a mensagem diz nem por quê: quem monta
 * assunto e corpo é o gateway; aqui só existe o transporte.
 *
 * <p><strong>Contrato:</strong> falha de transporte não se propaga — a implementação registra e
 * segue. É o mesmo contrato de {@code IMailGateway}, repassado a quem de fato fala com o servidor
 * de e-mail: se uma falha de envio chegasse ao "esqueci minha senha", a resposta passaria a
 * diferir entre e-mail cadastrado e desconhecido.
 */
public interface IMailSender {

    void send(String to, String subject, String body);
}
