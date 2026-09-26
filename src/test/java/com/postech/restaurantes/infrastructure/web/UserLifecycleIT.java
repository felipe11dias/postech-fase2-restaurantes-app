package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * O cenário principal de sucesso do cadastro (Cockburn), do começo ao fim, na ordem em que um
 * usuário o vive. Os outros testes de integração isolam cada passo; este prova que os passos se
 * encadeiam — o estado deixado por um é exatamente o que o seguinte precisa — e que, ao fim, a
 * exclusão não deixa nada para trás no banco.
 */
class UserLifecycleIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String LOGIN = "/api/v1/auth/login";

    /** Único dublê permitido: o e-mail do "esqueci minha senha" só precisa deixar o token gravado. */
    @MockitoBean
    private JavaMailSender mailSender;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Cadastro, login, consulta, atualização, troca de senha e exclusão sem deixar órfãos")
    void devePercorrerOCicloDeVidaDoCadastro() {
        String login = "ciclo" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        // 1. Cadastro público, com um endereço
        ResponseEntity<JsonNode> cadastro = rest.postForEntity(USERS, corpo(Map.of(
                "name", "Ciclo de Vida", "email", login + "@email.com", "login", login,
                "password", "senhaSegura123", "roles", List.of("ROLE_CUSTOMER"),
                "addresses", List.of(endereco("Rua das Flores")))), JsonNode.class);
        assertEquals(HttpStatus.CREATED, cadastro.getStatusCode());
        URI location = cadastro.getHeaders().getLocation();
        UUID id = UUID.fromString(cadastro.getBody().get("id").asText());

        // 2. Login e consulta do próprio cadastro pelo Location devolvido
        String token = autenticar(login, "senhaSegura123");
        ResponseEntity<JsonNode> consulta = rest.exchange(location, HttpMethod.GET, autenticado(token), JsonNode.class);
        assertEquals(HttpStatus.OK, consulta.getStatusCode());
        assertEquals("Rua das Flores", consulta.getBody().get("addresses").get(0).get("street").asText());

        // 3. Atualização troca o endereço inteiro; a senha continua a mesma
        ResponseEntity<JsonNode> atualizacao = rest.exchange(location, HttpMethod.PUT, corpoAutenticado(Map.of(
                "name", "Ciclo Atualizado", "email", login + "@email.com", "login", login,
                "addresses", List.of(endereco("Avenida Nova"))), token), JsonNode.class);
        assertEquals(HttpStatus.OK, atualizacao.getStatusCode());
        assertEquals(List.of("Avenida Nova"), ruas(id), "o endereço antigo foi removido, não acumulado");
        token = autenticar(login, "senhaSegura123");

        // 4. Troca de senha: a nova entra, a antiga sai
        ResponseEntity<Void> troca = rest.exchange(location + "/password", HttpMethod.PATCH, corpoAutenticado(Map.of(
                "currentPassword", "senhaSegura123", "newPassword", "senhaNova456",
                "confirmPassword", "senhaNova456"), token), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, troca.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, statusDoLogin(login, "senhaSegura123"));
        token = autenticar(login, "senhaNova456");

        // 5. Um pedido de redefinição pendente, para que a exclusão tenha um token a levar junto
        rest.postForEntity("/api/v1/auth/forgot-password", corpo(Map.of("email", login + "@email.com")), Void.class);
        assertEquals(1, linhas("password_reset_tokens", id));

        // 6. Exclusão: o cadastro some, e com ele tudo o que dependia dele
        ResponseEntity<Void> exclusao = rest.exchange(location, HttpMethod.DELETE, autenticado(token), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, exclusao.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                rest.exchange(location, HttpMethod.GET, autenticado(token), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, statusDoLogin(login, "senhaNova456"));
        assertEquals(0, linhas("users", id, "id"));
        assertEquals(0, linhas("addresses", id));
        assertEquals(0, linhas("user_roles", id));
        assertEquals(0, linhas("password_reset_tokens", id));
    }

    private HttpStatus statusDoLogin(String login, String senha) {
        return HttpStatus.valueOf(rest.postForEntity(LOGIN, corpo(Map.of("login", login, "password", senha)),
                JsonNode.class).getStatusCode().value());
    }

    private List<String> ruas(UUID userId) {
        return jdbc.queryForList("SELECT street FROM addresses WHERE user_id = ?", String.class, userId);
    }

    private int linhas(String tabela, UUID userId) {
        return linhas(tabela, userId, "user_id");
    }

    /** Tabela e coluna são constantes deste teste, nunca entrada externa. */
    private int linhas(String tabela, UUID userId, String coluna) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabela + " WHERE " + coluna + " = ?",
                Integer.class, userId);
    }

    private static Map<String, Object> endereco(String rua) {
        return Map.of("street", rua, "number", "100", "complement", "Apto 21", "neighborhood", "Centro",
                "city", "São Paulo", "state", "SP", "zipCode", "01001-000");
    }
}
