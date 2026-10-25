package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * O catálogo de tipos de cozinha por HTTP de verdade, com banco real: consulta pública, manutenção só do
 * administrador e nome único sem diferenciar maiúsculas. O banco é compartilhado entre os testes, então cada
 * teste cria nomes com marca própria.
 */
class CuisineLifecycleIT extends WebIntegrationTestSupport {

    private static final String CUISINES = "/api/v1/cuisines";
    private static final String URN = "urn:restaurantes:problema:";

    @Test
    @DisplayName("O administrador cria, altera e exclui; qualquer um consulta e lista sem login")
    void devePercorrerCicloDeVida() {
        String admin = autenticar("admin.demo", "admin12345");
        String nome = nomeNovo();

        ResponseEntity<JsonNode> criacao = rest.exchange(CUISINES, HttpMethod.POST,
                corpoAutenticado(Map.of("name", "  " + nome + "  "), admin), JsonNode.class);
        assertEquals(HttpStatus.CREATED, criacao.getStatusCode());
        URI location = criacao.getHeaders().getLocation();
        assertNotNull(location);
        assertEquals(nome, criacao.getBody().get("name").asText(), "o nome é gravado sem os espaços das pontas");
        assertNotNull(criacao.getBody().get("createdAt").asText(), "a auditoria é preenchida ao gravar");
        assertTrue(criacao.getBody().get("_links").has("self"));

        ResponseEntity<JsonNode> consulta = rest.getForEntity(location, JsonNode.class);
        assertEquals(HttpStatus.OK, consulta.getStatusCode());
        assertEquals(nome, consulta.getBody().get("name").asText());

        ResponseEntity<JsonNode> busca = rest.getForEntity(CUISINES + "?name=" + nome.toLowerCase(), JsonNode.class);
        assertEquals(HttpStatus.OK, busca.getStatusCode());
        assertEquals(1, busca.getBody().at("/page/totalElements").asInt());
        assertEquals(nome, busca.getBody().at("/_embedded/cuisineResponseList/0/name").asText());

        String novoNome = nomeNovo();
        ResponseEntity<JsonNode> alteracao = rest.exchange(location, HttpMethod.PUT,
                corpoAutenticado(Map.of("name", novoNome), admin), JsonNode.class);
        assertEquals(HttpStatus.OK, alteracao.getStatusCode());
        assertEquals(novoNome, alteracao.getBody().get("name").asText());

        assertEquals(HttpStatus.NO_CONTENT,
                rest.exchange(location, HttpMethod.DELETE, autenticado(admin), Void.class).getStatusCode());
        ResponseEntity<JsonNode> aposExclusao = rest.getForEntity(location, JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, aposExclusao.getStatusCode());
        assertEquals(URN + "recurso-nao-encontrado", aposExclusao.getBody().get("type").asText());
    }

    @Test
    @DisplayName("Quem não é administrador não mantém o catálogo: sem login é 401; dono e cliente são 403")
    void deveReservarAManutencaoAoAdministrador() {
        String admin = autenticar("admin.demo", "admin12345");
        URI location = rest.exchange(CUISINES, HttpMethod.POST, corpoAutenticado(Map.of("name", nomeNovo()), admin),
                JsonNode.class).getHeaders().getLocation();
        String dono = autenticar("dono.restaurante", "dono12345");
        String cliente = autenticar("cliente.demo", "cliente12345");

        ResponseEntity<JsonNode> semLogin =
                rest.exchange(CUISINES, HttpMethod.POST, corpo(Map.of("name", nomeNovo())), JsonNode.class);
        assertEquals(HttpStatus.UNAUTHORIZED, semLogin.getStatusCode());
        assertEquals(URN + "nao-autenticado", semLogin.getBody().get("type").asText());

        for (String token : new String[] {dono, cliente}) {
            ResponseEntity<JsonNode> criacao = rest.exchange(CUISINES, HttpMethod.POST,
                    corpoAutenticado(Map.of("name", nomeNovo()), token), JsonNode.class);
            assertEquals(HttpStatus.FORBIDDEN, criacao.getStatusCode());
            assertEquals(URN + "acesso-negado", criacao.getBody().get("type").asText());
            assertEquals(HttpStatus.FORBIDDEN, rest.exchange(location, HttpMethod.PUT,
                    corpoAutenticado(Map.of("name", nomeNovo()), token), JsonNode.class).getStatusCode());
            assertEquals(HttpStatus.FORBIDDEN,
                    rest.exchange(location, HttpMethod.DELETE, autenticado(token), JsonNode.class).getStatusCode());
        }

        assertEquals(HttpStatus.OK, rest.getForEntity(location, JsonNode.class).getStatusCode(),
                "as recusas não mexeram no tipo");
        rest.exchange(location, HttpMethod.DELETE, autenticado(admin), Void.class);
    }

