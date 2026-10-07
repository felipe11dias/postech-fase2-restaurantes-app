package com.postech.restaurantes.infrastructure.persistence.jpa.user.admin;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Tabela {@code admins}: perfil de administrador. A chave primária é a do usuário, atribuída pela
 * origem de dados; o perfil não referencia o usuário (ver {@code UserJpaEntity}).
 */
@Entity
@Table(name = "admins")
public class AdminJpaEntity extends AuditableJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "is_super_admin", nullable = false)
    private boolean superAdmin;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    public void setSuperAdmin(boolean superAdmin) {
        this.superAdmin = superAdmin;
    }
}
