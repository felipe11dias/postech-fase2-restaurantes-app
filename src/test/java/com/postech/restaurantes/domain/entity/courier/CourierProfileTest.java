package com.postech.restaurantes.domain.entity.courier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.vo.Cpf;
import com.postech.restaurantes.domain.vo.Phone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CourierProfileTest {

    private static final String CPF = "529.982.247-25";
    private static final String PHONE = "(11) 91234-5678";
    private static final String CNH = "02650306461";
    private static final String PLACA = "ABC-1D23";

    @Test
    @DisplayName("Entregador de moto nasce fora de serviço, com CNH e placa normalizadas")
    void deveCriarMotorizadoForaDeServico() {
        CourierProfile profile = CourierProfile.create(CPF, PHONE, CourierVehicleType.MOTORCYCLE, CNH, PLACA);

        assertEquals(Cpf.of(CPF), profile.getCpf());
        assertEquals(Phone.of(PHONE), profile.getPhone());
        assertEquals(CourierVehicleType.MOTORCYCLE, profile.getVehicleType());
        assertEquals("02650306461", profile.getDriverLicense().value());
        assertEquals("ABC1D23", profile.getVehiclePlate().value());
        assertEquals(CourierStatus.OFFLINE, profile.getStatus());
    }

    @Test
    @DisplayName("Entregador de bicicleta não tem CNH nem placa; documentos em branco contam como ausentes")
    void deveCriarNaoMotorizadoSemDocumentos() {
        CourierProfile profile = CourierProfile.create(CPF, PHONE, CourierVehicleType.BICYCLE, "  ", null);

        assertNull(profile.getDriverLicense());
        assertNull(profile.getVehiclePlate());
    }

    @Test
    @DisplayName("Restaura com o status gravado")
    void deveRestaurarComOStatus() {
        CourierProfile profile = CourierProfile.restore(CPF, PHONE, CourierVehicleType.CAR, CNH, PLACA,
                CourierStatus.BUSY);

        assertEquals(CourierStatus.BUSY, profile.getStatus());
    }

    @ParameterizedTest
    @EnumSource(value = CourierVehicleType.class, names = {"MOTORCYCLE", "CAR"})
    @DisplayName("Moto e carro sem CNH ou sem placa são recusados")
    void deveExigirDocumentosDeMotorizado(CourierVehicleType tipo) {
        assertThrows(IllegalArgumentException.class, () -> CourierProfile.create(CPF, PHONE, tipo, null, PLACA));
        assertThrows(IllegalArgumentException.class, () -> CourierProfile.create(CPF, PHONE, tipo, CNH, null));
    }

    @ParameterizedTest
    @EnumSource(value = CourierVehicleType.class, names = {"ON_FOOT", "BICYCLE"})
    @DisplayName("A pé ou de bicicleta, CNH ou placa informada é recusada")
    void deveRecusarDocumentosDeNaoMotorizado(CourierVehicleType tipo) {
        assertThrows(IllegalArgumentException.class, () -> CourierProfile.create(CPF, PHONE, tipo, CNH, null));
        assertThrows(IllegalArgumentException.class, () -> CourierProfile.create(CPF, PHONE, tipo, null, PLACA));
    }

    @Test
    @DisplayName("Recusa tipo de veículo e status ausentes, e documentos em formato inválido")
    void deveRecusarCamposInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> CourierProfile.create(CPF, PHONE, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> CourierProfile.restore(CPF, PHONE, CourierVehicleType.ON_FOOT, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> CourierProfile.create(CPF, PHONE, CourierVehicleType.CAR, "123", PLACA));
        assertThrows(IllegalArgumentException.class,
                () -> CourierProfile.create(CPF, PHONE, CourierVehicleType.CAR, CNH, "PLACA"));
        assertThrows(IllegalArgumentException.class,
                () -> CourierProfile.create("123", PHONE, CourierVehicleType.ON_FOOT, null, null));
    }

    @Test
    @DisplayName("Trocar de moto para bicicleta descarta CNH e placa; troca inválida não altera o veículo")
    void deveTrocarOVeiculoComADocumentacao() {
        CourierProfile profile = CourierProfile.create(CPF, PHONE, CourierVehicleType.MOTORCYCLE, CNH, PLACA);

        assertThrows(IllegalArgumentException.class,
                () -> profile.changeVehicle(CourierVehicleType.CAR, CNH, "invalida"));
        assertEquals(CourierVehicleType.MOTORCYCLE, profile.getVehicleType());
        assertEquals("ABC1D23", profile.getVehiclePlate().value());

        profile.changeVehicle(CourierVehicleType.BICYCLE, null, null);

        assertEquals(CourierVehicleType.BICYCLE, profile.getVehicleType());
        assertNull(profile.getDriverLicense());
        assertNull(profile.getVehiclePlate());
    }

    @Test
    @DisplayName("Setters revalidam: status, CPF e telefone")
    void deveRevalidarNosSetters() {
        CourierProfile profile = CourierProfile.create(CPF, PHONE, CourierVehicleType.ON_FOOT, null, null);

        profile.changeStatus(CourierStatus.AVAILABLE);
        profile.setCpf(Cpf.of("111.444.777-35"));
        profile.setPhone(Phone.of("1131234567"));

        assertEquals(CourierStatus.AVAILABLE, profile.getStatus());
        assertEquals("11144477735", profile.getCpf().value());
        assertEquals("1131234567", profile.getPhone().value());
        assertThrows(IllegalArgumentException.class, () -> profile.changeStatus(null));
        assertThrows(IllegalArgumentException.class, () -> profile.setCpf(null));
        assertThrows(IllegalArgumentException.class, () -> profile.setPhone(null));
    }
}
