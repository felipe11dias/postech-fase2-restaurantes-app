package com.postech.restaurantes.adapter.controller;

import static com.postech.restaurantes.adapter.AdapterFixtures.CLIENT_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.COURIER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.HASH;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.OWNER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.AdapterFixtures.CountingUnitOfWork;
import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import com.postech.restaurantes.application.dto.user.UserAddressDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * O controller é testado de ponta a ponta dentro do núcleo: origens de dados mockadas, casos
 * de uso, gateways e presenters reais. Prova a orquestração, não as regras (que têm testes
 * próprios).
 */
class UserControllerTest {

    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    private static final ClientProfileDTO CLIENT = new ClientProfileDTO("529.982.247-25", "(11) 91234-5678", null);

    private IUserDataSource userDataSource;
    private IRestaurantDataSource restaurantDataSource;
    private IPasswordEncoder passwordEncoder;
    private CountingUnitOfWork unitOfWork;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userDataSource = mock(IUserDataSource.class);
        restaurantDataSource = mock(IRestaurantDataSource.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        unitOfWork = new CountingUnitOfWork();
        controller = UserController.create(userDataSource, restaurantDataSource, passwordEncoder, unitOfWork, CLOCK);
    }

    @Test
    @DisplayName("Recusa qualquer dependência nula na criação")
    void deveRecusarDependenciasNulas() {
        assertThrows(IllegalArgumentException.class,
                () -> UserController.create(null, restaurantDataSource, passwordEncoder, unitOfWork, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> UserController.create(userDataSource, null, passwordEncoder, unitOfWork, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> UserController.create(userDataSource, restaurantDataSource, null, unitOfWork, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> UserController.create(userDataSource, restaurantDataSource, passwordEncoder, null, CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> UserController.create(userDataSource, restaurantDataSource, passwordEncoder, unitOfWork, null));
    }

    @Test
    @DisplayName("Cadastro: monta gateways e caso de uso, executa na unidade de trabalho e apresenta a view")
    void deveOrquestrarCadastro() {
        when(userDataSource.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userDataSource.findByLogin(anyString())).thenReturn(Optional.empty());
        when(userDataSource.findByCpf("52998224725")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha123")).thenReturn(HASH);
        when(userDataSource.insert(any())).thenReturn(USER_DATA);
        NewUserDTO dto = new NewUserDTO("João Silva", "joao.silva@email.com", "joao.silva", "senha123",
                null, CLIENT, null, List.of(new UserAddressDTO(null, "Casa", true,
                        new AddressDTO("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000"))));

        UserView view = controller.register(dto);

        assertEquals(USER_ID, view.id());
        assertEquals("joao.silva", view.login());
        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(userDataSource).insert(captor.capture());
        assertEquals(HASH, captor.getValue().passwordHash());
        assertEquals("52998224725", captor.getValue().client().cpf());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Cadastro com CPF já usado propaga a exceção de domínio do caso de uso")
    void devePropagarExcecaoDeDominio() {
        when(userDataSource.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userDataSource.findByLogin(anyString())).thenReturn(Optional.empty());
        when(userDataSource.findByCpf("52998224725")).thenReturn(Optional.of(USER_DATA));
        NewUserDTO dto = new NewUserDTO("Ana", "ana@x.com", "ana", "senha", null, CLIENT, null, null);

        assertThrows(DuplicateResourceException.class, () -> controller.register(dto));
    }

    @Test
    @DisplayName("Consulta por id apresenta a view; inexistente propaga ResourceNotFound")
    void deveConsultarPorId() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));

        assertEquals("João Silva", controller.findById(USER_ID).name());

        when(userDataSource.findById(USER_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> controller.findById(USER_ID));
        assertEquals(2, unitOfWork.executions());
    }

    @Test
    @DisplayName("Busca paginada repassa nome e ordenação sanitizada e apresenta a página")
    void deveBuscarPaginado() {
        PageRequest request = PageRequest.of(0, 10);
        when(userDataSource.search(any(), any())).thenReturn(new PageResult<>(List.of(USER_DATA), 0, 10, 1));

        PageResult<UserView> page = controller.search(" jo ", request);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(userDataSource).search(org.mockito.ArgumentMatchers.eq("jo"), captor.capture());
        assertEquals("name", captor.getValue().sortBy());
        assertEquals(USER_ID, page.content().get(0).id());
    }

    @Test
    @DisplayName("Atualização grava via origem de dados e apresenta a view atualizada")
    void deveAtualizar() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));
        when(userDataSource.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userDataSource.findByLogin(anyString())).thenReturn(Optional.empty());
        when(userDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UserView view = controller.update(USER_ID, new UpdateUserDTO("Novo Nome", "novo@x.com", "novo", null));

        assertEquals("Novo Nome", view.name());
        assertEquals("novo@x.com", view.email());
        assertEquals(0, view.addresses().size());
    }

    @Test
    @DisplayName("Troca de senha executa na unidade de trabalho e grava o novo hash")
    void deveTrocarSenha() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));
        when(passwordEncoder.matches("atual", HASH)).thenReturn(true);
        when(passwordEncoder.encode("nova")).thenReturn("novoHash");
        when(userDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.changePassword(USER_ID, new ChangePasswordDTO("atual", "nova", "nova"));

        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(userDataSource).update(captor.capture());
        assertEquals("novoHash", captor.getValue().passwordHash());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Exclusão apaga os restaurantes do usuário e o usuário, dentro da unidade de trabalho")
    void deveExcluir() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));

        controller.delete(USER_ID);

        verify(restaurantDataSource).deleteByUserId(USER_ID);
        verify(userDataSource).delete(USER_ID);
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Inclusão de perfil executa na unidade de trabalho e apresenta a view com o papel novo")
    void deveIncluirPerfil() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));
        when(userDataSource.findByCpf(anyString())).thenReturn(Optional.empty());
        when(userDataSource.findByCnpj(anyString())).thenReturn(Optional.empty());
        when(userDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UserView comDono = controller.saveProfile(USER_ID,
                new OwnerProfileDTO("11222333000181", "Sabor Ltda", "1131234567"));

        assertEquals(List.of("ROLE_OWNER", "ROLE_CLIENT"), comDono.roles());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Remover o perfil de dono consulta os restaurantes do usuário pela origem de dados de restaurante")
    void deveConsultarRestaurantesAoRemoverDono() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(new UserData(USER_ID, "João Silva",
                "joao.silva@email.com", "joao.silva", HASH, OWNER_DATA, CLIENT_DATA, null, null, List.of(), null, null)));
        when(restaurantDataSource.existsByUserId(USER_ID)).thenReturn(false);
        when(userDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.removeProfile(USER_ID, "owner");

        verify(restaurantDataSource).existsByUserId(USER_ID);
        ArgumentCaptor<UserData> captor = ArgumentCaptor.forClass(UserData.class);
        verify(userDataSource).update(captor.capture());
        assertNull(captor.getValue().owner());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Troca de status do entregador apresenta a view com o status novo")
    void deveTrocarOStatusDoEntregador() {
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(new UserData(USER_ID, "João Silva",
                "joao.silva@email.com", "joao.silva", HASH, null, null, COURIER_DATA, null, List.of(), null, null)));
        when(userDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UserView view = controller.changeCourierStatus(USER_ID, "BUSY");

        assertEquals("BUSY", view.courier().status());
        assertEquals(1, unitOfWork.executions());
    }
}
