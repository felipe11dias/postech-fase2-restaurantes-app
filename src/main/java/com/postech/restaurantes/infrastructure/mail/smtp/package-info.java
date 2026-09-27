/**
 * Módulo SMTP (Spring Mail). Implementa IMailSender (adapter/service) e só conhece o próprio
 * remetente: transporta uma mensagem pronta. Assunto e texto são do PasswordResetMailGateway, no
 * adaptador.
 *
 * <p>Substituir (ex.: API HTTP de um provedor): novo subpacote de {@code mail} que implemente
 * IMailSender honrando o mesmo contrato — falha de transporte não se propaga. O texto do e-mail
 * não muda.
 */
package com.postech.restaurantes.infrastructure.mail.smtp;
