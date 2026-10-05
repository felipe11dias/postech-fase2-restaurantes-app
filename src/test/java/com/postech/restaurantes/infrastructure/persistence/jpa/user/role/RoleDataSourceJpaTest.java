package com.postech.restaurantes.infrastructure.persistence.jpa.user.role;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.roleEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.RoleData;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleDataSourceJpaTest {

    private final SpringDataRoleRepository repository = mock(SpringDataRoleRepository.class);
    private final RoleDataSourceJpa dataSource = new RoleDataSourceJpa(repository);

    @Test
    @DisplayName("Traduz os papéis encontrados para registros do adaptador")
    void deveTraduzirOsEncontrados() {
        when(repository.findByNameIn(Set.of("ROLE_CUSTOMER"))).thenReturn(Set.of(roleEntity()));

        Set<RoleData> roles = dataSource.findByNames(Set.of("ROLE_CUSTOMER"));

        assertEquals(1, roles.size());
        assertEquals(ROLE_ID, roles.iterator().next().id());
        assertEquals("ROLE_CUSTOMER", roles.iterator().next().name());
    }

    @Test
    @DisplayName("Nome sem correspondência no catálogo devolve conjunto vazio")
    void deveDevolverVazioQuandoNaoEncontra() {
        when(repository.findByNameIn(any())).thenReturn(Set.of());

        assertTrue(dataSource.findByNames(Set.of("ROLE_ADMIN")).isEmpty());
    }
}
