package com.postech.restaurantes.adapter.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CuisinePresenterTest {

    @Test
    @DisplayName("Converte a entidade na view com todos os dados")
    void deveConverterParaView() {
        UUID id = UUID.randomUUID();
        LocalDateTime criado = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime alterado = LocalDateTime.of(2026, 1, 2, 10, 0);

        CuisineView view = CuisinePresenter.toView(Cuisine.restore(id, "Italiana", criado, alterado));

        assertEquals(new CuisineView(id, "Italiana", criado, alterado), view);
    }

    @Test
    @DisplayName("Converte cada item da página e mantém o total")
    void deveConverterPagina() {
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);

        PageResult<CuisineView> pagina = CuisinePresenter.toView(new PageResult<>(List.of(italiana), 0, 10, 1));

        assertEquals(1, pagina.totalElements());
        assertEquals("Italiana", pagina.content().get(0).name());
    }

    @Test
    @DisplayName("Recusa entidade nula e página nula")
    void deveRecusarParametrosNulos() {
        assertThrows(IllegalArgumentException.class, () -> CuisinePresenter.toView((Cuisine) null));
        assertThrows(IllegalArgumentException.class, () -> CuisinePresenter.toView((PageResult<Cuisine>) null));
    }
}
