package com.postech.restaurantes.domain.entity.user;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.vo.Email;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Raiz do agregado de usuário. Não existe instância inválida: {@link #create} e
 * {@link #restore} passam pela mesma validação, e cada setter revalida o campo que altera.
 *
 * <p>Invariantes: nome, login e hash de senha não vazios; e-mail válido e normalizado;
 * ao menos um perfil (ver {@link UserProfiles}, que também dá os papéis); endereços válidos (a
 * lista pode ser vazia) e, havendo endereços, exatamente um deles é o padrão; endereço com id só se
 * for um dos que o usuário já tem.
 */
public final class User {

    private final UUID id;
    private String name;
    private Email email;
    private String login;
    private String passwordHash;
    private UserProfiles profiles;
    private final List<UserAddress> addresses = new ArrayList<>();
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUpdatedAt;

    private User(UUID id, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /** Usuário novo, ainda sem id nem auditoria. */
    public static User create(String name, String email, String login, String passwordHash,
                              UserProfiles profiles, List<UserAddress> addresses) {
        User user = fill(new User(null, null, null), name, email, login, passwordHash, profiles);
        user.replaceAddresses(addresses);
        return user;
    }

    /** Usuário reconstruído a partir da origem de dados, com id e auditoria conhecidos. */
    public static User restore(UUID id, String name, String email, String login, String passwordHash,
                               UserProfiles profiles, List<UserAddress> addresses,
                               LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        User user = new User(Guard.requireNonNull(id, "Id do usuário inválido"), createdAt, lastUpdatedAt);
        fill(user, name, email, login, passwordHash, profiles);
        user.setAddresses(addresses);
        return user;
    }

    private static User fill(User user, String name, String email, String login, String passwordHash,
                             UserProfiles profiles) {
        user.setName(name);
        user.setEmail(Email.of(email));
        user.setLogin(login);
        user.changePasswordHash(passwordHash);
        user.replaceProfiles(profiles);
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

    /** Troca o conjunto de perfis; as invariantes do conjunto são do próprio {@link UserProfiles}. */
    public void replaceProfiles(UserProfiles newProfiles) {
        this.profiles = Guard.requireNonNull(newProfiles, "Perfis do usuário inválidos");
    }

    /**
     * Troca a lista inteira. Lista vazia é válida (o usuário pode não ter endereço); havendo
     * endereços, um e só um é o padrão. Endereço com id é um dos que o usuário já tem e continua
     * sendo o mesmo (mantém o id); sem id, é novo; os que não vierem deixam de ser do usuário. Id
     * que não é de um endereço deste usuário é recusado — conhecer o id não dá posse dele.
     */
    public void replaceAddresses(List<UserAddress> newAddresses) {
        validateAddresses(newAddresses);
        Set<UUID> current = addresses.stream().map(UserAddress::getId).collect(Collectors.toSet());
        List<UUID> kept = newAddresses.stream().map(UserAddress::getId).filter(Objects::nonNull).toList();
        Guard.require(current.containsAll(kept), "Endereço do usuário não encontrado");
        Guard.require(Set.copyOf(kept).size() == kept.size(), "Endereço do usuário repetido");
        addresses.clear();
        addresses.addAll(newAddresses);
    }

    /** O que vem da origem de dados já é do usuário: só as invariantes da lista se aplicam. */
    private void setAddresses(List<UserAddress> restored) {
        validateAddresses(restored);
        addresses.clear();
        addresses.addAll(restored);
    }

    private static void validateAddresses(List<UserAddress> list) {
        Guard.requireNonNull(list, "Lista de endereços inválida");
        Guard.require(list.stream().noneMatch(Objects::isNull), "Endereço inválido");
        long defaults = list.stream().filter(UserAddress::isDefault).count();
        Guard.require(list.isEmpty() || defaults == 1, "Exatamente um endereço deve ser o padrão");
    }

    public boolean hasRole(RoleName roleName) {
        return getRoles().contains(roleName);
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

    public UserProfiles getProfiles() {
        return profiles;
    }

    /** Papéis derivados dos perfis. */
    public Set<RoleName> getRoles() {
        return profiles.roles();
    }

    public List<UserAddress> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
