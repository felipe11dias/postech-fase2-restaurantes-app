package com.postech.restaurantes.domain.entity.user;

import com.postech.restaurantes.domain.Guard;
import java.util.Objects;
import java.util.UUID;

/**
 * Papel de autorização. A identidade de negócio é o {@link RoleName}: dois papéis com o mesmo
 * nome são o mesmo papel, tenham ou não id, o que permite usá-los em conjuntos.
 */
public final class Role {

    private final UUID id;
    private final RoleName name;

    private Role(UUID id, RoleName name) {
        this.id = id;
        this.name = Guard.requireNonNull(name, "Papel inválido");
    }

    public static Role create(RoleName name) {
        return new Role(null, name);
    }

    public static Role restore(UUID id, RoleName name) {
        return new Role(Guard.requireNonNull(id, "Id do papel inválido"), name);
    }

    public UUID getId() {
        return id;
    }

    public RoleName getName() {
        return name;
    }

    public boolean isPrivileged() {
        return name.isPrivileged();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Role role && name == role.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name.name();
    }
}
