package com.postech.restaurantes.infrastructure.web.doc;

/**
 * Nomes compartilhados da documentação OpenAPI.
 *
 * <p>Moram em um pacote próprio, e não na {@code OpenApiConfig}, para não criar ciclo: a
 * configuração já depende dos controllers (a {@code SecurityConfig} usa os caminhos base deles),
 * e os controllers precisam do nome do esquema de segurança. Com os nomes aqui, as duas pontas
 * dependem deste pacote e nenhuma depende da outra — o Princípio das Dependências Acíclicas.
 */
public final class ApiDocumentation {

    /** Esquema de segurança Bearer JWT; é ele que acende o cadeado e o botão "Authorize". */
    public static final String BEARER_AUTH = "bearerAuth";

    /** Componente de esquema com o formato de toda resposta de erro da API. */
    public static final String PROBLEM_DETAIL_SCHEMA = "ProblemDetail";

    private ApiDocumentation() {
    }
}
