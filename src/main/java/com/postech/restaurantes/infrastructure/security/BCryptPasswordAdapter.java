package com.postech.restaurantes.infrastructure.security;

import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link IPasswordEncoder} com BCrypt. É o único lugar do sistema que conhece
 * o algoritmo: o núcleo recebe e guarda um "hash", sem saber de que tipo.
 */
@Component
public class BCryptPasswordAdapter implements IPasswordEncoder {

    /**
     * Hash de um valor aleatório, descartado. Serve só para dar a {@link #simulateMatch} o
     * mesmo custo de uma comparação real — nenhuma senha corresponde a ele.
     */
    private static final String DUMMY_HASH = "$2a$10$ge7YbLMv8qGHCEA7Cz8lYO9gK1wX.eZ1IeCO.CR3Jf7oEltTtivUe";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    /**
     * Gasta o tempo de um BCrypt de verdade e ignora o resultado, para que "login inexistente"
     * demore o mesmo que "senha errada" e o tempo de resposta não revele quais logins existem.
     */
    @Override
    public void simulateMatch(String rawPassword) {
        encoder.matches(rawPassword, DUMMY_HASH);
    }
}
