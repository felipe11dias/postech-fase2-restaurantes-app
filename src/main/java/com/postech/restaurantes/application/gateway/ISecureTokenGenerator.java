package com.postech.restaurantes.application.gateway;

/**
 * Geração de tokens aleatórios e seu hash de armazenamento. Fica atrás de interface para que
 * os casos de uso sejam determinísticos em teste e a escolha de algoritmo (SecureRandom,
 * SHA-256) seja detalhe da infraestrutura.
 */
public interface ISecureTokenGenerator {

    /** Token em claro, imprevisível, seguro para transporte em URL. */
    String generate();

    /** Hash determinístico do token, usado para persistir e consultar. */
    String hash(String rawToken);
}
