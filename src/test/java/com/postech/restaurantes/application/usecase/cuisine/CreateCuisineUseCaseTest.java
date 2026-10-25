package com.postech.restaurantes.application.usecase.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.cuisine.CreateCuisineDTO;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreateCuisineUseCaseTest {

    private ICuisineGateway cuisineGateway;
    private CreateCuisineUseCase useCase;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = CreateCuisineUseCase.create(cuisineGateway);
    }

    @Test
    @DisplayName("Cria o tipo de cozinha com o nome sem espaços nas pontas quando o nome está livre")
    void deveCriarQuandoNomeLivre() {
        UUID id = UUID.randomUUID();
        when(cuisineGateway.findByName("Italiana")).thenReturn(Optional.empty());
        when(cuisineGateway.insert(any())).thenReturn(Cuisine.restore(id, "Italiana", null, null));

        Cuisine criado = useCase.run(new CreateCuisineDTO("  Italiana "));

        ArgumentCaptor<Cuisine> gravado = ArgumentCaptor.forClass(Cuisine.class);
        verify(cuisineGateway).insert(gravado.capture());
        assertEquals("Italiana", gravado.getValue().getName());
        assertEquals(id, criado.getId());
    }

    @Test
    @DisplayName("Recusa o nome que outro tipo já tem, sem gravar")
    void deveRecusarNomeRepetido() {
        when(cuisineGateway.findByName("Italiana"))
                .thenReturn(Optional.of(Cuisine.restore(UUID.randomUUID(), "italiana", null, null)));

        DuplicateResourceException erro = assertThrows(DuplicateResourceException.class,
                () -> useCase.run(new CreateCuisineDTO("Italiana")));

        assertEquals("Tipo de cozinha já cadastrado", erro.getMessage());
        verify(cuisineGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa nome em branco e pedido nulo, sem gravar")
    void deveRecusarEntradaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(new CreateCuisineDTO(" ")));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));

        verify(cuisineGateway, never()).insert(any());
    }
}
