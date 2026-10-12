package com.postech.restaurantes.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.IntegrationTestSupport;
import com.postech.restaurantes.adapter.datasource.IRoleDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.domain.entity.user.RoleName;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O contexto só sobe se o Flyway aplicar as migrations e o Hibernate validar o mapeamento
 * contra o schema resultante. Cada teste abaixo confere o que as migrations prometem.
 */
class SchemaMigrationIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IRoleDataSource roleDataSource;

    @Autowired
    private IUserDataSource userDataSource;

    @Test
    @DisplayName("As migrations foram aplicadas com sucesso e ficaram registradas no histórico")
    void deveAplicarAsMigrations() {
        List<String> versoes = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL "
                        + "ORDER BY installed_rank", String.class);

        assertEquals(List.of("1", "2", "3", "4"), versoes);
    }

    @Test
    @DisplayName("O schema contém as tabelas dos agregados e os catálogos geográficos")
    void deveCriarAsTabelas() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name <> 'flyway_schema_history' ORDER BY table_name", String.class);

        assertEquals(List.of("addresses", "cities", "password_reset_tokens", "restaurants", "roles", "states",
                "user_roles", "users"), tabelas);
    }

    @Test
    @DisplayName("O catálogo de papéis tem os três papéis reconhecidos pelo domínio")
    void deveSemearOCatalogoDePapeis() {
        Set<String> nomes = Arrays.stream(RoleName.values()).map(Enum::name).collect(Collectors.toSet());

        assertEquals(nomes, roleDataSource.findByNames(nomes).stream()
                .map(role -> role.name())
                .collect(Collectors.toSet()));
    }

    @ParameterizedTest(name = "{0} com {1}")
    @CsvSource({
            "dono.restaurante, ROLE_OWNER",
            "cliente.demo,     ROLE_CUSTOMER",
            "admin.demo,       ROLE_ADMIN"
    })
    @DisplayName("Os usuários de demonstração existem, com o papel previsto e a senha em hash")
    void deveSemearOsUsuariosDeDemonstracao(String login, String papel) {
        UserData usuario = userDataSource.findByLogin(login).orElseThrow();

        assertEquals(Set.of(papel), usuario.roles().stream().map(role -> role.name()).collect(Collectors.toSet()));
        assertTrue(usuario.passwordHash().startsWith("$2a$"), "senha da seed deve estar em hash BCrypt");
        assertEquals("system", jdbc.queryForObject(
                "SELECT created_by FROM users WHERE login = ?", String.class, login));
    }

    @Test
    @DisplayName("O endereço da seed é lido junto com o agregado do dono")
    void deveLerOEnderecoDaSeed() {
        UserData dono = userDataSource.findByLogin("dono.restaurante").orElseThrow();

        assertEquals(1, dono.addresses().size());
        assertEquals("Avenida Paulista", dono.addresses().get(0).street());
        assertEquals("São Paulo", dono.addresses().get(0).city());
        assertEquals("SP", dono.addresses().get(0).state());
        assertTrue(dono.addresses().get(0).cityId() != null);
        assertEquals("01310100", dono.addresses().get(0).zipCode());
    }

    @Test
    @DisplayName("A migration preserva endereço e normaliza a chave estrangeira para cidade")
    void deveMigrarEnderecoParaCidade() {
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM addresses a JOIN cities c ON c.id = a.city_id "
                        + "JOIN states s ON s.id = c.state_id "
                        + "WHERE a.street = 'Avenida Paulista' AND c.name = 'São Paulo' "
                        + "AND s.name = 'São Paulo' AND s.acronym = 'SP'", Integer.class));
        assertEquals(UUID.fromString("c0000000-0000-4000-8000-000000000001"), jdbc.queryForObject(
                "SELECT city_id FROM addresses WHERE street = 'Avenida Paulista'", UUID.class));
        assertEquals(List.of("city_id", "complement", "id", "neighborhood", "number", "street", "user_id",
                "zip_code"), jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'addresses' ORDER BY column_name", String.class));
    }

    @Test
    @DisplayName("Chaves estrangeiras apagam em cascata no banco, independentemente do ORM")
    void deveApagarEmCascataNoBanco() {
        jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                + "VALUES ('b0000000-0000-4000-8000-0000000000ff', 'Efêmero', 'efemero@email.com', 'efemero', "
                + "'$2a$10$hash', NOW(), NOW())");
        UUID cityId = jdbc.queryForObject(
                "SELECT id FROM cities WHERE name = 'São Paulo' "
                        + "AND state_id = (SELECT id FROM states WHERE acronym = 'SP')", UUID.class);
        jdbc.update("INSERT INTO addresses (user_id, city_id, street, number, neighborhood, zip_code) "
                + "VALUES ('b0000000-0000-4000-8000-0000000000ff', ?, 'Rua A', '1', 'Centro', '01001000')", cityId);
        jdbc.update("INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) "
                + "VALUES ('b0000000-0000-4000-8000-0000000000ff', 'hash-efemero', NOW())");

        jdbc.update("DELETE FROM users WHERE login = 'efemero'");

        assertEquals(0, contar("addresses", "b0000000-0000-4000-8000-0000000000ff"));
        assertEquals(0, contar("password_reset_tokens", "b0000000-0000-4000-8000-0000000000ff"));
    }

    private int contar(String tabela, String userId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE user_id = ?::uuid",
                Integer.class, userId);
    }
}
