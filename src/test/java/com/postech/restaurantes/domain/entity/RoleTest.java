package com.postech.restaurantes.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    @DisplayName("Cria papel novo sem id")
    void deveCriarSemId() {
        Role role = Role.create(RoleName.ROLE_CUSTOMER);

        assertNull(role.getId());
        assertEquals(RoleName.ROLE_CUSTOMER, role.getName());
        assertEquals("ROLE_CUSTOMER", role.toString());
    }

    @Test
    @DisplayName("Restaura papel com id conhecido")
    void deveRestaurarComId() {
        UUID id = UUID.randomUUID();

        Role role = Role.restore(id, RoleName.ROLE_ADMIN);

        assertEquals(id, role.getId());
        assertTrue(role.isPrivileged());
    }

    @Test
    @DisplayName("Recusa papel sem nome")
    void deveRecusarQuandoNomeNulo() {
        assertThrows(IllegalArgumentException.class, () -> Role.create(null));
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> Role.restore(null, RoleName.ROLE_OWNER));
    }

    @Test
    @DisplayName("Papéis com o mesmo nome são iguais, tenham ou não id")
    void deveSerIgualQuandoMesmoNome() {
        Role semId = Role.create(RoleName.ROLE_OWNER);
        Role comId = Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER);

        assertEquals(semId, comId);
        assertEquals(semId.hashCode(), comId.hashCode());
        assertEquals(1, Set.of(semId, Role.create(RoleName.ROLE_CUSTOMER)).stream()
                .filter(r -> r.equals(comId)).count());
    }

    @Test
    @DisplayName("Papéis com nomes diferentes não são iguais")
    void naoDeveSerIgualQuandoNomeDiferente() {
        assertNotEquals(Role.create(RoleName.ROLE_OWNER), Role.create(RoleName.ROLE_CUSTOMER));
    }

    @Test
    @DisplayName("Papel não é igual a nulo nem a objeto de outro tipo")
    void naoDeveSerIgualAOutroTipo() {
        Role role = Role.create(RoleName.ROLE_OWNER);

        assertFalse(role.equals(null));
        assertFalse(role.equals("ROLE_OWNER"));
    }

    @Test
    @DisplayName("Só ROLE_ADMIN é privilegiado")
    void deveDelegarPrivilegioAoNome() {
        assertFalse(Role.create(RoleName.ROLE_CUSTOMER).isPrivileged());
        assertTrue(Role.create(RoleName.ROLE_ADMIN).isPrivileged());
    }
}
