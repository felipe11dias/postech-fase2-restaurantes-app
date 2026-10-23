package com.postech.restaurantes.infrastructure.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.Documentos;
import com.postech.restaurantes.EnderecosNoBanco;
import com.postech.restaurantes.IntegrationTestSupport;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.OwnerData;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.gateway.UserGateway;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.domain.entity.user.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O agregado de usuário contra um PostgreSQL de verdade: gravação, leitura, substituição de
 * coleções e exclusão. Prova o que o teste unitário não alcança — o SQL emitido, o cascade, a
 * remoção de órfãos e o preenchimento do autor pela auditoria.
 */
class UserPersistenceIT extends IntegrationTestSupport {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @Autowired
    private IUserDataSource userDataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Usuário gravado é lido de volta inteiro, com perfil e endereços")
    void deveGravarELerOAgregadoInteiro() {
        UserData gravado = inserir("Ana Integração", List.of(endereco("Rua das Acácias", "10")));

        UserData lido = userDataSource.findById(gravado.id()).orElseThrow();

        assertNotNull(lido.id());
        assertEquals("Ana Integração", lido.name());
        assertEquals(gravado.client(), lido.client());
        assertNull(lido.owner());
        assertEquals(1, lido.addresses().size());
        assertEquals("Casa", lido.addresses().get(0).label());
        assertTrue(lido.addresses().get(0).isDefault());
        assertEquals("Rua das Acácias", lido.addresses().get(0).address().street());
        assertNotNull(lido.addresses().get(0).id());
        assertNotNull(lido.addresses().get(0).address().id());
    }

    @Test
    @DisplayName("Usuário com três perfis e um endereço é lido com o endereço uma vez só, nas consultas por id, login, e-mail e página")
    void deveLerOEnderecoUmaVezComVariosPerfis() {
        String cpf = Documentos.cpf();
        UserData gravado = inserir("Ivo Integração", dono(), new ClientData(cpf, "11912345678", null),
                new CourierData(cpf, "11912345678", "MOTORCYCLE", Documentos.cnh(), "ABC1D23", "OFFLINE"),
                List.of(endereco("Rua dos Perfis", "3")));

        assertEquals(1, userDataSource.findById(gravado.id()).orElseThrow().addresses().size());
        assertEquals(1, userDataSource.findByLogin(gravado.login()).orElseThrow().addresses().size());
        assertEquals(1, userDataSource.findByEmail(gravado.email()).orElseThrow().addresses().size());
        assertEquals(1, userDataSource.search("Ivo Integração", PageRequest.of(0, 10)).content().get(0)
                .addresses().size());
        assertEquals(3, UserGateway.create(userDataSource).findById(gravado.id()).orElseThrow().getRoles().size());
    }

    @Test
    @DisplayName("Perfis de dono, cliente e entregador vão e voltam do banco, com os ENUMs do entregador")
    void deveGravarELerOsPerfis() {
        String cpf = Documentos.cpf();
        OwnerData dono = dono();
        CourierData entregador = new CourierData(cpf, "11912345678", "CAR", Documentos.cnh(), "XYZ9A87", "BUSY");

        UserData gravado = inserir("Jonas Integração", dono, new ClientData(cpf, "11912345678",
                LocalDate.of(1990, 5, 20)), entregador, List.of());
        UserData lido = userDataSource.findById(gravado.id()).orElseThrow();

        assertEquals(dono, lido.owner());
        assertEquals(LocalDate.of(1990, 5, 20), lido.client().birthDate());
        assertEquals(entregador, lido.courier());
        assertEquals("BUSY", jdbc.queryForObject("SELECT status::text FROM couriers WHERE id = ?", String.class,
                gravado.id()));
        assertEquals("system", jdbc.queryForObject("SELECT created_by FROM owners WHERE id = ?", String.class,
                gravado.id()));
    }

    @Test
    @DisplayName("Trocar os perfis mantém a linha do que continua, cria a do novo e apaga a do que saiu")
    void deveTrocarOsPerfis() {
        UserData gravado = inserir("Fábio Integração", List.of());
        OwnerData dono = dono();

        UserData atualizado = userDataSource.update(new UserData(gravado.id(), gravado.name(), gravado.email(),
                gravado.login(), gravado.passwordHash(), dono, null, null, null, List.of(), gravado.createdAt(),
                LocalDateTime.now()));

        assertEquals(dono, atualizado.owner());
        assertNull(atualizado.client());
        assertEquals(0, (int) jdbc.queryForObject("SELECT count(*) FROM clients WHERE id = ?", Integer.class,
                gravado.id()));
        assertEquals(1, (int) jdbc.queryForObject("SELECT count(*) FROM owners WHERE id = ?", Integer.class,
                gravado.id()));
    }

