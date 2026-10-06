package com.postech.restaurantes.infrastructure.persistence.jpa.user.client;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.clientEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientJpaEntityTest {

    @Test
    @DisplayName("Guarda o perfil de cliente com o id do usuário")
    void deveGuardarOsCampos() {
        ClientJpaEntity client = clientEntity();

        assertEquals(USER_ID, client.getId());
        assertEquals("52998224725", client.getCpf());
        assertEquals("11912345678", client.getPhone());
        assertEquals(LocalDate.of(1990, 5, 20), client.getBirthDate());
    }

    @Test
    @DisplayName("Auditoria e data de nascimento começam vazias")
    void deveNascerSemAuditoria() {
        ClientJpaEntity client = new ClientJpaEntity();

        assertNull(client.getCreatedAt());
        assertNull(client.getBirthDate());
    }
}
