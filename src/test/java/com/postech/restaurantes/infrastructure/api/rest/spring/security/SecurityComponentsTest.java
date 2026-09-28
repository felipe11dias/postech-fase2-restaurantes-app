package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class SecurityComponentsTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("Portador do token")
    class Portador {

        @Test
        @DisplayName("O nome do principal é o login, que é o que a auditoria grava")
        void deveExporOLoginComoNome() {
            AuthenticatedUser user = new AuthenticatedUser(USER_ID, "joao.silva", Set.of("ROLE_CUSTOMER"));

            assertEquals("joao.silva", user.getName());
            assertEquals(USER_ID, user.id());
        }

        @Test
        @DisplayName("Os papéis ficam imutáveis")
        void deveCopiarOsPapeis() {
            Set<String> originais = new java.util.HashSet<>(Set.of("ROLE_CUSTOMER"));
            AuthenticatedUser user = new AuthenticatedUser(USER_ID, "joao.silva", originais);

            originais.add("ROLE_ADMIN");

            assertEquals(Set.of("ROLE_CUSTOMER"), user.roles());
        }
    }

    @Nested
    @DisplayName("Regra de posse")
    class Posse {

        private final UserSecurity userSecurity = new UserSecurity();

        private static UsernamePasswordAuthenticationToken autenticado(UUID id) {
            return new UsernamePasswordAuthenticationToken(
                    new AuthenticatedUser(id, "joao.silva", Set.of("ROLE_CUSTOMER")), null,
                    java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        }

        @Test
        @DisplayName("O próprio dono passa")
        void devePermitirODono() {
            assertTrue(userSecurity.isSelf(USER_ID, autenticado(USER_ID)));
        }

        @Test
        @DisplayName("Outro usuário não passa, mesmo conhecendo o id")
        void deveRecusarOutroUsuario() {
            assertFalse(userSecurity.isSelf(USER_ID, autenticado(UUID.randomUUID())));
        }

        @Test
        @DisplayName("Sem autenticação, sem id ou com principal de outro tipo, não passa")
        void deveRecusarContextoIncompleto() {
            assertFalse(userSecurity.isSelf(USER_ID, null));
            assertFalse(userSecurity.isSelf(null, autenticado(USER_ID)));
            assertFalse(userSecurity.isSelf(USER_ID, new AnonymousAuthenticationToken("chave", "anonymousUser",
                    java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))));
        }
    }
}
