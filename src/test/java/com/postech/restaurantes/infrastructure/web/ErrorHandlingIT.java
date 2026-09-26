package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * A tabela de tratamento de erros da especificação, percorrida por HTTP de verdade: cada linha
 * é provocada pelo caminho real — Bean Validation, domínio, caso de uso, segurança, banco — e a
 * resposta é conferida no formato ProblemDetail completo.
 */
class ErrorHandlingIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String URN = "urn:restaurantes:problema:";

    // --- 400 --------------------------------------------------------------------------------

    @Test
    @DisplayName("Bean Validation: 400 com o mapa errors por campo, mensagens em português")
    void deveDetalharCamposInvalidos() {
        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(Map.of(
                "name", "", "email", "nao-e-email", "login", "x", "password", "curta",
                "roles", List.of("ROLE_CUSTOMER"))), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida");
        JsonNode errors = problema.get("errors");
        assertTrue(errors.has("name"));
        assertTrue(errors.has("email"));
        assertTrue(errors.has("password"));
        assertFalse(errors.has("login"), "só os campos recusados aparecem");
        assertEquals("não deve estar em branco", errors.get("name").get(0).asText());
    }

    @Test
    @DisplayName("Corpo malformado: 400 sem a mensagem do parser")
    void deveRecusarCorpoMalformado() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS,
                new HttpEntity<>("{\"name\": ", headers), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida");
        assertEquals("O corpo da requisição está ausente ou malformado.", problema.get("detail").asText());
        assertFalse(problema.toString().contains("Unexpected"), "a mensagem do Jackson não vaza");
    }

    @Test
    @DisplayName("Id que não é UUID: 400 citando o parâmetro, não o valor")
    void deveRecusarIdMalformado() {
        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/nao-e-uuid", HttpMethod.GET,
                autenticado(admin()), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida");
        assertEquals("O parâmetro 'id' tem formato inválido.", problema.get("detail").asText());
    }

    @Test
    @DisplayName("Invariante do domínio: 400 com a mensagem do objeto de valor")
    void deveTraduzirInvarianteDoDominio() {
        Map<String, Object> corpo = novoUsuario("cep" + sufixo());
        corpo.put("addresses", List.of(Map.of("street", "Rua A", "number", "1", "neighborhood", "Centro",
                "city", "São Paulo", "state", "SP", "zipCode", "123")));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida");
        assertEquals("CEP deve ter 8 dígitos", problema.get("detail").asText());
    }

    @Test
    @DisplayName("Papel inexistente: 400 com a mensagem do domínio, não um erro de formato")
    void deveRecusarPapelInexistente() {
        Map<String, Object> corpo = novoUsuario("papel" + sufixo());
        corpo.put("roles", List.of("ROLE_INEXISTENTE"));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Requisição inválida");
        assertEquals("Papel inválido: ROLE_INEXISTENTE", problema.get("detail").asText());
    }

    @Test
    @DisplayName("Senha atual incorreta na troca: 400 senha-invalida")
    void deveRecusarSenhaAtualIncorreta() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + eu.id() + "/password", HttpMethod.PATCH,
                corpoAutenticado(Map.of("currentPassword", "senhaErrada1", "newPassword", "novaSenha456",
                        "confirmPassword", "novaSenha456"), eu.token()), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.BAD_REQUEST, "senha-invalida", "Senha inválida");
        assertEquals("Senha atual incorreta", problema.get("detail").asText());
    }

    @Test
    @DisplayName("Token de redefinição desconhecido: 400 token-invalido")
    void deveRecusarTokenDeRedefinicaoDesconhecido() {
        ResponseEntity<JsonNode> resposta = rest.postForEntity("/api/v1/auth/reset-password",
                corpo(Map.of("token", "token-que-nunca-existiu", "newPassword", "novaSenha456",
                        "confirmPassword", "novaSenha456")), JsonNode.class);

        conferir(resposta, HttpStatus.BAD_REQUEST, "token-invalido", "Token inválido ou expirado");
    }

    // --- 401 --------------------------------------------------------------------------------

    @Test
    @DisplayName("Credenciais erradas: 401 falha-na-autenticacao, com a mesma mensagem para login e senha")
    void deveRecusarCredenciais() {
        ResponseEntity<JsonNode> senhaErrada = rest.postForEntity("/api/v1/auth/login",
                corpo(Map.of("login", "cliente.demo", "password", "senhaErrada")), JsonNode.class);
        ResponseEntity<JsonNode> loginInexistente = rest.postForEntity("/api/v1/auth/login",
                corpo(Map.of("login", "nao.existe." + sufixo(), "password", "qualquer")), JsonNode.class);

        JsonNode a = conferir(senhaErrada, HttpStatus.UNAUTHORIZED, "falha-na-autenticacao", "Falha na autenticação");
        JsonNode b = conferir(loginInexistente, HttpStatus.UNAUTHORIZED, "falha-na-autenticacao",
                "Falha na autenticação");
        assertEquals(a.get("detail"), b.get("detail"), "a resposta não diz qual dos dois estava errado");
    }

    @Test
    @DisplayName("Sem token: 401 nao-autenticado em ProblemDetail, com o caminho da requisição")
    void deveResponderNaoAutenticado() {
        String caminho = USERS + "/" + UUID.randomUUID();

        ResponseEntity<JsonNode> resposta = rest.getForEntity(caminho, JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.UNAUTHORIZED, "nao-autenticado", "Não autenticado");
        assertEquals(caminho, problema.get("instance").asText());
    }

    // --- 403 --------------------------------------------------------------------------------

    @Test
    @DisplayName("Autocadastro pedindo ROLE_ADMIN: 403 operacao-nao-permitida")
    void deveRecusarAdministradorNoAutocadastro() {
        Map<String, Object> corpo = novoUsuario("admin" + sufixo());
        corpo.put("roles", List.of("ROLE_ADMIN"));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        conferir(resposta, HttpStatus.FORBIDDEN, "operacao-nao-permitida", "Operação não permitida");
    }

    @Test
    @DisplayName("Cadastro de outro usuário: 403 acesso-negado")
    void deveNegarCadastroAlheio() {
        Usuario eu = cadastrarEAutenticar();
        Usuario outro = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + outro.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.FORBIDDEN, "acesso-negado", "Acesso negado");
        assertFalse(problema.toString().contains(outro.login()), "nada do cadastro alheio vaza no erro");
    }

    // --- 404 e 409 --------------------------------------------------------------------------

    @Test
    @DisplayName("Usuário inexistente: 404 recurso-nao-encontrado")
    void deveResponderNaoEncontrado() {
        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + UUID.randomUUID(), HttpMethod.GET,
                autenticado(admin()), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.NOT_FOUND, "recurso-nao-encontrado", "Recurso não encontrado");
        assertEquals("Usuário não encontrado", problema.get("detail").asText());
    }

    @Test
    @DisplayName("E-mail já cadastrado: 409 conflito-de-dados")
    void deveResponderConflito() {
        String login = "dup" + sufixo();
        rest.postForEntity(USERS, corpo(novoUsuario(login)), JsonNode.class);
        Map<String, Object> repetido = novoUsuario("outro" + sufixo());
        repetido.put("email", login + "@email.com");

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(repetido), JsonNode.class);

        JsonNode problema = conferir(resposta, HttpStatus.CONFLICT, "conflito-de-dados", "Conflito de dados");
        assertEquals("E-mail já cadastrado", problema.get("detail").asText());
    }

    // --- Erros que o próprio Spring traduz --------------------------------------------------

    @Test
    @DisplayName("Método não suportado: 405 em ProblemDetail, também com timestamp")
    void deveTraduzirMetodoNaoSuportado() {
        ResponseEntity<JsonNode> resposta = rest.exchange(USERS, HttpMethod.DELETE, autenticado(admin()),
                JsonNode.class);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resposta.getStatusCode());
        assertProblemJson(resposta);
        assertNotNull(resposta.getBody().get("timestamp"), "a resposta padrão do Spring também é carimbada");
    }

    // --- Apoio ------------------------------------------------------------------------------

    /** Confere o formato completo e devolve o corpo para as asserções específicas do caso. */
    private static JsonNode conferir(ResponseEntity<JsonNode> resposta, HttpStatus status, String slug,
                                     String titulo) {
        assertEquals(status, resposta.getStatusCode());
        assertProblemJson(resposta);
        JsonNode corpo = resposta.getBody();
        assertEquals(URN + slug, corpo.get("type").asText());
        assertEquals(titulo, corpo.get("title").asText());
        assertEquals(status.value(), corpo.get("status").asInt());
        assertFalse(corpo.get("detail").asText().isBlank());
        assertTrue(corpo.get("instance").asText().startsWith("/api/v1/"), "instance aponta a requisição");
        assertTrue(corpo.get("timestamp").asText().matches("\\d{4}-\\d{2}-\\d{2}T.*"),
                "timestamp em ISO-8601, não como número");
        return corpo;
    }

    private static void assertProblemJson(ResponseEntity<JsonNode> resposta) {
        assertTrue(MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(resposta.getHeaders().getContentType()),
                "Content-Type application/problem+json");
    }

    private static String sufixo() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, Object> novoUsuario(String login) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("name", "Usuário " + login);
        corpo.put("email", login + "@email.com");
        corpo.put("login", login);
        corpo.put("password", "senhaSegura123");
        corpo.put("roles", List.of("ROLE_CUSTOMER"));
        return corpo;
    }

    private String admin() {
        return autenticar("admin.demo", "admin12345");
    }

    private Usuario cadastrarEAutenticar() {
        String login = "erro" + sufixo();
        JsonNode criado = rest.postForEntity(USERS, corpo(novoUsuario(login)), JsonNode.class).getBody();
        return new Usuario(UUID.fromString(criado.get("id").asText()), login, autenticar(login, "senhaSegura123"));
    }

    private String autenticar(String login, String senha) {
        return rest.postForEntity("/api/v1/auth/login", corpo(Map.of("login", login, "password", senha)),
                JsonNode.class).getBody().get("token").asText();
    }

    private record Usuario(UUID id, String login, String token) {
    }
}
