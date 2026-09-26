package com.postech.restaurantes.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tokens que um atacante — ou o simples passar do tempo — produz, contra a aplicação real. Todos
 * se passam pelo administrador da seed e pedem a listagem, que só ele pode ver: se algum desses
 * tokens fosse aceito, o dano seria máximo.
 *
 * <p>O primeiro teste é o controle: o mesmo formato, assinado com a chave certa e dentro da
 * validade, é aceito. Sem ele, os outros passariam também se o token montado aqui estivesse
 * simplesmente malformado — e não provariam nada sobre expiração ou assinatura.
 */
class JwtAuthenticationIT extends WebIntegrationTestSupport {

    private static final String LISTAGEM = "/api/v1/users";
    private static final String ADMIN_ID = "a0000000-0000-4000-8000-000000000003";

    @Autowired
    private JwtProperties properties;

    @Test
    @DisplayName("Controle: token do administrador, com a chave certa e dentro da validade, é aceito")
    void deveAceitarTokenValidoDoAdministrador() {
        String token = tokenDoAdministrador(chaveDaAplicacao(), Instant.now().plus(Duration.ofMinutes(5)));

        assertEquals(HttpStatus.OK, listar(token).getStatusCode());
    }

    @Test
    @DisplayName("Token expirado é recusado com 401, mesmo com assinatura válida")
    void deveRecusarTokenExpirado() {
        String token = tokenDoAdministrador(chaveDaAplicacao(), Instant.now().minus(Duration.ofMinutes(1)));

        ResponseEntity<JsonNode> resposta = listar(token);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("urn:restaurantes:problema:nao-autenticado", resposta.getBody().get("type").asText());
    }

    @Test
    @DisplayName("Token forjado com outra chave é recusado com 401, mesmo dizendo ser administrador")
    void deveRecusarTokenAssinadoComOutraChave() {
        SecretKey outraChave = Keys.hmacShaKeyFor(
                "chave-de-um-atacante-que-nao-conhece-o-segredo-real".getBytes(StandardCharsets.UTF_8));
        String token = tokenDoAdministrador(outraChave, Instant.now().plus(Duration.ofMinutes(5)));

        assertEquals(HttpStatus.UNAUTHORIZED, listar(token).getStatusCode());
    }

    @Test
    @DisplayName("Token sem assinatura (alg none) é recusado com 401")
    void deveRecusarTokenSemAssinatura() {
        String token = Jwts.builder()
                .subject(ADMIN_ID)
                .claim("login", "admin.demo")
                .claim("roles", List.of("ROLE_ADMIN"))
                .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(5))))
                .compact();

        assertEquals(HttpStatus.UNAUTHORIZED, listar(token).getStatusCode());
    }

    private ResponseEntity<JsonNode> listar(String token) {
        return rest.exchange(LISTAGEM, HttpMethod.GET, autenticado(token), JsonNode.class);
    }

    private SecretKey chaveDaAplicacao() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    private static String tokenDoAdministrador(SecretKey chave, Instant expiraEm) {
        return Jwts.builder()
                .subject(ADMIN_ID)
                .claim("login", "admin.demo")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(expiraEm.minus(Duration.ofHours(1))))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
    }
}
