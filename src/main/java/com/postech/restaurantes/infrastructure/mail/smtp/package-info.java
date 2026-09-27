/**
 * Módulo SMTP (Spring Mail). Implementa IMailGateway e só conhece o próprio remetente.
 *
 * <p>Substituir (ex.: API HTTP de um provedor): novo subpacote de {@code mail} que implemente
 * IMailGateway honrando o mesmo contrato — falha de transporte não se propaga.
 */
package com.postech.restaurantes.infrastructure.mail.smtp;
