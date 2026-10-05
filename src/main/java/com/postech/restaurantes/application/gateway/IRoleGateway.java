package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.domain.entity.role.Role;
import com.postech.restaurantes.domain.entity.role.RoleName;
import java.util.Set;

/** Resolução de papéis persistidos a partir dos nomes. */
public interface IRoleGateway {

    /** Devolve apenas os papéis encontrados; cabe ao caso de uso conferir se faltou algum. */
    Set<Role> findByNames(Set<RoleName> names);
}
