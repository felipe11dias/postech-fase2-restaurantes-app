package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.vo.Email;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import com.postech.restaurantes.domain.entity.address.Address;

class UserTest {

    private static final UserProfiles CLIENTE =
            new UserProfiles(null, ClientProfile.restore("52998224725", "11912345678", null), null, null);
    private static final UserAddress ADDRESS = UserAddress.create("Casa", true,
            Address.create("Rua das Flores", "100", null, "Centro", "São Paulo", "SP", "01001000"));
    private static final Address OUTRO_ENDERECO = Address.create("Av. B", null, null, null, "Rio", "RJ", "20000000");

    private static User valido() {
        return User.create("João Silva", "Joao.Silva@Email.com", "joao.silva", "$2a$hash", CLIENTE, List.of(ADDRESS));
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
        assertEquals(Set.of(RoleName.ROLE_CLIENT), user.getRoles());
        assertEquals(CLIENTE, user.getProfiles());
        assertEquals(List.of(ADDRESS), user.getAddresses());
    }

    @Test
    @DisplayName("Restaura usuário com id e auditoria conhecidos")
    void deveRestaurarComIdEAuditoria() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime alterado = LocalDateTime.of(2026, 1, 2, 10, 0);

        User user = User.restore(id, "Ana", "ana@x.com", "ana", "hash", CLIENTE, List.of(), criado, alterado);

        assertEquals(id, user.getId());
        assertEquals(criado, user.getCreatedAt());
        assertEquals(alterado, user.getLastUpdatedAt());
        assertTrue(user.getAddresses().isEmpty());
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> User.restore(null, "Ana", "ana@x.com", "ana", "hash", CLIENTE, List.of(), null, null));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa nome em branco")
    void deveRecusarNomeEmBranco(String name) {
        assertThrows(IllegalArgumentException.class,
                () -> User.create(name, "a@x.com", "login", "hash", CLIENTE, List.of()));
    }

    @Test
    @DisplayName("Recusa e-mail inválido")
    void deveRecusarEmailInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "invalido", "login", "hash", CLIENTE, List.of()));
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
                () -> User.create("Ana", "a@x.com", login, "hash", CLIENTE, List.of()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa hash de senha em branco")
    void deveRecusarHashEmBranco(String hash) {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", hash, CLIENTE, List.of()));
    }

    @Test
    @DisplayName("Recusa usuário sem papéis (conjunto nulo)")
    void deveRecusarPapeisNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", null, List.of()));
    }

    @Test
    @DisplayName("Recusa usuário sem o conjunto de perfis")
    void deveRecusarPerfisNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", null, List.of()));
    }

    @Test
    @DisplayName("Recusa lista de endereços nula")
    void deveRecusarEnderecosNulos() {
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", CLIENTE, null));
    }

    @Test
    @DisplayName("Recusa endereço nulo dentro da lista")
    void deveRecusarEnderecoNuloNaLista() {
        List<UserAddress> comNulo = new ArrayList<>();
        comNulo.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", CLIENTE, comNulo));
    }

    @Test
    @DisplayName("Substitui os endereços por completo")
    void deveSubstituirEnderecos() {
        User user = valido();
        UserAddress padrao = UserAddress.create("Trabalho", true, OUTRO_ENDERECO);
        UserAddress outro = UserAddress.create(null, false, OUTRO_ENDERECO);

        user.replaceAddresses(List.of(padrao, outro));

        assertEquals(List.of(padrao, outro), user.getAddresses());
    }

    @Test
    @DisplayName("Recusa endereços sem nenhum padrão, sem alterar os atuais")
    void deveRecusarEnderecosSemPadrao() {
        User user = valido();
        List<UserAddress> semPadrao = List.of(UserAddress.create(null, false, OUTRO_ENDERECO));

        assertThrows(IllegalArgumentException.class, () -> user.replaceAddresses(semPadrao));
        assertEquals(List.of(ADDRESS), user.getAddresses());
    }

    @Test
    @DisplayName("Endereço com id que o usuário já tem é mantido; sem id, entra como novo")
    void deveManterEnderecoPeloId() {
        UUID idDoEndereco = UUID.randomUUID();
        User user = User.restore(UUID.randomUUID(), "Ana", "ana@x.com", "ana", "hash", CLIENTE,
                List.of(UserAddress.restore(idDoEndereco, "Casa", true, OUTRO_ENDERECO)), null, null);
        UserAddress mantido = UserAddress.restore(idDoEndereco, "Casa Nova", true, OUTRO_ENDERECO);
        UserAddress novo = UserAddress.create("Trabalho", false, OUTRO_ENDERECO);

        user.replaceAddresses(List.of(mantido, novo));

        assertEquals(List.of(mantido, novo), user.getAddresses());
    }

    @Test
    @DisplayName("Recusa id de endereço que não é do usuário — no cadastro e na troca —, sem alterar os atuais")
    void deveRecusarEnderecoDeOutroUsuario() {
        User user = valido();
        List<UserAddress> alheio = List.of(UserAddress.restore(UUID.randomUUID(), "Casa", true, OUTRO_ENDERECO));

        assertThrows(IllegalArgumentException.class, () -> user.replaceAddresses(alheio));
        assertThrows(IllegalArgumentException.class,
                () -> User.create("Ana", "a@x.com", "login", "hash", CLIENTE, alheio));
        assertEquals(List.of(ADDRESS), user.getAddresses());
    }

    @Test
    @DisplayName("Recusa o mesmo endereço do usuário duas vezes na lista")
    void deveRecusarEnderecoRepetido() {
        UUID idDoEndereco = UUID.randomUUID();
        User user = User.restore(UUID.randomUUID(), "Ana", "ana@x.com", "ana", "hash", CLIENTE,
                List.of(UserAddress.restore(idDoEndereco, "Casa", true, OUTRO_ENDERECO)), null, null);
        List<UserAddress> repetido = List.of(UserAddress.restore(idDoEndereco, null, true, OUTRO_ENDERECO),
                UserAddress.restore(idDoEndereco, null, false, OUTRO_ENDERECO));

        assertThrows(IllegalArgumentException.class, () -> user.replaceAddresses(repetido));
    }

    @Test
    @DisplayName("Recusa mais de um endereço padrão")
    void deveRecusarDoisEnderecosPadrao() {
        User user = valido();
        List<UserAddress> doisPadroes = List.of(UserAddress.create(null, true, OUTRO_ENDERECO),
                UserAddress.create(null, true, OUTRO_ENDERECO));

        assertThrows(IllegalArgumentException.class, () -> user.replaceAddresses(doisPadroes));
    }

    @Test
    @DisplayName("Substitui os perfis por completo, e os papéis acompanham: hasRole/isAdmin")
    void deveSubstituirPerfis() {
        User user = valido();
        assertFalse(user.isAdmin());

        user.replaceProfiles(new UserProfiles(
                OwnerProfile.create("11222333000181", "Sabor Ltda", "1131234567"), null, null,
                AdminProfile.create("ADM-1", null, false)));

        assertTrue(user.hasRole(RoleName.ROLE_OWNER));
        assertTrue(user.isAdmin());
        assertFalse(user.hasRole(RoleName.ROLE_CLIENT));
        assertThrows(IllegalArgumentException.class, () -> user.replaceProfiles(null));
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
