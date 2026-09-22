package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import java.util.Optional;

/** Origem de dados de tokens de redefinição de senha. */
public interface IPasswordResetTokenDataSource {

    Optional<PasswordResetTokenData> findByTokenHash(String tokenHash);

    PasswordResetTokenData insert(PasswordResetTokenData token);

    PasswordResetTokenData update(PasswordResetTokenData token);
}
