package com.postech.restaurantes.application.usecase.user;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLOCK;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.NOW;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ADDRESS_DTO;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import com.postech.restaurantes.application.dto.user.CourierProfileDTO;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
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

class RegisterUserUseCaseTest {

    private static final ClientProfileDTO CLIENT = new ClientProfileDTO("529.982.247-25", "(11) 91234-5678", null);
    private static final OwnerProfileDTO OWNER =
            new OwnerProfileDTO("11.222.333/0001-81", "Sabor Ltda", "(11) 3123-4567");
    private static final CourierProfileDTO COURIER =
            new CourierProfileDTO("529.982.247-25", "(11) 91234-5678", "MOTORCYCLE", "02650306461", "ABC1D23");

    private IUserGateway userGateway;
    private IPasswordEncoder passwordEncoder;
    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        useCase = RegisterUserUseCase.create(userGateway, passwordEncoder, CLOCK);
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(userGateway.findByCpf(any())).thenReturn(Optional.empty());
        when(userGateway.findByCnpj(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static NewUserDTO dto(OwnerProfileDTO owner, ClientProfileDTO client, CourierProfileDTO courier) {
        return new NewUserDTO("João Silva", "Joao.Silva@Email.com", "joao.silva", "senhaSegura123",
                owner, client, courier, List.of(USER_ADDRESS_DTO));
    }

    private User inserted() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userGateway).insert(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Cadastra cliente válido com e-mail normalizado, senha em hash e papel derivado do perfil")
    void deveCadastrarClienteQuandoValido() {
        User persisted = existingUser();
        when(userGateway.insert(any())).thenReturn(persisted);

        User result = useCase.run(dto(null, CLIENT, null));

        assertSame(persisted, result);
        User created = inserted();
        assertEquals(Email.of("joao.silva@email.com"), created.getEmail());
        assertEquals("hash", created.getPasswordHash());
        assertEquals(Set.of(RoleName.ROLE_CLIENT), created.getRoles());
        assertEquals(Cpf.of("52998224725"), created.getProfiles().client().getCpf());
        assertEquals(1, created.getAddresses().size());
        verify(userGateway).findByEmail(Email.of("joao.silva@email.com"));
        verify(userGateway).findByCpf(Cpf.of("52998224725"));
        verify(userGateway, never()).findByCnpj(any());
    }

    @Test
    @DisplayName("Cadastra dono, cliente e entregador ao mesmo tempo; o entregador começa fora de serviço")
    void deveCadastrarComVariosPerfis() {
        useCase.run(dto(OWNER, CLIENT, COURIER));

        User created = inserted();
        assertEquals(Set.of(RoleName.ROLE_OWNER, RoleName.ROLE_CLIENT, RoleName.ROLE_COURIER), created.getRoles());
        assertEquals(CourierStatus.OFFLINE, created.getProfiles().courier().getStatus());
        assertNull(created.getProfiles().admin());
        verify(userGateway).findByCnpj(Cnpj.of("11222333000181"));
    }

    @Test
    @DisplayName("Cadastra entregador sem perfil de cliente consultando o CPF dele")
    void deveConsultarCpfDoEntregadorQuandoSemCliente() {
        useCase.run(dto(null, null, COURIER));

        assertEquals(Set.of(RoleName.ROLE_COURIER), inserted().getRoles());
        verify(userGateway).findByCpf(Cpf.of("52998224725"));
    }

    @Test
    @DisplayName("Cadastra dono sem CPF sem consultar CPF")
    void deveCadastrarDonoSemConsultarCpf() {
        useCase.run(dto(OWNER, null, null));

        assertEquals(Set.of(RoleName.ROLE_OWNER), inserted().getRoles());
        verify(userGateway, never()).findByCpf(any());
    }

    @Test
    @DisplayName("Usa o dia do relógio da aplicação para recusar nascimento futuro")
    void deveRecusarNascimentoFuturoPeloRelogio() {
        ClientProfileDTO futuro = new ClientProfileDTO("52998224725", "11912345678", NOW.toLocalDate().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(null, futuro, null)));

        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Aceita nascimento no próprio dia do relógio")
    void deveAceitarNascimentoNoDiaDoRelogio() {
        ClientProfileDTO hoje = new ClientProfileDTO("52998224725", "11912345678", NOW.toLocalDate());

        useCase.run(dto(null, hoje, null));

        assertEquals(NOW.toLocalDate(), inserted().getProfiles().client().getBirthDate());
    }

    @Test
    @DisplayName("Recusa cadastro sem nenhum perfil, sem consultar o banco")
    void deveRecusarQuandoSemPerfis() {
        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(null, null, null)));

        assertEquals("Usuário deve ter ao menos um perfil", erro.getMessage());
        verify(userGateway, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Recusa cliente e entregador com CPFs diferentes")
    void deveRecusarQuandoCpfsDivergem() {
        CourierProfileDTO outroCpf =
                new CourierProfileDTO("111.444.777-35", "11912345678", "BICYCLE", null, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(null, CLIENT, outroCpf)));
    }

    @Test
    @DisplayName("Recusa tipo de veículo desconhecido com a mensagem do domínio")
    void deveRecusarQuandoVeiculoDesconhecido() {
        CourierProfileDTO caminhao =
                new CourierProfileDTO("52998224725", "11912345678", "TRUCK", null, null);

        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(null, null, caminhao)));

