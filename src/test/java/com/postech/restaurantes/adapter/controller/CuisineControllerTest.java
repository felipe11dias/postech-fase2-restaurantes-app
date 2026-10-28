package com.postech.restaurantes.adapter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.AdapterFixtures.CountingUnitOfWork;
import com.postech.restaurantes.adapter.datasource.ICuisineDataSource;
import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.cuisine.CreateCuisineDTO;
import com.postech.restaurantes.application.dto.cuisine.UpdateCuisineDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CuisineControllerTest {

    private ICuisineDataSource dataSource;
    private CountingUnitOfWork unitOfWork;
    private CuisineController controller;
    private UUID id;
    private CuisineData italiana;

    @BeforeEach
    void setUp() {
        dataSource = mock(ICuisineDataSource.class);
        unitOfWork = new CountingUnitOfWork();
        controller = CuisineController.create(dataSource, unitOfWork);
        id = UUID.randomUUID();
        italiana = new CuisineData(id, "Italiana", null, null);
    }

    @Test
    @DisplayName("Cadastra dentro de uma transação e devolve a view")
    void deveCriar() {
        when(dataSource.findByName("Italiana")).thenReturn(Optional.empty());
        when(dataSource.insert(any())).thenReturn(italiana);

        CuisineView view = controller.create(new CreateCuisineDTO("Italiana"));

        assertEquals(new CuisineView(id, "Italiana", null, null), view);
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Busca por id dentro de uma transação e devolve a view")
    void deveBuscarPorId() {
        when(dataSource.findById(id)).thenReturn(Optional.of(italiana));

        assertEquals("Italiana", controller.findById(id).name());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Lista dentro de uma transação e devolve a página de views")
    void deveListar() {
        when(dataSource.search(any(), any())).thenReturn(new PageResult<>(List.of(italiana), 0, 10, 1));

        PageResult<CuisineView> pagina = controller.search("ital", PageRequest.of(0, 10));

        assertEquals(1, pagina.totalElements());
        assertEquals("Italiana", pagina.content().get(0).name());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Altera dentro de uma transação e devolve a view")
    void deveAtualizar() {
        when(dataSource.findById(id)).thenReturn(Optional.of(italiana));
        when(dataSource.findByName("Massas")).thenReturn(Optional.empty());
        when(dataSource.update(any())).thenReturn(new CuisineData(id, "Massas", null, null));

        assertEquals("Massas", controller.update(new UpdateCuisineDTO(id, "Massas")).name());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Exclui dentro de uma transação")
    void deveExcluir() {
        when(dataSource.findById(id)).thenReturn(Optional.of(italiana));
        when(dataSource.existsInRestaurants(id)).thenReturn(false);

        controller.delete(id);

        verify(dataSource).delete(id);
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Recusa origem de dados nula e unidade de trabalho nula")
    void deveRecusarDependenciasNulas() {
        assertThrows(IllegalArgumentException.class, () -> CuisineController.create(null, unitOfWork));
        assertThrows(IllegalArgumentException.class, () -> CuisineController.create(dataSource, null));
    }
}
