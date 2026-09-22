package com.postech.restaurantes.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.IntegrationTestSupport;
import com.postech.restaurantes.adapter.datasource.IRoleDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.gateway.UserGateway;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.domain.entity.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
    private IRoleDataSource roleDataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Usuário gravado é lido de volta inteiro, com papéis e endereços")
    void deveGravarELerOAgregadoInteiro() {
        UserData gravado = inserir("Ana Integração", List.of(endereco("Rua das Acácias", "10")));

        UserData lido = userDataSource.findById(gravado.id()).orElseThrow();

        assertNotNull(lido.id());
        assertEquals("Ana Integração", lido.name());
        assertEquals(Set.of("ROLE_CUSTOMER"), nomesDosPapeis(lido));
        assertEquals(1, lido.addresses().size());
        assertEquals("Rua das Acácias", lido.addresses().get(0).street());
        assertNotNull(lido.addresses().get(0).id());
    }

    @Test
    @DisplayName("O agregado lido reconstrói uma entidade de domínio válida")
    void deveReconstruirOAgregadoDeDominio() {
        UserData gravado = inserir("Bruno Integração", List.of(endereco("Rua B", "20")));

        User usuario = UserGateway.create(userDataSource).findById(gravado.id()).orElseThrow();

        assertEquals("Bruno Integração", usuario.getName());
        assertEquals(gravado.email(), usuario.getEmail().value());
        assertEquals(1, usuario.getRoles().size());
        assertEquals("20", usuario.getAddresses().get(0).getNumber());
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
    @DisplayName("Substituir os endereços apaga os órfãos e grava os novos")
    void deveRemoverOsEnderecosOrfaos() {
        UserData gravado = inserir("Elisa Integração",
                List.of(endereco("Rua Antiga", "1"), endereco("Rua Também Antiga", "2")));

        UserData atualizado = userDataSource.update(new UserData(gravado.id(), gravado.name(), gravado.email(),
                gravado.login(), gravado.passwordHash(), gravado.roles(),
                List.of(endereco("Rua Nova", "99")), gravado.createdAt(), LocalDateTime.now()));

        assertEquals(1, atualizado.addresses().size());
        assertEquals("Rua Nova", atualizado.addresses().get(0).street());
        assertEquals(1, (int) jdbc.queryForObject(
                "SELECT count(*) FROM addresses WHERE user_id = ?", Integer.class, gravado.id()));
    }

    @Test
    @DisplayName("Trocar o papel reescreve o vínculo N:M sem tocar no catálogo")
    void deveTrocarOPapel() {
        UserData gravado = inserir("Fábio Integração", List.of());
        Set<RoleData> owner = roleDataSource.findByNames(Set.of("ROLE_OWNER"));

        UserData atualizado = userDataSource.update(new UserData(gravado.id(), gravado.name(), gravado.email(),
                gravado.login(), gravado.passwordHash(), owner, List.of(), gravado.createdAt(), LocalDateTime.now()));

        assertEquals(Set.of("ROLE_OWNER"), nomesDosPapeis(atualizado));
        assertEquals(3, (int) jdbc.queryForObject("SELECT count(*) FROM roles", Integer.class));
    }

    @Test
    @DisplayName("Alterar o nome não recria a linha: id e data de criação são preservados")
    void devePreservarIdentidadeNaAtualizacao() {
        UserData gravado = inserir("Gustavo Integração", List.of());

        UserData atualizado = userDataSource.update(new UserData(gravado.id(), "Gustavo Renomeado", gravado.email(),
                gravado.login(), gravado.passwordHash(), gravado.roles(), List.of(),
                gravado.createdAt(), gravado.lastUpdatedAt().plusHours(1)));

        assertEquals(gravado.id(), atualizado.id());
        assertEquals("Gustavo Renomeado", atualizado.name());
        assertEquals(gravado.createdAt(), atualizado.createdAt());
        assertNotEquals(gravado.lastUpdatedAt(), atualizado.lastUpdatedAt());
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
    @DisplayName("Exclusão remove o usuário e, em cascata, os endereços")
    void deveExcluirEmCascata() {
        UserData gravado = inserir("Helena Integração", List.of(endereco("Rua H", "8")));

        userDataSource.delete(gravado.id());

        assertTrue(userDataSource.findById(gravado.id()).isEmpty());
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT count(*) FROM addresses WHERE user_id = ?", Integer.class, gravado.id()));
        assertEquals(0, (int) jdbc.queryForObject(
                "SELECT count(*) FROM user_roles WHERE user_id = ?", Integer.class, gravado.id()));
    }

    private UserData inserir(String nome, List<AddressData> enderecos) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        LocalDateTime agora = LocalDateTime.now().withNano(0);
        return userDataSource.insert(new UserData(null, nome, "usuario." + sufixo + "@email.com",
                "usuario." + sufixo, "$2a$10$hashDeIntegracaoComTamanhoSuficiente",
                roleDataSource.findByNames(Set.of("ROLE_CUSTOMER")), enderecos, agora, agora));
    }

    private static AddressData endereco(String rua, String numero) {
        return new AddressData(null, rua, numero, null, "Centro", "São Paulo", "SP", "01001000");
    }

    private static Set<String> nomesDosPapeis(UserData usuario) {
        return usuario.roles().stream().map(RoleData::name).collect(Collectors.toSet());
    }

    private static List<String> nomes(PageResult<UserData> pagina) {
        return pagina.content().stream().map(UserData::name).toList();
    }
}
