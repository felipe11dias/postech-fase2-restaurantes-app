package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.EnderecosNoBanco;
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

class RestaurantLifecycleIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String RESTAURANTS = "/api/v1/restaurants";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Ciclo de vida do restaurante: criação por dono com endereço próprio, consulta pública, edição e exclusão")
    void devePercorrerCicloDeVidaRestaurante() {
        String loginDono = "dono" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        // 1. Cadastra dono, com o endereço dele
        ResponseEntity<JsonNode> cadastroDono = rest.postForEntity(USERS, corpo(Map.of(
                "name", "Dono Restaurante", "email", loginDono + "@email.com", "login", loginDono,
                "password", "senhaSegura123", "owner", perfilDeDono(),
                "addresses", List.of(Map.of("label", "Casa", "address", endereco("Rua do Dono")))
        )), JsonNode.class);
        assertEquals(HttpStatus.CREATED, cadastroDono.getStatusCode());
        UUID userId = UUID.fromString(cadastroDono.getBody().get("id").asText());
        String enderecoDoDono = cadastroDono.getBody().at("/addresses/0/address/id").asText();

        // 2. Autentica como dono
        String tokenDono = autenticar(loginDono, "senhaSegura123");

        // 3. Cadastra restaurante: o endereço vem no corpo e é do restaurante
        ResponseEntity<JsonNode> cadastroRestaurante = rest.exchange(RESTAURANTS, HttpMethod.POST,
                corpoAutenticado(Map.of(
                        "userId", userId,
                        "address", endereco("Rua do Restaurante"),
                        "name", "Restaurante Teste IT",
                        "officeHours", List.of(Map.of("dayOfWeek", "MONDAY", "startTime", "08:00:00", "endTime", "22:00:00"))
                ), tokenDono), JsonNode.class);

        assertEquals(HttpStatus.CREATED, cadastroRestaurante.getStatusCode());
        URI location = cadastroRestaurante.getHeaders().getLocation();
        assertNotNull(location);
        String enderecoDoRestaurante = cadastroRestaurante.getBody().at("/address/id").asText();
        assertNotEquals(enderecoDoDono, enderecoDoRestaurante, "o restaurante não compartilha o endereço do dono");
        assertEquals("01000000", cadastroRestaurante.getBody().at("/address/zipCode").asText());

        // 4. Consulta pública (sem token)
        ResponseEntity<JsonNode> consulta = rest.getForEntity(location, JsonNode.class);
        assertEquals(HttpStatus.OK, consulta.getStatusCode());
        assertEquals("Restaurante Teste IT", consulta.getBody().get("name").asText());
        assertEquals("Rua do Restaurante", consulta.getBody().at("/address/street").asText());

        // 5. Atualização pelo dono: o endereço muda na mesma linha
        ResponseEntity<JsonNode> atualizacao = rest.exchange(location, HttpMethod.PUT,
                corpoAutenticado(Map.of(
                        "userId", userId,
                        "address", endereco("Avenida Nova"),
                        "name", "Restaurante Atualizado IT",
                        "officeHours", List.of(Map.of("dayOfWeek", "MONDAY", "startTime", "09:00:00", "endTime", "23:00:00"))
                ), tokenDono), JsonNode.class);
        assertEquals(HttpStatus.OK, atualizacao.getStatusCode());
        assertEquals("Restaurante Atualizado IT", atualizacao.getBody().get("name").asText());
        assertEquals("Avenida Nova", atualizacao.getBody().at("/address/street").asText());
        assertEquals(enderecoDoRestaurante, atualizacao.getBody().at("/address/id").asText());

        // 6. Exclusão pelo dono: o endereço do restaurante vai junto; o do dono fica
        ResponseEntity<Void> exclusao = rest.exchange(location, HttpMethod.DELETE, autenticado(tokenDono), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, exclusao.getStatusCode());
        assertEquals(0, EnderecosNoBanco.existentes(jdbc, List.of(UUID.fromString(enderecoDoRestaurante))));
        assertEquals(1, EnderecosNoBanco.existentes(jdbc, List.of(UUID.fromString(enderecoDoDono))));

        // 7. Consulta pós-exclusão devolve 404
        ResponseEntity<JsonNode> consultaPosExclusao = rest.getForEntity(location, JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, consultaPosExclusao.getStatusCode());
    }

    @Test
    @DisplayName("Restaurante sem endereço no corpo é recusado com 400")
    void deveRecusarRestauranteSemEndereco() {
        String loginDono = "dono" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ResponseEntity<JsonNode> cadastroDono = rest.postForEntity(USERS, corpo(Map.of(
                "name", "Dono Sem Endereço", "email", loginDono + "@email.com", "login", loginDono,
                "password", "senhaSegura123", "owner", perfilDeDono())), JsonNode.class);
        UUID userId = UUID.fromString(cadastroDono.getBody().get("id").asText());

        ResponseEntity<JsonNode> resposta = rest.exchange(RESTAURANTS, HttpMethod.POST,
                corpoAutenticado(Map.of(
                        "userId", userId,
                        "name", "Sem Endereço",
                        "officeHours", List.of(Map.of("dayOfWeek", "MONDAY", "startTime", "08:00:00", "endTime", "22:00:00"))
                ), autenticar(loginDono, "senhaSegura123")), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertTrue(resposta.getBody().get("errors").has("address"), "a recusa aponta o endereço ausente");
    }

    private static Map<String, Object> endereco(String rua) {
        return Map.of("street", rua, "number", "10", "neighborhood", "Bairro",
                "city", "São Paulo", "state", "SP", "zipCode", "01000-000");
    }
}
