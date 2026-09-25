package com.postech.restaurantes.infrastructure.mail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.postech.restaurantes.domain.vo.Email;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

class SmtpMailGatewayTest {

    private final MailSender mailSender = mock(MailSender.class);
    private final MailProperties properties = new MailProperties("no-reply@restaurantes.postech", 30);
    private final SmtpMailGateway gateway = new SmtpMailGateway(mailSender, properties);

    @Test
    @DisplayName("A mensagem vai para o e-mail do usuário, com o token em claro e a validade")
    void deveEnviarOTokenEmClaro() {
        gateway.sendPasswordReset(Email.of("joao.silva@email.com"), "token-em-claro");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("no-reply@restaurantes.postech", mensagem.getFrom());
        assertArrayEquals(new String[] {"joao.silva@email.com"}, mensagem.getTo());
        assertEquals(SmtpMailGateway.SUBJECT, mensagem.getSubject());
        assertTrue(mensagem.getText().contains("token-em-claro"));
        assertTrue(mensagem.getText().contains("30 minutos"));
    }

    @Test
    @DisplayName("Falha de SMTP não sobe: o chamador não pode distinguir e-mail enviado de não enviado")
    void naoDevePropagarFalhaDeTransporte() {
        doThrow(new MailSendException("SMTP fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> gateway.sendPasswordReset(Email.of("joao.silva@email.com"), "token"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("A validade configurada em minutos vira a duração usada pelo caso de uso")
    void deveConverterAValidade() {
        assertEquals(Duration.ofMinutes(30), properties.resetTokenValidity());
    }

    @Test
    @DisplayName("Remetente ausente e validade não positiva são recusados na configuração")
    void deveRecusarConfiguracaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new MailProperties(null, 30));
        assertThrows(IllegalArgumentException.class, () -> new MailProperties("  ", 30));
        assertThrows(IllegalArgumentException.class, () -> new MailProperties("a@b.com", 0));
        assertThrows(IllegalArgumentException.class, () -> new MailProperties("a@b.com", -1));
    }
}
