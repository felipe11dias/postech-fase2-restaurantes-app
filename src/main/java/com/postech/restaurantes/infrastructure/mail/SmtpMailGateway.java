package com.postech.restaurantes.infrastructure.mail;

import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.domain.vo.Email;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Component;

/**
 * Implementação SMTP de {@link IMailGateway}. É o único ponto por onde o token de redefinição
 * sai em claro — o banco guarda apenas o hash.
 */
@Component
public class SmtpMailGateway implements IMailGateway {

    static final String SUBJECT = "Redefinição de senha";

    private final MailSender mailSender;
    private final MailProperties properties;

    public SmtpMailGateway(MailSender mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendPasswordReset(Email to, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(to.value());
        message.setSubject(SUBJECT);
        message.setText(corpo(rawToken));
        mailSender.send(message);
    }

    private String corpo(String rawToken) {
        return """
                Recebemos um pedido de redefinição de senha para a sua conta.

                Use o token abaixo para escolher uma nova senha:

                %s

                Ele vale por %d minutos e só pode ser usado uma vez.

                Se não foi você quem pediu, ignore esta mensagem: nada muda até que o token seja usado.
                """.formatted(rawToken, properties.resetTokenExpirationMinutes());
    }
}
