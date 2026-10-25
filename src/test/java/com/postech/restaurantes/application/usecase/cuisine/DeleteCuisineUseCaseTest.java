package com.postech.restaurantes.application.usecase.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.ResourceInUseException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeleteCuisineUseCaseTest {

    private ICuisineGateway cuisineGateway;
    private DeleteCuisineUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = DeleteCuisineUseCase.create(cuisineGateway);
        id = UUID.randomUUID();
    }

    @Test
    @DisplayName("Exclui o tipo quando ele existe e nenhum restaurante o usa")
    void deveExcluirQuandoNaoEstaEmUso() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));
        when(cuisineGateway.isUsedByRestaurants(id)).thenReturn(false);

        useCase.run(id);

        verify(cuisineGateway).delete(id);
    }

    @Test
    @DisplayName("Recusa excluir o tipo que algum restaurante usa, sem excluir")
    void deveRecusarQuandoEmUso() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));
        when(cuisineGateway.isUsedByRestaurants(id)).thenReturn(true);

        ResourceInUseException erro = assertThrows(ResourceInUseException.class, () -> useCase.run(id));

        assertEquals("Tipo de cozinha em uso por restaurantes", erro.getMessage());
        verify(cuisineGateway, never()).delete(id);
    }

    @Test
    @DisplayName("Lança 'não encontrado' quando o tipo não existe, sem excluir")
    void deveLancarExcecaoQuandoNaoExiste() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(id));

        verify(cuisineGateway, never()).delete(id);
    }

    @Test
    @DisplayName("Recusa id nulo")
    void deveRecusarIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
