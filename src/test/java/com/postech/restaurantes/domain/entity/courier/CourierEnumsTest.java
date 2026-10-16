package com.postech.restaurantes.domain.entity.courier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Os dois enums do modelo de dados usados pelo entregador. */
class CourierEnumsTest {

    @Test
    @DisplayName("Tipos de veículo são exatamente os do tipo courier_vehicle_type do modelo")
    void deveTerOsTiposDoModelo() {
        assertEquals(List.of("ON_FOOT", "BICYCLE", "MOTORCYCLE", "CAR"),
                Arrays.stream(CourierVehicleType.values()).map(Enum::name).toList());
    }

    @Test
    @DisplayName("Só moto e carro exigem CNH e placa")
    void deveExigirDocumentacaoSoDeMotorizados() {
        assertTrue(CourierVehicleType.MOTORCYCLE.requiresLicense());
        assertTrue(CourierVehicleType.CAR.requiresLicense());
        assertFalse(CourierVehicleType.ON_FOOT.requiresLicense());
        assertFalse(CourierVehicleType.BICYCLE.requiresLicense());
    }

    @Test
    @DisplayName("Tipo de veículo é lido do texto sem diferenciar maiúsculas e sem espaços nas bordas")
    void deveLerOTipoDoTexto() {
        assertEquals(CourierVehicleType.MOTORCYCLE, CourierVehicleType.from(" motorcycle "));
        assertEquals(CourierVehicleType.ON_FOOT, CourierVehicleType.from("ON_FOOT"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "TRUCK"})
    @DisplayName("Tipo de veículo em branco ou desconhecido é recusado com mensagem do domínio")
    void deveRecusarTipoDesconhecido(String valor) {
        assertThrows(IllegalArgumentException.class, () -> CourierVehicleType.from(valor));
    }

    @Test
    @DisplayName("Status são exatamente os do tipo courier_status do modelo e são lidos do texto")
    void deveTerOsStatusDoModelo() {
        assertEquals(List.of("OFFLINE", "AVAILABLE", "BUSY"),
                Arrays.stream(CourierStatus.values()).map(Enum::name).toList());
        assertEquals(CourierStatus.AVAILABLE, CourierStatus.from(" available "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ONLINE"})
    @DisplayName("Status em branco ou desconhecido é recusado com mensagem do domínio")
    void deveRecusarStatusDesconhecido(String valor) {
        assertThrows(IllegalArgumentException.class, () -> CourierStatus.from(valor));
    }
}
