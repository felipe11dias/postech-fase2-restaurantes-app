package com.postech.restaurantes.infrastructure.persistence.jpa.user.owner;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Tabela {@code owners}: perfil de dono. A chave primária é a do usuário, atribuída pela origem de
 * dados; o perfil não referencia o usuário (ver {@code UserJpaEntity}).
 */
@Entity
@Table(name = "owners")
public class OwnerJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cnpj", nullable = false, unique = true, length = 14)
    private String cnpj;

    @Column(name = "legal_name", nullable = false, length = 150)
    private String legalName;

    @Column(name = "business_phone", nullable = false, length = 20)
    private String businessPhone;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getBusinessPhone() {
        return businessPhone;
    }

    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }
}