    @Test
    @DisplayName("Consulta por CPF acha o cliente e o entregador; por CNPJ, o dono")
    void deveConsultarPorDocumento() {
        String cpfDoEntregador = Documentos.cpf();
        UserData cliente = inserir("Karen Integração", List.of());
        UserData entregador = inserir("Lauro Integração", null, null,
                new CourierData(cpfDoEntregador, "11912345678", "BICYCLE", null, null, "OFFLINE"), List.of());
        UserData dono = inserir("Marta Integração", dono(), null, null, List.of());

        assertEquals(cliente.id(), userDataSource.findByCpf(cliente.client().cpf()).orElseThrow().id());
        assertEquals(entregador.id(), userDataSource.findByCpf(cpfDoEntregador).orElseThrow().id());
        assertEquals(dono.id(), userDataSource.findByCnpj(dono.owner().cnpj()).orElseThrow().id());
        assertTrue(userDataSource.findByCpf(Documentos.cpf()).isEmpty());
        assertTrue(userDataSource.findByCnpj(Documentos.cnpj()).isEmpty());
    }

    @Test
    @DisplayName("O banco recusa o mesmo CPF em dois clientes")
    void deveRecusarCpfRepetido() {
        UserData primeiro = inserir("Nina Integração", List.of());

        assertThrows(DataIntegrityViolationException.class, () -> inserir("Otto Integração", null,
                primeiro.client(), null, List.of()));
    }

    @Test
    @DisplayName("O agregado lido reconstrói uma entidade de domínio válida")
    void deveReconstruirOAgregadoDeDominio() {
        UserData gravado = inserir("Bruno Integração", List.of(endereco("Rua B", "20")));

        User usuario = UserGateway.create(userDataSource).findById(gravado.id()).orElseThrow();

        assertEquals("Bruno Integração", usuario.getName());
        assertEquals(gravado.email(), usuario.getEmail().value());
        assertEquals(1, usuario.getRoles().size());
        assertEquals("20", usuario.getAddresses().get(0).getAddress().getNumber());
    }

    @Test
    @DisplayName("Consulta por login e por e-mail encontram o mesmo registro")
    void deveConsultarPorLoginEEmail() {
        UserData gravado = inserir("Carla Integração", List.of());

        assertEquals(gravado.id(), userDataSource.findByLogin(gravado.login()).orElseThrow().id());
        assertEquals(gravado.id(), userDataSource.findByEmail(gravado.email()).orElseThrow().id());
    }

    @Test
    @DisplayName("Auditoria grava o autor 'system' quando ninguém está autenticado")
    void deveGravarOAutorDaAuditoria() {
        UserData gravado = inserir("Diana Integração", List.of());

        assertEquals("system", jdbc.queryForObject(
                "SELECT created_by FROM users WHERE id = ?", String.class, gravado.id()));
        assertEquals("system", jdbc.queryForObject(
                "SELECT last_updated_by FROM users WHERE id = ?", String.class, gravado.id()));
    }

    @Test
    @DisplayName("Substituir os endereços apaga vínculos e endereços antigos, inclusive trocando o padrão")
    void deveRemoverOsEnderecosOrfaos() {
        UserData gravado = inserir("Elisa Integração",
                List.of(endereco("Rua Antiga", "1"), enderecoSecundario("Rua Também Antiga", "2")));
        List<UUID> enderecosAntigos = idsDosEnderecos(gravado);

        UserData atualizado = userDataSource.update(new UserData(gravado.id(), gravado.name(), gravado.email(),
                gravado.login(), gravado.passwordHash(), gravado.owner(), gravado.client(), gravado.courier(), gravado.admin(),
                List.of(endereco("Rua Nova", "99")), gravado.createdAt(), LocalDateTime.now()));

        assertEquals(1, atualizado.addresses().size());
        assertEquals("Rua Nova", atualizado.addresses().get(0).address().street());
        assertEquals(1, vinculos(gravado.id()));
        assertEquals(0, EnderecosNoBanco.existentes(jdbc, enderecosAntigos),
                "os endereços antigos saíram com os vínculos");
        assertEquals(1, EnderecosNoBanco.existentes(jdbc, idsDosEnderecos(atualizado)));
    }

