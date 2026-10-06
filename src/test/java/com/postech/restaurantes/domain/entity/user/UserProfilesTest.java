package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.vo.Cpf;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserProfilesTest {

    private static final OwnerProfile DONO = OwnerProfile.create("11222333000181", "Sabor Ltda", "1131234567");
    private static final ClientProfile CLIENTE = ClientProfile.restore("52998224725", "11912345678", null);
    private static final CourierProfile ENTREGADOR =
            CourierProfile.create("52998224725", "11912345678", CourierVehicleType.BICYCLE, null, null);
    private static final AdminProfile ADMIN = AdminProfile.create("ADM-1", null, true);

    @Test
    @DisplayName("Os papéis saem dos perfis, sempre na mesma ordem")
    void deveDerivarOsPapeisDosPerfis() {
        UserProfiles todos = new UserProfiles(DONO, CLIENTE, ENTREGADOR, ADMIN);

        assertEquals(List.of(RoleName.ROLE_OWNER, RoleName.ROLE_CLIENT, RoleName.ROLE_COURIER, RoleName.ROLE_ADMIN),
                List.copyOf(todos.roles()));
        assertEquals(List.of(RoleName.ROLE_COURIER),
                List.copyOf(new UserProfiles(null, null, ENTREGADOR, null).roles()));
        assertThrows(UnsupportedOperationException.class, () -> todos.roles().clear());
    }

    @Test
    @DisplayName("Recusa usuário sem nenhum perfil")
    void deveRecusarSemPerfil() {
        assertThrows(IllegalArgumentException.class, () -> new UserProfiles(null, null, null, null));
    }

    @Test
    @DisplayName("Cliente e entregador do mesmo usuário precisam ter o mesmo CPF")
    void deveExigirOMesmoCpfDeClienteEEntregador() {
        CourierProfile outroCpf =
                CourierProfile.create("11144477735", "11912345678", CourierVehicleType.ON_FOOT, null, null);

        assertThrows(IllegalArgumentException.class, () -> new UserProfiles(null, CLIENTE, outroCpf, null));
    }

    @Test
    @DisplayName("O CPF da pessoa vem do cliente ou do entregador; dono ou admin sozinhos não têm")
    void deveExporOCpfDaPessoa() {
        assertEquals(Optional.of(Cpf.of("52998224725")), new UserProfiles(null, CLIENTE, null, null).cpf());
        assertEquals(Optional.of(Cpf.of("52998224725")), new UserProfiles(null, null, ENTREGADOR, null).cpf());
        assertTrue(new UserProfiles(DONO, null, null, ADMIN).cpf().isEmpty());
    }

    @Test
    @DisplayName("Incluir ou trocar um perfil devolve um conjunto novo, sem mudar o original")
    void deveIncluirOuTrocarPerfilSemMudarOOriginal() {
        UserProfiles soCliente = new UserProfiles(null, CLIENTE, null, null);
        OwnerProfile outroDono = OwnerProfile.create("04252011000110", "Outra Ltda", "1131234567");

        UserProfiles completo = soCliente.withOwner(DONO).withCourier(ENTREGADOR).withAdmin(ADMIN);

        assertEquals(new UserProfiles(DONO, CLIENTE, ENTREGADOR, ADMIN), completo);
        assertSame(outroDono, completo.withOwner(outroDono).owner());
        assertSame(CLIENTE, new UserProfiles(DONO, null, null, null).withClient(CLIENTE).client());
        assertNull(soCliente.owner());
    }

    @Test
    @DisplayName("Incluir perfil nulo é recusado")
    void deveRecusarPerfilNuloAoIncluir() {
        UserProfiles soCliente = new UserProfiles(null, CLIENTE, null, null);

        assertThrows(IllegalArgumentException.class, () -> soCliente.withOwner(null));
        assertThrows(IllegalArgumentException.class, () -> soCliente.withClient(null));
        assertThrows(IllegalArgumentException.class, () -> soCliente.withCourier(null));
        assertThrows(IllegalArgumentException.class, () -> soCliente.withAdmin(null));
    }

    @Test
    @DisplayName("Incluir entregador com CPF diferente do cliente passa pela mesma regra do construtor")
    void deveRecusarEntregadorComOutroCpfAoIncluir() {
        CourierProfile outroCpf =
                CourierProfile.create("11144477735", "11912345678", CourierVehicleType.ON_FOOT, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> new UserProfiles(null, CLIENTE, null, null).withCourier(outroCpf));
    }

    @Test
    @DisplayName("Tirar um perfil deixa os outros; tirar o último é recusado")
    void deveTirarUmPerfil() {
        UserProfiles todos = new UserProfiles(DONO, CLIENTE, ENTREGADOR, ADMIN);

        assertEquals(new UserProfiles(null, CLIENTE, ENTREGADOR, ADMIN), todos.without(ProfileType.OWNER));
        assertEquals(new UserProfiles(DONO, null, ENTREGADOR, ADMIN), todos.without(ProfileType.CLIENT));
        assertEquals(new UserProfiles(DONO, CLIENTE, null, ADMIN), todos.without(ProfileType.COURIER));
        assertEquals(new UserProfiles(DONO, CLIENTE, ENTREGADOR, null), todos.without(ProfileType.ADMIN));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> new UserProfiles(DONO, null, null, null).without(ProfileType.OWNER));
        assertEquals("Usuário deve ter ao menos um perfil", erro.getMessage());
        assertThrows(IllegalArgumentException.class, () -> todos.without(null));
    }

    @Test
    @DisplayName("Diz se tem cada tipo de perfil")
    void deveDizerSeTemOPerfil() {
        UserProfiles donoEAdmin = new UserProfiles(DONO, null, null, ADMIN);

        assertTrue(donoEAdmin.has(ProfileType.OWNER));
        assertFalse(donoEAdmin.has(ProfileType.CLIENT));
        assertFalse(donoEAdmin.has(ProfileType.COURIER));
        assertTrue(donoEAdmin.has(ProfileType.ADMIN));
        assertTrue(new UserProfiles(null, CLIENTE, ENTREGADOR, null).has(ProfileType.COURIER));
        assertTrue(new UserProfiles(null, CLIENTE, ENTREGADOR, null).has(ProfileType.CLIENT));
        assertFalse(new UserProfiles(null, CLIENTE, ENTREGADOR, null).has(ProfileType.ADMIN));
        assertFalse(new UserProfiles(null, CLIENTE, ENTREGADOR, null).has(ProfileType.OWNER));
        assertThrows(IllegalArgumentException.class, () -> donoEAdmin.has(null));
    }
}
