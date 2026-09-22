package com.postech.restaurantes.infrastructure.persistence.user;

import com.postech.restaurantes.adapter.datasource.IRoleDataSource;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Implementação JPA da origem de dados de papéis: consulta o catálogo criado por migration. */
@Repository
public class RoleDataSourceJpa implements IRoleDataSource {

    private final SpringDataRoleRepository roles;

    public RoleDataSourceJpa(SpringDataRoleRepository roles) {
        this.roles = roles;
    }

    /** Devolve só o que existe; papel pedido e não encontrado é decisão do caso de uso. */
    @Override
    @Transactional(readOnly = true)
    public Set<RoleData> findByNames(Set<String> names) {
        return roles.findByNameIn(names).stream()
                .map(role -> new RoleData(role.getId(), role.getName()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
