package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.RoleData;
import java.util.Set;

/** Origem de dados de papéis. */
public interface IRoleDataSource {

    /** Devolve apenas os papéis encontrados. */
    Set<RoleData> findByNames(Set<String> names);
}
