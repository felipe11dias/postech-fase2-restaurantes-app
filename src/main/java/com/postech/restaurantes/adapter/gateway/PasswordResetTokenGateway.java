package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.application.gateway.IPasswordResetTokenGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.PasswordResetToken;
import java.util.Optional;

/** Tradutor entre {@link PasswordResetToken} e o record da origem de dados. */
public final class PasswordResetTokenGateway implements IPasswordResetTokenGateway {

    private final IPasswordResetTokenDataSource dataSource;

    private PasswordResetTokenGateway(IPasswordResetTokenDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de token inválida");
    }

    public static PasswordResetTokenGateway create(IPasswordResetTokenDataSource dataSource) {
        return new PasswordResetTokenGateway(dataSource);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return dataSource.findByTokenHash(tokenHash).map(PasswordResetTokenGateway::toEntity);
    }

    @Override
    public PasswordResetToken insert(PasswordResetToken token) {
        return toEntity(dataSource.insert(toData(token)));
    }

    @Override
    public PasswordResetToken update(PasswordResetToken token) {
        return toEntity(dataSource.update(toData(token)));
    }

    static PasswordResetToken toEntity(PasswordResetTokenData data) {
        return PasswordResetToken.restore(data.id(), data.userId(), data.tokenHash(), data.expiresAt(), data.used());
    }

    static PasswordResetTokenData toData(PasswordResetToken token) {
        return new PasswordResetTokenData(token.getId(), token.getUserId(), token.getTokenHash(),
                token.getExpiresAt(), token.isUsed());
    }
}
