package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IRoleDataSource;
import com.postech.restaurantes.application.gateway.IRoleGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.RoleName;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Tradutor entre {@link Role}/{@link RoleName} e os nomes textuais da origem de dados. */
public final class RoleGateway implements IRoleGateway {

    private final IRoleDataSource dataSource;

    private RoleGateway(IRoleDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de papel inválida");
    }

    public static RoleGateway create(IRoleDataSource dataSource) {
        return new RoleGateway(dataSource);
    }

    @Override
    public Set<Role> findByNames(Set<RoleName> names) {
        Set<String> textual = names.stream().map(RoleName::name).collect(Collectors.toSet());
        return dataSource.findByNames(textual).stream()
                .map(data -> Role.restore(data.id(), RoleName.from(data.name())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
