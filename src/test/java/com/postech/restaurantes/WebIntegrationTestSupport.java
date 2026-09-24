package com.postech.restaurantes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes que exercitam a API por HTTP de verdade: servidor em porta aleatória, banco
 * real e a cadeia de filtros de segurança inteira. É a única forma de provar que o
 * {@code @PreAuthorize} está mesmo ligado — um teste de unidade do controller passaria
 * exatamente igual com a anotação apagada.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class WebIntegrationTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = SharedPostgres.INSTANCE;

    @Autowired
    protected TestRestTemplate rest;

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
