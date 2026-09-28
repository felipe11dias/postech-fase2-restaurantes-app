/**
 * API REST em Spring (Spring MVC, Spring Security, Spring HATEOAS, springdoc, Bean Validation),
 * organizada como MVC — um pacote por papel da classe, não por feature:
 * {@code controller} (@RestController), {@code dto/request} e {@code dto/response} (corpo que entra
 * e que sai), {@code assembler} (links HATEOAS), {@code route} (caminhos), {@code config},
 * {@code exception} (ProblemDetail), {@code doc} (OpenAPI), {@code security} e {@code validation}.
 * Restaurante e cardápio entram nos mesmos pacotes.
 *
 * <p>Dentro de um detalhe, a organização segue a convenção do framework; a <em>screaming
 * architecture</em> vale onde está o domínio (domain, application, adapter). Não conhece
 * persistência, formato de token, criptografia nem e-mail: fala com o núcleo pelos controllers de
 * adaptação e, dentro do anel externo, pelas portas que ele mesmo declara (IAccessTokenReader).
 * Substituir: {@code api/rest/<outra>} com as mesmas rotas e os mesmos controllers de adaptação.
 */
package com.postech.restaurantes.infrastructure.api.rest.spring;
