package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
