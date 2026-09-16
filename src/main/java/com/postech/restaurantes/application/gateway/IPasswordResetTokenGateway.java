package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.domain.entity.PasswordResetToken;
import java.util.Optional;

/** Acesso aos tokens de redefinição de senha, sempre pelo hash — o valor em claro nunca é persistido. */
public interface IPasswordResetTokenGateway {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    PasswordResetToken insert(PasswordResetToken token);

    PasswordResetToken update(PasswordResetToken token);
}
