package com.postech.restaurantes.application.usecase.user;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLOCK;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.HASH;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.NOW;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.otherUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.user.AdminProfileDTO;
import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import com.postech.restaurantes.application.dto.user.CourierProfileDTO;
import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Cpf;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaveUserProfileUseCaseTest {

    private static final OwnerProfileDTO DONO = new OwnerProfileDTO("11.222.333/0001-81", "Sabor Ltda", "1131234567");
    private static final CourierProfileDTO ENTREGADOR =
            new CourierProfileDTO("529.982.247-25", "11912345678", "BICYCLE", null, null);

    private IUserGateway userGateway;
    private SaveUserProfileUseCase useCase;
    private User usuario;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        useCase = SaveUserProfileUseCase.create(userGateway, CLOCK);
        usuario = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(usuario));
        when(userGateway.findByCpf(any())).thenReturn(Optional.empty());
        when(userGateway.findByCnpj(any())).thenReturn(Optional.empty());
        when(userGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Inclui o perfil de dono num cliente: os dois papéis passam a valer")
    void deveIncluirPerfilDeDono() {
        User result = useCase.run(USER_ID, DONO);

        assertSame(usuario, result);
        assertEquals(Set.of(RoleName.ROLE_OWNER, RoleName.ROLE_CLIENT), result.getRoles());
        assertEquals(Cnpj.of("11222333000181"), result.getProfiles().owner().getCnpj());
        verify(userGateway).update(usuario);
    }

    @Test
    @DisplayName("Altera o perfil de cliente que o usuário já tem; o dia do relógio vale para o nascimento")
    void deveAlterarPerfilDeCliente() {
        User result = useCase.run(USER_ID, new ClientProfileDTO("52998224725", "1133334444", NOW.toLocalDate()));

        assertEquals("1133334444", result.getProfiles().client().getPhone().value());
        assertEquals(NOW.toLocalDate(), result.getProfiles().client().getBirthDate());
    }

    @Test
    @DisplayName("Recusa nascimento depois do dia do relógio")
    void deveRecusarNascimentoFuturo() {
        ClientProfileDTO futuro = new ClientProfileDTO("52998224725", "11912345678", NOW.toLocalDate().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, futuro));

        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Entregador novo começa fora de serviço")
    void deveIncluirEntregadorForaDeServico() {
        User result = useCase.run(USER_ID, ENTREGADOR);

        assertEquals(CourierStatus.OFFLINE, result.getProfiles().courier().getStatus());
        assertTrue(result.hasRole(RoleName.ROLE_COURIER));
    }

    @Test
    @DisplayName("Alterar o entregador troca o veículo e mantém o status em que ele está")
    void deveManterOStatusAoAlterarEntregador() {
        CourierProfile atual =
                CourierProfile.restore("52998224725", "11912345678", CourierVehicleType.BICYCLE, null, null,
                        CourierStatus.BUSY);
        usuario.replaceProfiles(usuario.getProfiles().withCourier(atual));

        User result = useCase.run(USER_ID,
                new CourierProfileDTO("52998224725", "11912345678", "CAR", "02650306461", "ABC1D23"));

        assertEquals(CourierVehicleType.CAR, result.getProfiles().courier().getVehicleType());
        assertEquals(CourierStatus.BUSY, result.getProfiles().courier().getStatus());
    }

    @Test
    @DisplayName("Recusa entregador com CPF diferente do cliente (regra do domínio)")
    void deveRecusarEntregadorComOutroCpf() {
        CourierProfileDTO outroCpf = new CourierProfileDTO("11144477735", "11912345678", "BICYCLE", null, null);

        assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, outroCpf));

        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Inclui o perfil de administrador")
    void deveIncluirPerfilDeAdministrador() {
        User result = useCase.run(USER_ID, new AdminProfileDTO("ADM-7", "Suporte", null));

        assertTrue(result.isAdmin());
        assertEquals("ADM-7", result.getProfiles().admin().getEmployeeCode());
    }

    @Test
    @DisplayName("Documento que não mudou não é consultado; o que já está com o próprio usuário não é conflito")
    void deveAceitarOsDocumentosDoProprioUsuario() {
        when(userGateway.findByCnpj(Cnpj.of("11222333000181"))).thenReturn(Optional.of(usuario));

        User result = useCase.run(USER_ID, DONO);

        assertEquals(Set.of(RoleName.ROLE_OWNER, RoleName.ROLE_CLIENT), result.getRoles());
        verify(userGateway, never()).findByCpf(any());
    }

    @Test
    @DisplayName("Recusa CPF de outro cadastro ao trocar o CPF")
    void deveRecusarCpfDeOutroCadastro() {
        when(userGateway.findByCpf(Cpf.of("11144477735"))).thenReturn(Optional.of(otherUser()));
        ClientProfileDTO outroCpf = new ClientProfileDTO("111.444.777-35", "11912345678", null);

        DuplicateResourceException erro =
                assertThrows(DuplicateResourceException.class, () -> useCase.run(USER_ID, outroCpf));

        assertEquals("CPF já cadastrado", erro.getMessage());
        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa CNPJ de outro cadastro")
    void deveRecusarCnpjDeOutroCadastro() {
        when(userGateway.findByCnpj(Cnpj.of("11222333000181"))).thenReturn(Optional.of(otherUser()));

        DuplicateResourceException erro = assertThrows(DuplicateResourceException.class, () -> useCase.run(USER_ID, DONO));

        assertEquals("CNPJ já cadastrado", erro.getMessage());
    }

    @Test
    @DisplayName("Usuário só com perfil de administrador não tem CPF a conferir")
    void naoDeveConferirCpfSemPessoa() {
        User admin = User.restore(USER_ID, "Admin", "admin@email.com", "admin", HASH,
                new UserProfiles(null, null, null,
                        AdminProfile.restore("ADM-1", null, true)),
                List.of(), NOW, NOW);
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(admin));

        useCase.run(USER_ID, new AdminProfileDTO("ADM-2", null, true));

        verify(userGateway, never()).findByCpf(any());
        verify(userGateway, never()).findByCnpj(any());
    }

    @Test
    @DisplayName("Usuário inexistente: 404")
    void deveRecusarUsuarioInexistente() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(USER_ID, DONO));
    }

    @Test
    @DisplayName("Recusa perfil nulo, id nulo e relógio nulo")
    void deveRecusarEntradasNulas() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, null));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null, DONO));
        assertThrows(IllegalArgumentException.class, () -> SaveUserProfileUseCase.create(userGateway, null));
    }

    @Test
    @DisplayName("O cliente da fixture não é alterado por um perfil recusado")
    void naoDeveAlterarPerfisQuandoRecusa() {
        ClientProfile antes = usuario.getProfiles().client();
        when(userGateway.findByCnpj(any())).thenReturn(Optional.of(otherUser()));

        assertThrows(DuplicateResourceException.class, () -> useCase.run(USER_ID, DONO));

        assertSame(antes, usuario.getProfiles().client());
        assertNull(usuario.getProfiles().owner());
    }

    @Test
    @DisplayName("Corrigir o CPF do cliente corrige o do entregador, e o CPF novo é conferido contra os outros cadastros")
    void deveCorrigirOCpfDaPessoa() {
        usuario.replaceProfiles(usuario.getProfiles().withCourier(
                CourierProfile.create("52998224725", "11912345678", CourierVehicleType.ON_FOOT, null, null)));

        User result = useCase.run(USER_ID, new ClientProfileDTO("111.444.777-35", "11912345678", null));

        assertEquals(Cpf.of("11144477735"), result.getProfiles().client().getCpf());
        assertEquals(Cpf.of("11144477735"), result.getProfiles().courier().getCpf());
        verify(userGateway).findByCpf(Cpf.of("11144477735"));
    }
}
