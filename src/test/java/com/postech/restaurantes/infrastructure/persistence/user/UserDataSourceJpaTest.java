package com.postech.restaurantes.infrastructure.persistence.user;

import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.HASH;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.USER_DATA;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.roleEntity;
import static com.postech.restaurantes.infrastructure.persistence.PersistenceFixtures.userEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Origem de dados de usuário com repositórios Spring Data mockados: prova a tradução entre o
 * record do adaptador e a entidade JPA, sem subir contexto nem banco.
 */
class UserDataSourceJpaTest {

    private SpringDataUserRepository users;
    private SpringDataRoleRepository roles;
    private UserDataSourceJpa dataSource;

    @BeforeEach
    void setUp() {
        users = mock(SpringDataUserRepository.class);
        roles = mock(SpringDataRoleRepository.class);
        dataSource = new UserDataSourceJpa(users, roles);
    }

    @Test
    @DisplayName("Consulta por id traduz a entidade JPA para o registro do adaptador")
    void deveBuscarPorId() {
        when(users.findById(USER_ID)).thenReturn(Optional.of(userEntity()));

        UserData data = dataSource.findById(USER_ID).orElseThrow();

        assertEquals(USER_ID, data.id());
        assertEquals("João Silva", data.name());
        assertEquals("joao.silva@email.com", data.email());
        assertEquals("joao.silva", data.login());
        assertEquals(HASH, data.passwordHash());
        assertEquals(ROLE_ID, data.roles().iterator().next().id());
        assertEquals("ROLE_CUSTOMER", data.roles().iterator().next().name());
        assertEquals(ADDRESS_ID, data.addresses().get(0).id());
        assertEquals("01001000", data.addresses().get(0).zipCode());
        assertEquals(NOW.minusDays(1), data.createdAt());
        assertEquals(NOW, data.lastUpdatedAt());
    }

