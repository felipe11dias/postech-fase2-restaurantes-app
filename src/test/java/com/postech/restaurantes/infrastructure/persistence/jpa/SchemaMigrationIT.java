package com.postech.restaurantes.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.Documentos;
import com.postech.restaurantes.EnderecosNoBanco;
import com.postech.restaurantes.IntegrationTestSupport;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * O contexto só sobe se o Flyway aplicar as migrations e o Hibernate validar o mapeamento
 * contra o schema resultante. Cada teste abaixo confere o que as migrations prometem.
 */
class SchemaMigrationIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private javax.sql.DataSource dataSource;

    @Autowired
    private IUserDataSource userDataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("Upgrades a V10 database without changing legacy address ids or duplicating seeded cities")
    void shouldUpgradeExistingDatabase() {
        String schema = "location_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        UUID addressId = UUID.randomUUID();
        try {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource)
                    .schemas(schema).defaultSchema(schema).target("10").load().migrate();
            jdbc.update("UPDATE " + schema + ".addresses SET city = 'são paulo', state = 'sp'");
            jdbc.update("INSERT INTO " + schema + ".addresses (id, street, number, neighborhood, city, state, zip_code) "
                    + "VALUES (?, 'Legacy street', '1', 'Centro', ' campinas ', 'sp', '01001000')", addressId);
            org.flywaydb.core.Flyway.configure().dataSource(dataSource)
                    .schemas(schema).defaultSchema(schema).load().migrate();
            assertEquals("campinas", jdbc.queryForObject("SELECT c.name FROM " + schema + ".addresses a JOIN "
                    + schema + ".cities c ON c.id = a.city_id WHERE a.id = ?", String.class, addressId));
            assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".cities WHERE LOWER(name) = 'campinas'", Integer.class));
            assertEquals(27, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".states", Integer.class));
            assertEquals(28, jdbc.queryForObject("SELECT count(*) FROM " + schema + ".cities", Integer.class));
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    @DisplayName("The database rejects addresses without a city or with an unknown city")
    void shouldEnforceCityForeignKey() {
        String sql = "INSERT INTO addresses (city_id, street, number, neighborhood, zip_code) "
                + "VALUES (?, 'Invalid reference', '1', 'Centro', '01001000')";
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, (UUID) null));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, UUID.randomUUID()));
    }

    @Test
    @DisplayName("As migrations foram aplicadas com sucesso e ficaram registradas no histórico")
    void deveAplicarAsMigrations() {
        List<String> versoes = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL "
                        + "ORDER BY installed_rank", String.class);

        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"), versoes);
    }

    @Test
    @DisplayName("O schema contém as tabelas dos agregados e os catálogos geográficos")
    void deveCriarAsTabelas() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name <> 'flyway_schema_history' ORDER BY table_name", String.class);

        assertEquals(List.of("addresses", "admins", "cities", "clients", "couriers",
                "owners", "password_reset_tokens", "restaurant_office_hours", "restaurants", "states", "user_addresses", "users"), tabelas);
    }

    @Test
    @DisplayName("Os usuários de demonstração existem, cada um com o seu perfil e a senha em hash")
    void deveSemearOsUsuariosDeDemonstracao() {
        UserData dono = userDataSource.findByLogin("dono.restaurante").orElseThrow();
        UserData cliente = userDataSource.findByLogin("cliente.demo").orElseThrow();
        UserData admin = userDataSource.findByLogin("admin.demo").orElseThrow();

        assertEquals("04252011000110", dono.owner().cnpj());
        assertNull(dono.client());
        assertEquals("52998224725", cliente.client().cpf());
        assertNull(cliente.owner());
        assertEquals("ADM-0001", admin.admin().employeeCode());
        assertTrue(admin.admin().superAdmin());
        assertNull(admin.courier());
        for (UserData usuario : List.of(dono, cliente, admin)) {
            assertTrue(usuario.passwordHash().startsWith("$2a$"), "senha da seed deve estar em hash BCrypt");
            assertEquals("system", jdbc.queryForObject(
                    "SELECT created_by FROM users WHERE id = ?", String.class, usuario.id()));
        }
    }

    @ParameterizedTest(name = "{0}: CNH {1}, placa {2}")
    @CsvSource({
            "MOTORCYCLE, ,            ABC1D23",
            "CAR,        02650306461, ",
            "ON_FOOT,    02650306461, ",
            "BICYCLE,    ,            ABC1D23"
    })
    @DisplayName("O banco recusa CNH e placa que não combinam com o veículo (motorizado exige as duas)")
    void deveRecusarDocumentosQueNaoCombinamComOVeiculo(String veiculo, String cnh, String placa) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                    + "VALUES (?, 'Entregador', ?, ?, '$2a$10$hash', NOW(), NOW())", id, id + "@email.com", id.toString());

            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update("INSERT INTO couriers (id, cpf, "
                    + "phone, driver_license_number, vehicle_type, vehicle_plate, created_at, last_updated_at) "
                    + "VALUES (?, ?, '11912345678', ?, ?::courier_vehicle_type, ?, NOW(), NOW())",
                    id, Documentos.cpf(), cnh, veiculo, placa));
        } finally {
            jdbc.update("DELETE FROM users WHERE id = ?", id);
        }
    }

    @Test
    @DisplayName("O endereço da seed virou vínculo do dono (V4), como endereço padrão e sem rótulo")
    void deveLerOEnderecoDaSeed() {
        UserData dono = userDataSource.findByLogin("dono.restaurante").orElseThrow();

        assertEquals(1, dono.addresses().size());
        assertTrue(dono.addresses().get(0).isDefault());
        assertNull(dono.addresses().get(0).label());
        assertEquals("Avenida Paulista", dono.addresses().get(0).address().street());
        assertEquals("São Paulo", dono.addresses().get(0).address().city().name());
        assertEquals("SP", dono.addresses().get(0).address().city().state().acronym());
        assertTrue(dono.addresses().get(0).address().cityId() != null);
        assertEquals("01310100", dono.addresses().get(0).address().zipCode());
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
        assertEquals(List.of("city_id", "complement", "id", "neighborhood", "number", "street",
                "zip_code"), jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = 'public' AND table_name = 'addresses' ORDER BY column_name", String.class));
    }

    @Test
    @DisplayName("Chaves estrangeiras apagam em cascata no banco, independentemente do ORM")
    void deveApagarEmCascataNoBanco() {
        try {
            jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                    + "VALUES ('b0000000-0000-4000-8000-0000000000ff', 'Efêmero', 'efemero@email.com', 'efemero', "
                    + "'$2a$10$hash', NOW(), NOW())");
            UUID cityId = jdbc.queryForObject(
                "SELECT id FROM cities WHERE name = 'São Paulo' "
                        + "AND state_id = (SELECT id FROM states WHERE acronym = 'SP')", UUID.class);
        jdbc.update("INSERT INTO addresses (id, city_id, street, number, neighborhood, zip_code) "
                    + "VALUES ('c0000000-0000-4000-8000-0000000000ff', ?, 'Rua A', '1', 'Centro', "
                    + "'01001000')", cityId);
            jdbc.update("INSERT INTO user_addresses (user_id, address_id, is_default, created_at, last_updated_at) "
                    + "VALUES ('b0000000-0000-4000-8000-0000000000ff', 'c0000000-0000-4000-8000-0000000000ff', "
                    + "TRUE, NOW(), NOW())");
            jdbc.update("INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) "
                    + "VALUES ('b0000000-0000-4000-8000-0000000000ff', 'hash-efemero', NOW())");
            jdbc.update("INSERT INTO clients (id, cpf, phone, created_at, last_updated_at) "
                    + "VALUES ('b0000000-0000-4000-8000-0000000000ff', ?, '11912345678', NOW(), NOW())", Documentos.cpf());

            jdbc.update("DELETE FROM users WHERE login = 'efemero'");

            assertEquals(0, contar("user_addresses", "b0000000-0000-4000-8000-0000000000ff"));
            assertEquals(0, contar("password_reset_tokens", "b0000000-0000-4000-8000-0000000000ff"));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM clients "
                    + "WHERE id = 'b0000000-0000-4000-8000-0000000000ff'", Integer.class));
            assertEquals(1, EnderecosNoBanco.existentes(jdbc,
                    List.of(UUID.fromString("c0000000-0000-4000-8000-0000000000ff"))));
        } finally {
            // O banco é compartilhado: o que este teste criou sai mesmo se uma asserção falhar.
            jdbc.update("DELETE FROM users WHERE login = 'efemero'");
            jdbc.update("DELETE FROM addresses WHERE id = 'c0000000-0000-4000-8000-0000000000ff'");
        }
    }

    @Test
    @DisplayName("O banco recusa dois endereços padrão do mesmo usuário (conferido no commit)")
    void deveRecusarDoisEnderecosPadrao() {
        String vinculoPadrao = "INSERT INTO user_addresses (user_id, address_id, is_default, created_at, "
                + "last_updated_at) VALUES ('b0000000-0000-4000-8000-0000000000fe', ?::uuid, TRUE, NOW(), NOW())";
        try {
            jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                    + "VALUES ('b0000000-0000-4000-8000-0000000000fe', 'Dois Padrões', 'dois.padroes@email.com', "
                    + "'dois.padroes', '$2a$10$hash', NOW(), NOW())");
            jdbc.update("INSERT INTO addresses (id, street, number, neighborhood, city_id, zip_code) VALUES "
                    + "('c0000000-0000-4000-8000-0000000000a1', 'Rua A', '1', 'Centro', 'c0000000-0000-4000-8000-000000000001', "
                    + "'01001000'), "
                    + "('c0000000-0000-4000-8000-0000000000a2', 'Rua B', '2', 'Centro', 'c0000000-0000-4000-8000-000000000001', "
                    + "'01001000')");
            jdbc.update(vinculoPadrao, "c0000000-0000-4000-8000-0000000000a1");

            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update(vinculoPadrao, "c0000000-0000-4000-8000-0000000000a2"));
        } finally {
            jdbc.update("DELETE FROM users WHERE login = 'dois.padroes'");
            jdbc.update("DELETE FROM addresses WHERE id IN ('c0000000-0000-4000-8000-0000000000a1', "
                    + "'c0000000-0000-4000-8000-0000000000a2')");
        }
    }

    @Test
    @DisplayName("O banco recusa o mesmo CPF em dois usuários, mesmo um sendo cliente e o outro entregador (V7)")
    void deveRecusarOMesmoCpfEmDuasPessoas() {
        UUID cliente = UUID.randomUUID();
        UUID entregador = UUID.randomUUID();
        String cpf = Documentos.cpf();
        String comoEntregador = "INSERT INTO couriers (id, cpf, phone, vehicle_type, created_at, last_updated_at) "
                + "VALUES (?, ?, '11912345678', 'ON_FOOT', NOW(), NOW())";
        try {
            inserirUsuario(cliente);
            inserirUsuario(entregador);
            jdbc.update("INSERT INTO clients (id, cpf, phone, created_at, last_updated_at) "
                    + "VALUES (?, ?, '11912345678', NOW(), NOW())", cliente, cpf);

            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(comoEntregador, entregador, cpf));
            assertEquals(1, jdbc.update(comoEntregador, cliente, cpf), "a mesma pessoa pode ser cliente e entregador");
        } finally {
            jdbc.update("DELETE FROM users WHERE id IN (?, ?)", cliente, entregador);
        }
    }

    @Test
    @DisplayName("O restaurante só pode ser de quem tem perfil de dono, e o perfil não sai com restaurante (V7)")
    void deveExigirPerfilDeDonoParaORestaurante() {
        UUID usuario = UUID.randomUUID();
        UUID endereco = UUID.randomUUID();
        String restaurante = "INSERT INTO restaurants (user_id, address_id, name, "
                + "created_at, last_updated_at) VALUES (?, ?, 'Sem Dono', NOW(), NOW())";
        try {
            inserirUsuario(usuario);
            jdbc.update("INSERT INTO addresses (id, street, number, neighborhood, city_id, zip_code) "
                    + "VALUES (?, 'Rua A', '1', 'Centro', 'c0000000-0000-4000-8000-000000000001', '01001000')", endereco);

            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(restaurante, usuario, endereco));
            jdbc.update("INSERT INTO owners (id, cnpj, legal_name, business_phone, created_at, last_updated_at) "
                    + "VALUES (?, ?, 'Dono Ltda', '1131234567', NOW(), NOW())", usuario, Documentos.cnpj());
            jdbc.update(restaurante, usuario, endereco);
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("DELETE FROM owners WHERE id = ?", usuario));
        } finally {
            jdbc.update("DELETE FROM restaurants WHERE user_id = ?", usuario);
            jdbc.update("DELETE FROM users WHERE id = ?", usuario);
            jdbc.update("DELETE FROM addresses WHERE id = ?", endereco);
        }
    }

    @Test
    @DisplayName("Excluir o usuário direto no banco leva os restaurantes dele, mesmo com a chave para owners (V8)")
    void deveApagarOsRestaurantesDoUsuarioNoBanco() {
        UUID usuario = UUID.randomUUID();
        UUID endereco = UUID.randomUUID();
        try {
            inserirUsuario(usuario);
            jdbc.update("INSERT INTO owners (id, cnpj, legal_name, business_phone, created_at, last_updated_at) "
                    + "VALUES (?, ?, 'Dono Ltda', '1131234567', NOW(), NOW())", usuario, Documentos.cnpj());
            jdbc.update("INSERT INTO addresses (id, street, number, neighborhood, city_id, zip_code) "
                    + "VALUES (?, 'Rua A', '1', 'Centro', 'c0000000-0000-4000-8000-000000000001', '01001000')", endereco);
            jdbc.update("INSERT INTO restaurants (user_id, address_id, name, "
                    + "created_at, last_updated_at) VALUES (?, ?, 'Cascata', NOW(), NOW())",
                    usuario, endereco);

            jdbc.update("DELETE FROM users WHERE id = ?", usuario);

            assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM restaurants WHERE user_id = ?",
                    Integer.class, usuario));
            assertEquals(1, EnderecosNoBanco.existentes(jdbc, List.of(endereco)),
                    "a cascata do banco não alcança o endereço: quem o remove é a aplicação");
        } finally {
            jdbc.update("DELETE FROM users WHERE id = ?", usuario);
            jdbc.update("DELETE FROM addresses WHERE id = ?", endereco);
        }
    }

    @Test
    @DisplayName("O banco aceita apagar um administrador quando sobra outro, e recusa apagar o último (V10)")
    void deveManterAoMenosUmAdministrador() {
        UUID outro = UUID.randomUUID();
        try {
            inserirUsuario(outro);
            jdbc.update("INSERT INTO admins (id, employee_code, created_at, last_updated_at) VALUES (?, ?, NOW(), NOW())",
                    outro, "ADM-" + outro);

            assertEquals(1, jdbc.update("DELETE FROM admins WHERE id = ?", outro), "sobra o administrador da seed");
            // Apagar todos, numa transação desfeita no fim: se o gatilho falhar, a seed não se perde.
            TransactionTemplate transacao = new TransactionTemplate(transactionManager);
            assertThrows(DataIntegrityViolationException.class, () -> transacao.executeWithoutResult(status -> {
                status.setRollbackOnly();
                jdbc.update("DELETE FROM admins");
            }));
            assertEquals(1, (int) jdbc.queryForObject("SELECT count(*) FROM admins WHERE id = ?::uuid",
                    Integer.class, "a0000000-0000-4000-8000-000000000003"));
        } finally {
            jdbc.update("DELETE FROM users WHERE id = ?", outro);
        }
    }

    private void inserirUsuario(UUID id) {
        jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                + "VALUES (?, 'Usuário', ?, ?, '$2a$10$hash', NOW(), NOW())", id, id + "@email.com", id.toString());
    }

    private int contar(String tabela, String userId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE user_id = ?::uuid",
                Integer.class, userId);
    }
}
