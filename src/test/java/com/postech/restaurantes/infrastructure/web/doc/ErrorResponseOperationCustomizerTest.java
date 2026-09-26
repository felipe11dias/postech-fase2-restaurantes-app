package com.postech.restaurantes.infrastructure.web.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.infrastructure.web.error.ProblemType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

class ErrorResponseOperationCustomizerTest {

    private final ErrorResponseOperationCustomizer customizer = new ErrorResponseOperationCustomizer();

    @RequestMapping("/api/v1/exemplos")
    static class ControllerDeExemplo {

        @GetMapping("/{id}")
        @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Exemplo não encontrado")
        @ErrorResponse(type = ProblemType.INVALID_PASSWORD, description = "Senha atual incorreta")
        @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido")
        public void comErros(UUID id) {
        }

        @PostMapping
        public void semErros() {
        }

        public void semMapeamento() {
        }
    }

    static class ControllerSemPrefixo {

        @GetMapping("/solto")
        public void solto() {
        }
    }

    private static HandlerMethod metodo(Object bean, String nome, Class<?>... parametros) throws NoSuchMethodException {
        return new HandlerMethod(bean, bean.getClass().getMethod(nome, parametros));
    }

    private Operation customizar(HandlerMethod metodo) {
        Operation operacao = new Operation().responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("ok")));
        return customizer.customize(operacao, metodo);
    }

    @Test
    @DisplayName("O código documentado sai da categoria — o mesmo catálogo que o handler usa para responder")
    void deveDerivarOCodigoDaCategoria() throws NoSuchMethodException {
        Operation operacao = customizar(metodo(new ControllerDeExemplo(), "comErros", UUID.class));

        assertEquals(List.of("200", "400", "404"), List.copyOf(operacao.getResponses().keySet()));
        assertEquals("ok", operacao.getResponses().get("200").getDescription(), "a resposta de sucesso fica");
    }

    @Test
    @DisplayName("Cada erro é ProblemDetail, com exemplo carregando type, título e status reais da categoria")
    void deveMontarOExemploDaCategoria() throws NoSuchMethodException {
        Operation operacao = customizar(metodo(new ControllerDeExemplo(), "comErros", UUID.class));

        MediaType problema = operacao.getResponses().get("404").getContent()
                .get(ProblemDetailOpenApiCustomizer.PROBLEM_JSON);
        assertEquals(ProblemDetailOpenApiCustomizer.SCHEMA_REF, problema.getSchema().get$ref());
        @SuppressWarnings("unchecked")
        Map<String, Object> exemplo = (Map<String, Object>) problema.getExamples().get("Exemplo não encontrado").getValue();
        assertEquals("urn:restaurantes:problema:recurso-nao-encontrado", exemplo.get("type"));
        assertEquals("Recurso não encontrado", exemplo.get("title"));
        assertEquals(404, exemplo.get("status"));
        assertEquals("Exemplo não encontrado", exemplo.get("detail"));
        assertEquals("/api/v1/exemplos/" + ErrorResponseOperationCustomizer.EXAMPLE_ID, exemplo.get("instance"));
        assertEquals(ErrorResponseOperationCustomizer.EXAMPLE_TIMESTAMP, exemplo.get("timestamp"));
    }

    @Test
    @DisplayName("Casos com o mesmo código viram uma resposta só, com um exemplo nomeado para cada caso")
    void deveAgruparCasosDoMesmoCodigo() throws NoSuchMethodException {
        Operation operacao = customizar(metodo(new ControllerDeExemplo(), "comErros", UUID.class));

        ApiResponse badRequest = operacao.getResponses().get("400");
        assertEquals("Senha atual incorreta; Campo inválido", badRequest.getDescription());
        Map<String, io.swagger.v3.oas.models.examples.Example> exemplos =
                badRequest.getContent().get(ProblemDetailOpenApiCustomizer.PROBLEM_JSON).getExamples();
        assertEquals(List.of("Senha atual incorreta", "Campo inválido"), List.copyOf(exemplos.keySet()));
        assertEquals("urn:restaurantes:problema:senha-invalida",
                ((Map<?, ?>) exemplos.get("Senha atual incorreta").getValue()).get("type"));
    }

    @Test
    @DisplayName("Operação sem @ErrorResponse fica como estava")
    void deveIgnorarOperacaoSemErrosDeclarados() throws NoSuchMethodException {
        Operation operacao = customizar(metodo(new ControllerDeExemplo(), "semErros"));

        assertEquals(List.of("200"), List.copyOf(operacao.getResponses().keySet()));
    }

    @Test
    @DisplayName("O caminho de exemplo junta o prefixo da classe e o do método, com o que existir")
    void deveMontarOCaminhoDaOperacao() throws NoSuchMethodException {
        assertEquals("/api/v1/exemplos/{id}", ErrorResponseOperationCustomizer.requestPath(
                metodo(new ControllerDeExemplo(), "comErros", UUID.class)));
        assertEquals("/api/v1/exemplos", ErrorResponseOperationCustomizer.requestPath(
                metodo(new ControllerDeExemplo(), "semErros")), "mapeamento sem caminho próprio");
        assertEquals("/api/v1/exemplos", ErrorResponseOperationCustomizer.requestPath(
                metodo(new ControllerDeExemplo(), "semMapeamento")), "método sem mapeamento");
        assertEquals("/solto", ErrorResponseOperationCustomizer.requestPath(
                metodo(new ControllerSemPrefixo(), "solto")), "classe sem prefixo");
    }

    @Test
    @DisplayName("A anotação é repetível: todas as declaradas chegam ao customizador")
    void deveLerTodasAsAnotacoes() throws NoSuchMethodException {
        ErrorResponse[] erros = ControllerDeExemplo.class.getMethod("comErros", UUID.class)
                .getAnnotationsByType(ErrorResponse.class);

        assertEquals(3, erros.length);
        assertTrue(ControllerDeExemplo.class.getMethod("comErros", UUID.class).isAnnotationPresent(ErrorResponses.class));
    }
}
