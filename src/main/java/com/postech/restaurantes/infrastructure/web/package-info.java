/**
 * Módulo de entrega HTTP (Spring MVC) — "a Web é um detalhe". Subpacotes: {@code api} (um por
 * feature), {@code error} (ProblemDetail), {@code doc} (OpenAPI), {@code validation} e
 * {@code security} (Spring Security aplicado às requisições).
 *
 * <p>Não conhece persistência, formato de token, criptografia nem e-mail: fala com o núcleo pelos
 * controllers de adaptação e, dentro do anel externo, pelas portas que ele mesmo declara
 * (IAccessTokenReader).
 */
package com.postech.restaurantes.infrastructure.web;
