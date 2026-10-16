package com.postech.restaurantes.infrastructure.persistence.jpa.user.password;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data de {@code password_reset_tokens}. */
public interface SpringDataPasswordResetTokenRepository
        extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {

    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    Optional<PasswordResetTokenJpaEntity> findByUserId(UUID userId);
}
