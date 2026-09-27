package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.service.IMailSender;
import java.util.ArrayList;
import java.util.List;

/**
 * Caixa de saída de uma operação: guarda as mensagens que o caso de uso pediu e só as entrega
 * quando o controller mandar — depois que a unidade de trabalho confirmou.
 *
 * <p>Um e-mail não se desfaz. Enviado dentro da transação, ele sairia mesmo que a gravação do
 * token fosse desfeita no commit, e o usuário receberia um token que não existe; e a transação
 * ficaria aberta durante toda a conversa com o servidor de e-mail. Demarcar o que acontece
 * depois do commit é orquestração, o mesmo papel da unidade de trabalho — por isso mora aqui.
 */
final class MailOutbox implements IMailSender {

    private final IMailSender sender;
    private final List<Message> pending = new ArrayList<>();

    MailOutbox(IMailSender sender) {
        this.sender = sender;
    }

    @Override
    public void send(String to, String subject, String body) {
        pending.add(new Message(to, subject, body));
    }

    /** Entrega o que foi pedido, na ordem, e esvazia a caixa. */
    void deliver() {
        pending.forEach(message -> sender.send(message.to(), message.subject(), message.body()));
        pending.clear();
    }

    private record Message(String to, String subject, String body) {
    }
}
