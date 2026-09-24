package com.postech.restaurantes.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Colunas de auditoria comuns às entidades JPA, todas preenchidas pelo
 * {@link AuditingEntityListener}.
 *
 * <p>Auditoria é <strong>metadado de gravação</strong>, não regra de negócio: nenhuma
 * invariante do domínio depende de quando ou por quem uma linha foi escrita, e o agregado de
 * usuário nem sequer recebe um instante ao ser criado — quem recebe é
 * {@code PasswordResetToken}, porque ali o tempo <em>é</em> regra (o token vence). Quem grava
 * é quem sabe quando gravou e em nome de quem, e é aqui que isso é registrado: o instante vem
 * do {@code DateTimeProvider} ligado ao {@code Clock} da aplicação, e o autor do
 * {@code AuditorAware}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableJpaEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
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

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    /**
     * Carimba os instantes de uma vez só, em vez de dois setters avulsos: em uso normal quem
     * os escreve é o listener, e este método existe para montar em memória uma entidade
     * equivalente a uma linha já gravada — o mesmo que o Hibernate faz por reflexão ao
     * carregar. <strong>Nenhuma origem de dados deve chamá-lo</strong>: escrever a auditoria a
     * partir do registro do adaptador devolveria ao núcleo um "quando" que ele não tem no
     * cadastro. Há teste guardando essa regra.
     */
    public void auditadaEm(LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }
}
