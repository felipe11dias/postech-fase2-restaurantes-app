package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.EnderecosNoBanco;
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
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Posse do restaurante (Etapa 23), por HTTP de verdade: o dono cadastra para si, altera e exclui só os
 * próprios restaurantes e não troca de dono; o administrador faz tudo isso por qualquer um. E a exclusão do
 * usuário leva os restaurantes dele, com o endereço de cada um.
 */
class RestaurantOwnershipIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String RESTAURANTS = "/api/v1/restaurants";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Sem userId no corpo, o restaurante é de quem cadastrou; dono não cadastra para outro dono")
    void deveCadastrarParaOProprioDono() {
        Dono eu = cadastrarDono();
        Dono outro = cadastrarDono();

        ResponseEntity<JsonNode> meu = criar(eu.token(), null);
        ResponseEntity<JsonNode> paraOutro = criar(eu.token(), outro.id());

        assertEquals(HttpStatus.CREATED, meu.getStatusCode());
        assertEquals(eu.id().toString(), meu.getBody().get("userId").asText());
        assertEquals(HttpStatus.FORBIDDEN, paraOutro.getStatusCode());
        assertEquals("urn:restaurantes:problema:acesso-negado", paraOutro.getBody().get("type").asText());
    }

    @Test
    @DisplayName("Dono não altera nem exclui o restaurante de outro dono (403); o restaurante continua como estava")
    void naoDeveMexerNoRestauranteDeOutroDono() {
        Dono dono = cadastrarDono();
        Dono intruso = cadastrarDono();
        String restaurante = criar(dono.token(), null).getBody().get("id").asText();

        ResponseEntity<JsonNode> alteracao = rest.exchange(RESTAURANTS + "/" + restaurante, HttpMethod.PUT,
                corpoAutenticado(corpo("Invadido", null), intruso.token()), JsonNode.class);
        ResponseEntity<JsonNode> exclusao = rest.exchange(RESTAURANTS + "/" + restaurante, HttpMethod.DELETE,
                autenticado(intruso.token()), JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, alteracao.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, exclusao.getStatusCode());
        JsonNode atual = rest.getForEntity(RESTAURANTS + "/" + restaurante, JsonNode.class).getBody();
        assertEquals(dono.id().toString(), atual.get("userId").asText());
        assertEquals("Restaurante do Dono", atual.get("name").asText());
    }

    @Test
    @DisplayName("Trocar o dono é do administrador: o dono recebe 403, o administrador consegue")
    void deveTrocarODonoSoPeloAdministrador() {
        Dono dono = cadastrarDono();
        Dono novo = cadastrarDono();
        String restaurante = criar(dono.token(), null).getBody().get("id").asText();

        ResponseEntity<JsonNode> peloDono = rest.exchange(RESTAURANTS + "/" + restaurante, HttpMethod.PUT,
                corpoAutenticado(corpo("Transferido", novo.id()), dono.token()), JsonNode.class);
        ResponseEntity<JsonNode> semTrocar = rest.exchange(RESTAURANTS + "/" + restaurante, HttpMethod.PUT,
                corpoAutenticado(corpo("Renomeado", null), dono.token()), JsonNode.class);
        ResponseEntity<JsonNode> peloAdmin = rest.exchange(RESTAURANTS + "/" + restaurante, HttpMethod.PUT,
                corpoAutenticado(corpo("Transferido", novo.id()), admin()), JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, peloDono.getStatusCode());
        assertEquals(HttpStatus.OK, semTrocar.getStatusCode());
        assertEquals(dono.id().toString(), semTrocar.getBody().get("userId").asText(), "sem userId, o dono fica");
        assertEquals(HttpStatus.OK, peloAdmin.getStatusCode());
        assertEquals(novo.id().toString(), peloAdmin.getBody().get("userId").asText());
    }

    @Test
    @DisplayName("Restaurante inexistente: o dono recebe 403 (não é dele), o administrador 404")
    void deveEsconderORestauranteInexistenteDoDono() {
        Dono dono = cadastrarDono();
        String inexistente = RESTAURANTS + "/" + UUID.randomUUID();

        assertEquals(HttpStatus.FORBIDDEN, rest.exchange(inexistente, HttpMethod.DELETE, autenticado(dono.token()),
                JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange(inexistente, HttpMethod.DELETE, autenticado(admin()),
                JsonNode.class).getStatusCode());
    }

    @Test
    @DisplayName("A listagem filtra pelo dono, e os links de navegação repetem o filtro")
    void deveListarPorDono() {
        Dono dono = cadastrarDono();
        criar(dono.token(), null);
        criar(dono.token(), null);
        criar(cadastrarDono().token(), null);

        JsonNode pagina = rest.getForEntity(RESTAURANTS + "?ownerId=" + dono.id() + "&size=1", JsonNode.class).getBody();

        assertEquals(2, pagina.at("/page/totalElements").asInt());
        assertEquals(dono.id().toString(), pagina.at("/_embedded/restaurantResponseList/0/userId").asText());
        assertEquals(true, pagina.at("/_links/next/href").asText().contains("ownerId=" + dono.id()));
    }

    @Test
    @DisplayName("Excluir o usuário exclui os restaurantes dele, com o endereço de cada um")
    void deveExcluirOsRestaurantesComOUsuario() {
        Dono dono = cadastrarDono();
        JsonNode primeiro = criar(dono.token(), null).getBody();
        JsonNode segundo = criar(dono.token(), null).getBody();
        List<UUID> enderecos = List.of(UUID.fromString(primeiro.at("/address/id").asText()),
                UUID.fromString(segundo.at("/address/id").asText()));

        ResponseEntity<Void> exclusao = rest.exchange(USERS + "/" + dono.id(), HttpMethod.DELETE,
                autenticado(dono.token()), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, exclusao.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.getForEntity(RESTAURANTS + "/" + primeiro.get("id").asText(),
                JsonNode.class).getStatusCode());
        assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM restaurants WHERE user_id = ?",
                Integer.class, dono.id()));
        assertEquals(0, EnderecosNoBanco.existentes(jdbc, enderecos), "o endereço do restaurante sai junto");
    }

    private Dono cadastrarDono() {
        String login = "posse." + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<JsonNode> criado = rest.postForEntity(USERS, corpo(Map.of("name", "Dono " + login,
                "email", login + "@email.com", "login", login, "password", "senhaSegura123",
                "owner", perfilDeDono())), JsonNode.class);
        assertEquals(HttpStatus.CREATED, criado.getStatusCode(), String.valueOf(criado.getBody()));
        return new Dono(UUID.fromString(criado.getBody().get("id").asText()), autenticar(login, "senhaSegura123"));
    }

    private ResponseEntity<JsonNode> criar(String token, UUID userId) {
        return rest.exchange(RESTAURANTS, HttpMethod.POST, corpoAutenticado(corpo("Restaurante do Dono", userId), token),
                JsonNode.class);
    }

    private static Map<String, Object> corpo(String nome, UUID userId) {
        Map<String, Object> corpo = new HashMap<>(Map.of("name", nome, "officeHourStart", "08:00:00",
                "officeHourEnd", "22:00:00", "address", Map.of("street", "Rua do Restaurante", "number", "1",
                        "neighborhood", "Centro", "city", "São Paulo", "state", "SP", "zipCode", "01001000")));
        if (userId != null) {
            corpo.put("userId", userId);
        }
        return corpo;
    }

    private String admin() {
        return autenticar("admin.demo", "admin12345");
    }

    private record Dono(UUID id, String token) {
    }
}
