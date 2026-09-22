package com.postech.restaurantes.adapter.gateway;

import static com.postech.restaurantes.adapter.AdapterFixtures.ADDRESS_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.ADDRESS_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.CUSTOMER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.HASH;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.ROLE_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.vo.Email;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserGatewayTest {

    private IUserDataSource dataSource;
    private UserGateway gateway;

    @BeforeEach
    void setUp() {
        dataSource = mock(IUserDataSource.class);
        gateway = UserGateway.create(dataSource);
    }

    @Test
    @DisplayName("Recusa origem de dados nula")
    void deveRecusarOrigemNula() {
        assertThrows(IllegalArgumentException.class, () -> UserGateway.create(null));
    }

    @Test
    @DisplayName("Reconstrói a entidade completa a partir do record da origem de dados")
    void deveReconstruirEntidadeQuandoEncontrado() {
        when(dataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));

        User user = gateway.findById(USER_ID).orElseThrow();

        assertEquals(USER_ID, user.getId());
        assertEquals("João Silva", user.getName());
        assertEquals(Email.of("joao.silva@email.com"), user.getEmail());
        assertEquals("joao.silva", user.getLogin());
        assertEquals(HASH, user.getPasswordHash());
        assertEquals(Set.of(Role.restore(ROLE_ID, RoleName.ROLE_CUSTOMER)), user.getRoles());
        Address address = user.getAddresses().get(0);
        assertEquals(ADDRESS_ID, address.getId());
        assertEquals("01001000", address.getZipCode().value());
        assertEquals(NOW.minusDays(1), user.getCreatedAt());
        assertEquals(NOW, user.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Consultas vazias na origem viram Optional vazio")
    void deveDevolverVazioQuandoNaoEncontrado() {
        when(dataSource.findById(USER_ID)).thenReturn(Optional.empty());
        when(dataSource.findByLogin("x")).thenReturn(Optional.empty());
        when(dataSource.findByEmail("x@x.com")).thenReturn(Optional.empty());

        assertTrue(gateway.findById(USER_ID).isEmpty());
        assertTrue(gateway.findByLogin("x").isEmpty());
        assertTrue(gateway.findByEmail(Email.of("x@x.com")).isEmpty());
    }

    @Test
    @DisplayName("Busca por login e por e-mail (valor normalizado do VO) delegam à origem")
    void deveDelegarBuscasPorLoginEEmail() {
        when(dataSource.findByLogin("joao.silva")).thenReturn(Optional.of(USER_DATA));
        when(dataSource.findByEmail("joao.silva@email.com")).thenReturn(Optional.of(USER_DATA));

        assertEquals(USER_ID, gateway.findByLogin("joao.silva").orElseThrow().getId());
        assertEquals(USER_ID, gateway.findByEmail(Email.of("Joao.Silva@Email.com")).orElseThrow().getId());
    }

    @Test
    @DisplayName("Busca paginada mapeia o conteúdo preservando os metadados")
    void deveMapearPagina() {
        PageRequest request = PageRequest.of(0, 10);
        when(dataSource.search("jo", request)).thenReturn(new PageResult<>(List.of(USER_DATA), 0, 10, 1));

        PageResult<User> page = gateway.search("jo", request);

        assertEquals(1, page.totalElements());
        assertEquals(USER_ID, page.content().get(0).getId());
    }

    @Test
    @DisplayName("Inserção traduz a entidade nova (sem id) para o record e reconstrói com o registro devolvido")
    void deveTraduzirNaInsercao() {
        User novo = User.create("Ana", "Ana@X.com", "ana", "hash", Set.of(Role.restore(ROLE_ID, RoleName.ROLE_CUSTOMER)),
                List.of(Address.create("Rua A", null, null, null, "Cidade", "sp", "01001-000")));
        when(dataSource.insert(any())).thenReturn(USER_DATA);

        User result = gateway.insert(novo);

        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(dataSource).insert(captor.capture());
        UserData sent = captor.getValue();
        assertNull(sent.id());
        assertEquals("ana@x.com", sent.email());
        assertEquals("ROLE_CUSTOMER", sent.roles().iterator().next().name());
        assertEquals(ROLE_ID, sent.roles().iterator().next().id());
        assertNull(sent.addresses().get(0).id());
        assertEquals("SP", sent.addresses().get(0).state());
        assertEquals("01001000", sent.addresses().get(0).zipCode());
        assertNull(sent.createdAt());
        assertEquals(USER_ID, result.getId());
    }

    @Test
    @DisplayName("Atualização traduz e reconstrói do mesmo modo")
    void deveTraduzirNaAtualizacao() {
        User existente = UserGateway.toEntity(USER_DATA);
        when(dataSource.update(any())).thenReturn(USER_DATA);

        User result = gateway.update(existente);

        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(dataSource).update(captor.capture());
        assertEquals(USER_ID, captor.getValue().id());
        assertEquals(ADDRESS_ID, captor.getValue().addresses().get(0).id());
        assertEquals(CUSTOMER_DATA, captor.getValue().roles().iterator().next());
        assertEquals(ADDRESS_DATA, captor.getValue().addresses().get(0));
        assertEquals(USER_ID, result.getId());
    }

    @Test
    @DisplayName("Exclusão delega à origem")
    void deveDelegarExclusao() {
        gateway.delete(USER_ID);

        verify(dataSource).delete(USER_ID);
    }
}
