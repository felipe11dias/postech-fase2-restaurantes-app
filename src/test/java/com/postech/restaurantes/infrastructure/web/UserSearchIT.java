package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Listagem paginada por HTTP, do parâmetro da URL ao {@code ORDER BY} e de volta aos links.
 *
 * <p>O banco é compartilhado com as outras classes, então cada teste cria três cadastros com uma
 * marca própria no nome e busca só por ela: o resultado esperado não depende de quem mais existe.
 * A marca aparece com caixas diferentes em cada nome, e a busca usa um trecho do meio dela.
 */
class UserSearchIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";

    private String marca;
    private String admin;

    @BeforeEach
    void cadastrarTresComAMesmaMarca() {
        marca = "Mk" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        cadastrar("Ana " + marca);
        cadastrar("Bruno " + marca.toUpperCase());
        cadastrar("Carla " + marca.toLowerCase());
        admin = autenticar("admin.demo", "admin12345");
    }

    @Test
    @DisplayName("Busca por trecho do nome encontra os três, sem diferenciar maiúsculas")
    void deveBuscarPorTrechoDoNomeSemDiferenciarMaiusculas() {
        String trecho = marca.substring(1, 7).toLowerCase();

        JsonNode pagina = listar(USERS + "?name=" + trecho + "&size=10&sort=name,asc").getBody();

        assertEquals(List.of("Ana", "Bruno", "Carla"), primeirosNomes(pagina));
        assertEquals(3, pagina.get("page").get("totalElements").asInt());
    }

    @Test
    @DisplayName("Seguindo os links next a partir da primeira página, o cliente percorre tudo sem montar URL")
    void devePercorrerAsPaginasSeguindoOsLinks() {
        List<String> visitados = new ArrayList<>();
        ResponseEntity<JsonNode> resposta = listar(USERS + "?name=" + marca + "&size=1&sort=name,asc");
        JsonNode links = resposta.getBody().get("_links");
        String ultima = links.get("last").get("href").asText();
        assertFalse(links.has("prev"), "a primeira página não tem anterior");

        while (true) {
            visitados.addAll(primeirosNomes(resposta.getBody()));
            links = resposta.getBody().get("_links");
            if (!links.has("next")) {
                break;
            }
            resposta = listar(links.get("next").get("href").asText());
        }

        assertEquals(List.of("Ana", "Bruno", "Carla"), visitados);
        assertEquals(ultima, links.get("self").get("href").asText(), "a página sem next é a apontada por last");
        assertTrue(links.get("prev").get("href").asText().contains("page=1"));
        assertTrue(links.get("first").get("href").asText().contains("page=0"));
    }

    @Test
    @DisplayName("Ordenação decrescente pedida pela URL chega ao banco")
    void deveOrdenarDeFormaDecrescente() {
        JsonNode pagina = listar(USERS + "?name=" + marca + "&sort=name,desc").getBody();

        assertEquals(List.of("Carla", "Bruno", "Ana"), primeirosNomes(pagina));
    }

    @Test
    @DisplayName("Ordenar pela senha é ignorado: responde 200 na ordenação padrão, nome crescente")
    void deveIgnorarOrdenacaoPelaSenha() {
        ResponseEntity<JsonNode> resposta = listar(USERS + "?name=" + marca + "&sort=password,desc");

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(List.of("Ana", "Bruno", "Carla"), primeirosNomes(resposta.getBody()),
                "o pedido inteiro é descartado, direção inclusive: nada da ordem revela o hash");
    }

    @Test
    @DisplayName("Tamanho de página fora do limite é recusado com 400, antes de consultar o banco")
    void deveRecusarTamanhoDePaginaForaDoLimite() {
        ResponseEntity<JsonNode> resposta = listar(USERS + "?size=101");

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("urn:restaurantes:problema:requisicao-invalida", resposta.getBody().get("type").asText());
    }

    private ResponseEntity<JsonNode> listar(String url) {
        return rest.exchange(url, HttpMethod.GET, autenticado(admin), JsonNode.class);
    }

    /** O primeiro nome de cada item, na ordem da página — a marca é igual em todos. */
    private static List<String> primeirosNomes(JsonNode pagina) {
        List<String> nomes = new ArrayList<>();
        JsonNode embutidos = pagina.path("_embedded").path("userResponseList");
        embutidos.forEach(item -> nomes.add(item.get("name").asText().split(" ")[0]));
        return nomes;
    }

    private void cadastrar(String nome) {
        String login = "busca" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(Map.of(
                "name", nome,
                "email", login + "@email.com",
                "login", login,
                "password", "senhaSegura123",
                "roles", List.of("ROLE_CUSTOMER"),
                "addresses", List.of())), JsonNode.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
    }
}
