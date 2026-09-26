package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * A documentação é um contrato, e contrato que ninguém confere diverge do código em silêncio.
 * Estes testes leem o OpenAPI que a aplicação publica e o confrontam com a aplicação rodando:
 * o que está marcado como protegido tem de recusar quem chega sem token, o que está marcado
 * como público não pode recusar, e o exemplo documentado tem de ser aceito.
 */
class OpenApiDocumentationIT extends WebIntegrationTestSupport {

    private static final String BEARER = "bearerAuth";
    private static final String PROBLEM_REF = "#/components/schemas/ProblemDetail";

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode doc;

    @BeforeEach
    void lerDocumento() {
        ResponseEntity<JsonNode> resposta = rest.getForEntity("/v3/api-docs", JsonNode.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode(), "o documento é público");
        doc = resposta.getBody();
    }

    @Test
    @DisplayName("O documento identifica a API e declara o esquema Bearer JWT do botão Authorize")
    void deveDeclararIdentificacaoESeguranca() {
        assertEquals("API de Gestão de Restaurantes", doc.at("/info/title").asText());
        assertEquals("1.0.0", doc.at("/info/version").asText());
        JsonNode esquema = doc.at("/components/securitySchemes/" + BEARER);
        assertEquals("http", esquema.get("type").asText());
        assertEquals("bearer", esquema.get("scheme").asText());
        assertEquals("JWT", esquema.get("bearerFormat").asText());
    }

    @Test
    @DisplayName("Os nove endpoints da especificação estão documentados, cada um sob a sua tag")
    void deveDocumentarTodosOsEndpoints() {
        Set<String> documentados = new TreeSet<>();
        operacoes().forEach(op -> documentados.add(op.metodo() + " " + op.caminho() + " " + op.tag()));

        assertEquals(new TreeSet<>(Set.of(
                "POST /api/v1/auth/login Autenticação",
                "POST /api/v1/auth/forgot-password Autenticação",
                "POST /api/v1/auth/reset-password Autenticação",
                "POST /api/v1/users Usuários",
                "GET /api/v1/users Usuários",
                "GET /api/v1/users/{id} Usuários",
                "PUT /api/v1/users/{id} Usuários",
                "PATCH /api/v1/users/{id}/password Usuários",
                "DELETE /api/v1/users/{id} Usuários")), documentados);
    }

    /**
     * O cadeado da documentação precisa bater com a {@code SecurityConfig}. Cada operação é
     * chamada sem token: a marcada como protegida tem de responder 401, e a marcada como
     * pública não pode — senão a documentação mente para quem a usa.
     */
    @Test
    @DisplayName("O cadeado de cada operação bate com o que a aplicação exige de verdade")
    void deveMarcarComoProtegidoExatamenteOQueExigeToken() {
        List<String> divergencias = new ArrayList<>();
        for (Operacao op : operacoes()) {
            HttpStatus status = chamarSemToken(op);
            if (op.protegida() && status != HttpStatus.UNAUTHORIZED) {
                divergencias.add(op + " documentada como protegida, mas respondeu " + status + " sem token");
            }
            if (!op.protegida() && status == HttpStatus.UNAUTHORIZED) {
                divergencias.add(op + " documentada como pública, mas exigiu token");
            }
        }

        assertTrue(divergencias.isEmpty(), String.join("\n", divergencias));
        assertEquals(5, operacoes().stream().filter(Operacao::protegida).count());
    }

    @Test
    @DisplayName("Toda resposta de erro documentada é ProblemDetail em application/problem+json")
    void deveDocumentarTodoErroComoProblemDetail() {
        int erros = 0;
        for (Operacao op : operacoes()) {
            Iterator<Map.Entry<String, JsonNode>> respostas = op.no().get("responses").fields();
            while (respostas.hasNext()) {
                Map.Entry<String, JsonNode> resposta = respostas.next();
                if (resposta.getKey().startsWith("4") || resposta.getKey().startsWith("5")) {
                    erros++;
                    JsonNode conteudo = resposta.getValue().get("content");
                    assertEquals(1, conteudo.size(), op + " " + resposta.getKey());
                    assertEquals(PROBLEM_REF, conteudo.at("/application~1problem+json/schema/$ref").asText(),
                            op + " " + resposta.getKey());
                }
            }
        }
        assertTrue(erros > 20, "os erros de cada operação estão declarados");
        assertEquals(Set.of("type", "title", "status", "detail", "instance", "timestamp", "errors"),
                nomesDosCampos(doc.at("/components/schemas/ProblemDetail/properties")));
    }

    /**
     * Cada exemplo de erro vem da categoria declarada. O que se confere aqui é a coerência que o
     * leitor da documentação assume: o exemplo de um 401 é um 401 de verdade — e não o exemplo
     * de outro erro repetido em todas as respostas.
     */
    @Test
    @DisplayName("Todo exemplo de erro é coerente com o código da resposta em que aparece")
    void deveTerExemplosDeErroCoerentes() {
        List<String> incoerencias = new ArrayList<>();
        int exemplos = 0;
        for (Operacao op : operacoes()) {
            Iterator<Map.Entry<String, JsonNode>> respostas = op.no().get("responses").fields();
            while (respostas.hasNext()) {
                Map.Entry<String, JsonNode> resposta = respostas.next();
                JsonNode casos = resposta.getValue().at("/content/application~1problem+json/examples");
                Iterator<JsonNode> valores = casos.elements();
                while (valores.hasNext()) {
                    JsonNode exemplo = valores.next().get("value");
                    exemplos++;
                    if (!resposta.getKey().equals(exemplo.get("status").asText())) {
                        incoerencias.add(op + " " + resposta.getKey() + " com exemplo de status " + exemplo.get("status"));
                    }
                    if (!exemplo.get("type").asText().startsWith("urn:restaurantes:problema:")) {
                        incoerencias.add(op + " " + resposta.getKey() + " com type fora do catálogo");
                    }
                }
            }
        }

        assertTrue(incoerencias.isEmpty(), String.join("\n", incoerencias));
        assertTrue(exemplos > 20, "cada erro declarado tem exemplo");
    }

