package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import java.util.Optional;
import java.util.UUID;

/** Origem de dados de tokens de redefinição de senha. */
public interface IPasswordResetTokenDataSource {

    Optional<PasswordResetTokenData> findByTokenHash(String tokenHash);

    Optional<PasswordResetTokenData> findByUserId(UUID userId);

    PasswordResetTokenData insert(PasswordResetTokenData token);

    PasswordResetTokenData update(PasswordResetTokenData token);
}
