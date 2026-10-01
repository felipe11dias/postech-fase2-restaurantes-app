package com.postech.restaurantes.infrastructure.api.rest.spring.route;

/**
 * Os caminhos base da API, em um lugar só.
 *
 * <p>Quem precisa de um caminho — o {@code @RequestMapping} do controller, a lista de rotas
 * públicas da {@code SecurityConfig}, os links do assembler HATEOAS — lê daqui. Se cada um lesse
 * do controller, o assembler e o controller dependeriam um do outro (o controller usa o assembler),
 * e os pacotes {@code controller} e {@code assembler} formariam um ciclo — contra o Princípio das
 * Dependências Acíclicas.
 */
public final class ApiRoutes {

    /** Versão da API no caminho: uma mudança incompatível ganha {@code /api/v2}, ao lado. */
    public static final String V1 = "/api/v1";

    public static final String USERS = V1 + "/users";

    public static final String AUTH = V1 + "/auth";

    public static final String RESTAURANTS = V1 + "/restaurants";

    private ApiRoutes() {
    }
}
