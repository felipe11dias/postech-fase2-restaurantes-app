package com.postech.restaurantes.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.postech.restaurantes.infrastructure.web.error.ProblemDetailFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class JwtAuthenticationEntryPointTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 12, 0);

    /**
     * Configurado como o Spring Boot configura o seu: o builder registra o mixin que achata o
     * ProblemDetail, e o Boot desliga a escrita de datas como número. O teste de integração
     * confere o formato com o ObjectMapper real da aplicação.
     */
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(
            new ProblemDetailFactory(Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)), objectMapper);

    @Test
    @DisplayName("Sem credencial, responde 401 em ProblemDetail, como todo outro erro da API")
    void deveResponderEmProblemDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("sem token"));

        assertEquals(401, response.getStatus());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON_VALUE, response.getContentType().split(";")[0]);
        JsonNode corpo = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals("urn:restaurantes:problema:nao-autenticado", corpo.get("type").asText());
        assertEquals("Não autenticado", corpo.get("title").asText());
        assertEquals(401, corpo.get("status").asInt());
        assertEquals(JwtAuthenticationEntryPoint.DETAIL, corpo.get("detail").asText());
        assertEquals("/api/v1/users/123", corpo.get("instance").asText());
        assertEquals("2026-03-10T12:00:00", corpo.get("timestamp").asText());
    }

    @Test
    @DisplayName("O detalhe não revela o motivo da recusa: ausente, expirado ou adulterado dão a mesma resposta")
    void naoDeveRevelarOMotivo() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(new MockHttpServletRequest("GET", "/x"), response,
                new InsufficientAuthenticationException("JWT expired at 2026-03-10T11:00:00Z"));

        assertEquals(JwtAuthenticationEntryPoint.DETAIL,
                objectMapper.readTree(response.getContentAsByteArray()).get("detail").asText());
    }
}