    /**
     * A comparação é feita contra o valor <em>relido do banco</em>, e não contra o que ficou
     * em memória logo após a inserção: o PostgreSQL guarda {@code timestamp} em microssegundos
     * e descarta os nanossegundos do Java.
     */
    @Test
    @DisplayName("Alterar o nome não recria a linha: id e data de criação são preservados")
    void devePreservarIdentidadeNaAtualizacao() {
        UserData gravado = inserir("Gustavo Integração", List.of());
        UserData persistido = userDataSource.findById(gravado.id()).orElseThrow();

        UserData atualizado = userDataSource.update(new UserData(persistido.id(), "Gustavo Renomeado",
                persistido.email(), persistido.login(), persistido.passwordHash(), persistido.owner(), persistido.client(), persistido.courier(), persistido.admin(), List.of(),
                persistido.createdAt(), persistido.lastUpdatedAt()));

        assertEquals(persistido.id(), atualizado.id());
        assertEquals("Gustavo Renomeado", atualizado.name());
        assertEquals(persistido.createdAt(), atualizado.createdAt());
    }

    /**
     * O registro devolvido pela atualização precisa trazer o {@code last_updated_at} novo. O
     * listener só o carimba quando a alteração é descarregada; sem o flush na origem de dados,
     * a resposta de um {@code PUT} carregaria o instante <em>anterior</em> à edição.
     */
    @Test
    @DisplayName("A atualização devolve o instante novo de alteração, o mesmo que o banco passa a ter")
    void deveDevolverOInstanteNovoNaAtualizacao() {
        UserData gravado = inserir("Heitor Integração", List.of());
        UserData antes = userDataSource.findById(gravado.id()).orElseThrow();

        UserData atualizado = userDataSource.update(new UserData(antes.id(), "Heitor Renomeado",
                antes.email(), antes.login(), antes.passwordHash(), antes.owner(), antes.client(), antes.courier(), antes.admin(), List.of(),
                antes.createdAt(), antes.lastUpdatedAt()));
        UserData relido = userDataSource.findById(gravado.id()).orElseThrow();

        assertTrue(atualizado.lastUpdatedAt().isAfter(antes.lastUpdatedAt()),
                "a resposta não pode trazer o instante anterior à edição");
        assertEquals(relido.lastUpdatedAt(), atualizado.lastUpdatedAt(),
                "o que a resposta diz é exatamente o que o banco gravou, até o microssegundo");
    }

    @Test
    @DisplayName("A auditoria é carimbada na gravação, mesmo quando o registro chega sem instante")
    void deveCarimbarAAuditoriaNaGravacao() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        UserData semInstante = new UserData(null, "Sem Instante", "semdata." + sufixo + "@email.com",
                "semdata." + sufixo, "$2a$10$hashDeIntegracaoComTamanhoSuficiente",
                null, cliente(), null, null, List.of(), null, null);

        UserData gravado = userDataSource.insert(semInstante);

