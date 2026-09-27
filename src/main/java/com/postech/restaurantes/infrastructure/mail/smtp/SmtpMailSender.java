package com.postech.restaurantes.infrastructure.mail.smtp;

import com.postech.restaurantes.adapter.service.IMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;

/**
 * Implementação SMTP de {@link IMailSender}: só transporte. Assunto e corpo chegam prontos do
 * gateway no adaptador — trocar o SMTP por outro provedor não reescreve o que o usuário lê.
 */
@Component
public class SmtpMailSender implements IMailSender {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpMailSender.class);

    private final MailSender mailSender;
    private final MailProperties properties;

    public SmtpMailSender(MailSender mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /**
     * Honra o contrato da porta: falha de SMTP é registrada em ERROR e não sobe. Resposta vaga
     * para o cliente, registro detalhado para quem opera. O destinatário não vai para o log —
     * um log com a lista de quem pediu redefinição seria o mesmo vazamento por outra porta.
     */
    @Override
    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            LOG.error("Falha ao enviar e-mail", e);
        }
    }
}
