package com.postech.restaurantes.infrastructure.persistence.user;

import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Implementação JPA da origem de dados de tokens de redefinição de senha. */
@Repository
public class PasswordResetTokenDataSourceJpa implements IPasswordResetTokenDataSource {

    private final SpringDataPasswordResetTokenRepository tokens;

    public PasswordResetTokenDataSourceJpa(SpringDataPasswordResetTokenRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasswordResetTokenData> findByTokenHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(PasswordResetTokenDataSourceJpa::toData);
    }

    @Override
    @Transactional
    public PasswordResetTokenData insert(PasswordResetTokenData token) {
        PasswordResetTokenJpaEntity entity = new PasswordResetTokenJpaEntity();
        entity.setUserId(token.userId());
        entity.setTokenHash(token.tokenHash());
        entity.setExpiresAt(token.expiresAt());
        entity.setUsed(token.used());
        return toData(tokens.save(entity));
    }

    /**
     * Só o consumo do token muda depois de emitido; hash, dono e validade são imutáveis por
     * contrato, e por isso não são reescritos aqui.
     */
    @Override
    @Transactional
    public PasswordResetTokenData update(PasswordResetTokenData token) {
        PasswordResetTokenJpaEntity entity = tokens.findById(token.id())
                .orElseThrow(() -> new IllegalStateException("Token inexistente para atualização: " + token.id()));
        entity.setUsed(token.used());
        return toData(tokens.save(entity));
    }

    static PasswordResetTokenData toData(PasswordResetTokenJpaEntity entity) {
        return new PasswordResetTokenData(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.isUsed());
    }
}