        assertNotNull(gravado.createdAt());
        assertNotNull(gravado.lastUpdatedAt());
        assertEquals("system", jdbc.queryForObject(
                "SELECT created_by FROM users WHERE id = ?", String.class, gravado.id()));
    }

    @Test
    @DisplayName("Busca por nome não diferencia maiúsculas e pagina no banco")
    void deveBuscarPorNomePaginando() {
        String marca = "Zeta" + SEQUENCIA.incrementAndGet();
        inserir(marca + " Carlos", List.of());
        inserir(marca + " Ana", List.of());
        inserir(marca + " Bruno", List.of());

        PageResult<UserData> primeira = userDataSource.search(marca.toLowerCase(), PageRequest.of(0, 2));

        assertEquals(2, primeira.content().size());
        assertEquals(3, primeira.totalElements());
        assertEquals(1, userDataSource.search(marca.toLowerCase(), PageRequest.of(1, 2)).content().size());
    }

    @Test
    @DisplayName("A ordenação pedida pelo núcleo chega ao ORDER BY do banco")
    void deveOrdenarNoBanco() {
        String marca = "Ordem" + SEQUENCIA.incrementAndGet();
        inserir(marca + " Carlos", List.of());
        inserir(marca + " Ana", List.of());

        List<String> ascendente = nomes(userDataSource.search(marca,
                PageRequest.of(0, 10).withSort("name", SortDirection.ASC)));
        List<String> descendente = nomes(userDataSource.search(marca,
                PageRequest.of(0, 10).withSort("name", SortDirection.DESC)));

        assertEquals(List.of(marca + " Ana", marca + " Carlos"), ascendente);
        assertEquals(List.of(marca + " Carlos", marca + " Ana"), descendente);
    }

    /**
     * A coluna pedida é trocada pelo padrão; a direção, que não expõe nada, é respeitada.
     * O resultado sai ordenado por nome, e não há como afirmar que o banco ordenou por
     * {@code password} — a consulta nem chega a mencionar a coluna.
     */
    @Test
    @DisplayName("Ordenar por uma propriedade fora da lista permitida cai no nome, sem quebrar a consulta")
    void deveIgnorarOrdenacaoNaoPermitida() {
        String marca = "Seguro" + SEQUENCIA.incrementAndGet();
        inserir(marca + " Carlos", List.of());
        inserir(marca + " Ana", List.of());

        List<String> ascendente = nomes(userDataSource.search(marca,
                PageRequest.of(0, 10).withSort("password", SortDirection.ASC)));
        List<String> descendente = nomes(userDataSource.search(marca,
                PageRequest.of(0, 10).withSort("password", SortDirection.DESC)));

        assertEquals(List.of(marca + " Ana", marca + " Carlos"), ascendente);
        assertEquals(List.of(marca + " Carlos", marca + " Ana"), descendente);
    }

    @Test
    @DisplayName("Busca sem nome lista todos, inclusive os usuários de demonstração")
    void deveListarTodosSemFiltro() {
        assertTrue(userDataSource.search(null, PageRequest.of(0, 100)).totalElements() >= 3);
    }

    @Test
    @DisplayName("Exclusão remove o usuário e, em cascata, o perfil, os vínculos e os endereços deles")
    void deveExcluirEmCascata() {
        UserData gravado = inserir("Helena Integração", List.of(endereco("Rua H", "8")));

        userDataSource.delete(gravado.id());

        assertTrue(userDataSource.findById(gravado.id()).isEmpty());
        assertEquals(0, vinculos(gravado.id()));
        assertEquals(0, EnderecosNoBanco.existentes(jdbc, idsDosEnderecos(gravado)));
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT count(*) FROM clients WHERE id = ?", Integer.class, gravado.id()));
    }

    private UserData inserir(String nome, List<UserAddressData> enderecos) {
        return inserir(nome, null, cliente(), null, enderecos);
    }

    private UserData inserir(String nome, OwnerData owner, ClientData client, CourierData courier,
                             List<UserAddressData> enderecos) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        return userDataSource.insert(new UserData(null, nome, "usuario." + sufixo + "@email.com",
                "usuario." + sufixo, "$2a$10$hashDeIntegracaoComTamanhoSuficiente",
                owner, client, courier, null, enderecos, agora, agora));
    }

    /** Perfil de dono com CNPJ novo: o CNPJ também é único. */
    private static OwnerData dono() {
        return new OwnerData(Documentos.cnpj(), "Integração Ltda", "1131234567");
    }

    /** Perfil de cliente com CPF novo: o banco é compartilhado e o CPF é único. */
    private static ClientData cliente() {
        return new ClientData(Documentos.cpf(), "11912345678", null);
    }

    /** Endereço padrão do usuário, rotulado "Casa". */
    private static UserAddressData endereco(String rua, String numero) {
        return new UserAddressData(null, "Casa", true, enderecoAvulso(rua, numero));
    }

    private static UserAddressData enderecoSecundario(String rua, String numero) {
        return new UserAddressData(null, null, false, enderecoAvulso(rua, numero));
    }

    private static AddressData enderecoAvulso(String rua, String numero) {
        return new AddressData(null, rua, numero, null, "Centro", "São Paulo", "SP", "01001000");
    }

    private int vinculos(UUID userId) {
        return jdbc.queryForObject("SELECT count(*) FROM user_addresses WHERE user_id = ?", Integer.class, userId);
    }

    private static List<UUID> idsDosEnderecos(UserData usuario) {
        return usuario.addresses().stream().map(vinculo -> vinculo.address().id()).toList();
    }

    private static List<String> nomes(PageResult<UserData> pagina) {
        return pagina.content().stream().map(UserData::name).toList();
    }
}
