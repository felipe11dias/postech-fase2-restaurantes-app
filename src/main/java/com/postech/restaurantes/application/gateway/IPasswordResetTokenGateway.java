package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.domain.entity.password.PasswordResetToken;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso aos tokens de redefinição de senha. O valor em claro nunca é persistido: o token é achado
 * pelo hash (na redefinição) ou pelo dono (para reemitir — cada usuário tem um só).
 */
public interface IPasswordResetTokenGateway {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    Optional<PasswordResetToken> findByUserId(UUID userId);

    PasswordResetToken insert(PasswordResetToken token);

    PasswordResetToken update(PasswordResetToken token);
}
