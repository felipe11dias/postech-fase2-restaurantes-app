package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.service.IMailSender;
import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.vo.Email;
import java.time.Duration;

/**
 * Tradutor entre o pedido do caso de uso ("mande este token a este e-mail, válido por tanto
 * tempo") e uma mensagem de texto pronta para o {@link IMailSender}. O que o usuário lê é decidido
 * aqui, no adaptador; trocar o transporte (SMTP, API de um provedor) não reescreve o texto.
 */
public final class PasswordResetMailGateway implements IMailGateway {

    private static final String SUBJECT = "Redefinição de senha";

    private final IMailSender mailSender;

    private PasswordResetMailGateway(IMailSender mailSender) {
        this.mailSender = Guard.requireNonNull(mailSender, "Serviço de e-mail inválido");
    }

    public static PasswordResetMailGateway create(IMailSender mailSender) {
        return new PasswordResetMailGateway(mailSender);
    }

    @Override
    public void sendPasswordReset(Email to, String rawToken, Duration validity) {
        mailSender.send(to.value(), SUBJECT, body(rawToken, validity));
    }

    private static String body(String rawToken, Duration validity) {
        return """
                Recebemos um pedido de redefinição de senha para a sua conta.

                Use o token abaixo para escolher uma nova senha:

                %s

                Ele vale por %s e só pode ser usado uma vez.

                Se não foi você quem pediu, ignore esta mensagem: nada muda até que o token seja usado.
                """.formatted(rawToken, describe(validity));
    }

    /**
     * A validade como o usuário a lê: em minutos quando é um número inteiro deles (o caso da
     * configuração), senão em segundos. Só a fração de segundo é descartada — "1 minuto e meio"
     * vira "90 segundos", e não "1 minutos".
     */
    static String describe(Duration validity) {
        long seconds = validity.toSeconds();
        if (seconds == 0) {
            return "menos de um segundo";
        }
        return seconds % 60 == 0 ? count(seconds / 60, "minuto") : count(seconds, "segundo");
    }

    private static String count(long amount, String unit) {
        return amount + " " + unit + (amount == 1 ? "" : "s");
    }
}
