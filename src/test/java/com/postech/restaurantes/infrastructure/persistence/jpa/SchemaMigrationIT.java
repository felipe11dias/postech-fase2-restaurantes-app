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

/**
 * O contexto só sobe se o Flyway aplicar as migrations e o Hibernate validar o mapeamento
 * contra o schema resultante. Cada teste abaixo confere o que as migrations prometem.
 */
class SchemaMigrationIT extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private IUserDataSource userDataSource;

    @Test
    @DisplayName("As sete migrations foram aplicadas com sucesso e ficaram registradas no histórico")
    void deveAplicarAsMigrations() {
        List<String> versoes = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true AND version IS NOT NULL "
                        + "ORDER BY installed_rank", String.class);

        assertEquals(List.of("1", "2", "3", "4", "5", "6", "7"), versoes);
    }

    @Test
    @DisplayName("O schema tem as tabelas do modelo v2: perfis no lugar do catálogo de papéis")
    void deveCriarAsTabelas() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' "
                        + "AND table_name <> 'flyway_schema_history' ORDER BY table_name", String.class);

        assertEquals(List.of("addresses", "admins", "clients", "couriers", "owners", "password_reset_tokens",
                "restaurants", "user_addresses", "users"), tabelas);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "courier_vehicle_type, ON_FOOT;BICYCLE;MOTORCYCLE;CAR",
            "courier_status,       OFFLINE;AVAILABLE;BUSY"
    })
    @DisplayName("Os tipos ENUM do entregador têm os valores do modelo, na ordem do modelo")
    void deveCriarOsEnums(String tipo, String valores) {
        List<String> rotulos = jdbc.queryForList("SELECT e.enumlabel FROM pg_enum e JOIN pg_type t "
                + "ON t.oid = e.enumtypid WHERE t.typname = ? ORDER BY e.enumsortorder", String.class, tipo);

        assertEquals(List.of(valores.split(";")), rotulos);
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
        assertEquals("01310100", dono.addresses().get(0).address().zipCode());
    }

    @Test
    @DisplayName("O banco apaga em cascata vínculos, tokens e perfis; o endereço fica, porque é ele o referenciado")
    void deveApagarEmCascataNoBanco() {
        try {
            jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                    + "VALUES ('b0000000-0000-4000-8000-0000000000ff', 'Efêmero', 'efemero@email.com', 'efemero', "
                    + "'$2a$10$hash', NOW(), NOW())");
            jdbc.update("INSERT INTO addresses (id, street, number, neighborhood, city, state, zip_code) "
                    + "VALUES ('c0000000-0000-4000-8000-0000000000ff', 'Rua A', '1', 'Centro', 'São Paulo', 'SP', "
                    + "'01001000')");
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
            jdbc.update("INSERT INTO addresses (id, street, number, neighborhood, city, state, zip_code) VALUES "
                    + "('c0000000-0000-4000-8000-0000000000a1', 'Rua A', '1', 'Centro', 'São Paulo', 'SP', "
                    + "'01001000'), "
                    + "('c0000000-0000-4000-8000-0000000000a2', 'Rua B', '2', 'Centro', 'São Paulo', 'SP', "
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
        String restaurante = "INSERT INTO restaurants (user_id, address_id, name, office_hour_start, office_hour_end, "
                + "created_at, last_updated_at) VALUES (?, ?, 'Sem Dono', '08:00', '22:00', NOW(), NOW())";
        try {
            inserirUsuario(usuario);
            jdbc.update("INSERT INTO addresses (id, street, number, neighborhood, city, state, zip_code) "
                    + "VALUES (?, 'Rua A', '1', 'Centro', 'São Paulo', 'SP', '01001000')", endereco);

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

    private void inserirUsuario(UUID id) {
        jdbc.update("INSERT INTO users (id, name, email, login, password, created_at, last_updated_at) "
                + "VALUES (?, 'Usuário', ?, ?, '$2a$10$hash', NOW(), NOW())", id, id + "@email.com", id.toString());
    }

    private int contar(String tabela, String userId) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE user_id = ?::uuid",
                Integer.class, userId);
    }
}
