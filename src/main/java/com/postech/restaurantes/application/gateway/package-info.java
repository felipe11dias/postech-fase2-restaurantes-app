/**
 * Portas do núcleo (IUserGateway, IMailGateway, IPasswordEncoder, ...), expressas em termos de
 * domínio. Declaradas aqui porque é o caso de uso quem define o que precisa (DIP; Aula 02, p. 10).
 *
 * <p>Implementadas pelos gateways de adapter/gateway, que traduzem; só as portas técnicas
 * (IPasswordEncoder, ISecureTokenGenerator, IUnitOfWork) são implementadas direto pela
 * infraestrutura. Ver docs/arquitetura/02-casos-de-uso.md.
 */
package com.postech.restaurantes.application.gateway;
