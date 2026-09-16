package com.postech.restaurantes.domain.entity;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.vo.Email;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Raiz do agregado de usuário. Não existe instância inválida: {@link #create} e
 * {@link #restore} passam pela mesma validação, e cada setter revalida o campo que altera.
 *
 * <p>Invariantes: nome, login e hash de senha não vazios; e-mail válido e normalizado;
 * ao menos um papel; endereços válidos (a lista pode ser vazia).
 */
public final class User {

    private final UUID id;
    private String name;
    private Email email;
    private String login;
    private String passwordHash;
    private final Set<Role> roles = new LinkedHashSet<>();
    private final List<Address> addresses = new ArrayList<>();
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUpdatedAt;

    private User(UUID id, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /** Usuário novo, ainda sem id nem auditoria. */
    public static User create(String name, String email, String login, String passwordHash,
                              Set<Role> roles, List<Address> addresses) {
        return fill(new User(null, null, null), name, email, login, passwordHash, roles, addresses);
    }

    /** Usuário reconstruído a partir da origem de dados, com id e auditoria conhecidos. */
    public static User restore(UUID id, String name, String email, String login, String passwordHash,
                               Set<Role> roles, List<Address> addresses,
                               LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        User user = new User(Guard.requireNonNull(id, "Id do usuário inválido"), createdAt, lastUpdatedAt);
        return fill(user, name, email, login, passwordHash, roles, addresses);
    }

    private static User fill(User user, String name, String email, String login, String passwordHash,
                             Set<Role> roles, List<Address> addresses) {
        user.setName(name);
        user.setEmail(Email.of(email));
        user.setLogin(login);
        user.changePasswordHash(passwordHash);
        user.replaceRoles(roles);
        user.replaceAddresses(addresses);
        return user;
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome inválido");
    }

    public void setEmail(Email email) {
        this.email = Guard.requireNonNull(email, "E-mail inválido");
    }

    public void setLogin(String login) {
        this.login = Guard.requireNonBlank(login, "Login inválido");
    }

    /** Recebe o hash já calculado: o domínio não conhece o algoritmo de hashing. */
    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Guard.requireNonBlank(passwordHash, "Hash de senha inválido");
    }

    public void replaceRoles(Set<Role> newRoles) {
        Guard.require(newRoles != null && !newRoles.isEmpty(), "Usuário deve ter ao menos um papel");
        Guard.require(newRoles.stream().noneMatch(Objects::isNull), "Papel inválido");
        roles.clear();
        roles.addAll(newRoles);
    }

    public void replaceAddresses(List<Address> newAddresses) {
        Guard.requireNonNull(newAddresses, "Lista de endereços inválida");
        Guard.require(newAddresses.stream().noneMatch(Objects::isNull), "Endereço inválido");
        addresses.clear();
        addresses.addAll(newAddresses);
    }

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(role -> role.getName() == roleName);
    }

    public boolean isAdmin() {
        return hasRole(RoleName.ROLE_ADMIN);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public List<Address> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
