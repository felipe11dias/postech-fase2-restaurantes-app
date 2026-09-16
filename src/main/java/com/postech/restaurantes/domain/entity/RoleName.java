package com.postech.restaurantes.domain.entity;

import com.postech.restaurantes.domain.Guard;

/** Papéis de autorização reconhecidos pelo sistema. */
public enum RoleName {
    ROLE_OWNER,
    ROLE_CUSTOMER,
    ROLE_ADMIN;

    /** Converte o nome textual, recusando valores desconhecidos com mensagem de domínio. */
    public static RoleName from(String name) {
        String normalized = Guard.requireNonBlank(name, "Papel inválido").toUpperCase();
        for (RoleName candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Papel inválido: " + name);
    }

    /** Papel que nunca pode ser obtido por autocadastro público. */
    public boolean isPrivileged() {
        return this == ROLE_ADMIN;
    }
}
