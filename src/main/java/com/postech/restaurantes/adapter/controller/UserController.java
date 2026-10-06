package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.gateway.RestaurantGateway;
import com.postech.restaurantes.adapter.gateway.UserGateway;
import com.postech.restaurantes.adapter.presenter.UserPresenter;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import com.postech.restaurantes.application.dto.user.UserProfileDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.application.usecase.user.ChangeCourierStatusUseCase;
import com.postech.restaurantes.application.usecase.user.ChangePasswordUseCase;
import com.postech.restaurantes.application.usecase.user.DeleteUserUseCase;
import com.postech.restaurantes.application.usecase.user.FindUserByIdUseCase;
import com.postech.restaurantes.application.usecase.user.RegisterUserUseCase;
import com.postech.restaurantes.application.usecase.user.RemoveUserProfileUseCase;
import com.postech.restaurantes.application.usecase.user.SaveUserProfileUseCase;
import com.postech.restaurantes.application.usecase.user.SearchUsersUseCase;
import com.postech.restaurantes.application.usecase.user.UpdateUserUseCase;
import com.postech.restaurantes.domain.Guard;
import java.time.Clock;
import java.util.UUID;

/**
 * Controller de adaptação do agregado de usuário. Recebe as origens de dados por interface e,
 * a cada operação, monta gateway + caso de uso, executa dentro da unidade de trabalho e entrega
 * o resultado ao presenter. Não contém regra de negócio: sabe "quem sabe" e em que ordem.
 */
public final class UserController {

    private final IUserDataSource userDataSource;
    private final IRestaurantDataSource restaurantDataSource;
    private final IPasswordEncoder passwordEncoder;
    private final IUnitOfWork unitOfWork;
    private final Clock clock;

    private UserController(IUserDataSource userDataSource, IRestaurantDataSource restaurantDataSource,
                           IPasswordEncoder passwordEncoder, IUnitOfWork unitOfWork, Clock clock) {
        this.userDataSource = Guard.requireNonNull(userDataSource, "Origem de dados de usuário inválida");
        this.restaurantDataSource = Guard.requireNonNull(restaurantDataSource, "Origem de dados de restaurante inválida");
        this.passwordEncoder = Guard.requireNonNull(passwordEncoder, "Codificador de senha inválido");
        this.unitOfWork = Guard.requireNonNull(unitOfWork, "Unidade de trabalho inválida");
        this.clock = Guard.requireNonNull(clock, "Relógio inválido");
    }

    public static UserController create(IUserDataSource userDataSource, IRestaurantDataSource restaurantDataSource,
                                        IPasswordEncoder passwordEncoder, IUnitOfWork unitOfWork, Clock clock) {
        return new UserController(userDataSource, restaurantDataSource, passwordEncoder, unitOfWork, clock);
    }

    public UserView register(NewUserDTO dto) {
        var useCase = RegisterUserUseCase.create(userGateway(), passwordEncoder, clock);
        return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(dto)));
    }

    public UserView findById(UUID id) {
        var useCase = FindUserByIdUseCase.create(userGateway());
        return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(id)));
    }

    public PageResult<UserView> search(String name, PageRequest request) {
        var useCase = SearchUsersUseCase.create(userGateway());
        return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(name, request)));
    }

    public UserView update(UUID id, UpdateUserDTO dto) {
        var useCase = UpdateUserUseCase.create(userGateway());
        return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(id, dto)));
    }

    public void changePassword(UUID id, ChangePasswordDTO dto) {
        var useCase = ChangePasswordUseCase.create(userGateway(), passwordEncoder);
        unitOfWork.execute(() -> useCase.run(id, dto));
    }

    public void delete(UUID id) {
        var useCase = DeleteUserUseCase.create(userGateway());
        unitOfWork.execute(() -> useCase.run(id));
    }

    public UserView saveProfile(UUID id, UserProfileDTO profile) {
        var useCase = SaveUserProfileUseCase.create(userGateway(), clock);
        return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(id, profile)));
    }

    /** O perfil de dono sai só se o usuário não tiver restaurante: por isso o gateway de restaurante. */
    public void removeProfile(UUID id, String type) {
        var useCase = RemoveUserProfileUseCase.create(userGateway(), RestaurantGateway.create(restaurantDataSource));
        unitOfWork.execute(() -> useCase.run(id, type));
    }

    public UserView changeCourierStatus(UUID id, String status) {
        var useCase = ChangeCourierStatusUseCase.create(userGateway());
        return UserPresenter.toView(unitOfWork.execute(() -> useCase.run(id, status)));
    }

    private UserGateway userGateway() {
        return UserGateway.create(userDataSource);
    }
}
