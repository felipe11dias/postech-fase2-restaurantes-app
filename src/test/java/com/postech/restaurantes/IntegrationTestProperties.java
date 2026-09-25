package com.postech.restaurantes;

/**
 * Propriedades que os testes de integração precisam fornecer porque a aplicação, de propósito,
 * não tem valor padrão para elas.
 *
 * <p>O segredo do JWT é o caso: sem {@code JWT_SECRET} a subida falha, para que nenhum ambiente
 * rode com um segredo versionado. O valor abaixo só existe no classpath de teste e nunca vai
 * para um artefato publicado.
 *
 * <p>Fica em constante — e não em um {@code application.yml} de teste — porque um arquivo de
 * mesmo nome no classpath de teste <em>substitui</em> o principal inteiro, em vez de
 * complementá-lo.
 */
public final class IntegrationTestProperties {

    public static final String JWT_SECRET =
            "security.jwt.secret=segredo-exclusivo-dos-testes-de-integracao-com-mais-de-256-bits";

    private IntegrationTestProperties() {
    }
}
