package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Autenticação e recuperação de senha por HTTP. O SMTP é a única coisa mockada — mandar
 * e-mail de verdade em teste não prova nada e depende de um servidor externo.
 */
class AuthApiIT extends WebIntegrationTestSupport {

    private static final String LOGIN = "/api/v1/auth/login";
    private static final String FORGOT = "/api/v1/auth/forgot-password";
    private static final String RESET = "/api/v1/auth/reset-password";

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void limparMock() {
        reset(mailSender);
    }

    @Test
    @DisplayName("Login com as credenciais da seed devolve token Bearer com expiração")
    void deveAutenticarUsuarioDaSeed() {
        ResponseEntity<JsonNode> resposta = rest.postForEntity(LOGIN,
                corpo(Map.of("login", "cliente.demo", "password", "cliente12345")), JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("Bearer", resposta.getBody().get("type").asText());
        assertEquals(3, resposta.getBody().get("token").asText().split("\\.").length, "o token é um JWT");
        assertNotNull(resposta.getBody().get("expiresAt").asText());
    }

    @Test
    @DisplayName("Login com senha errada e login inexistente respondem igual, sem revelar qual falhou")
    void naoDeveRevelarQuaisLoginsExistem() {
        ResponseEntity<JsonNode> senhaErrada = rest.postForEntity(LOGIN,
                corpo(Map.of("login", "cliente.demo", "password", "senhaErrada")), JsonNode.class);
        ResponseEntity<JsonNode> loginInexistente = rest.postForEntity(LOGIN,
                corpo(Map.of("login", "nao.existe", "password", "cliente12345")), JsonNode.class);

        assertEquals(senhaErrada.getStatusCode(), loginInexistente.getStatusCode());
    }

    @Test
    @DisplayName("Esqueci minha senha responde 202 e envia o token em claro apenas por e-mail")
    void deveEnviarOTokenPorEmail() {
        ResponseEntity<Void> resposta = rest.postForEntity(FORGOT,
                corpo(Map.of("email", "dono.restaurante@email.com")), Void.class);

        assertEquals(HttpStatus.ACCEPTED, resposta.getStatusCode());
        assertNotNull(tokenEnviado());
    }

    @Test
    @DisplayName("E-mail desconhecido recebe a mesma resposta 202, e nenhum e-mail é enviado")
    void naoDeveRevelarQuaisEmailsExistem() {
        ResponseEntity<Void> resposta = rest.postForEntity(FORGOT,
                corpo(Map.of("email", "ninguem." + UUID.randomUUID() + "@email.com")), Void.class);

        assertEquals(HttpStatus.ACCEPTED, resposta.getStatusCode());
        verifyNoInteractions(mailSender);
    }

    /**
     * Com o SMTP fora do ar, um e-mail cadastrado — o único caso que dispara envio — precisa
     * responder o mesmo 202 de um e-mail desconhecido. Se a falha subisse, duas requisições
     * bastariam para descobrir quem tem conta.
     */
    @Test
    @DisplayName("SMTP fora do ar não muda a resposta: e-mail cadastrado e desconhecido recebem o mesmo 202")
    void naoDeveRevelarContasQuandoOSmtpFalha() {
        doThrow(new MailSendException("SMTP fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        ResponseEntity<Void> cadastrado = rest.postForEntity(FORGOT,
                corpo(Map.of("email", "cliente.demo@email.com")), Void.class);
        ResponseEntity<Void> desconhecido = rest.postForEntity(FORGOT,
                corpo(Map.of("email", "ninguem." + UUID.randomUUID() + "@email.com")), Void.class);

        assertEquals(HttpStatus.ACCEPTED, cadastrado.getStatusCode());
        assertEquals(desconhecido.getStatusCode(), cadastrado.getStatusCode());
    }

    @Test
    @DisplayName("Ciclo completo: pedir, redefinir com o token do e-mail e entrar com a senha nova")
    void deveRedefinirASenhaComOTokenRecebido() {
        String login = "reset" + UUID.randomUUID().toString().substring(0, 8);
        cadastrar(login);
        rest.postForEntity(FORGOT, corpo(Map.of("email", login + "@email.com")), Void.class);
        String token = tokenEnviado();

        ResponseEntity<Void> redefinicao = rest.postForEntity(RESET, corpo(Map.of("token", token,
                "newPassword", "senhaNova456", "confirmPassword", "senhaNova456")), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, redefinicao.getStatusCode());
        assertEquals(HttpStatus.OK, rest.postForEntity(LOGIN,
                corpo(Map.of("login", login, "password", "senhaNova456")), JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, rest.postForEntity(LOGIN,
                corpo(Map.of("login", login, "password", "senhaSegura123")), JsonNode.class).getStatusCode(),
                "a senha antiga deixa de valer");
    }

    @Test
    @DisplayName("O mesmo token não redefine a senha duas vezes")
    void deveRecusarTokenJaUsado() {
        String login = "reuso" + UUID.randomUUID().toString().substring(0, 8);
        cadastrar(login);
        rest.postForEntity(FORGOT, corpo(Map.of("email", login + "@email.com")), Void.class);
        String token = tokenEnviado();
        Map<String, Object> redefinicao = Map.of("token", token, "newPassword", "senhaNova456",
                "confirmPassword", "senhaNova456");
        rest.postForEntity(RESET, corpo(redefinicao), Void.class);

        ResponseEntity<JsonNode> segunda = rest.postForEntity(RESET, corpo(redefinicao), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, segunda.getStatusCode());
        assertEquals("urn:restaurantes:problema:token-invalido", segunda.getBody().get("type").asText());
    }

    @Test
    @DisplayName("Os três endpoints de autenticação dispensam token")
    void deveSerPublico() {
        assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity(LOGIN, corpo(Map.of()), JsonNode.class)
                .getStatusCode(), "recusado pela validação, não pela autenticação");
        assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity(FORGOT, corpo(Map.of()), JsonNode.class)
                .getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, rest.postForEntity(RESET, corpo(Map.of()), JsonNode.class)
                .getStatusCode());
    }

    private void cadastrar(String login) {
        ResponseEntity<JsonNode> resposta = rest.postForEntity("/api/v1/users", corpo(Map.of(
                "name", "Usuário " + login,
                "email", login + "@email.com",
                "login", login,
                "password", "senhaSegura123",
                "roles", List.of("ROLE_CUSTOMER"),
                "addresses", List.of())), JsonNode.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
    }

    /** O token em claro só existe no corpo do e-mail: o banco guarda apenas o hash. */
    private String tokenEnviado() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue().getText().lines()
                .map(String::trim)
                .filter(linha -> linha.matches("[A-Za-z0-9_-]{43}"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("token não encontrado no corpo do e-mail"));
    }
}
