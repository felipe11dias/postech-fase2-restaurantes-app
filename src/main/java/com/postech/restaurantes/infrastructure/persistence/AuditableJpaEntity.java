package com.postech.restaurantes.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Colunas de auditoria comuns às entidades JPA.
 *
 * <p>Divisão deliberada entre "quando" e "quem": o <strong>instante</strong>
 * ({@code created_at}, {@code last_updated_at}) é decidido pelo núcleo — as entidades de
 * domínio recebem o momento por parâmetro e nunca chamam o relógio — e chega aqui pronto,
 * pelo record da origem de dados. Já o <strong>autor</strong> ({@code created_by},
 * {@code last_updated_by}) é informação do contexto de execução, que o núcleo não conhece:
 * esse o {@link AuditingEntityListener} preenche, lendo o {@code AuditorAware}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableJpaEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_updated_by", length = 100)
    private String lastUpdatedBy;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }
}
