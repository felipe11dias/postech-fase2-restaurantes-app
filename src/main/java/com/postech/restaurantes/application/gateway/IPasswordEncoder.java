package com.postech.restaurantes.application.gateway;

/** Hash e verificação de senha. O algoritmo é detalhe da infraestrutura. */
public interface IPasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Gasta o mesmo tempo de {@link #matches} sem haver um hash real para comparar. Usado
     * quando o login não existe, para que o caminho "usuário não encontrado" não seja mais
     * rápido que "senha incorreta" — a implementação sabe qual hash fictício usar para o seu
     * algoritmo; o núcleo não precisa saber.
     */
    void simulateMatch(String rawPassword);
}
