package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.Documentos;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Perfis em cadastro existente (Etapa 22), por HTTP de verdade: incluir, alterar e remover perfil,
 * a troca de status do entregador, e as regras de posse e de administrador. Cada teste cria os
 * próprios usuários, com documentos gerados: o banco é compartilhado.
 */
class UserProfilesApiIT extends WebIntegrationTestSupport {

    private static final String USERS = "/api/v1/users";
    private static final String URN = "urn:restaurantes:problema:";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Cliente inclui o perfil de dono; o papel novo vale na hora, com o mesmo token")
    void deveIncluirPerfilDeDonoQueValeNaHora() {
        Usuario cliente = cadastrar(Map.of("client", perfilDeCliente()));

        ResponseEntity<JsonNode> resposta = rest.exchange(perfil(cliente, "owner"), HttpMethod.PUT,
                corpoAutenticado(perfilDeDono(), cliente.token()), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(List.of("ROLE_OWNER", "ROLE_CLIENT"), papeis(resposta.getBody()));
        assertEquals(14, resposta.getBody().at("/owner/cnpj").asText().length());
        assertEquals(HttpStatus.CREATED, criarRestaurante(cliente, cliente.token()),
                "os papéis vêm do cadastro a cada requisição, não do token do login");
    }

    @Test
    @DisplayName("Entregador: inclusão começa OFFLINE, o status muda pelo PATCH, e alterar o veículo mantém o status")
    void devePercorrerOPerfilDeEntregador() {
        String cpf = Documentos.cpf();
        Usuario cliente = cadastrar(Map.of("client", Map.of("cpf", cpf, "phone", "11912345678")));

        JsonNode incluido = rest.exchange(perfil(cliente, "courier"), HttpMethod.PUT, corpoAutenticado(
                entregador(cpf, "BICYCLE", null, null), cliente.token()), JsonNode.class).getBody();
        JsonNode disponivel = rest.exchange(perfil(cliente, "courier") + "/status", HttpMethod.PATCH,
                corpoAutenticado(Map.of("status", "available"), cliente.token()), JsonNode.class).getBody();
        JsonNode deCarro = rest.exchange(perfil(cliente, "courier"), HttpMethod.PUT, corpoAutenticado(
                entregador(cpf, "CAR", Documentos.cnh(), "XYZ9A87"), cliente.token()), JsonNode.class).getBody();

        assertEquals("OFFLINE", incluido.at("/courier/status").asText());
        assertEquals("AVAILABLE", disponivel.at("/courier/status").asText());
        assertEquals("CAR", deCarro.at("/courier/vehicleType").asText());
        assertEquals("AVAILABLE", deCarro.at("/courier/status").asText(), "alterar o veículo não muda o status");
        assertEquals("AVAILABLE", jdbc.queryForObject("SELECT status::text FROM couriers WHERE id = ?",
                String.class, cliente.id()));
    }

    @Test
    @DisplayName("Remover perfil: sai o pedido; o último não sai; o que não existe é 404; tipo desconhecido é 400")
    void deveRemoverPerfilRespeitandoAsRegras() {
        Usuario usuario = cadastrar(Map.of("client", perfilDeCliente(), "owner", perfilDeDono()));

        ResponseEntity<Void> removido = rest.exchange(perfil(usuario, "client"), HttpMethod.DELETE,
                autenticado(usuario.token()), Void.class);
        ResponseEntity<JsonNode> ultimo = rest.exchange(perfil(usuario, "owner"), HttpMethod.DELETE,
                autenticado(usuario.token()), JsonNode.class);
        ResponseEntity<JsonNode> inexistente = rest.exchange(perfil(usuario, "courier"), HttpMethod.DELETE,
                autenticado(usuario.token()), JsonNode.class);
        ResponseEntity<JsonNode> desconhecido = rest.exchange(perfil(usuario, "gerente"), HttpMethod.DELETE,
                autenticado(usuario.token()), JsonNode.class);

        assertEquals(HttpStatus.NO_CONTENT, removido.getStatusCode());
        assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM clients WHERE id = ?", Integer.class,
                usuario.id()));
        problema(ultimo, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Usuário deve ter ao menos um perfil");
        problema(inexistente, HttpStatus.NOT_FOUND, "recurso-nao-encontrado", "O usuário não tem perfil de entregador");
        problema(desconhecido, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Tipo de perfil inválido: gerente");
    }

    @Test
    @DisplayName("O perfil de dono não sai enquanto houver restaurante; sem o restaurante, sai")
    void naoDeveRemoverDonoComRestaurante() {
        Usuario dono = cadastrar(Map.of("owner", perfilDeDono(), "client", perfilDeCliente()));
        JsonNode restaurante = rest.exchange("/api/v1/restaurants", HttpMethod.POST,
                corpoAutenticado(restaurante(dono), dono.token()), JsonNode.class).getBody();

        ResponseEntity<JsonNode> recusado = rest.exchange(perfil(dono, "owner"), HttpMethod.DELETE,
                autenticado(dono.token()), JsonNode.class);
        rest.exchange("/api/v1/restaurants/" + restaurante.get("id").asText(), HttpMethod.DELETE,
                autenticado(dono.token()), Void.class);
        ResponseEntity<Void> aceito = rest.exchange(perfil(dono, "owner"), HttpMethod.DELETE,
                autenticado(dono.token()), Void.class);

        problema(recusado, HttpStatus.CONFLICT, "recurso-em-uso",
                "O perfil de dono não pode ser removido enquanto o usuário tiver restaurantes");
        assertEquals(HttpStatus.NO_CONTENT, aceito.getStatusCode());
    }

    @Test
    @DisplayName("Perfil de administrador: o próprio usuário não se torna admin (403); um administrador inclui")
    void deveIncluirAdministradorSoPorAdministrador() {
        Usuario cliente = cadastrar(Map.of("client", perfilDeCliente()));
        Map<String, Object> admin = Map.of("employeeCode", "ADM-" + UUID.randomUUID().toString().substring(0, 8),
                "department", "Suporte");

        ResponseEntity<JsonNode> peloProprio = rest.exchange(perfil(cliente, "admin"), HttpMethod.PUT,
                corpoAutenticado(admin, cliente.token()), JsonNode.class);
        ResponseEntity<JsonNode> peloAdmin = rest.exchange(perfil(cliente, "admin"), HttpMethod.PUT,
                corpoAutenticado(admin, admin()), JsonNode.class);
        ResponseEntity<JsonNode> codigoRepetido = rest.exchange(perfil(cadastrar(Map.of("client", perfilDeCliente())),
                "admin"), HttpMethod.PUT, corpoAutenticado(admin, admin()), JsonNode.class);

        problema(peloProprio, HttpStatus.FORBIDDEN, "acesso-negado", null);
        assertEquals(HttpStatus.OK, peloAdmin.getStatusCode());
        assertEquals(List.of("ROLE_CLIENT", "ROLE_ADMIN"), papeis(peloAdmin.getBody()));
        assertTrue(peloAdmin.getBody().at("/admin/superAdmin").isBoolean());
        problema(codigoRepetido, HttpStatus.CONFLICT, "conflito-de-dados", null);
    }

    @Test
    @DisplayName("Perfil de administrador removido deixa de autorizar na hora, mesmo com o token emitido antes")
    void deveRevogarOAdministradorNaHora() {
        Usuario usuario = cadastrar(Map.of("client", perfilDeCliente()));
        rest.exchange(perfil(usuario, "admin"), HttpMethod.PUT, corpoAutenticado(Map.of("employeeCode",
                "ADM-" + UUID.randomUUID().toString().substring(0, 8)), admin()), JsonNode.class);

        HttpStatus comoAdmin = listar(usuario.token());
        rest.exchange(perfil(usuario, "admin"), HttpMethod.DELETE, autenticado(admin()), Void.class);
        HttpStatus depoisDeRemovido = listar(usuario.token());

        assertEquals(HttpStatus.OK, comoAdmin, "o perfil incluído já vale com o token antigo");
        assertEquals(HttpStatus.FORBIDDEN, depoisDeRemovido, "o perfil removido já não vale com o mesmo token");
    }

    @Test
    @DisplayName("Posse: perfil de outro cadastro é 403, sem token é 401; administrador altera o de qualquer um")
    void deveExigirPosse() {
        Usuario eu = cadastrar(Map.of("client", perfilDeCliente()));
        Usuario outro = cadastrar(Map.of("client", perfilDeCliente()));

        ResponseEntity<JsonNode> alheio = rest.exchange(perfil(outro, "owner"), HttpMethod.PUT,
                corpoAutenticado(perfilDeDono(), eu.token()), JsonNode.class);
        ResponseEntity<JsonNode> semToken = rest.exchange(perfil(outro, "owner"), HttpMethod.PUT,
                corpo(perfilDeDono()), JsonNode.class);
        ResponseEntity<JsonNode> peloAdmin = rest.exchange(perfil(outro, "owner"), HttpMethod.PUT,
                corpoAutenticado(perfilDeDono(), admin()), JsonNode.class);

        problema(alheio, HttpStatus.FORBIDDEN, "acesso-negado", null);
        problema(semToken, HttpStatus.UNAUTHORIZED, "nao-autenticado", null);
        assertEquals(HttpStatus.OK, peloAdmin.getStatusCode());
    }

    @Test
    @DisplayName("CPF de outro cadastro é 409; o próprio CPF, ao alterar o telefone, não é conflito")
    void deveConferirOCpfContraOsOutrosCadastros() {
        Map<String, Object> clienteDoOutro = perfilDeCliente();
        cadastrar(Map.of("client", clienteDoOutro));
        String meuCpf = Documentos.cpf();
        Usuario eu = cadastrar(Map.of("client", Map.of("cpf", meuCpf, "phone", "11912345678")));

        ResponseEntity<JsonNode> doOutro = rest.exchange(perfil(eu, "client"), HttpMethod.PUT,
                corpoAutenticado(clienteDoOutro, eu.token()), JsonNode.class);
        ResponseEntity<JsonNode> oMeu = rest.exchange(perfil(eu, "client"), HttpMethod.PUT,
                corpoAutenticado(Map.of("cpf", meuCpf, "phone", "1133334444"), eu.token()), JsonNode.class);

        problema(doOutro, HttpStatus.CONFLICT, "conflito-de-dados", "CPF já cadastrado");
        assertEquals(HttpStatus.OK, oMeu.getStatusCode());
        assertEquals("1133334444", oMeu.getBody().at("/client/phone").asText());
    }

    @Test
    @DisplayName("Status: quem não é entregador recebe 404; status desconhecido é 400")
    void deveRecusarStatusInvalidoOuDeQuemNaoEEntregador() {
        Usuario cliente = cadastrar(Map.of("client", perfilDeCliente()));

        ResponseEntity<JsonNode> naoEntregador = rest.exchange(perfil(cliente, "courier") + "/status",
                HttpMethod.PATCH, corpoAutenticado(Map.of("status", "BUSY"), cliente.token()), JsonNode.class);
        ResponseEntity<JsonNode> desconhecido = rest.exchange(perfil(cliente, "courier") + "/status",
                HttpMethod.PATCH, corpoAutenticado(Map.of("status", "DORMINDO"), cliente.token()), JsonNode.class);

        problema(naoEntregador, HttpStatus.NOT_FOUND, "recurso-nao-encontrado", "O usuário não tem perfil de entregador");
        problema(desconhecido, HttpStatus.BAD_REQUEST, "requisicao-invalida", "Status do entregador inválido: DORMINDO");
    }

    private Usuario cadastrar(Map<String, Object> perfis) {
        String login = "perfil." + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> corpo = new HashMap<>(perfis);
        corpo.putAll(Map.of("name", "Usuário " + login, "email", login + "@email.com", "login", login,
                "password", "senhaSegura123"));
        ResponseEntity<JsonNode> criado = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);
        assertEquals(HttpStatus.CREATED, criado.getStatusCode(), String.valueOf(criado.getBody()));
        return new Usuario(UUID.fromString(criado.getBody().get("id").asText()), login,
                autenticar(login, "senhaSegura123"));
    }

