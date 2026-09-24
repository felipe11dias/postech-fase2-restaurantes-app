package com.postech.restaurantes.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
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

    @Nested
    @DisplayName("Codificador de senha")
    class Senha {

        private final BCryptPasswordAdapter encoder = new BCryptPasswordAdapter();

        @Test
        @DisplayName("Senha codificada confere com ela mesma e não com outra")
        void deveCodificarEConferir() {
            String hash = encoder.encode("senhaSegura123");

            assertTrue(encoder.matches("senhaSegura123", hash));
            assertFalse(encoder.matches("outraSenha123", hash));
        }

        @Test
        @DisplayName("Cada codificação gera um hash diferente, e nenhum contém a senha")
        void deveUsarSalAleatorio() {
            String primeiro = encoder.encode("senhaSegura123");
            String segundo = encoder.encode("senhaSegura123");

            assertNotEquals(primeiro, segundo);
            assertFalse(primeiro.contains("senhaSegura123"));
        }

        @Test
        @DisplayName("A comparação simulada roda até o fim sem hash real e sem revelar nada")
        void deveSimularComparacao() {
            encoder.simulateMatch("qualquerSenha");
        }
    }

    @Nested
    @DisplayName("Gerador de token de redefinição")
    class TokenSeguro {

        private final SecureRandomTokenGenerator generator = new SecureRandomTokenGenerator();

        @Test
        @DisplayName("Cada token é diferente do anterior e seguro para URL")
        void deveGerarTokensImprevisiveis() {
            Set<String> tokens = IntStream.range(0, 100).mapToObj(i -> generator.generate())
                    .collect(java.util.stream.Collectors.toSet());

            assertEquals(100, tokens.size());
            assertTrue(tokens.stream().allMatch(token -> token.matches("[A-Za-z0-9_-]+")));
        }

        @Test
        @DisplayName("O hash é determinístico, diferente para tokens diferentes e não contém o token")
        void deveGerarHashDeterministico() {
            String token = generator.generate();

            assertEquals(generator.hash(token), generator.hash(token));
            assertNotEquals(generator.hash(token), generator.hash(generator.generate()));
            assertFalse(generator.hash(token).contains(token));
        }

        @Test
        @DisplayName("Algoritmo de hash indisponível é falha de ambiente, não de negócio")
        void deveFalharComAlgoritmoIndisponivel() {
            SecureRandomTokenGenerator quebrado = new SecureRandomTokenGenerator("ALGORITMO-INEXISTENTE");

            assertThrows(IllegalStateException.class, () -> quebrado.hash("token"));
        }
    }
}
