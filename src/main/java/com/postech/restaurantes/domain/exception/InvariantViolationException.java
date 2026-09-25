package com.postech.restaurantes.domain.exception;

/**
 * Violação de invariante de entidade ou objeto de valor: o dado nunca chegou a existir no
 * domínio. Lançada pelo {@code Guard}.
 *
 * <p>Estende {@link IllegalArgumentException} — é isso que ela é, e todo código que já a trata
 * como tal continua certo. Existe como tipo próprio por um motivo só: a mensagem dela foi
 * escrita para o usuário ("E-mail inválido", "CEP inválido") e pode ir para a resposta HTTP,
 * enquanto uma {@code IllegalArgumentException} qualquer, vinda de biblioteca, carrega detalhe
 * interno e não pode. A infraestrutura precisa distinguir as duas, e o tipo é a forma honesta
 * de fazê-lo.
 */
public class InvariantViolationException extends IllegalArgumentException {

    public InvariantViolationException(String message) {
        super(message);
    }
}
