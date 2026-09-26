package com.postech.restaurantes.infrastructure.web.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.infrastructure.web.error.ProblemType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProblemDetailOpenApiCustomizerTest {

    private final ProblemDetailOpenApiCustomizer customizer = new ProblemDetailOpenApiCustomizer();

    private static Content conteudoDeSucesso() {
        return new Content().addMediaType("application/hal+json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/EntityModelUserResponse")));
    }

    private static OpenAPI documentoCom(ApiResponses respostas) {
        return new OpenAPI()
                .components(new Components())
                .paths(new Paths().addPathItem("/api/v1/users/{id}",
                        new PathItem().get(new Operation().responses(respostas))));
    }

    @Test
    @DisplayName("Respostas de erro passam a ser ProblemDetail em application/problem+json")
    void deveDocumentarErrosComoProblemDetail() {
        ApiResponse naoEncontrado = new ApiResponse().description("Usuário não encontrado")
                .content(conteudoDeSucesso());
        ApiResponses respostas = new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("ok").content(conteudoDeSucesso()))
                .addApiResponse("404", naoEncontrado);

        customizer.customise(documentoCom(respostas));

        MediaType problema = naoEncontrado.getContent().get(ProblemDetailOpenApiCustomizer.PROBLEM_JSON);
        assertNotNull(problema, "o erro passa a ter o tipo de mídia de ProblemDetail");
        assertEquals(ProblemDetailOpenApiCustomizer.SCHEMA_REF, problema.getSchema().get$ref());
        assertEquals(1, naoEncontrado.getContent().size(), "o conteúdo herdado do tipo de retorno sai");
        assertEquals("Usuário não encontrado", naoEncontrado.getDescription(), "a descrição do controller fica");
    }

    @Test
    @DisplayName("Erro já documentado como ProblemDetail fica como está, com os exemplos por categoria")
    void deveManterErroJaDocumentadoComoProblemDetail() {
        Content jaProblema = new Content().addMediaType(ProblemDetailOpenApiCustomizer.PROBLEM_JSON,
                new MediaType().schema(new Schema<>().$ref(ProblemDetailOpenApiCustomizer.SCHEMA_REF)));
        ApiResponses respostas = new ApiResponses().addApiResponse("403", new ApiResponse().content(jaProblema));

        customizer.customise(documentoCom(respostas));

        assertSame(jaProblema, respostas.get("403").getContent());
    }

    @Test
    @DisplayName("Erro declarado sem conteúdo nenhum também recebe o formato ProblemDetail")
    void deveCompletarErroSemConteudo() {
        ApiResponses respostas = new ApiResponses().addApiResponse("401", new ApiResponse().description("x"));

        customizer.customise(documentoCom(respostas));

        assertNotNull(respostas.get("401").getContent().get(ProblemDetailOpenApiCustomizer.PROBLEM_JSON));
    }

    @Test
    @DisplayName("Respostas de sucesso ficam como o springdoc as gerou")
    void deveManterRespostasDeSucesso() {
        Content sucesso = conteudoDeSucesso();
        ApiResponses respostas = new ApiResponses().addApiResponse("200", new ApiResponse().content(sucesso));

        customizer.customise(documentoCom(respostas));

        assertSame(sucesso, respostas.get("200").getContent());
    }

    @ParameterizedTest
    @CsvSource({"400,true", "401,true", "404,true", "409,true", "500,true", "200,false", "201,false", "204,false",
            "default,false"})
    @DisplayName("Erro é toda resposta 4xx ou 5xx")
    void deveReconhecerErros(String codigo, boolean erro) {
        assertEquals(erro, ProblemDetailOpenApiCustomizer.isError(codigo));
    }

    @Test
    @DisplayName("Registra o esquema ProblemDetail com os campos que a API realmente produz")
    void deveRegistrarOEsquema() {
        OpenAPI documento = documentoCom(new ApiResponses());

        customizer.customise(documento);

        Schema<?> esquema = documento.getComponents().getSchemas().get(ApiDocumentation.PROBLEM_DETAIL_SCHEMA);
        assertEquals(List.of("type", "title", "status", "detail", "instance", "timestamp", "errors"),
                List.copyOf(esquema.getProperties().keySet()));
        assertEquals(Set.of("type", "title", "status", "detail", "instance", "timestamp"),
                Set.copyOf(esquema.getRequired()), "a ordem dos obrigatórios não tem significado no OpenAPI");
        assertFalse(esquema.getRequired().contains("errors"), "errors só existe em falha de validação");
        assertFalse(esquema.getProperties().containsKey("properties"),
                "o mapa interno do ProblemDetail do Spring não aparece: em execução ele sai achatado");
        List<?> tipos = esquema.getProperties().get("type").getEnum();
        assertEquals(ProblemType.values().length + 1, tipos.size(), "o catálogo inteiro, mais about:blank");
        assertTrue(tipos.contains("urn:restaurantes:problema:acesso-negado"));
        assertTrue(tipos.contains(ProblemDetailOpenApiCustomizer.ABOUT_BLANK));
    }

    @Test
    @DisplayName("Documento sem componentes ganha os componentes para receber o esquema")
    void deveCriarOsComponentesQuandoAusentes() {
        OpenAPI documento = new OpenAPI().paths(new Paths());

        customizer.customise(documento);

        assertTrue(documento.getComponents().getSchemas().containsKey(ApiDocumentation.PROBLEM_DETAIL_SCHEMA));
    }

    @Test
    @DisplayName("Documento sem caminhos só recebe o esquema")
    void deveTolerarDocumentoSemCaminhos() {
        OpenAPI documento = new OpenAPI().components(new Components());

        customizer.customise(documento);

        assertTrue(documento.getComponents().getSchemas().containsKey(ApiDocumentation.PROBLEM_DETAIL_SCHEMA));
    }
}
