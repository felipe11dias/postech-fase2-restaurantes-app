package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.datasource.ICuisineDataSource;
import com.postech.restaurantes.adapter.gateway.CuisineGateway;
import com.postech.restaurantes.adapter.presenter.CuisinePresenter;
import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.cuisine.CreateCuisineDTO;
import com.postech.restaurantes.application.dto.cuisine.UpdateCuisineDTO;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.application.usecase.cuisine.*;
import com.postech.restaurantes.domain.Guard;

import java.util.UUID;

public final class CuisineController {

    private final ICuisineDataSource cuisineDataSource;
    private final IUnitOfWork unitOfWork;

    private CuisineController(ICuisineDataSource cuisineDataSource, IUnitOfWork unitOfWork) {
        this.cuisineDataSource = Guard.requireNonNull(cuisineDataSource, "Origem de dados de tipo de cozinha inválida");
        this.unitOfWork = Guard.requireNonNull(unitOfWork, "Unidade de trabalho inválida");
    }

    public static CuisineController create(ICuisineDataSource cuisineDataSource, IUnitOfWork unitOfWork) {
        return new CuisineController(cuisineDataSource, unitOfWork);
    }

    public CuisineView create(CreateCuisineDTO dto) {
        var useCase = CreateCuisineUseCase.create(cuisineGateway());
        return CuisinePresenter.toView(unitOfWork.execute(() -> useCase.run(dto)));
    }

    public CuisineView findById(UUID id) {
        var useCase = FindCuisineByIdUseCase.create(cuisineGateway());
        return CuisinePresenter.toView(unitOfWork.execute(() -> useCase.run(id)));
    }

    public PageResult<CuisineView> search(String name, PageRequest request) {
        var useCase = SearchCuisinesUseCase.create(cuisineGateway());
        return CuisinePresenter.toView(unitOfWork.execute(() -> useCase.run(name, request)));
    }

    public CuisineView update(UpdateCuisineDTO dto) {
        var useCase = UpdateCuisineUseCase.create(cuisineGateway());
        return CuisinePresenter.toView(unitOfWork.execute(() -> useCase.run(dto)));
    }

    public void delete(UUID id) {
        var useCase = DeleteCuisineUseCase.create(cuisineGateway());
        unitOfWork.execute(() -> useCase.run(id));
    }

    private CuisineGateway cuisineGateway() {
        return CuisineGateway.create(cuisineDataSource);
    }
}