    @Test
    @DisplayName("Nome repetido, mesmo mudando as maiúsculas, é 409; manter o próprio nome não é")
    void deveRecusarNomeRepetido() {
        String admin = autenticar("admin.demo", "admin12345");
        String nome = nomeNovo();
        URI primeiro = rest.exchange(CUISINES, HttpMethod.POST, corpoAutenticado(Map.of("name", nome), admin),
                JsonNode.class).getHeaders().getLocation();
        URI segundo = rest.exchange(CUISINES, HttpMethod.POST, corpoAutenticado(Map.of("name", nomeNovo()), admin),
                JsonNode.class).getHeaders().getLocation();

        ResponseEntity<JsonNode> criacaoRepetida = rest.exchange(CUISINES, HttpMethod.POST,
                corpoAutenticado(Map.of("name", nome.toUpperCase()), admin), JsonNode.class);
        assertEquals(HttpStatus.CONFLICT, criacaoRepetida.getStatusCode());
        assertEquals(URN + "conflito-de-dados", criacaoRepetida.getBody().get("type").asText());

        assertEquals(HttpStatus.CONFLICT, rest.exchange(segundo, HttpMethod.PUT,
                corpoAutenticado(Map.of("name", nome), admin), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.OK, rest.exchange(primeiro, HttpMethod.PUT,
                corpoAutenticado(Map.of("name", nome.toUpperCase()), admin), JsonNode.class).getStatusCode());

        rest.exchange(primeiro, HttpMethod.DELETE, autenticado(admin), Void.class);
        rest.exchange(segundo, HttpMethod.DELETE, autenticado(admin), Void.class);
    }

    @Test
    @DisplayName("Nome em branco ou maior que 100 caracteres é 400 e aponta o campo; id que não é UUID é 400")
    void deveRecusarEntradaInvalida() {
        String admin = autenticar("admin.demo", "admin12345");

        ResponseEntity<JsonNode> emBranco = rest.exchange(CUISINES, HttpMethod.POST,
                corpoAutenticado(Map.of("name", "   "), admin), JsonNode.class);
        assertEquals(HttpStatus.BAD_REQUEST, emBranco.getStatusCode());
        assertTrue(emBranco.getBody().get("errors").has("name"));

        assertEquals(HttpStatus.BAD_REQUEST, rest.exchange(CUISINES, HttpMethod.POST,
                corpoAutenticado(Map.of("name", "x".repeat(101)), admin), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                rest.getForEntity(CUISINES + "/nao-e-uuid", JsonNode.class).getStatusCode());
    }

    @Test
    @DisplayName("Alterar ou excluir tipo inexistente é 404")
    void deveRecusarTipoInexistente() {
        String admin = autenticar("admin.demo", "admin12345");
        String inexistente = CUISINES + "/" + UUID.randomUUID();

        assertEquals(HttpStatus.NOT_FOUND, rest.exchange(inexistente, HttpMethod.PUT,
                corpoAutenticado(Map.of("name", nomeNovo()), admin), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                rest.exchange(inexistente, HttpMethod.DELETE, autenticado(admin), JsonNode.class).getStatusCode());
    }

    private static String nomeNovo() {
        return "Cozinha" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
