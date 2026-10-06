package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.Documentos;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A API de usuários por HTTP de verdade, com a cadeia de segurança inteira no caminho.
 */
class UserApiIT extends WebIntegrationTestSupport {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();
    private static final String USERS = "/api/v1/users";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Cadastro é público, responde 201 com Location e links, e nunca devolve a senha")
    void deveCadastrarSemAutenticacao() {
        String login = "api" + SEQUENCIA.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(novoUsuario(login)), JsonNode.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        JsonNode corpo = resposta.getBody();
        assertNotNull(corpo.get("id").asText());
        assertEquals(login, corpo.get("login").asText());
        assertEquals("ROLE_CLIENT", corpo.get("roles").get(0).asText());
        assertEquals(11, corpo.at("/client/cpf").asText().length(), "CPF sai sem máscara");
        assertTrue(corpo.get("owner").isNull());
        assertEquals("01001000", corpo.at("/addresses/0/address/zipCode").asText(), "CEP sai normalizado");
        assertEquals("Casa", corpo.at("/addresses/0/label").asText());
        assertTrue(corpo.at("/addresses/0/isDefault").asBoolean(), "o único endereço é o padrão");
        assertTrue(corpo.has("_links"));
        assertTrue(resposta.getHeaders().getLocation().toString().endsWith(USERS + "/" + corpo.get("id").asText()));
        assertFalse(corpo.has("password"), "a resposta não tem campo de senha");
        assertFalse(corpo.toString().contains("$2a$"), "nenhum hash vaza no corpo");
    }

    @Test
    @DisplayName("Sem endereço marcado como padrão, o primeiro passa a ser; marcar dois é recusado com 400")
    void deveDefinirOEnderecoPadrao() {
        Map<String, Object> semPadrao = new HashMap<>(novoUsuario(novoLogin()));
        semPadrao.put("addresses", List.of(Map.of("address", endereco("Rua 1")), Map.of("address", endereco("Rua 2"))));
        Map<String, Object> doisPadroes = new HashMap<>(novoUsuario(novoLogin()));
        doisPadroes.put("addresses", List.of(Map.of("isDefault", true, "address", endereco("Rua 1")),
                Map.of("isDefault", true, "address", endereco("Rua 2"))));

        JsonNode criado = rest.postForEntity(USERS, corpo(semPadrao), JsonNode.class).getBody();
        ResponseEntity<JsonNode> recusado = rest.postForEntity(USERS, corpo(doisPadroes), JsonNode.class);

        assertTrue(criado.at("/addresses/0/isDefault").asBoolean());
        assertFalse(criado.at("/addresses/1/isDefault").asBoolean());
        assertEquals(HttpStatus.BAD_REQUEST, recusado.getStatusCode());
        assertEquals("Exatamente um endereço deve ser o padrão", recusado.getBody().get("detail").asText());
    }

    @Test
    @DisplayName("Consulta sem token é recusada com 401")
    void deveRecusarSemToken() {
        assertEquals(HttpStatus.UNAUTHORIZED,
                rest.getForEntity(USERS + "/" + UUID.randomUUID(), JsonNode.class).getStatusCode());
    }

