package com.postech.restaurantes.infrastructure.api.rest.spring.dto;

import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_REQUEST;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CreateRestaurantRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateRestaurantRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.RestaurantResponse;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DTOs HTTP do restaurante: o endereço viaja no corpo, aninhado, nos dois sentidos. */
class RestaurantDtoMappingTest {

    private static final UUID DONO = UUID.randomUUID();
    private static final LocalTime ABRE = LocalTime.of(8, 0);
    private static final LocalTime FECHA = LocalTime.of(22, 0);

    @Test
    @DisplayName("Cadastro repassa o endereço do corpo ao caso de uso")
    void deveConverterOCadastro() {
        CreateRestaurantDTO dto = new CreateRestaurantRequest(DONO, ADDRESS_REQUEST, "Sabor", ABRE, FECHA).toDTO();

        assertEquals(DONO, dto.userId());
        assertEquals("Rua das Flores", dto.address().street());
        assertEquals("01001-000", dto.address().zipCode());
    }

    @Test
    @DisplayName("Sem endereço no corpo, o caso de uso recebe a ausência e decide")
    void deveRepassarAusenciaDeEndereco() {
        assertNull(new CreateRestaurantRequest(DONO, null, "Sabor", ABRE, FECHA).toDTO().address());
        assertNull(new UpdateRestaurantRequest(DONO, null, "Sabor", ABRE, FECHA).toDTO(UUID.randomUUID()).address());
    }

    @Test
    @DisplayName("Atualização leva o id do caminho e o endereço do corpo")
    void deveConverterAAtualizacao() {
        UUID id = UUID.randomUUID();

        UpdateRestaurantDTO dto = new UpdateRestaurantRequest(DONO, ADDRESS_REQUEST, "Novo", ABRE, FECHA).toDTO(id);

        assertEquals(id, dto.id());
        assertEquals("São Paulo", dto.address().city());
    }

    @Test
    @DisplayName("A resposta traz o endereço aninhado, com o CEP normalizado")
    void deveConverterAView() {
        RestaurantView view = new RestaurantView(UUID.randomUUID(), DONO, ADDRESS_VIEW, "Sabor", ABRE, FECHA, null, null);

        RestaurantResponse response = RestaurantResponse.from(view);

        assertEquals(ADDRESS_ID, response.address().id());
        assertEquals("01001000", response.address().zipCode());
    }
}
