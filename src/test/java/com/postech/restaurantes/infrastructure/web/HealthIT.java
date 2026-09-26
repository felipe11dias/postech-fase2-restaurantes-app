package com.postech.restaurantes.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.TestPropertySource;

/**
 * O {@code /actuator/health} é o que o healthcheck do container consulta (Etapa 10). Ele tem
 * de refletir só o que impede a API de funcionar — banco e disco — e não um serviço opcional
 * como o SMTP, cuja falha a aplicação já absorve (Etapa 7).
 *
 * <p>Aqui o SMTP é real, não um dublê, e aponta para uma porta em que nada escuta: se o
 * indicador de e-mail voltasse a contar, a saúde viraria 503 e este teste falharia.
 */
@TestPropertySource(properties = {"spring.mail.host=127.0.0.1", "spring.mail.port=1"})
class HealthIT extends WebIntegrationTestSupport {

    private static final String HEALTH = "/actuator/health";

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Test
    @DisplayName("Com o SMTP fora do ar, a saúde da API continua UP")
    void deveContinuarSaudavelQuandoOSmtpEstaForaDoAr() {
        assertThrows(MessagingException.class, mailSender::testConnection,
                "pré-condição: o SMTP deste contexto precisa estar de fato inalcançável");

        ResponseEntity<JsonNode> resposta = rest.getForEntity(HEALTH, JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("UP", resposta.getBody().get("status").asText());
    }

    @Test
    @DisplayName("A saúde é pública e, sem autenticação, não expõe os componentes")
    void deveSerPublicaSemExporDetalhes() {
        ResponseEntity<JsonNode> resposta = rest.getForEntity(HEALTH, JsonNode.class);

        assertEquals(HttpStatus.OK, resposta.getStatusCode(), "o healthcheck do container chama sem token");
        assertFalse(resposta.getBody().has("components"));
    }

    @Test
    @DisplayName("Os componentes da saúde são o banco e o disco, e o e-mail fica de fora")
    void deveConsiderarBancoEDiscoSemEmail() {
        String token = autenticar("admin.demo", "admin12345");

        JsonNode componentes = rest.exchange(HEALTH, HttpMethod.GET, autenticado(token), JsonNode.class)
                .getBody().get("components");

        assertTrue(componentes.has("db"));
        assertTrue(componentes.has("diskSpace"));
        assertFalse(componentes.has("mail"));
    }
}
