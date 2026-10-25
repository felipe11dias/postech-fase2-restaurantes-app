package com.postech.restaurantes.application.policy.cuisine;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UniqueCuisineNamePolicyTest {

    private ICuisineGateway cuisineGateway;
    private UniqueCuisineNamePolicy policy;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        policy = UniqueCuisineNamePolicy.create(cuisineGateway);
    }

    @Test
    @DisplayName("Aceita o nome que nenhum tipo tem")
    void deveAceitarNomeLivre() {
        when(cuisineGateway.findByName("Italiana")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> policy.requireUnique("Italiana", null));
    }

    @Test
    @DisplayName("Recusa o nome que outro tipo já tem")
    void deveRecusarNomeDeOutroTipo() {
        Cuisine outro = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        when(cuisineGateway.findByName("Italiana")).thenReturn(Optional.of(outro));

        DuplicateResourceException erro =
                assertThrows(DuplicateResourceException.class, () -> policy.requireUnique("Italiana", UUID.randomUUID()));

        assertEquals("Tipo de cozinha já cadastrado", erro.getMessage());
    }

    @Test
    @DisplayName("Num tipo novo (sem id), qualquer tipo com o nome é conflito")
    void deveRecusarNomeEmTipoNovo() {
        Cuisine existente = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        when(cuisineGateway.findByName("Italiana")).thenReturn(Optional.of(existente));

        assertThrows(DuplicateResourceException.class, () -> policy.requireUnique("Italiana", null));
    }

    @Test
    @DisplayName("O próprio tipo com o mesmo nome não é conflito")
    void deveAceitarOProprioNome() {
        UUID id = UUID.randomUUID();
        when(cuisineGateway.findByName("Italiana")).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));

        assertDoesNotThrow(() -> policy.requireUnique("Italiana", id));
    }
}
