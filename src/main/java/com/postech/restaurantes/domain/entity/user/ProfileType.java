package com.postech.restaurantes.domain.entity.user;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.InvariantViolationException;

/**
 * Os tipos de perfil do usuário — as especializações {@code owners}, {@code clients}, {@code couriers}
 * e {@code admins}. Identifica o perfil que uma operação inclui, altera ou remove; a descrição é o
 * nome do perfil nas mensagens ao usuário.
 */
public enum ProfileType {
    OWNER("dono"),
    CLIENT("cliente"),
    COURIER("entregador"),
    ADMIN("administrador");

    private final String description;

    ProfileType(String description) {
        this.description = description;
    }

    /** Converte o nome textual sem diferenciar maiúsculas, recusando valores desconhecidos com mensagem de domínio. */
    public static ProfileType from(String name) {
        String normalized = Guard.requireNonBlank(name, "Tipo de perfil inválido").toUpperCase();
        for (ProfileType candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new InvariantViolationException("Tipo de perfil inválido: " + name);
    }

    public String description() {
        return description;
    }
}
