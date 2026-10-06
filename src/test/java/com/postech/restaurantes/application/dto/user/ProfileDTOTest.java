package com.postech.restaurantes.application.dto.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileDTOTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

    @Test
    @DisplayName("Perfil de dono vira entidade com o CNPJ normalizado")
    void deveConverterPerfilDeDono() {
        OwnerProfile owner = new OwnerProfileDTO("11.222.333/0001-81", "Sabor Ltda", "(11) 3123-4567").toEntity();

        assertEquals("11222333000181", owner.getCnpj().value());
        assertEquals("Sabor Ltda", owner.getLegalName());
        assertEquals("1131234567", owner.getBusinessPhone().value());
    }

    @Test
    @DisplayName("Perfil de cliente vira entidade com o CPF normalizado e a data de nascimento")
    void deveConverterPerfilDeCliente() {
        ClientProfile client = new ClientProfileDTO("529.982.247-25", "(11) 91234-5678",
                LocalDate.of(1990, 5, 20)).toEntity(TODAY);

        assertEquals("52998224725", client.getCpf().value());
        assertEquals(LocalDate.of(1990, 5, 20), client.getBirthDate());
    }

    @Test
    @DisplayName("Perfil de cliente recusa nascimento depois do dia de referência")
    void deveRecusarNascimentoFuturo() {
        ClientProfileDTO dto = new ClientProfileDTO("52998224725", "11912345678", TODAY.plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> dto.toEntity(TODAY));
    }

    @Test
    @DisplayName("Perfil de entregador converte o tipo de veículo e começa fora de serviço")
    void deveConverterPerfilDeEntregador() {
        CourierProfile courier = new CourierProfileDTO("52998224725", "11912345678", "MOTORCYCLE",
                "02650306461", "ABC1D23").toEntity();

        assertEquals(CourierVehicleType.MOTORCYCLE, courier.getVehicleType());
        assertEquals("ABC1D23", courier.getVehiclePlate().value());
        assertEquals(CourierStatus.OFFLINE, courier.getStatus());
    }

    @Test
    @DisplayName("Perfil de entregador a pé não tem CNH nem placa")
    void deveConverterEntregadorAPe() {
        CourierProfile courier = new CourierProfileDTO("52998224725", "11912345678", "ON_FOOT", null, null).toEntity();

        assertNull(courier.getDriverLicense());
        assertNull(courier.getVehiclePlate());
    }

    @Test
    @DisplayName("Perfil de entregador recusa tipo de veículo desconhecido com a mensagem do domínio")
    void deveRecusarVeiculoDesconhecido() {
        CourierProfileDTO dto = new CourierProfileDTO("52998224725", "11912345678", "TRUCK", null, null);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, dto::toEntity);

        assertEquals("Tipo de veículo inválido: TRUCK", erro.getMessage());
    }
}
