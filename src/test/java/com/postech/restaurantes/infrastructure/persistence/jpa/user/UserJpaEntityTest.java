package com.postech.restaurantes.infrastructure.persistence.jpa.user;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.HASH;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.userAddressEntity;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.userEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.infrastructure.persistence.jpa.user.address.UserAddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.role.RoleJpaEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As entidades JPA não têm regra de negócio — só mapeamento. O que se verifica aqui é o
 * que o mapeamento promete: os dados entram e saem íntegros e a troca de coleções substitui o
 * conteúdo inteiro. As partes do agregado têm o teste no próprio subpacote.
 */
class UserJpaEntityTest {

    @Test
    @DisplayName("Guarda e devolve todos os campos mapeados, inclusive a auditoria")
    void deveGuardarOsCampos() {
        UserJpaEntity user = userEntity();

        assertEquals(USER_ID, user.getId());
        assertEquals("João Silva", user.getName());
        assertEquals("joao.silva@email.com", user.getEmail());
        assertEquals("joao.silva", user.getLogin());
        assertEquals(HASH, user.getPassword());
        assertEquals(NOW.minusDays(1), user.getCreatedAt());
        assertEquals(NOW, user.getLastUpdatedAt());
        assertEquals(1, user.getRoles().size());
        assertEquals(1, user.getAddresses().size());
    }

    @Test
    @DisplayName("Autor da auditoria começa vazio: quem preenche é o listener do Spring Data")
    void deveNascerSemAutor() {
        UserJpaEntity user = userEntity();

        assertNull(user.getCreatedBy());
        assertNull(user.getLastUpdatedBy());
    }

    @Test
    @DisplayName("Substituir os papéis descarta os anteriores")
    void deveSubstituirPapeis() {
        UserJpaEntity user = userEntity();
        RoleJpaEntity admin = new RoleJpaEntity();
        admin.setId(UUID.randomUUID());
        admin.setName("ROLE_ADMIN");

        user.replaceRoles(Set.of(admin));

        assertEquals(1, user.getRoles().size());
        assertEquals("ROLE_ADMIN", user.getRoles().iterator().next().getName());
    }

    @Test
    @DisplayName("Substituir os endereços descarta os anteriores e mantém só os novos")
    void deveSubstituirEnderecos() {
        UserJpaEntity user = userEntity();
        UserAddressJpaEntity novo = userAddressEntity();
        novo.setId(null);

        user.replaceAddresses(List.of(novo));

        assertEquals(1, user.getAddresses().size());
        assertSame(novo, user.getAddresses().get(0));
        assertNull(user.getAddresses().get(0).getId());
    }

    @Test
    @DisplayName("Lista de endereços vazia deixa o usuário sem nenhum")
    void deveAceitarListaVazia() {
        UserJpaEntity user = userEntity();

        user.replaceAddresses(List.of());

        assertTrue(user.getAddresses().isEmpty());
    }
}