    @Test
    @DisplayName("Consulta por id inexistente devolve vazio")
    void deveDevolverVazioQuandoNaoEncontraPorId() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertTrue(dataSource.findById(USER_ID).isEmpty());
    }

    @Test
    @DisplayName("Consulta por login traduz o registro encontrado; ausência vira vazio")
    void deveBuscarPorLogin() {
        when(users.findByLogin("joao.silva")).thenReturn(Optional.of(userEntity()));
        when(users.findByLogin("ninguem")).thenReturn(Optional.empty());

        assertEquals(USER_ID, dataSource.findByLogin("joao.silva").orElseThrow().id());
        assertTrue(dataSource.findByLogin("ninguem").isEmpty());
    }

    @Test
    @DisplayName("Consulta por e-mail traduz o registro encontrado; ausência vira vazio")
    void deveBuscarPorEmail() {
        when(users.findByEmail("joao.silva@email.com")).thenReturn(Optional.of(userEntity()));
        when(users.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertEquals(USER_ID, dataSource.findByEmail("joao.silva@email.com").orElseThrow().id());
        assertTrue(dataSource.findByEmail("ninguem@email.com").isEmpty());
    }

    @Test
    @DisplayName("Busca pagina os ids no banco e carrega a página em uma segunda consulta")
    void deveBuscarEmDuasConsultas() {
        PageRequest request = new PageRequest(1, 5, "name", SortDirection.ASC);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(1, 5, Sort.by(Sort.Direction.ASC, "name"));
        when(users.findIdsByName(eq("jo"), any())).thenReturn(new PageImpl<>(List.of(USER_ID), pageable, 11));
        when(users.findByIdIn(eq(List.of(USER_ID)), any())).thenReturn(List.of(userEntity()));

        PageResult<UserData> page = dataSource.search("jo", request);

        assertEquals(1, page.content().size());
        assertEquals(USER_ID, page.content().get(0).id());
        assertEquals(1, page.page());
        assertEquals(5, page.size());
        assertEquals(11, page.totalElements());
    }

    @Test
    @DisplayName("Nome nulo vira filtro vazio, que casa com todos os usuários")
    void deveListarTodosQuandoNomeNulo() {
        when(users.findIdsByName(eq(""), any())).thenReturn(new PageImpl<>(List.of(USER_ID)));
        when(users.findByIdIn(any(), any())).thenReturn(List.of(userEntity()));

        assertEquals(1, dataSource.search(null, PageRequest.first()).content().size());
        verify(users).findIdsByName(eq(""), any());
    }

    @Test
    @DisplayName("Página sem ids não dispara a segunda consulta")
    void deveEvitarSegundaConsultaQuandoPaginaVazia() {
        when(users.findIdsByName(anyString(), any())).thenReturn(Page.empty());

        PageResult<UserData> page = dataSource.search("zzz", PageRequest.first());

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        verify(users, never()).findByIdIn(any(), any());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"id", "name", "email", "login", "createdAt", "lastUpdatedAt"})
    @DisplayName("Propriedade permitida é traduzida para o atributo correspondente da entidade JPA")
    void deveTraduzirAOrdenacao(String propriedade) {
        when(users.findIdsByName(anyString(), any())).thenReturn(Page.empty());

        dataSource.search(null, PageRequest.first().withSort(propriedade, SortDirection.ASC));

        assertEquals(Sort.by(Sort.Direction.ASC, propriedade), capturarPageable().getSort());
    }

    @Test
    @DisplayName("Propriedade desconhecida não vira ORDER BY: a ordenação cai no nome")
    void deveIgnorarPropriedadeDesconhecida() {
        when(users.findIdsByName(anyString(), any())).thenReturn(Page.empty());

        dataSource.search(null, PageRequest.first().withSort("password", SortDirection.ASC));

        assertEquals(Sort.by(Sort.Direction.ASC, "name"), capturarPageable().getSort());
    }

    @Test
    @DisplayName("Pedido sem ordenação também cai no nome")
    void deveOrdenarPorNomeQuandoNaoHaOrdenacao() {
        when(users.findIdsByName(anyString(), any())).thenReturn(Page.empty());

        dataSource.search(null, PageRequest.first());

        assertEquals(Sort.by(Sort.Direction.ASC, "name"), capturarPageable().getSort());
    }

    @Test
    @DisplayName("Direção decrescente do núcleo vira DESC no Spring Data")
    void deveTraduzirADirecaoDescendente() {
        when(users.findIdsByName(anyString(), any())).thenReturn(Page.empty());

        dataSource.search(null, PageRequest.first().withSort("email", SortDirection.DESC));

        assertEquals(Sort.by(Sort.Direction.DESC, "email"), capturarPageable().getSort());
    }

    @Test
    @DisplayName("Inserção monta a entidade nova, vincula os papéis do catálogo e grava")
    void deveInserir() {
        when(roles.findAllById(List.of(ROLE_ID))).thenReturn(List.of(roleEntity()));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserData salvo = dataSource.insert(new UserData(null, "João Silva", "joao.silva@email.com", "joao.silva",
                HASH, USER_DATA.roles(), USER_DATA.addresses(), NOW, NOW));

        UserJpaEntity gravado = capturarGravado();
        assertNull(gravado.getId());
        assertEquals("joao.silva", gravado.getLogin());
        assertEquals(HASH, gravado.getPassword());
        assertEquals(NOW, gravado.getCreatedAt());
        assertEquals(ROLE_ID, gravado.getRoles().iterator().next().getId());
        assertEquals("joao.silva", salvo.login());
    }

    @Test
    @DisplayName("Endereço gravado nasce sem id e com o dono religado")
    void deveMontarOsEnderecosDaInsercao() {
        when(roles.findAllById(any())).thenReturn(List.of(roleEntity()));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        dataSource.insert(new UserData(null, "João Silva", "joao.silva@email.com", "joao.silva", HASH,
                USER_DATA.roles(), List.of(new AddressData(ADDRESS_ID, "Rua A", "1", null, "Centro", "São Paulo",
                        "SP", "01001000")), NOW, NOW));

        UserJpaEntity gravado = capturarGravado();
        assertEquals(1, gravado.getAddresses().size());
        assertNull(gravado.getAddresses().get(0).getId());
        assertNull(gravado.getAddresses().get(0).getComplement());
        assertEquals("Rua A", gravado.getAddresses().get(0).getStreet());
        assertSame(gravado, gravado.getAddresses().get(0).getUser());
    }

    @Test
    @DisplayName("Atualização reaproveita a linha existente e substitui a coleção de endereços")
    void deveAtualizar() {
        UserJpaEntity existente = userEntity();
        when(users.findById(USER_ID)).thenReturn(Optional.of(existente));
        when(roles.findAllById(any())).thenReturn(List.of(roleEntity()));
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserData atualizado = dataSource.update(new UserData(USER_ID, "Novo Nome", "novo@email.com", "novo", HASH,
                USER_DATA.roles(), List.of(), NOW.minusDays(1), NOW.plusHours(1)));

        assertSame(existente, capturarGravado());
        assertEquals("Novo Nome", existente.getName());
        assertEquals("novo@email.com", existente.getEmail());
        assertTrue(existente.getAddresses().isEmpty());
        assertEquals(NOW.plusHours(1), existente.getLastUpdatedAt());
        assertEquals("Novo Nome", atualizado.name());
    }

    @Test
    @DisplayName("Atualizar linha inexistente é falha de estado, não regra de negócio")
    void deveRecusarAtualizacaoDeLinhaInexistente() {
        when(users.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> dataSource.update(USER_DATA));
    }

    @Test
    @DisplayName("Exclusão delega ao repositório pelo id")
    void deveExcluir() {
        UUID id = UUID.randomUUID();

        dataSource.delete(id);

        verify(users).deleteById(id);
    }

    private Pageable capturarPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(users).findIdsByName(anyString(), captor.capture());
        return captor.getValue();
    }

    private UserJpaEntity capturarGravado() {
        ArgumentCaptor<UserJpaEntity> captor = ArgumentCaptor.forClass(UserJpaEntity.class);
        verify(users).save(captor.capture());
        return captor.getValue();
    }
}
