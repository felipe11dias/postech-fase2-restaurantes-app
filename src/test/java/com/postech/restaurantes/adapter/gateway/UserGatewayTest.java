package com.postech.restaurantes.adapter.gateway;

import static com.postech.restaurantes.adapter.AdapterFixtures.ADDRESS_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.ADMIN_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.CLIENT_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.COURIER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.HASH;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.OWNER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ADDRESS_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ADDRESS_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Cpf;
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
        assertEquals(Set.of(RoleName.ROLE_CLIENT), user.getRoles());
        assertEquals(Cpf.of("52998224725"), user.getProfiles().client().getCpf());
        UserAddress userAddress = user.getAddresses().get(0);
        assertEquals(USER_ADDRESS_ID, userAddress.getId());
        assertEquals("Casa", userAddress.getLabel());
        assertTrue(userAddress.isDefault());
        assertEquals(ADDRESS_ID, userAddress.getAddress().getId());
        assertEquals("01001000", userAddress.getAddress().getZipCode().value());
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
        User novo = User.create("Ana", "Ana@X.com", "ana", "hash", new UserProfiles(null, ClientProfile.create("52998224725", "11912345678", null, NOW.toLocalDate()), null, null),
                List.of(UserAddress.create("Casa", true,
                        Address.create("Rua A", null, null, null, "Cidade", "sp", "01001-000"))));
        when(dataSource.insert(any())).thenReturn(USER_DATA);

        User result = gateway.insert(novo);

        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(dataSource).insert(captor.capture());
        UserData sent = captor.getValue();
        assertNull(sent.id());
        assertEquals("ana@x.com", sent.email());
        assertEquals(CLIENT_DATA, sent.client());
        assertNull(sent.owner());
        assertNull(sent.courier());
        assertNull(sent.admin());
        assertNull(sent.addresses().get(0).id());
        assertEquals("Casa", sent.addresses().get(0).label());
        assertTrue(sent.addresses().get(0).isDefault());
        assertNull(sent.addresses().get(0).address().id());
        assertEquals("SP", sent.addresses().get(0).address().state());
        assertEquals("01001000", sent.addresses().get(0).address().zipCode());
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
        assertEquals(USER_ADDRESS_ID, captor.getValue().addresses().get(0).id());
        assertEquals(CLIENT_DATA, captor.getValue().client());
        assertEquals(USER_ADDRESS_DATA, captor.getValue().addresses().get(0));
        assertEquals(USER_ID, result.getId());
    }

    @Test
    @DisplayName("Busca por CPF e por CNPJ envia à origem o valor sem máscara do VO")
    void deveDelegarBuscasPorDocumento() {
        when(dataSource.findByCpf("52998224725")).thenReturn(Optional.of(USER_DATA));
        when(dataSource.findByCnpj("11222333000181")).thenReturn(Optional.empty());

        assertEquals(USER_ID, gateway.findByCpf(Cpf.of("529.982.247-25")).orElseThrow().getId());
        assertTrue(gateway.findByCnpj(Cnpj.of("11.222.333/0001-81")).isEmpty());
    }

    @Test
    @DisplayName("Os quatro perfis vão e voltam da origem sem perda, com veículo e status do entregador")
    void deveTraduzirTodosOsPerfis() {
        UserData completo = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH,
                OWNER_DATA, CLIENT_DATA, COURIER_DATA, ADMIN_DATA, List.of(), NOW, NOW);

        User user = UserGateway.toEntity(completo);

        assertEquals("Sabor Ltda", user.getProfiles().owner().getLegalName());
        CourierProfile courier = user.getProfiles().courier();
        assertEquals(CourierVehicleType.MOTORCYCLE, courier.getVehicleType());
        assertEquals(CourierStatus.AVAILABLE, courier.getStatus());
        assertEquals("02650306461", courier.getDriverLicense().value());
        assertTrue(user.getProfiles().admin().isSuperAdmin());
        assertEquals(completo, UserGateway.toData(user));
    }

    @Test
    @DisplayName("Entregador a pé vai para a origem sem CNH nem placa")
    void deveTraduzirEntregadorSemDocumentosDeVeiculo() {
        UserData aPe = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH, null, null,
                new CourierData("52998224725", "11912345678", "ON_FOOT", null, null, "OFFLINE"), null, List.of(),
                NOW, NOW);

        UserData data = UserGateway.toData(UserGateway.toEntity(aPe));

        assertNull(data.courier().driverLicense());
        assertNull(data.courier().vehiclePlate());
        assertEquals("ON_FOOT", data.courier().vehicleType());
        assertFalse(UserGateway.toEntity(aPe).isAdmin());
    }

    @Test
    @DisplayName("Valor de enum desconhecido na origem é recusado ao reconstruir")
    void deveRecusarEnumDesconhecidoDaOrigem() {
        UserData invalido = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH, null,
                null, new CourierData("52998224725", "11912345678", "ON_FOOT", null, null, "SLEEPING"), null,
                List.of(), NOW, NOW);

        assertThrows(IllegalArgumentException.class, () -> UserGateway.toEntity(invalido));
    }

    @Test
    @DisplayName("Contagem de administradores delega à origem")
    void deveDelegarAContagemDeAdministradores() {
        when(dataSource.countAdmins()).thenReturn(3L);

        assertEquals(3L, gateway.countAdmins());
    }

    @Test
    @DisplayName("Exclusão delega à origem")
    void deveDelegarExclusao() {
        gateway.delete(USER_ID);

        verify(dataSource).delete(USER_ID);
    }
}
