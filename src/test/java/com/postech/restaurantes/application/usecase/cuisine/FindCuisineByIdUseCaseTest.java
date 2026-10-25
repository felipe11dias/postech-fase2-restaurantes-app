package com.postech.restaurantes.application.usecase.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindCuisineByIdUseCaseTest {

    private ICuisineGateway cuisineGateway;
    private FindCuisineByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = FindCuisineByIdUseCase.create(cuisineGateway);
    }

    @Test
    @DisplayName("Devolve o tipo de cozinha quando o id existe")
    void deveBuscarQuandoExiste() {
        UUID id = UUID.randomUUID();
        when(cuisineGateway.findById(id)).thenReturn(Optional.of(Cuisine.restore(id, "Italiana", null, null)));

        Cuisine encontrado = useCase.run(id);

        assertEquals("Italiana", encontrado.getName());
    }

    @Test
    @DisplayName("Lança 'não encontrado' quando o id não existe")
    void deveLancarExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(cuisineGateway.findById(id)).thenReturn(Optional.empty());

        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class, () -> useCase.run(id));

        assertEquals("Tipo de cozinha não encontrado", erro.getMessage());
    }

    @Test
    @DisplayName("Recusa id nulo")
    void deveRecusarIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
