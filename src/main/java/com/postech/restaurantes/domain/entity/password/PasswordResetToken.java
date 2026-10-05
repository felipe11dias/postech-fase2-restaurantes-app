package com.postech.restaurantes.domain.entity.password;

import com.postech.restaurantes.domain.Guard;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de uso único para redefinição de senha. O domínio guarda apenas o hash do token; o
 * valor em claro só existe no e-mail enviado ao usuário. O instante de referência é sempre
 * recebido por parâmetro, mantendo a entidade independente de relógio.
 *
 * <p>Cada usuário tem um token só: um pedido novo <em>reemite</em> o existente ({@link #reissue}),
 * e o hash anterior deixa de existir — o link do e-mail antigo para de funcionar.
 */
public final class PasswordResetToken {

    private final UUID id;
    private final UUID userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private boolean used;

    private PasswordResetToken(UUID id, UUID userId, String tokenHash, LocalDateTime expiresAt, boolean used) {
        this.id = id;
        this.userId = Guard.requireNonNull(userId, "Usuário do token inválido");
        this.tokenHash = Guard.requireNonBlank(tokenHash, "Hash do token inválido");
        this.expiresAt = Guard.requireNonNull(expiresAt, "Expiração do token inválida");
        this.used = used;
    }

    /** Token novo: precisa expirar no futuro em relação a {@code now}. */
    public static PasswordResetToken create(UUID userId, String tokenHash, LocalDateTime expiresAt, LocalDateTime now) {
        Guard.requireNonNull(now, "Instante de referência inválido");
        PasswordResetToken token = new PasswordResetToken(null, userId, tokenHash, expiresAt, false);
        Guard.require(token.expiresAt.isAfter(now), "Expiração do token deve ser futura");
        return token;
    }

    public static PasswordResetToken restore(UUID id, UUID userId, String tokenHash, LocalDateTime expiresAt, boolean used) {
        return new PasswordResetToken(Guard.requireNonNull(id, "Id do token inválido"), userId, tokenHash, expiresAt, used);
    }

    /**
     * Reemite o token: hash e validade novos, e de novo sem uso. Tudo é validado antes de mudar
     * qualquer campo — um pedido inválido não deixa o token pela metade.
     */
    public void reissue(String newTokenHash, LocalDateTime newExpiresAt, LocalDateTime now) {
        Guard.requireNonNull(now, "Instante de referência inválido");
        String hash = Guard.requireNonBlank(newTokenHash, "Hash do token inválido");
        LocalDateTime expiration = Guard.requireNonNull(newExpiresAt, "Expiração do token inválida");
        Guard.require(expiration.isAfter(now), "Expiração do token deve ser futura");
        this.tokenHash = hash;
        this.expiresAt = expiration;
        this.used = false;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(Guard.requireNonNull(now, "Instante de referência inválido"));
    }

    /** Utilizável = ainda não usado e não expirado. */
    public boolean isUsable(LocalDateTime now) {
        return !used && !isExpired(now);
    }

    /** Uso único: a segunda chamada é uma violação de estado, não de argumento. */
    public void markUsed() {
        if (used) {
            throw new IllegalStateException("Token já utilizado");
        }
        used = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }
}
