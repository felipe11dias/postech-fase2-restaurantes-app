package com.postech.restaurantes.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.domain.vo.Email;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

    private static final Set<Role> CUSTOMER = Set.of(Role.create(RoleName.ROLE_CUSTOMER));
    private static final Address ADDRESS =
            Address.create("Rua das Flores", "100", null, "Centro", "São Paulo", "SP", "01001000");

    private static User valido() {
        return User.create("João Silva", "Joao.Silva@Email.com", "joao.silva", "$2a$hash", CUSTOMER, List.of(ADDRESS));
    }

    @Test
    @DisplayName("Cria usuário válido com e-mail normalizado, sem id nem auditoria")
    void deveCriarQuandoValido() {
        User user = valido();

        assertNull(user.getId());
        assertNull(user.getCreatedAt());
        assertNull(user.getLastUpdatedAt());
        assertEquals("João Silva", user.getName());
        assertEquals(Email.of("joao.silva@email.com"), user.getEmail());
        assertEquals("joao.silva", user.getLogin());
        assertEquals("$2a$hash", user.getPasswordHash());
        assertEquals(CUSTOMER, user.getRoles());
        assertEquals(List.of(ADDRESS), user.getAddresses());
    }

    @Test
    @DisplayName("Restaura usuário com id e auditoria conhecidos")
    void deveRestaurarComIdEAuditoria() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime alterado = LocalDateTime.of(2026, 1, 2, 10, 0);

        User user = User.restore(id, "Ana", "ana@x.com", "ana", "hash", CUSTOMER, List.of(), criado, alterado);

        assertEquals(id, user.getId());
        assertEquals(criado, user.getCreatedAt());
        assertEquals(alterado, user.getLastUpdatedAt());
        assertTrue(user.getAddresses().isEmpty());
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> User.restore(null, "Ana", "ana@x.com", "ana", "hash", CUSTOMER, List.of(), null, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa nome em branco")
    void deveRecusarNomeEmBranco(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> User.create(name, "a@x.com", "login", "hash", CUSTOMER, List.of()));
    }

    @Test
    @DisplayName("Recusa e-mail inválido")
    void deveRecusarEmailInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "invalido", "login", "hash", CUSTOMER, List.of()));
    }

    @Test
    @DisplayName("Recusa e-mail nulo ao alterar")
    void deveRecusarEmailNuloAoAlterar() {
        User user = valido();

        assertThrows(IllegalArgumentException.class, () -> user.setEmail(null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa login em branco")
    void deveRecusarLoginEmBranco(String login) {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", login, "hash", CUSTOMER, List.of()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa hash de senha em branco")
    void deveRecusarHashEmBranco(String hash) {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", hash, CUSTOMER, List.of()));
    }

    @Test
    @DisplayName("Recusa usuário sem papéis (conjunto nulo)")
    void deveRecusarPapeisNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", null, List.of()));
    }

    @Test
    @DisplayName("Recusa usuário sem papéis (conjunto vazio)")
    void deveRecusarPapeisVazios() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", Set.of(), List.of()));
    }

    @Test
    @DisplayName("Recusa papel nulo dentro do conjunto")
    void deveRecusarPapelNuloNoConjunto() {
        Set<Role> comNulo = new HashSet<>(Arrays.asList(Role.create(RoleName.ROLE_OWNER), null));

        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", comNulo, List.of()));
    }

    @Test
    @DisplayName("Recusa lista de endereços nula")
    void deveRecusarEnderecosNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", CUSTOMER, null));
    }

    @Test
    @DisplayName("Recusa endereço nulo dentro da lista")
    void deveRecusarEnderecoNuloNaLista() {
        List<Address> comNulo = new ArrayList<>();
        comNulo.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", CUSTOMER, comNulo));
    }

    @Test
    @DisplayName("Substitui os endereços por completo")
    void deveSubstituirEnderecos() {
        User user = valido();
        Address novo = Address.create("Av. B", null, null, null, "Rio", "RJ", "20000000");

        user.replaceAddresses(List.of(novo, novo));

        assertEquals(List.of(novo, novo), user.getAddresses());
    }

    @Test
    @DisplayName("Substitui os papéis por completo e responde hasRole/isAdmin")
    void deveSubstituirPapeis() {
        User user = valido();
        assertFalse(user.isAdmin());

        user.replaceRoles(Set.of(Role.create(RoleName.ROLE_OWNER), Role.create(RoleName.ROLE_ADMIN)));

        assertTrue(user.hasRole(RoleName.ROLE_OWNER));
        assertTrue(user.isAdmin());
        assertFalse(user.hasRole(RoleName.ROLE_CUSTOMER));
    }

    @Test
    @DisplayName("Coleções expostas são somente leitura")
    void deveExporColecoesImutaveis() {
        User user = valido();

        assertThrows(UnsupportedOperationException.class, () -> user.getRoles().clear());
        assertThrows(UnsupportedOperationException.class, () -> user.getAddresses().clear());
    }

    @Test
    @DisplayName("Setters válidos alteram o estado; inválidos não corrompem")
    void deveRevalidarNosSetters() {
        User user = valido();

        user.setName("Ana");
        user.setLogin("ana");
        user.changePasswordHash("novoHash");
        user.setEmail(Email.of("ana@x.com"));
        assertThrows(IllegalArgumentException.class, () -> user.setName(""));

        assertEquals("Ana", user.getName());
        assertEquals("ana", user.getLogin());
        assertEquals("novoHash", user.getPasswordHash());
        assertEquals("ana@x.com", user.getEmail().value());
    }
}
