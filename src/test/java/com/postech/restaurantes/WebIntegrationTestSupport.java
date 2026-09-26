package com.postech.restaurantes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes que exercitam a API por HTTP de verdade: servidor em porta aleatória, banco
 * real e a cadeia de filtros de segurança inteira. É a única forma de provar que o
 * {@code @PreAuthorize} está mesmo ligado — um teste de unidade do controller passaria
 * exatamente igual com a anotação apagada.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = IntegrationTestProperties.JWT_SECRET)
public abstract class WebIntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = SharedPostgres.INSTANCE;

    @Autowired
    protected TestRestTemplate rest;

    /** Faz login pela API e devolve o token; falha o teste se o login não for aceito. */
    protected String autenticar(String login, String senha) {
        ResponseEntity<JsonNode> resposta = rest.postForEntity("/api/v1/auth/login",
                corpo(Map.of("login", login, "password", senha)), JsonNode.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode(), "login de " + login);
        return resposta.getBody().get("token").asText();
    }

    protected static HttpEntity<Object> corpo(Object body) {
        return new HttpEntity<>(body, cabecalhos(null));
    }

    protected static HttpEntity<Object> corpoAutenticado(Object body, String token) {
        return new HttpEntity<>(body, cabecalhos(token));
    }

    protected static HttpEntity<Void> autenticado(String token) {
        return new HttpEntity<>(cabecalhos(token));
    }

    private static HttpHeaders cabecalhos(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }
}
