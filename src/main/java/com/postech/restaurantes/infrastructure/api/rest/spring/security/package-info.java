/**
 * Spring Security aplicado ao HTTP: a cadeia de filtros e as rotas públicas (SecurityConfig), o
 * filtro que traduz o Bearer em contexto, o 401 em ProblemDetail, o principal autenticado e a
 * regra de posse usada no @PreAuthorize.
 *
 * <p>Não conhece o formato do token: declara IAccessTokenReader e o módulo de token a implementa. Os papéis
 * da autorização vêm do cadastro a cada requisição, pela porta ICurrentRolesReader, que a composição liga.
 * Também informa quem está autenticado (AuthenticatedActor) sem saber quem pergunta.
 */
package com.postech.restaurantes.infrastructure.api.rest.spring.security;
