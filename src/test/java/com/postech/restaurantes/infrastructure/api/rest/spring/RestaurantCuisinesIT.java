package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * O restaurante escolhe seus tipos de cozinha entre os do catálogo: cadastro, troca, remoção, recusa de tipo que não
 * existe, e a regra de que tipo em uso não sai do catálogo.
 */
class RestaurantCuisinesIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String RESTAURANTS = "/api/v1/restaurants";
    private static final String CUISINES = "/api/v1/cuisines";
    private static final String URN = "urn:restaurantes:problema:";

    @Test
    @DisplayName("O dono cadastra o restaurante com tipos de cozinha, que voltam em ordem alfabética na consulta pública")
    void deveCadastrarComTiposDeCozinha() {
        String admin = autenticar("admin.demo", "admin12345");
        UUID italiana = criarCozinha(admin, "Italiana");
        UUID arabe = criarCozinha(admin, "Árabe");
        Dono dono = novoDono();

        ResponseEntity<JsonNode> criacao = cadastrar(dono, List.of(italiana, arabe));

        assertEquals(HttpStatus.CREATED, criacao.getStatusCode());
        assertEquals(List.of("Árabe", "Italiana"), nomes(criacao.getBody()), "alfabética em português: Á antes de I");
        ResponseEntity<JsonNode> consulta = rest.getForEntity(criacao.getHeaders().getLocation(), JsonNode.class);
        assertEquals(List.of("Árabe", "Italiana"), nomes(consulta.getBody()));
        assertEquals(italiana.toString(), consulta.getBody().at("/cuisines/1/id").asText());
    }

    @Test
    @DisplayName("Sem tipos de cozinha no cadastro, o restaurante nasce sem nenhum")
    void deveCadastrarSemTiposDeCozinha() {
        ResponseEntity<JsonNode> criacao = cadastrar(novoDono(), null);

        assertEquals(HttpStatus.CREATED, criacao.getStatusCode());
        assertTrue(criacao.getBody().get("cuisines").isArray());
        assertEquals(0, criacao.getBody().get("cuisines").size());
    }

    @Test
    @DisplayName("Na atualização, sem cuisineIds os tipos continuam; com ids, a lista é trocada; lista vazia tira todos")
    void deveAtualizarOsTiposDeCozinha() {
        String admin = autenticar("admin.demo", "admin12345");
        UUID italiana = criarCozinha(admin, "Italiana");
        UUID japonesa = criarCozinha(admin, "Japonesa");
        Dono dono = novoDono();
        URI restaurante = cadastrar(dono, List.of(italiana)).getHeaders().getLocation();

        ResponseEntity<JsonNode> semIds = atualizar(restaurante, dono, null);
        assertEquals(List.of("Italiana"), nomes(semIds.getBody()), "ausente mantém");

        ResponseEntity<JsonNode> troca = atualizar(restaurante, dono, List.of(japonesa));
        assertEquals(HttpStatus.OK, troca.getStatusCode());
        assertEquals(List.of("Japonesa"), nomes(troca.getBody()), "com ids troca a lista inteira");

        ResponseEntity<JsonNode> ambas = atualizar(restaurante, dono, List.of(italiana, japonesa, italiana));
        assertEquals(List.of("Italiana", "Japonesa"), nomes(ambas.getBody()), "o id repetido conta uma vez");

        ResponseEntity<JsonNode> vazia = atualizar(restaurante, dono, List.of());
        assertEquals(0, vazia.getBody().get("cuisines").size(), "lista vazia tira todos");
        assertEquals(0, rest.getForEntity(restaurante, JsonNode.class).getBody().get("cuisines").size());
    }

    @Test
    @DisplayName("Tipo de cozinha que não está no catálogo é 404, no cadastro e na atualização, e nada é gravado")
    void deveRecusarTipoDeCozinhaInexistente() {
        Dono dono = novoDono();
        UUID desconhecido = UUID.randomUUID();

        ResponseEntity<JsonNode> cadastro = cadastrar(dono, List.of(desconhecido));
        assertEquals(HttpStatus.NOT_FOUND, cadastro.getStatusCode());
        assertEquals(URN + "recurso-nao-encontrado", cadastro.getBody().get("type").asText());
        ResponseEntity<JsonNode> listagem = rest.getForEntity(RESTAURANTS + "?ownerId=" + dono.id(), JsonNode.class);
        assertEquals(0, listagem.getBody().at("/page/totalElements").asInt(), "o cadastro recusado não gravou nada");

        URI restaurante = cadastrar(dono, null).getHeaders().getLocation();
        ResponseEntity<JsonNode> atualizacao = atualizar(restaurante, dono, List.of(desconhecido));
        assertEquals(HttpStatus.NOT_FOUND, atualizacao.getStatusCode());
        assertEquals(0, rest.getForEntity(restaurante, JsonNode.class).getBody().get("cuisines").size());
    }

    @Test
    @DisplayName("Tipo de cozinha em uso por restaurante não sai do catálogo (409); sem restaurante, sai; o tipo sobrevive à exclusão do restaurante")
    void deveProtegerTipoDeCozinhaEmUso() {
        String admin = autenticar("admin.demo", "admin12345");
        UUID italiana = criarCozinha(admin, "Italiana");
        Dono dono = novoDono();
        URI restaurante = cadastrar(dono, List.of(italiana)).getHeaders().getLocation();
        URI tipo = URI.create(CUISINES + "/" + italiana);

        ResponseEntity<JsonNode> emUso = rest.exchange(tipo, HttpMethod.DELETE, autenticado(admin), JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, emUso.getStatusCode());
        assertEquals(URN + "recurso-em-uso", emUso.getBody().get("type").asText());

        assertEquals(HttpStatus.NO_CONTENT,
                rest.exchange(restaurante, HttpMethod.DELETE, autenticado(dono.token()), Void.class).getStatusCode());
        assertEquals(HttpStatus.OK, rest.getForEntity(tipo, JsonNode.class).getStatusCode(),
                "excluir o restaurante não exclui o tipo do catálogo");

        assertEquals(HttpStatus.NO_CONTENT,
                rest.exchange(tipo, HttpMethod.DELETE, autenticado(admin), Void.class).getStatusCode());
    }

    @Test
    @DisplayName("Tipo de cozinha que sai da lista do restaurante deixa de estar em uso e pode ser excluído")
    void deveLiberarTipoQueSaiuDoRestaurante() {
        String admin = autenticar("admin.demo", "admin12345");
        UUID italiana = criarCozinha(admin, "Italiana");
        Dono dono = novoDono();
        URI restaurante = cadastrar(dono, List.of(italiana)).getHeaders().getLocation();

        atualizar(restaurante, dono, List.of());

        assertEquals(HttpStatus.NO_CONTENT, rest.exchange(CUISINES + "/" + italiana, HttpMethod.DELETE,
                autenticado(admin), Void.class).getStatusCode());
        rest.exchange(restaurante, HttpMethod.DELETE, autenticado(dono.token()), Void.class);
    }

    @Test
    @DisplayName("A listagem pública filtra pelo tipo de cozinha, sozinho ou junto com o dono, e a navegação repete o filtro")
    void deveFiltrarAListagemPeloTipoDeCozinha() {
        String admin = autenticar("admin.demo", "admin12345");
        UUID italiana = criarCozinha(admin, "Italiana");
        UUID japonesa = criarCozinha(admin, "Japonesa");
        Dono dono = novoDono();
        URI soItaliana = cadastrar(dono, List.of(italiana)).getHeaders().getLocation();
        URI ambas = cadastrar(dono, List.of(italiana, japonesa)).getHeaders().getLocation();
        URI deOutroDono = cadastrar(novoDono(), List.of(japonesa)).getHeaders().getLocation();

        JsonNode porItaliana = rest.getForEntity(RESTAURANTS + "?cuisineId=" + italiana, JsonNode.class).getBody();
        assertEquals(List.of(id(soItaliana), id(ambas)).stream().sorted().toList(), ids(porItaliana));
        assertTrue(porItaliana.at("/_links/self/href").asText().contains("cuisineId=" + italiana));

        JsonNode porJaponesa = rest.getForEntity(RESTAURANTS + "?cuisineId=" + japonesa, JsonNode.class).getBody();
        assertEquals(List.of(id(ambas), id(deOutroDono)).stream().sorted().toList(), ids(porJaponesa));

        JsonNode doDono = rest.getForEntity(RESTAURANTS + "?cuisineId=" + japonesa + "&ownerId=" + dono.id(),
                JsonNode.class).getBody();
        assertEquals(List.of(id(ambas)), ids(doDono), "os dois filtros se combinam");

        JsonNode semRestaurante = rest.getForEntity(RESTAURANTS + "?cuisineId=" + UUID.randomUUID(), JsonNode.class)
                .getBody();
        assertEquals(0, semRestaurante.at("/page/totalElements").asInt());
    }

    private static String id(URI restaurante) {
        String caminho = restaurante.getPath();
        return caminho.substring(caminho.lastIndexOf('/') + 1);
    }

    private static List<String> ids(JsonNode pagina) {
        List<String> ids = new ArrayList<>();
        pagina.path("_embedded").path("restaurantResponseList").forEach(r -> ids.add(r.get("id").asText()));
        return ids.stream().sorted().toList();
    }

    private UUID criarCozinha(String admin, String prefixo) {
        // O banco é compartilhado e o nome é único: marca própria depois do prefixo, que define a ordem.
        String nome = prefixo + " " + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ResponseEntity<JsonNode> resposta = rest.exchange(CUISINES, HttpMethod.POST,
                corpoAutenticado(Map.of("name", nome), admin), JsonNode.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        return UUID.fromString(resposta.getBody().get("id").asText());
    }

    private Dono novoDono() {
        String login = "dono" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        ResponseEntity<JsonNode> cadastro = rest.postForEntity(USERS, corpo(Map.of(
                "name", "Dono Cozinhas", "email", login + "@email.com", "login", login,
                "password", "senhaSegura123", "owner", perfilDeDono())), JsonNode.class);
        assertEquals(HttpStatus.CREATED, cadastro.getStatusCode());
        return new Dono(UUID.fromString(cadastro.getBody().get("id").asText()), autenticar(login, "senhaSegura123"));
    }

    private ResponseEntity<JsonNode> cadastrar(Dono dono, List<UUID> cuisineIds) {
        Map<String, Object> corpo = new HashMap<>(restaurante("Restaurante Cozinhas IT"));
        corpo.put("userId", dono.id());
        if (cuisineIds != null) {
            corpo.put("cuisineIds", cuisineIds);
        }
        return rest.exchange(RESTAURANTS, HttpMethod.POST, corpoAutenticado(corpo, dono.token()), JsonNode.class);
    }

    private ResponseEntity<JsonNode> atualizar(URI restaurante, Dono dono, List<UUID> cuisineIds) {
        Map<String, Object> corpo = new HashMap<>(restaurante("Restaurante Cozinhas IT"));
        if (cuisineIds != null) {
            corpo.put("cuisineIds", cuisineIds);
        }
        return rest.exchange(restaurante, HttpMethod.PUT, corpoAutenticado(corpo, dono.token()), JsonNode.class);
    }

    private static Map<String, Object> restaurante(String nome) {
        return Map.of(
                "address", Map.of("street", "Rua da Cozinha", "number", "10", "neighborhood", "Bairro",
                        "cityId", "c0000000-0000-4000-8000-000000000001", "zipCode", "01000-000"),
                "name", nome,
                "officeHours", List.of(Map.of("dayOfWeek", "MONDAY", "startTime", "08:00:00", "endTime", "22:00:00")));
    }

    private static List<String> nomes(JsonNode restaurante) {
        List<String> nomes = new ArrayList<>();
        restaurante.get("cuisines").forEach(cozinha -> nomes.add(semMarca(cozinha.get("name").asText())));
        return nomes;
    }

    /** Tira a marca única que o teste pôs no nome, para comparar só o que o catálogo diz. */
    private static String semMarca(String nome) {
        return nome.substring(0, nome.lastIndexOf(' '));
    }

    private record Dono(UUID id, String token) {
    }
}
