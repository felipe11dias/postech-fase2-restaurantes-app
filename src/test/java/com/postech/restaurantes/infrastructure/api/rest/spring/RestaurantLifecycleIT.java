package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class RestaurantLifecycleIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String RESTAURANTS = "/api/v1/restaurants";

    @Test
    @DisplayName("Ciclo de vida do restaurante: criacao por dono, consulta publica, edicao e delecao")
    void devePercorrerCicloDeVidaRestaurante() {
        String loginDono = "dono" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        // 1. Cadastra dono
        ResponseEntity<JsonNode> cadastroDono = rest.postForEntity(USERS, corpo(Map.of(
                "name", "Dono Restaurante", "email", loginDono + "@email.com", "login", loginDono,
                "password", "senhaSegura123", "roles", List.of("ROLE_OWNER"),
                "addresses", List.of(Map.of("street", "Rua A", "number", "10", "neighborhood", "Bairro",
                        "city", "São Paulo", "state", "SP", "zipCode", "01000000")))), JsonNode.class);
        assertEquals(HttpStatus.CREATED, cadastroDono.getStatusCode());
        UUID userId = UUID.fromString(cadastroDono.getBody().get("id").asText());
        UUID addressId = UUID.fromString(cadastroDono.getBody().get("addresses").get(0).get("id").asText());

        // 2. Autentica como dono
        String tokenDono = autenticar(loginDono, "senhaSegura123");

        // 3. Cadastra restaurante
        ResponseEntity<JsonNode> cadastroRestaurante = rest.exchange(RESTAURANTS, HttpMethod.POST,
                corpoAutenticado(Map.of(
                        "userId", userId,
                        "addressId", addressId,
                        "name", "Restaurante Teste IT",
                        "officeHourStart", "08:00:00",
                        "officeHourEnd", "22:00:00"
                ), tokenDono), JsonNode.class);

        assertEquals(HttpStatus.CREATED, cadastroRestaurante.getStatusCode());
        URI location = cadastroRestaurante.getHeaders().getLocation();
        assertNotNull(location);

        // 4. Consulta pública (sem token)
        ResponseEntity<JsonNode> consulta = rest.getForEntity(location, JsonNode.class);
        assertEquals(HttpStatus.OK, consulta.getStatusCode());
        assertEquals("Restaurante Teste IT", consulta.getBody().get("name").asText());

        // 5. Atualização pelo dono
        ResponseEntity<JsonNode> atualizacao = rest.exchange(location, HttpMethod.PUT,
                corpoAutenticado(Map.of(
                        "userId", userId,
                        "addressId", addressId,
                        "name", "Restaurante Atualizado IT",
                        "officeHourStart", "09:00:00",
                        "officeHourEnd", "23:00:00"
                ), tokenDono), JsonNode.class);
        assertEquals(HttpStatus.OK, atualizacao.getStatusCode());
        assertEquals("Restaurante Atualizado IT", atualizacao.getBody().get("name").asText());

        // 6. Deleção pelo dono
        ResponseEntity<Void> exclusao = rest.exchange(location, HttpMethod.DELETE, autenticado(tokenDono), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, exclusao.getStatusCode());

        // 7. Consulta pós-exclusão devolve 404
        ResponseEntity<JsonNode> consultaPosExclusao = rest.getForEntity(location, JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, consultaPosExclusao.getStatusCode());
    }
}
