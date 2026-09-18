package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Token de redefinição como a origem de dados o conhece: só o hash, nunca o valor em claro. */
public record PasswordResetTokenData(UUID id, UUID userId, String tokenHash, LocalDateTime expiresAt, boolean used) {
}