    private HttpStatus listar(String token) {
        return HttpStatus.valueOf(rest.exchange(USERS, HttpMethod.GET, autenticado(token), JsonNode.class)
                .getStatusCode().value());
    }

    private HttpStatus criarRestaurante(Usuario dono, String token) {
        return HttpStatus.valueOf(rest.exchange("/api/v1/restaurants", HttpMethod.POST,
                corpoAutenticado(restaurante(dono), token), JsonNode.class).getStatusCode().value());
    }

    private static Map<String, Object> restaurante(Usuario dono) {
        return Map.of("userId", dono.id(), "name", "Restaurante " + dono.login(), "officeHourStart", "08:00:00",
                "officeHourEnd", "22:00:00", "address", Map.of("street", "Rua A", "number", "1",
                        "neighborhood", "Centro", "city", "São Paulo", "state", "SP", "zipCode", "01001000"));
    }

    private static Map<String, Object> entregador(String cpf, String veiculo, String cnh, String placa) {
        Map<String, Object> corpo = new HashMap<>(Map.of("cpf", cpf, "phone", "11912345678",
                "vehicleType", veiculo));
        if (cnh != null) {
            corpo.put("driverLicense", cnh);
            corpo.put("vehiclePlate", placa);
        }
        return corpo;
    }

    private String admin() {
        return autenticar("admin.demo", "admin12345");
    }

    private static String perfil(Usuario usuario, String tipo) {
        return USERS + "/" + usuario.id() + "/profiles/" + tipo;
    }

    private static List<String> papeis(JsonNode usuario) {
        return StreamSupport.stream(usuario.get("roles").spliterator(), false)
                .map(JsonNode::asText).toList();
    }

    /** Status, categoria e, quando informado, a mensagem do ProblemDetail. */
    private static void problema(ResponseEntity<JsonNode> resposta, HttpStatus status, String categoria,
                                 String detalhe) {
        assertEquals(status, resposta.getStatusCode(), String.valueOf(resposta.getBody()));
        assertEquals(URN + categoria, resposta.getBody().get("type").asText());
        if (detalhe != null) {
            assertEquals(detalhe, resposta.getBody().get("detail").asText());
        }
    }

    private record Usuario(UUID id, String login, String token) {
    }
}
