package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.datasource.ILocationDataSource;
import com.postech.restaurantes.adapter.gateway.LocationGateway;
import com.postech.restaurantes.adapter.presenter.LocationPresenter;
import com.postech.restaurantes.adapter.presenter.view.CityView;
import com.postech.restaurantes.adapter.presenter.view.StateView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.application.usecase.location.FindStatesUseCase;
import com.postech.restaurantes.application.usecase.location.SearchCitiesUseCase;
import com.postech.restaurantes.domain.Guard;
import java.util.List;
import java.util.UUID;

public final class LocationController {
    private final ILocationDataSource dataSource;
    private final IUnitOfWork unitOfWork;

    private LocationController(ILocationDataSource dataSource, IUnitOfWork unitOfWork) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de localização inválida");
        this.unitOfWork = Guard.requireNonNull(unitOfWork, "Unidade de trabalho inválida");
    }

    public static LocationController create(ILocationDataSource dataSource, IUnitOfWork unitOfWork) {
        return new LocationController(dataSource, unitOfWork);
    }

    public List<StateView> findStates() {
        var useCase = FindStatesUseCase.create(LocationGateway.create(dataSource));
        return unitOfWork.execute(useCase::run).stream().map(LocationPresenter::toView).toList();
    }

    public PageResult<CityView> searchCities(UUID stateId, String name, PageRequest request) {
        var useCase = SearchCitiesUseCase.create(LocationGateway.create(dataSource));
        return unitOfWork.execute(() -> useCase.run(stateId, name, request)).map(LocationPresenter::toView);
    }
}
