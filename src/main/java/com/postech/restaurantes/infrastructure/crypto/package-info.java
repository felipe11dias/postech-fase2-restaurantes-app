/**
 * Módulo de criptografia. Implementa IPasswordEncoder (BCrypt, do spring-security-crypto) e
 * ISecureTokenGenerator (SecureRandom + SHA-256, do JDK).
 *
 * <p>Substituir (ex.: Argon2): nova classe que implemente a porta, e a antiga sai. Nenhum outro
 * módulo depende deste — o núcleo só conhece as interfaces.
 */
package com.postech.restaurantes.infrastructure.crypto;
