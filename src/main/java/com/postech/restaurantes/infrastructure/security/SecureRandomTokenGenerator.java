package com.postech.restaurantes.infrastructure.security;

import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link ISecureTokenGenerator} para os tokens de redefinição de senha.
 *
 * <p>São <strong>32 bytes de {@link SecureRandom}</strong> — não um UUID nem um número de
 * sequência: o token é a única credencial de quem esqueceu a senha, e precisa ser
 * imprevisível. O que vai para o banco é o <strong>SHA-256</strong> do token, não o token:
 * quem lê a tabela não consegue redefinir a senha de ninguém.
 *
 * <p>SHA-256 puro, e não BCrypt, porque aqui o segredo já tem 256 bits de entropia — não há
 * ataque de dicionário a encarecer, e a consulta pelo hash precisa ser determinística.
 */
@Component
public class SecureRandomTokenGenerator implements ISecureTokenGenerator {

    static final int TOKEN_BYTES = 32;
    static final String HASH_ALGORITHM = "SHA-256";

    private final SecureRandom random = new SecureRandom();
    private final String algorithm;

    public SecureRandomTokenGenerator() {
        this(HASH_ALGORITHM);
    }

    /** Visível para teste: permite exercitar o caminho de algoritmo indisponível. */
    SecureRandomTokenGenerator(String algorithm) {
        this.algorithm = algorithm;
    }

    /** Base64 URL sem preenchimento: o token viaja em link de e-mail sem escapar nada. */
    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance(algorithm)
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " indisponível nesta JVM", e);
        }
    }
}
