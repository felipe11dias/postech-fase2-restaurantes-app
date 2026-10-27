package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProductCatalogIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String RESTAURANTS = "/api/v1/restaurants";

    @Test
    @DisplayName("Cadastra, consulta, atualiza e exclui produto, imagens e opções por HTTP")
    void devePercorrerCicloDeVidaDoProduto() {
        String login = "produto" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ResponseEntity<JsonNode> newUser = rest.postForEntity(USERS, corpo(Map.of(
                "name", "Dono Produto",
                "email", login + "@email.com",
                "login", login,
                "password", "senhaSegura123",
                "owner", perfilDeDono()
        )), JsonNode.class);
        assertEquals(HttpStatus.CREATED, newUser.getStatusCode());
        String token = autenticar(login, "senhaSegura123");
        UUID userId = UUID.fromString(newUser.getBody().get("id").asText());

        ResponseEntity<JsonNode> newRestaurant = rest.exchange(RESTAURANTS, HttpMethod.POST,
                corpoAutenticado(Map.of(
                        "userId", userId,
                        "name", "Restaurante Produto",
                        "address", endereco(),
                        "officeHours", List.of(Map.of(
                                "dayOfWeek", "MONDAY", "startTime", "08:00:00", "endTime", "22:00:00"))
                ), token), JsonNode.class);
        assertEquals(HttpStatus.CREATED, newRestaurant.getStatusCode());
        UUID restaurantId = UUID.fromString(newRestaurant.getBody().get("id").asText());
        String products = "/api/v1/restaurants/" + restaurantId + "/products";

        ResponseEntity<JsonNode> created = rest.exchange(products, HttpMethod.POST,
                corpoAutenticado(productBody("Pizza", "/images/pizza.jpg", "Borda", "Recheada"), token),
                JsonNode.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getHeaders().getLocation());
        UUID productId = UUID.fromString(created.getBody().get("id").asText());
        assertEquals("Pizza", created.getBody().get("name").asText());
        assertEquals(0, created.getBody().get("price").decimalValue().compareTo(new BigDecimal("25.50")));
        assertEquals("/images/pizza.jpg", created.getBody().at("/imagePaths/0").asText());
        assertEquals("Recheada", created.getBody().at("/optionGroups/0/values/0/name").asText());

        URI productUri = created.getHeaders().getLocation();
        ResponseEntity<JsonNode> found = rest.getForEntity(productUri, JsonNode.class);
        assertEquals(HttpStatus.OK, found.getStatusCode());
        assertEquals(productId.toString(), found.getBody().get("id").asText());
        assertEquals(0, found.getBody().get("price").decimalValue().compareTo(new BigDecimal("25.50")));
        assertEquals(1, rest.getForEntity(products + "?page=0&size=10", JsonNode.class)
                .getBody().get("_embedded").get("productResponseList").size());

        ResponseEntity<JsonNode> updated = rest.exchange(productUri, HttpMethod.PUT,
                corpoAutenticado(productBody("Pizza atualizada", "/images/pizza-atualizada.jpg",
                        "Extras", "Azeitona"), token),
                JsonNode.class);
        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertEquals("Pizza atualizada", updated.getBody().get("name").asText());
        assertEquals("Azeitona", updated.getBody().at("/optionGroups/0/values/0/name").asText());

        ResponseEntity<Void> deleted = rest.exchange(productUri, HttpMethod.DELETE, autenticado(token), Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.getForEntity(productUri, JsonNode.class).getStatusCode());
    }

    private static Map<String, Object> productBody(String name, String image, String group, String value) {
        return Map.of(
                "name", name,
                "description", "Produto de integração",
                "price", new BigDecimal("25.50"),
                "dineInOnly", false,
                "imagePaths", List.of(image),
                "optionGroups", List.of(Map.of(
                        "name", group,
                        "required", true,
                        "minSelections", 1,
                        "maxSelections", 1,
                        "values", List.of(Map.of("name", value, "additionalPrice", 2.00))
                ))
        );
    }

    private static Map<String, Object> endereco() {
        return Map.of("cityId", LocationFixtures.CITY_ID, "street", "Rua do Restaurante",
                "number", "10", "neighborhood", "Centro", "zipCode", "01000-000");
    }
}
