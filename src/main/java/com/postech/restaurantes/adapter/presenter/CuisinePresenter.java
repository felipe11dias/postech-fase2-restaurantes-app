package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;

public final class CuisinePresenter {

    private CuisinePresenter() {
    }

    public static CuisineView toView(Cuisine cuisine) {
        Guard.requireNonNull(cuisine, "Tipo de cozinha inválido");
        return new CuisineView(cuisine.getId(), cuisine.getName(), cuisine.getCreatedAt(),
                cuisine.getLastUpdatedAt());
    }

    public static PageResult<CuisineView> toView(PageResult<Cuisine> page) {
        Guard.requireNonNull(page, "Página inválida");
        return page.map(CuisinePresenter::toView);
    }
}