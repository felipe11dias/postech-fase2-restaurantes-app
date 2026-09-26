package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A API de usuários por HTTP de verdade, com a cadeia de segurança inteira no caminho.
 */
class UserApiIT extends WebIntegrationTestSupport {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    private static final String USERS = "/api/v1/users";

    @Test
    @DisplayName("Cadastro é público, responde 201 com Location e links, e nunca devolve a senha")
    void deveCadastrarSemAutenticacao() {
        String login = "api" + SEQUENCIA.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(novoUsuario(login)), JsonNode.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        JsonNode corpo = resposta.getBody();
        assertNotNull(corpo.get("id").asText());
        assertEquals(login, corpo.get("login").asText());
        assertEquals("ROLE_CUSTOMER", corpo.get("roles").get(0).get("name").asText());
        assertEquals("01001000", corpo.get("addresses").get(0).get("zipCode").asText(), "CEP sai normalizado");
        assertTrue(corpo.has("_links"));
        assertTrue(resposta.getHeaders().getLocation().toString().endsWith(USERS + "/" + corpo.get("id").asText()));
        assertFalse(corpo.has("password"), "a resposta não tem campo de senha");
        assertFalse(corpo.toString().contains("$2a$"), "nenhum hash vaza no corpo");
    }

    @Test
    @DisplayName("Consulta sem token é recusada com 401")
    void deveRecusarSemToken() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.getForEntity(USERS + "/" + UUID.randomUUID(), JsonNode.class).getStatusCode());
    }

    @Test
    @DisplayName("Token inválido ou expirado é tratado como ausente")
    void deveRecusarTokenInvalido() {
        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + UUID.randomUUID(), HttpMethod.GET,
                autenticado("token.que.nao.vale"), JsonNode.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
    }

    @Test
    @DisplayName("Usuário autenticado lê o próprio cadastro")
    void devePermitirLerASiMesmo() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + eu.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(eu.login(), resposta.getBody().get("login").asText());
    }

    @Test
    @DisplayName("Conhecer o id de outro usuário não dá acesso a ele: 403")
    void deveRecusarAcessoAoCadastroAlheio() {
        Usuario eu = cadastrarEAutenticar();
        Usuario outro = cadastrarEAutenticar();

        ResponseEntity<JsonNode> leitura = rest.exchange(USERS + "/" + outro.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);
        ResponseEntity<JsonNode> exclusao = rest.exchange(USERS + "/" + outro.id(), HttpMethod.DELETE,
                autenticado(eu.token()), JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, leitura.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, exclusao.getStatusCode());
    }

    @Test
    @DisplayName("Administrador lê o cadastro de qualquer usuário")
    void devePermitirAoAdministrador() {
        Usuario outro = cadastrarEAutenticar();
        String admin = autenticar("admin.demo", "admin12345");

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + outro.id(), HttpMethod.GET,
                autenticado(admin), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(outro.login(), resposta.getBody().get("login").asText());
    }

    @Test
    @DisplayName("Listagem é do administrador e devolve página com metadados e links")
    void deveListarPaginadoParaOAdministrador() {
        String admin = autenticar("admin.demo", "admin12345");

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "?page=0&size=5&sort=name,asc", HttpMethod.GET,
                autenticado(admin), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(5, resposta.getBody().get("page").get("size").asInt());
        assertTrue(resposta.getBody().get("page").get("totalElements").asInt() >= 1);
        assertTrue(resposta.getBody().has("_links"));
    }

    /**
     * A listagem devolve e-mail, login e endereço de cada cadastro. Aberta a qualquer
     * autenticado, ela entregaria de uma vez o que a regra de posse recusa um a um.
     */
    @Test
    @DisplayName("Usuário comum não lista os cadastros alheios: 403; sem token, 401")
    void deveRecusarListagemAoUsuarioComum() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "?page=0&size=100", HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertFalse(String.valueOf(resposta.getBody()).contains("@email.com"), "nenhum e-mail vaza no 403");
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity(USERS + "?page=0&size=5", JsonNode.class)
                .getStatusCode());
    }

    @Test
    @DisplayName("Senha de 40 caracteres acentuados (80 bytes) é recusada na borda com 400, e não estoura no BCrypt")
    void deveRecusarSenhaAcimaDe72Bytes() {
        String login = "bytes" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> corpo = new java.util.HashMap<>(novoUsuario(login));
        corpo.put("password", "ç".repeat(40));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
    }

    @Test
    @DisplayName("Atualização do próprio cadastro grava e devolve o estado novo")
    void deveAtualizarOProprioCadastro() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + eu.id(), HttpMethod.PUT,
                corpoAutenticado(Map.of("name", "Nome Atualizado", "email", eu.login() + "@email.com",
                        "login", eu.login(), "addresses", List.of()), eu.token()), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("Nome Atualizado", resposta.getBody().get("name").asText());
        assertEquals(0, resposta.getBody().get("addresses").size());
    }

    @Test
    @DisplayName("Troca de senha responde 204 e passa a valer no login seguinte")
    void deveTrocarASenha() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<Void> troca = rest.exchange(USERS + "/" + eu.id() + "/password", HttpMethod.PATCH,
                corpoAutenticado(Map.of("currentPassword", "senhaSegura123", "newPassword", "novaSenha456",
                        "confirmPassword", "novaSenha456"), eu.token()), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, troca.getStatusCode());
        assertNotNull(autenticar(eu.login(), "novaSenha456"));
    }

    @Test
    @DisplayName("Exclusão do próprio cadastro responde 204 e o token deixa de dar acesso")
    void deveExcluirOProprioCadastro() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<Void> exclusao = rest.exchange(USERS + "/" + eu.id(), HttpMethod.DELETE,
                autenticado(eu.token()), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, exclusao.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange(USERS + "/" + eu.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class).getStatusCode(),
                "o token ainda é válido, mas o cadastro não existe mais");
    }

    @Test
    @DisplayName("Corpo inválido é recusado na borda, antes de chegar ao caso de uso")
    void deveRecusarCorpoInvalido() {
        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS,
                corpo(Map.of("name", "", "email", "nao-e-email", "login", "x", "password", "curta",
                        "roles", List.of("ROLE_CUSTOMER"))), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
    }

    private Map<String, Object> novoUsuario(String login) {
        return Map.of(
                "name", "Usuário " + login,
                "email", login + "@email.com",
                "login", login,
                "password", "senhaSegura123",
                "roles", List.of("ROLE_CUSTOMER"),
                "addresses", List.of(Map.of("street", "Rua das Flores", "number", "100",
                        "complement", "Apto 21", "neighborhood", "Centro", "city", "São Paulo",
                        "state", "SP", "zipCode", "01001-000")));
    }

    private Usuario cadastrarEAutenticar() {
        String login = "api" + SEQUENCIA.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);
        JsonNode criado = rest.postForEntity(USERS, corpo(novoUsuario(login)), JsonNode.class).getBody();
        return new Usuario(UUID.fromString(criado.get("id").asText()), login,
                autenticar(login, "senhaSegura123"));
    }

    private String autenticar(String login, String senha) {
        ResponseEntity<JsonNode> resposta = rest.postForEntity("/api/v1/auth/login",
                corpo(Map.of("login", login, "password", senha)), JsonNode.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode(), "login de " + login);
        return resposta.getBody().get("token").asText();
    }

    private record Usuario(UUID id, String login, String token) {
    }
}
