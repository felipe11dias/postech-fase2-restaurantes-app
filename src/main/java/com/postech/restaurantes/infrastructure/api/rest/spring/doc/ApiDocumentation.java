package com.postech.restaurantes.infrastructure.api.rest.spring.doc;

/**
 * Nomes compartilhados da documentação OpenAPI.
 *
 * <p>Moram numa classe própria, e não na {@code OpenApiConfig}: os controllers precisam do nome do
 * esquema de segurança, e ler uma constante não deve prendê-los à classe que configura o
 * springdoc. Mesmo raciocínio de {@code route/ApiRoutes}: o que várias pontas compartilham fica
 * num lugar de que todas dependem, e que não depende de nenhuma delas (Princípio das Dependências
 * Acíclicas).
 */
public final class ApiDocumentation {

    /** Esquema de segurança Bearer JWT; é ele que acende o cadeado e o botão "Authorize". */
    public static final String BEARER_AUTH = "bearerAuth";

    /** Componente de esquema com o formato de toda resposta de erro da API. */
    public static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";

    private ApiDocumentation() {
    }
}
