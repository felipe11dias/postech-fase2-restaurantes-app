package com.postech.restaurantes.infrastructure.mail.smtp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/** Só o transporte: o que a mensagem diz é testado no gateway do adaptador. */
class SmtpMailSenderTest {

    private final MailSender mailSender = mock(MailSender.class);
    private final MailProperties properties = new MailProperties("no-reply@restaurantes.postech");
    private final SmtpMailSender sender = new SmtpMailSender(mailSender, properties);

    @Test
    @DisplayName("A mensagem sai do remetente configurado, com destinatário, assunto e corpo recebidos")
    void deveEnviarAMensagemRecebida() {
        sender.send("joao.silva@email.com", "Assunto", "Corpo da mensagem");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensagem = captor.getValue();
        assertEquals("no-reply@restaurantes.postech", mensagem.getFrom());
        assertArrayEquals(new String[] {"joao.silva@email.com"}, mensagem.getTo());
        assertEquals("Assunto", mensagem.getSubject());
        assertEquals("Corpo da mensagem", mensagem.getText());
    }

    @Test
    @DisplayName("Falha de SMTP não sobe: o chamador não pode distinguir e-mail enviado de não enviado")
    void naoDevePropagarFalhaDeTransporte() {
        doThrow(new MailSendException("SMTP fora do ar")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> sender.send("joao.silva@email.com", "Assunto", "Corpo"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Remetente ausente é recusado na configuração")
    void deveRecusarConfiguracaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new MailProperties(null));
        assertThrows(IllegalArgumentException.class, () -> new MailProperties("  "));
    }
}