        assertEquals("Tipo de veículo inválido: TRUCK", erro.getMessage());
    }

    @Test
    @DisplayName("Recusa dados nulos")
    void deveRecusarQuandoDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }

    @Test
    @DisplayName("Recusa relógio nulo na criação")
    void deveRecusarQuandoRelogioNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> RegisterUserUseCase.create(userGateway, passwordEncoder, null));
    }

    @Test
    @DisplayName("Recusa e-mail já cadastrado")
    void deveRecusarQuandoEmailDuplicado() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.of(existingUser()));

        DuplicateResourceException erro =
                assertThrows(DuplicateResourceException.class, () -> useCase.run(dto(null, CLIENT, null)));

        assertEquals("E-mail já cadastrado", erro.getMessage());
        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa login já cadastrado")
    void deveRecusarQuandoLoginDuplicado() {
        when(userGateway.findByLogin("joao.silva")).thenReturn(Optional.of(existingUser()));

        DuplicateResourceException erro =
                assertThrows(DuplicateResourceException.class, () -> useCase.run(dto(null, CLIENT, null)));

        assertEquals("Login já cadastrado", erro.getMessage());
        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa CPF já cadastrado")
    void deveRecusarQuandoCpfDuplicado() {
        when(userGateway.findByCpf(Cpf.of("52998224725"))).thenReturn(Optional.of(existingUser()));

        DuplicateResourceException erro =
                assertThrows(DuplicateResourceException.class, () -> useCase.run(dto(null, CLIENT, null)));

        assertEquals("CPF já cadastrado", erro.getMessage());
        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa CNPJ já cadastrado")
    void deveRecusarQuandoCnpjDuplicado() {
        when(userGateway.findByCnpj(Cnpj.of("11222333000181"))).thenReturn(Optional.of(existingUser()));

        DuplicateResourceException erro =
                assertThrows(DuplicateResourceException.class, () -> useCase.run(dto(OWNER, null, null)));

        assertEquals("CNPJ já cadastrado", erro.getMessage());
        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa senha em branco antes de calcular o hash")
    void deveRecusarQuandoSenhaEmBranco() {
        NewUserDTO semSenha = new NewUserDTO("Ana", "ana@x.com", "ana", " ", OWNER, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.run(semSenha));

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Cadastro sem endereços resulta em lista vazia")
    void deveAceitarSemEnderecos() {
        NewUserDTO semEndereco = new NewUserDTO("Ana", "ana@x.com", "ana", "senha", OWNER, null, null, null);

        User result = useCase.run(semEndereco);

        assertTrue(result.getAddresses().isEmpty());
    }
}
