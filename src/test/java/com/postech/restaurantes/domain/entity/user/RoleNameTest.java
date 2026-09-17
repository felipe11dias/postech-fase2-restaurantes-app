package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RoleNameTest {

    @Test
    @DisplayName("Converte o nome textual, sem diferenciar maiúsculas")
    void deveConverterQuandoNomeConhecido() {
        assertEquals(RoleName.ROLE_OWNER, RoleName.from("ROLE_OWNER"));
        assertEquals(RoleName.ROLE_CUSTOMER, RoleName.from(" role_customer "));
        assertEquals(RoleName.ROLE_ADMIN, RoleName.from("Role_Admin"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "ROLE_GERENTE", "ADMIN"})
    @DisplayName("Recusa nome nulo, em branco ou desconhecido")
    void deveRecusarQuandoNomeDesconhecido(String name) {
        assertThrows(IllegalArgumentException.class, () -> RoleName.from(name));
    }

    @Test
    @DisplayName("Só ROLE_ADMIN é privilegiado")
    void deveIdentificarPapelPrivilegiado() {
        assertTrue(RoleName.ROLE_ADMIN.isPrivileged());
        assertFalse(RoleName.ROLE_OWNER.isPrivileged());
        assertFalse(RoleName.ROLE_CUSTOMER.isPrivileged());
    }
}