    @Test
    @DisplayName("Token malformado é tratado como ausente (expirado e forjado: JwtAuthenticationIT)")
    void deveRecusarTokenMalformado() {
        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + UUID.randomUUID(), HttpMethod.GET,
                autenticado("token.que.nao.vale"), JsonNode.class);

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
    }

    @Test
    @DisplayName("Usuário autenticado lê o próprio cadastro")
    void devePermitirLerASiMesmo() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + eu.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(eu.login(), resposta.getBody().get("login").asText());
    }

    @Test
    @DisplayName("Conhecer o id de outro usuário não dá acesso a ele: 403")
    void deveRecusarAcessoAoCadastroAlheio() {
        Usuario eu = cadastrarEAutenticar();
        Usuario outro = cadastrarEAutenticar();

        ResponseEntity<JsonNode> leitura = rest.exchange(USERS + "/" + outro.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);
        ResponseEntity<JsonNode> exclusao = rest.exchange(USERS + "/" + outro.id(), HttpMethod.DELETE,
                autenticado(eu.token()), JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, leitura.getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, exclusao.getStatusCode());
    }

    @Test
    @DisplayName("Administrador lê o cadastro de qualquer usuário")
    void devePermitirAoAdministrador() {
        Usuario outro = cadastrarEAutenticar();
        String admin = autenticar("admin.demo", "admin12345");

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + outro.id(), HttpMethod.GET,
                autenticado(admin), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(outro.login(), resposta.getBody().get("login").asText());
    }

    @Test
    @DisplayName("Listagem é do administrador e devolve página com metadados e links")
    void deveListarPaginadoParaOAdministrador() {
        String admin = autenticar("admin.demo", "admin12345");

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "?page=0&size=5&sort=name,asc", HttpMethod.GET,
                autenticado(admin), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals(5, resposta.getBody().get("page").get("size").asInt());
        assertTrue(resposta.getBody().get("page").get("totalElements").asInt() >= 1);
        assertTrue(resposta.getBody().has("_links"));
    }

    /**
     * A listagem devolve e-mail, login e endereço de cada cadastro. Aberta a qualquer
     * autenticado, ela entregaria de uma vez o que a regra de posse recusa um a um.
     */
    @Test
    @DisplayName("Usuário comum não lista os cadastros alheios: 403; sem token, 401")
    void deveRecusarListagemAoUsuarioComum() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "?page=0&size=100", HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class);

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertFalse(String.valueOf(resposta.getBody()).contains("@email.com"), "nenhum e-mail vaza no 403");
        assertEquals(HttpStatus.UNAUTHORIZED, rest.getForEntity(USERS + "?page=0&size=5", JsonNode.class)
                .getStatusCode());
    }

    @Test
    @DisplayName("Senha de 40 caracteres acentuados (80 bytes) é recusada na borda com 400, e não estoura no BCrypt")
    void deveRecusarSenhaAcimaDe72Bytes() {
        String login = "bytes" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> corpo = new java.util.HashMap<>(novoUsuario(login));
        corpo.put("password", "ç".repeat(40));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
    }

    @Test
    @DisplayName("Atualização do próprio cadastro grava e devolve o estado novo")
    void deveAtualizarOProprioCadastro() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + eu.id(), HttpMethod.PUT,
                corpoAutenticado(Map.of("name", "Nome Atualizado", "email", eu.login() + "@email.com",
                        "login", eu.login(), "addresses", List.of()), eu.token()), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("Nome Atualizado", resposta.getBody().get("name").asText());
        assertEquals(0, resposta.getBody().get("addresses").size());
    }

    @Test
    @DisplayName("Com o id do endereço, a atualização o mantém (mesmos ids); id de endereço alheio dá 400")
    void deveManterOEnderecoPeloId() {
        Usuario eu = cadastrarEAutenticar();
        Usuario outro = cadastrarEAutenticar();
        JsonNode antes = rest.exchange(USERS + "/" + eu.id(), HttpMethod.GET, autenticado(eu.token()), JsonNode.class)
                .getBody();
        String vinculo = antes.at("/addresses/0/id").asText();
        String endereco = antes.at("/addresses/0/address/id").asText();
        String vinculoAlheio = rest.exchange(USERS + "/" + outro.id(), HttpMethod.GET, autenticado(outro.token()),
                JsonNode.class).getBody().at("/addresses/0/id").asText();

        ResponseEntity<JsonNode> mantido = rest.exchange(USERS + "/" + eu.id(), HttpMethod.PUT, corpoAutenticado(Map.of(
                "name", "Com Endereço Mantido", "email", eu.login() + "@email.com", "login", eu.login(),
                "addresses", List.of(Map.of("id", vinculo, "label", "Casa Reformada",
                        "address", endereco("Rua Nova")))),
                eu.token()), JsonNode.class);
        ResponseEntity<JsonNode> alheio = rest.exchange(USERS + "/" + eu.id(), HttpMethod.PUT, corpoAutenticado(Map.of(
                "name", "Com Endereço Alheio", "email", eu.login() + "@email.com", "login", eu.login(),
                "addresses", List.of(Map.of("id", vinculoAlheio, "address", endereco("Rua Alheia")))),
                eu.token()), JsonNode.class);

        assertEquals(HttpStatus.OK, mantido.getStatusCode());
        assertEquals(vinculo, mantido.getBody().at("/addresses/0/id").asText());
        assertEquals(endereco, mantido.getBody().at("/addresses/0/address/id").asText());
        assertEquals("Casa Reformada", mantido.getBody().at("/addresses/0/label").asText());
        assertEquals("Rua Nova", mantido.getBody().at("/addresses/0/address/street").asText());
        assertEquals(HttpStatus.BAD_REQUEST, alheio.getStatusCode());
        assertEquals("Endereço do usuário não encontrado", alheio.getBody().get("detail").asText());
    }

    @Test
    @DisplayName("Atualização não toca na senha nem nos perfis: o login seguinte usa a mesma senha")
    void devePreservarSenhaEPapeisQuandoAtualiza() {
        Usuario eu = cadastrarEAutenticar();

        JsonNode atualizado = atualizar(eu, "Nome Atualizado", eu.token()).getBody();

        assertEquals("ROLE_CLIENT", atualizado.get("roles").get(0).asText());
        assertEquals(11, atualizado.at("/client/cpf").asText().length(), "o perfil continua lá");
        assertEquals(HttpStatus.OK, rest.postForEntity("/api/v1/auth/login",
                corpo(Map.of("login", eu.login(), "password", "senhaSegura123")), JsonNode.class).getStatusCode());
    }

    @Test
    @DisplayName("Auditoria grava o autor de cada gravação: system no autocadastro, depois quem alterou")
    void deveGravarOAutorDeCadaAlteracao() {
        Usuario eu = cadastrarEAutenticar();
        assertEquals(List.of("system", "system"), autoria(eu.id()), "autocadastro não tem autenticado");
        // Cada alteração muda o nome de propósito: um PUT idêntico ao estado gravado não gera
        // UPDATE nenhum, e então o autor anterior continua — "última alteração" é de dado, não de pedido.

        atualizar(eu, "Alterado Pelo Proprio", eu.token());
        assertEquals(List.of("system", eu.login()), autoria(eu.id()), "o próprio usuário alterou");

        atualizar(eu, "Alterado Pelo Administrador", autenticar("admin.demo", "admin12345"));
        assertEquals(List.of("system", "admin.demo"), autoria(eu.id()), "o administrador alterou; o criador não muda");
    }

    @Test
    @DisplayName("Troca de senha responde 204 e passa a valer no login seguinte")
    void deveTrocarASenha() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<Void> troca = rest.exchange(USERS + "/" + eu.id() + "/password", HttpMethod.PATCH,
                corpoAutenticado(Map.of("currentPassword", "senhaSegura123", "newPassword", "novaSenha456",
                        "confirmPassword", "novaSenha456"), eu.token()), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, troca.getStatusCode());
        assertNotNull(autenticar(eu.login(), "novaSenha456"));
    }

    @Test
    @DisplayName("Exclusão do próprio cadastro responde 204, e o cadastro some mesmo para o token ainda válido")
    void deveExcluirOProprioCadastro() {
        Usuario eu = cadastrarEAutenticar();

        ResponseEntity<Void> exclusao = rest.exchange(USERS + "/" + eu.id(), HttpMethod.DELETE,
                autenticado(eu.token()), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, exclusao.getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rest.exchange(USERS + "/" + eu.id(), HttpMethod.GET,
                autenticado(eu.token()), JsonNode.class).getStatusCode(),
                "o token ainda é válido, mas o cadastro não existe mais");
    }

    @Test
    @DisplayName("Cadastro com cliente e entregador (mesmo CPF) e dono: três papéis, entregador fora de serviço")
    void deveCadastrarComVariosPerfis() {
        String login = novoLogin();
        String cpf = Documentos.cpf();
        Map<String, Object> corpo = new HashMap<>(novoUsuario(login));
        corpo.put("owner", perfilDeDono());
        corpo.put("client", Map.of("cpf", cpf, "phone", "(11) 91234-5678", "birthDate", "1990-05-20"));
        corpo.put("courier", Map.of("cpf", cpf, "phone", "(11) 91234-5678", "vehicleType", "MOTORCYCLE",
                "driverLicense", Documentos.cnh(), "vehiclePlate", "abc1d23"));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        JsonNode criado = resposta.getBody();
        assertEquals(List.of("ROLE_OWNER", "ROLE_CLIENT", "ROLE_COURIER"),
                List.of(criado.at("/roles/0").asText(), criado.at("/roles/1").asText(), criado.at("/roles/2").asText()));
        assertEquals("1990-05-20", criado.at("/client/birthDate").asText());
        assertEquals("OFFLINE", criado.at("/courier/status").asText());
        assertEquals("ABC1D23", criado.at("/courier/vehiclePlate").asText(), "placa sai em maiúsculas");
        assertTrue(criado.get("admin").isNull());
    }

    @Test
    @DisplayName("Autocadastro não dá perfil de administrador: um bloco admin no corpo é ignorado")
    void naoDeveConcederAdministradorNoAutocadastro() {
        Map<String, Object> corpo = new HashMap<>(novoUsuario(novoLogin()));
        corpo.put("admin", Map.of("employeeCode", "ADM-9", "superAdmin", true));

        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS, corpo(corpo), JsonNode.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertTrue(resposta.getBody().get("admin").isNull());
        assertEquals(1, resposta.getBody().get("roles").size());
        assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM admins WHERE id = ?::uuid", Integer.class,
                resposta.getBody().get("id").asText()));
    }

    @Test
    @DisplayName("Corpo inválido é recusado na borda, antes de chegar ao caso de uso")
    void deveRecusarCorpoInvalido() {
        ResponseEntity<JsonNode> resposta = rest.postForEntity(USERS,
                corpo(Map.of("name", "", "email", "nao-e-email", "login", "x", "password", "curta")), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
    }

    private static String novoLogin() {
        return "api" + SEQUENCIA.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);
    }

    private static Map<String, Object> endereco(String rua) {
        return Map.of("street", rua, "number", "1", "neighborhood", "Centro", "city", "São Paulo", "state", "SP",
                "zipCode", "01001000");
    }

    private Map<String, Object> novoUsuario(String login) {
        return Map.of(
                "name", "Usuário " + login,
                "email", login + "@email.com",
                "login", login,
                "password", "senhaSegura123",
                "client", perfilDeCliente(),
                "addresses", List.of(Map.of("label", "Casa", "address", Map.of("street", "Rua das Flores",
                        "number", "100", "complement", "Apto 21", "neighborhood", "Centro", "city", "São Paulo",
                        "state", "SP", "zipCode", "01001-000"))));
    }

    private Usuario cadastrarEAutenticar() {
        String login = "api" + SEQUENCIA.incrementAndGet() + UUID.randomUUID().toString().substring(0, 6);
        JsonNode criado = rest.postForEntity(USERS, corpo(novoUsuario(login)), JsonNode.class).getBody();
        return new Usuario(UUID.fromString(criado.get("id").asText()), login,
                autenticar(login, "senhaSegura123"));
    }

    private ResponseEntity<JsonNode> atualizar(Usuario usuario, String nome, String token) {
        ResponseEntity<JsonNode> resposta = rest.exchange(USERS + "/" + usuario.id(), HttpMethod.PUT,
                corpoAutenticado(Map.of("name", nome, "email", usuario.login() + "@email.com",
                        "login", usuario.login(), "addresses", List.of()), token), JsonNode.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        return resposta;
    }

    /** Autor da criação e da última alteração, lidos direto da tabela: a API não os expõe. */
    private List<String> autoria(UUID id) {
        return jdbc.queryForObject("SELECT created_by, last_updated_by FROM users WHERE id = ?",
                (linha, numero) -> List.of(linha.getString(1), linha.getString(2)), id);
    }

    private record Usuario(UUID id, String login, String token) {
    }
}
