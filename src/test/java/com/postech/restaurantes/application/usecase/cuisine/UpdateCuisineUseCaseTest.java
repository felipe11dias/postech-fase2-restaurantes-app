package com.postech.restaurantes.application.usecase.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.cuisine.UpdateCuisineDTO;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateCuisineUseCaseTest {

    private ICuisineGateway cuisineGateway;
    private UpdateCuisineUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = UpdateCuisineUseCase.create(cuisineGateway);
        id = UUID.randomUUID();
        when(cuisineGateway.update(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    @DisplayName("Troca o nome quando o tipo existe e o nome novo está livre")
    void deveAtualizarQuandoNomeLivre() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));
        when(cuisineGateway.findByName("Massas")).thenReturn(Optional.empty());

        Cuisine atualizado = useCase.run(new UpdateCuisineDTO(id, " Massas "));

        assertEquals("Massas", atualizado.getName());
        assertEquals(id, atualizado.getId());
    }

    @Test
    @DisplayName("Permite manter o próprio nome, mudando só as maiúsculas")
    void devePermitirOProprioNome() {
        Cuisine existente = Cuisine.restore(id, "italiana", null, null);
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(existente));
        when(cuisineGateway.findByName("Italiana")).thenReturn(Optional.of(existente));

        Cuisine atualizado = useCase.run(new UpdateCuisineDTO(id, "Italiana"));

        assertEquals("Italiana", atualizado.getName());
    }

    @Test
    @DisplayName("Recusa o nome de outro tipo, sem gravar")
    void deveRecusarNomeDeOutroTipo() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));
        when(cuisineGateway.findByName("Japonesa"))
                .thenReturn(Optional.of(Cuisine.restore(UUID.randomUUID(), "Japonesa", null, null)));

        DuplicateResourceException erro = assertThrows(DuplicateResourceException.class,
                () -> useCase.run(new UpdateCuisineDTO(id, "Japonesa")));

        assertEquals("Tipo de cozinha já cadastrado", erro.getMessage());
        verify(cuisineGateway, never()).update(any());
    }

    @Test
    @DisplayName("Lança 'não encontrado' quando o tipo não existe")
    void deveLancarExcecaoQuandoNaoExiste() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(new UpdateCuisineDTO(id, "Massas")));
    }

    @Test
    @DisplayName("Recusa pedido nulo, id nulo e nome em branco")
    void deveRecusarEntradaInvalida() {
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));

        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(new UpdateCuisineDTO(null, "Massas")));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(new UpdateCuisineDTO(id, " ")));
        verify(cuisineGateway, never()).update(any());
    }
}
