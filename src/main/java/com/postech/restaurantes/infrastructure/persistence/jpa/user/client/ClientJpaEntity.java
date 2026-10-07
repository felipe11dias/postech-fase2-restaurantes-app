package com.postech.restaurantes.infrastructure.persistence.jpa.user.client;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tabela {@code clients}: perfil de cliente. A chave primária é a do usuário, atribuída pela origem
 * de dados; o perfil não referencia o usuário (ver {@code UserJpaEntity}).
 */
@Entity
@Table(name = "clients")
public class ClientJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
