package com.postech.restaurantes.infrastructure.persistence.user;

import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.TOKEN_DATA;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.TOKEN_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.roleEntity;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.tokenEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RoleAndTokenDataSourcesJpaTest {

    @Nested
    @DisplayName("Origem de dados de papéis")
    class Papeis {

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

    @Nested
    @DisplayName("Origem de dados de tokens de redefinição")
    class Tokens {

        private final SpringDataPasswordResetTokenRepository repository =
                mock(SpringDataPasswordResetTokenRepository.class);
        private final PasswordResetTokenDataSourceJpa dataSource = new PasswordResetTokenDataSourceJpa(repository);

        @Test
        @DisplayName("Busca pelo hash traduz o registro; ausência vira vazio")
        void deveBuscarPeloHash() {
            when(repository.findByTokenHash("hash-do-token")).thenReturn(Optional.of(tokenEntity()));
            when(repository.findByTokenHash("outro")).thenReturn(Optional.empty());

            PasswordResetTokenData data = dataSource.findByTokenHash("hash-do-token").orElseThrow();

            assertEquals(TOKEN_ID, data.id());
            assertEquals(USER_ID, data.userId());
            assertEquals("hash-do-token", data.tokenHash());
            assertEquals(NOW.plusMinutes(30), data.expiresAt());
            assertFalse(data.used());
            assertTrue(dataSource.findByTokenHash("outro").isEmpty());
        }

        @Test
        @DisplayName("Inserção grava uma linha nova, sem id, e devolve o registro emitido")
        void deveInserir() {
            when(repository.save(any())).thenReturn(tokenEntity());

            PasswordResetTokenData emitido = dataSource.insert(
                    new PasswordResetTokenData(null, USER_ID, "hash-do-token", NOW.plusMinutes(30), false));

            ArgumentCaptor<PasswordResetTokenJpaEntity> captor =
                    ArgumentCaptor.forClass(PasswordResetTokenJpaEntity.class);
            verify(repository).save(captor.capture());
            assertNull(captor.getValue().getId());
            assertEquals(USER_ID, captor.getValue().getUserId());
            assertEquals("hash-do-token", captor.getValue().getTokenHash());
            assertFalse(captor.getValue().isUsed());
            assertEquals(TOKEN_ID, emitido.id());
        }

        @Test
        @DisplayName("Atualização grava apenas o consumo do token")
        void deveAtualizarApenasOConsumo() {
            PasswordResetTokenJpaEntity existente = tokenEntity();
            when(repository.findById(TOKEN_ID)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            PasswordResetTokenData atualizado = dataSource.update(
                    new PasswordResetTokenData(TOKEN_ID, USER_ID, "outro-hash", NOW.plusYears(1), true));

            assertTrue(existente.isUsed());
            assertEquals("hash-do-token", existente.getTokenHash());
            assertEquals(NOW.plusMinutes(30), existente.getExpiresAt());
            assertTrue(atualizado.used());
        }

        @Test
        @DisplayName("Atualizar token inexistente é falha de estado")
        void deveRecusarTokenInexistente() {
            when(repository.findById(TOKEN_ID)).thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class, () -> dataSource.update(TOKEN_DATA));
        }
    }
}
