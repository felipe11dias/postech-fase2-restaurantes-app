package com.postech.restaurantes.infrastructure.persistence.user;

import com.postech.restaurantes.infrastructure.persistence.AuditableJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.address.AddressJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Tabela {@code users}. É uma classe <strong>separada</strong> de
 * {@link com.postech.restaurantes.domain.entity.user.User}: anotar a entidade de domínio
 * acoplaria as regras de negócio ao ciclo de vida do Hibernate, e o ORM é um detalhe.
 * Aqui não há invariante nenhuma — só mapeamento.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String login;

    /** Hash da senha. A coluna se chama {@code password} por convenção do schema. */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<RoleJpaEntity> roles = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AddressJpaEntity> addresses = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<RoleJpaEntity> getRoles() {
        return roles;
    }

    public List<AddressJpaEntity> getAddresses() {
        return addresses;
    }

    /** Troca o vínculo N:M inteiro, mantendo a mesma coleção gerenciada pelo Hibernate. */
    public void replaceRoles(Set<RoleJpaEntity> newRoles) {
        roles.clear();
        roles.addAll(newRoles);
    }

    /**
     * Troca a coleção de endereços inteira. Com {@code orphanRemoval = true}, os que saem
     * da lista são apagados na descarga da transação, sem DELETE explícito.
     */
    public void replaceAddresses(List<AddressJpaEntity> newAddresses) {
        addresses.clear();
        newAddresses.forEach(this::addAddress);
    }

    /** Mantém os dois lados da associação coerentes: a FK mora no endereço. */
    public void addAddress(AddressJpaEntity address) {
        address.setUser(this);
        addresses.add(address);
    }
}
