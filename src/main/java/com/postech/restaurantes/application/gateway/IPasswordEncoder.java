package com.postech.restaurantes.application.gateway;

/** Hash e verificação de senha. O algoritmo (BCrypt) é detalhe da infraestrutura. */
public interface IPasswordEncoder {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
