package com.postech.restaurantes.infrastructure.persistence;

import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.HASH;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.TOKEN_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.addressEntity;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.roleEntity;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.tokenEntity;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.userEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.infrastructure.persistence.address.AddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.user.PasswordResetTokenJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.user.RoleJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.user.UserJpaEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * As entidades JPA não têm regra de negócio — só mapeamento. O que se verifica aqui é o
 * que o mapeamento promete: os dados entram e saem íntegros e a troca de coleções mantém os
 * dois lados da associação coerentes.
 */
class JpaEntitiesTest {

    @Nested
    @DisplayName("Usuário")
    class Usuario {

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
        @DisplayName("Substituir os endereços descarta os anteriores e religa o dono de cada novo")
        void deveSubstituirEnderecos() {
            UserJpaEntity user = userEntity();
            AddressJpaEntity novo = addressEntity();
            novo.setId(null);

            user.replaceAddresses(List.of(novo));

            assertEquals(1, user.getAddresses().size());
            assertSame(user, user.getAddresses().get(0).getUser());
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

    @Nested
    @DisplayName("Papel")
    class Papel {

        @Test
        @DisplayName("Guarda e devolve id e nome")
        void deveGuardarOsCampos() {
            RoleJpaEntity role = roleEntity();

            assertEquals(ROLE_ID, role.getId());
            assertEquals("ROLE_CUSTOMER", role.getName());
        }
    }

    @Nested
    @DisplayName("Endereço")
    class Endereco {

        @Test
        @DisplayName("Guarda e devolve todos os campos, com o CEP sem máscara")
        void deveGuardarOsCampos() {
            AddressJpaEntity address = addressEntity();

            assertEquals(ADDRESS_ID, address.getId());
            assertEquals("Rua das Flores", address.getStreet());
            assertEquals("100", address.getNumber());
            assertEquals("Apto 21", address.getComplement());
            assertEquals("Centro", address.getNeighborhood());
            assertEquals("São Paulo", address.getCity());
            assertEquals("SP", address.getState());
            assertEquals("01001000", address.getZipCode());
            assertNull(address.getUser());
        }
    }

    @Nested
    @DisplayName("Token de redefinição")
    class Token {

        @Test
        @DisplayName("Guarda o dono por identidade e só o hash do token")
        void deveGuardarOsCampos() {
            PasswordResetTokenJpaEntity token = tokenEntity();

            assertEquals(TOKEN_ID, token.getId());
            assertEquals(USER_ID, token.getUserId());
            assertEquals("hash-do-token", token.getTokenHash());
            assertEquals(NOW.plusMinutes(30), token.getExpiresAt());
            assertFalse(token.isUsed());
        }

        @Test
        @DisplayName("Marcar como usado é a única mudança de estado prevista")
        void deveMarcarUsado() {
            PasswordResetTokenJpaEntity token = tokenEntity();

            token.setUsed(true);

            assertTrue(token.isUsed());
        }
    }
}
