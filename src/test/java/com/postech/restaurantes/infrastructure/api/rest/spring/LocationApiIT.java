package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class LocationApiIT extends WebIntegrationTestSupport {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Lists seeded states and filters public city pages by state and name")
    void shouldReadCatalog() {
        var states = rest.getForEntity("/api/v1/states", JsonNode.class);
        assertEquals(HttpStatus.OK, states.getStatusCode());
        assertEquals(27, states.getBody().at("/_embedded/states").size());
        String stateId = jdbc.queryForObject("SELECT id::text FROM states WHERE acronym = 'SP'", String.class);
        var cities = rest.getForEntity("/api/v1/cities?stateId=" + stateId + "&size=1", JsonNode.class);
        assertEquals(HttpStatus.OK, cities.getStatusCode());
        assertEquals(2, cities.getBody().at("/page/totalElements").asInt());
        assertEquals("Campinas", cities.getBody().at("/_embedded/cities/0/name").asText());
        assertTrue(cities.getBody().at("/_links/next/href").asText().contains("stateId=" + stateId));
        var next = rest.getForEntity(cities.getBody().at("/_links/next/href").asText(), JsonNode.class);
        assertEquals("São Paulo", next.getBody().at("/_embedded/cities/0/name").asText());
        var filtered = rest.getForEntity("/api/v1/cities?stateId=" + stateId + "&name=camp", JsonNode.class);
        assertEquals(1, filtered.getBody().at("/page/totalElements").asInt());
        assertEquals("SP", filtered.getBody().at("/_embedded/cities/0/state/acronym").asText());
        assertEquals(28, jdbc.queryForObject("SELECT count(*) FROM cities", Integer.class));
        assertEquals(HttpStatus.BAD_REQUEST, rest.getForEntity("/api/v1/cities?size=101", JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, rest.getForEntity("/api/v1/cities?stateId=invalid", JsonNode.class).getStatusCode());
        assertEquals(0, rest.getForEntity("/api/v1/cities?stateId=" + UUID.randomUUID(), JsonNode.class)
                .getBody().at("/page/totalElements").asInt());
    }

    @Test
    @DisplayName("Does not expose catalog mutations, even for administrators")
    void shouldKeepCatalogReadOnly() {
        String token = autenticar("admin.demo", "admin12345");
        for (String route : List.of("/api/v1/states", "/api/v1/cities")) {
            for (HttpMethod method : List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
                var response = rest.exchange(route, method,
                        corpoAutenticado(Map.of("name", "New catalog entry"), token), JsonNode.class);
                assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
            }
        }
        assertEquals(27, jdbc.queryForObject("SELECT count(*) FROM states", Integer.class));
        assertEquals(28, jdbc.queryForObject("SELECT count(*) FROM cities", Integer.class));
    }

    @Test
    @DisplayName("Validates address city ids and returns the persisted city and state through the user lifecycle")
    void shouldRequireExistingCityForUsers() {
        Map<String, Object> address = address(LocationFixtures.CITY_ID);
        Map<String, Object> user = user(address);
        var created = rest.postForEntity("/api/v1/users", corpo(user), JsonNode.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        JsonNode original = created.getBody();
        assertEquals(LocationFixtures.CITY_ID.toString(), original.at("/addresses/0/address/city/id").asText());
        assertEquals("São Paulo", original.at("/addresses/0/address/city/name").asText());
        assertEquals("São Paulo", original.at("/addresses/0/address/city/state/name").asText());
        assertFalse(original.at("/addresses/0/address/city/state/id").asText().isBlank());
        String token = autenticar(user.get("login").toString(), "password123");
        String path = "/api/v1/users/" + original.get("id").asText();
        var updated = new HashMap<>(user);
        updated.remove("password");
        updated.put("addresses", List.of(Map.of("id", original.at("/addresses/0/id").asText(),
                "address", address(LocationFixtures.RIO_CITY_ID))));
        var response = rest.exchange(path, HttpMethod.PUT, corpoAutenticado(updated, token), JsonNode.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(original.at("/addresses/0/id"), response.getBody().at("/addresses/0/id"));
        assertEquals(original.at("/addresses/0/address/id"), response.getBody().at("/addresses/0/address/id"));
        assertEquals("Rio de Janeiro", response.getBody().at("/addresses/0/address/city/name").asText());
        assertEquals("RJ", response.getBody().at("/addresses/0/address/city/state/acronym").asText());
        var read = rest.exchange(path, HttpMethod.GET, autenticado(token), JsonNode.class);
        assertEquals(response.getBody().get("addresses"), read.getBody().get("addresses"));
        var missing = rest.postForEntity("/api/v1/users", corpo(user(withoutCity())), JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, missing.getStatusCode());
        var unknown = rest.postForEntity("/api/v1/users", corpo(user(address(UUID.randomUUID()))), JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, unknown.getStatusCode());
        assertEquals("urn:restaurantes:problema:recurso-nao-encontrado", unknown.getBody().get("type").asText());
        Map<String, Object> nullCity = address(null);
        assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity("/api/v1/users", corpo(user(nullCity)), JsonNode.class).getStatusCode());
        assertEquals(27, jdbc.queryForObject("SELECT count(*) FROM states", Integer.class));
        assertEquals(28, jdbc.queryForObject("SELECT count(*) FROM cities", Integer.class));
        assertEquals(HttpStatus.NO_CONTENT, rest.exchange(path, HttpMethod.DELETE, autenticado(token), Void.class).getStatusCode());
    }

    @Test
    @DisplayName("Requires an existing city for restaurants and preserves the address row on update")
    void shouldRequireExistingCityForRestaurants() {
        String token = autenticar("dono.restaurante", "dono12345");
        Map<String, Object> restaurant = new HashMap<>();
        restaurant.put("name", "Location restaurant " + UUID.randomUUID());
        restaurant.put("officeHours", List.of(Map.of("dayOfWeek", "MONDAY", "startTime", "08:00", "endTime", "18:00")));
        restaurant.put("address", address(LocationFixtures.CITY_ID));
        var created = rest.postForEntity("/api/v1/restaurants", corpoAutenticado(restaurant, token), JsonNode.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        String path = "/api/v1/restaurants/" + created.getBody().get("id").asText();
        restaurant.put("address", address(LocationFixtures.RIO_CITY_ID));
        var updated = rest.exchange(path, HttpMethod.PUT, corpoAutenticado(restaurant, token), JsonNode.class);
        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertEquals(created.getBody().at("/address/id"), updated.getBody().at("/address/id"));
        assertEquals("RJ", updated.getBody().at("/address/city/state/acronym").asText());
        restaurant.put("address", address(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND, rest.postForEntity("/api/v1/restaurants", corpoAutenticado(restaurant, token), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange(path, HttpMethod.PUT, corpoAutenticado(restaurant, token), JsonNode.class).getStatusCode());
        assertEquals("RJ", rest.getForEntity(path, JsonNode.class).getBody().at("/address/city/state/acronym").asText());
        restaurant.put("address", withoutCity());
        assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity("/api/v1/restaurants", corpoAutenticado(restaurant, token), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, rest.exchange(path, HttpMethod.DELETE, autenticado(token), Void.class).getStatusCode());
    }

    private static Map<String, Object> address(UUID cityId) {
        var address = withoutCity();
        address.put("cityId", cityId);
        return address;
    }

    private static Map<String, Object> withoutCity() {
        return new HashMap<>(Map.of("street", "Rua A", "number", "10", "neighborhood", "Centro", "zipCode", "01001-000"));
    }

    private Map<String, Object> user(Map<String, Object> address) {
        String login = "location" + UUID.randomUUID().toString().replace("-", "");
        return Map.of("name", "Location user", "login", login, "email", login + "@example.com",
                "password", "password123", "client", perfilDeCliente(), "addresses", List.of(Map.of("address", address)));
    }
}
