package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.gateway.RestaurantGateway;
import com.postech.restaurantes.adapter.gateway.UserGateway;
import com.postech.restaurantes.adapter.presenter.RestaurantPresenter;
import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.application.usecase.restaurant.CreateRestaurantUseCase;
import com.postech.restaurantes.application.usecase.restaurant.DeleteRestaurantUseCase;
import com.postech.restaurantes.application.usecase.restaurant.FindRestaurantByIdUseCase;
import com.postech.restaurantes.application.usecase.restaurant.FindRestaurantOwnerUseCase;
import com.postech.restaurantes.application.usecase.restaurant.SearchRestaurantsUseCase;
import com.postech.restaurantes.application.usecase.restaurant.UpdateRestaurantUseCase;
import com.postech.restaurantes.domain.Guard;
import java.util.Optional;
import java.util.UUID;

/** Controller de adaptação do agregado de restaurante. */
public final class RestaurantController {

    private final IRestaurantDataSource restaurantDataSource;
    private final IUserDataSource userDataSource;
    private final IUnitOfWork unitOfWork;

    private RestaurantController(IRestaurantDataSource restaurantDataSource,
                                 IUserDataSource userDataSource,
                                 IUnitOfWork unitOfWork) {
        this.restaurantDataSource = Guard.requireNonNull(restaurantDataSource, "Origem de dados de restaurante inválida");
        this.userDataSource = Guard.requireNonNull(userDataSource, "Origem de dados de usuário inválida");
        this.unitOfWork = Guard.requireNonNull(unitOfWork, "Unidade de trabalho inválida");
    }

    public static RestaurantController create(IRestaurantDataSource restaurantDataSource,
                                               IUserDataSource userDataSource,
                                               IUnitOfWork unitOfWork) {
        return new RestaurantController(restaurantDataSource, userDataSource, unitOfWork);
    }

    public RestaurantView create(CreateRestaurantDTO dto) {
        var useCase = CreateRestaurantUseCase.create(restaurantGateway(), userGateway());
        return RestaurantPresenter.toView(unitOfWork.execute(() -> useCase.run(dto)));
    }

    public RestaurantView findById(UUID id) {
        var useCase = FindRestaurantByIdUseCase.create(restaurantGateway());
        return RestaurantPresenter.toView(unitOfWork.execute(() -> useCase.run(id)));
    }

    public PageResult<RestaurantView> search(String name, UUID ownerId, PageRequest request) {
        var useCase = SearchRestaurantsUseCase.create(restaurantGateway());
        return RestaurantPresenter.toView(unitOfWork.execute(() -> useCase.run(name, ownerId, request)));
    }

    public RestaurantView update(UpdateRestaurantDTO dto) {
        var useCase = UpdateRestaurantUseCase.create(restaurantGateway(), userGateway());
        return RestaurantPresenter.toView(unitOfWork.execute(() -> useCase.run(dto)));
    }

    public void delete(UUID id) {
        var useCase = DeleteRestaurantUseCase.create(restaurantGateway());
        unitOfWork.execute(() -> useCase.run(id));
    }

    /** O dono do restaurante, para a regra de posse; restaurante inexistente não tem dono. */
    public Optional<UUID> ownerOf(UUID restaurantId) {
        var useCase = FindRestaurantOwnerUseCase.create(restaurantGateway());
        return unitOfWork.execute(() -> useCase.run(restaurantId));
    }

    private RestaurantGateway restaurantGateway() {
        return RestaurantGateway.create(restaurantDataSource);
    }

    private UserGateway userGateway() {
        return UserGateway.create(userDataSource);
    }
}