    @Test
    @DisplayName("Casos distintos com o mesmo código aparecem cada um com o seu exemplo")
    void deveDocumentarCadaCasoDoMesmoCodigo() {
        JsonNode exemplos = doc.at("/paths/~1api~1v1~1users~1{id}~1password/patch/responses/400/content/"
                + "application~1problem+json/examples");

        Set<String> tipos = new TreeSet<>();
        exemplos.elements().forEachRemaining(exemplo -> tipos.add(exemplo.at("/value/type").asText()));
        assertEquals(Set.of("urn:restaurantes:problema:requisicao-invalida",
                "urn:restaurantes:problema:senha-invalida"), tipos);
    }

    @Test
    @DisplayName("O cadastro é documentado como 201, o status que de fato responde, e não 200")
    void deveDocumentarOStatusRealDoCadastro() {
        JsonNode respostas = doc.at("/paths/~1api~1v1~1users/post/responses");

        assertEquals(Set.of("201", "400", "403", "409"), nomesDosCampos(respostas));
    }

    /**
     * Exemplos são o que o "Try it out" envia. Montado com os exemplos do próprio documento —
     * só e-mail e login trocados, porque precisam ser únicos —, o cadastro tem de ser aceito, e
     * o login de exemplo tem de autenticar.
     */
    @Test
    @DisplayName("Os exemplos documentados funcionam: o cadastro de exemplo é aceito e o login de exemplo entra")
    void deveTerExemplosQueFuncionam() {
        ObjectNode cadastro = exemplo("NewUserRequest");
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        cadastro.put("email", "exemplo." + sufixo + "@email.com");
        cadastro.put("login", "exemplo." + sufixo);

        ResponseEntity<JsonNode> criado = rest.postForEntity("/api/v1/users", corpo(cadastro), JsonNode.class);
        ResponseEntity<JsonNode> login = rest.postForEntity("/api/v1/auth/login", corpo(exemplo("LoginRequest")),
                JsonNode.class);

        assertEquals(HttpStatus.CREATED, criado.getStatusCode(), String.valueOf(criado.getBody()));
        assertEquals("01001000", criado.getBody().at("/addresses/0/zipCode").asText(),
                "o CEP de exemplo, com máscara, é normalizado");
        assertEquals(HttpStatus.OK, login.getStatusCode());
    }

    @Test
    @DisplayName("A interface do Swagger é pública")
    void deveServirASwaggerUiSemToken() {
        ResponseEntity<String> pagina = rest.getForEntity("/swagger-ui/index.html", String.class);

        assertEquals(HttpStatus.OK, pagina.getStatusCode());
        assertTrue(pagina.getBody().contains("swagger-ui"));
        assertNotEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity("/swagger-ui.html", String.class).getStatusCode());
    }

    // --- Apoio ------------------------------------------------------------------------------

    private record Operacao(String metodo, String caminho, JsonNode no) {

        String tag() {
            return no.get("tags").get(0).asText();
        }

        boolean protegida() {
            JsonNode seguranca = no.get("security");
            return seguranca != null && seguranca.findValue(BEARER) != null;
        }

        @Override
        public String toString() {
            return metodo + " " + caminho;
        }
    }

    private List<Operacao> operacoes() {
        List<Operacao> operacoes = new ArrayList<>();
        doc.get("paths").fields().forEachRemaining(caminho ->
                caminho.getValue().fields().forEachRemaining(metodo ->
                        operacoes.add(new Operacao(metodo.getKey().toUpperCase(), caminho.getKey(), metodo.getValue()))));
        return operacoes;
    }

    private HttpStatus chamarSemToken(Operacao op) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requisicao = new HttpEntity<>(op.metodo().equals("GET") || op.metodo().equals("DELETE")
                ? null : "{}", headers);
        String caminho = op.caminho().replace("{id}", UUID.randomUUID().toString());
        return HttpStatus.valueOf(rest.exchange(caminho, HttpMethod.valueOf(op.metodo()), requisicao, String.class)
                .getStatusCode().value());
    }

    /** Monta um corpo com os exemplos de cada campo, seguindo referências e listas. */
    private ObjectNode exemplo(String esquema) {
        ObjectNode corpo = objectMapper.createObjectNode();
        doc.at("/components/schemas/" + esquema + "/properties").fields().forEachRemaining(campo -> {
            JsonNode definicao = campo.getValue();
            if (definicao.has("example")) {
                corpo.set(campo.getKey(), definicao.get("example"));
            } else if ("array".equals(definicao.path("type").asText())) {
                JsonNode itens = definicao.get("items");
                corpo.putArray(campo.getKey()).add(itens.has("$ref")
                        ? exemplo(itens.get("$ref").asText().replace("#/components/schemas/", ""))
                        : itens.get("example"));
            }
        });
        return corpo;
    }

    private static Set<String> nomesDosCampos(JsonNode no) {
        Set<String> nomes = new TreeSet<>();
        no.fieldNames().forEachRemaining(nomes::add);
        return nomes;
    }
}
