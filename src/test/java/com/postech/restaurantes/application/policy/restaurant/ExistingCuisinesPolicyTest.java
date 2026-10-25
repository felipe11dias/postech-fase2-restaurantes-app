package com.postech.restaurantes.application.policy.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExistingCuisinesPolicyTest {

    private ICuisineGateway cuisineGateway;
    private ExistingCuisinesPolicy policy;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        policy = ExistingCuisinesPolicy.create(cuisineGateway);
    }

    @Test
    @DisplayName("Devolve os tipos do catálogo quando todos os ids existem")
    void deveDevolverOsTiposQuandoTodosExistem() {
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        Cuisine japonesa = Cuisine.restore(UUID.randomUUID(), "Japonesa", null, null);
        when(cuisineGateway.findAllById(List.of(italiana.getId(), japonesa.getId())))
                .thenReturn(List.of(italiana, japonesa));

        List<Cuisine> result = policy.requireAll(List.of(italiana.getId(), japonesa.getId()));

        assertEquals(List.of(italiana, japonesa), result);
    }

    @Test
    @DisplayName("O mesmo id repetido conta uma vez só")
    void deveContarUmaVezOIdRepetido() {
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        when(cuisineGateway.findAllById(List.of(italiana.getId()))).thenReturn(List.of(italiana));

        List<Cuisine> result = policy.requireAll(List.of(italiana.getId(), italiana.getId()));

        assertEquals(List.of(italiana), result);
    }

    @Test
    @DisplayName("Lista vazia não escolhe nenhum tipo")
    void deveAceitarListaVazia() {
        when(cuisineGateway.findAllById(List.of())).thenReturn(List.of());

        assertTrue(policy.requireAll(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Recusa quando algum id não está no catálogo")
    void deveRecusarIdInexistente() {
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        UUID desconhecido = UUID.randomUUID();
        when(cuisineGateway.findAllById(List.of(italiana.getId(), desconhecido))).thenReturn(List.of(italiana));

        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class,
                () -> policy.requireAll(List.of(italiana.getId(), desconhecido)));

        assertEquals("Tipo de cozinha não encontrado", erro.getMessage());
    }

    @Test
    @DisplayName("Recusa lista nula e id nulo")
    void deveRecusarEntradaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> policy.requireAll(null));
        assertThrows(IllegalArgumentException.class, () -> policy.requireAll(Arrays.asList(UUID.randomUUID(), null)));
    }
}
