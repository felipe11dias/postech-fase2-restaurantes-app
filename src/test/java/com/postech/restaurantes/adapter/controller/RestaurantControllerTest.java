package com.postech.restaurantes.adapter.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.domain.entity.role.RoleName;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantControllerTest {

    private IRestaurantDataSource restaurantDataSource;
    private IUserDataSource userDataSource;
    private IUnitOfWork unitOfWork;
    private RestaurantController controller;

    private UUID restaurantId;
    private UUID userId;
    private UUID addressId;
    private RestaurantData restaurantData;
    private UserData userData;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        restaurantDataSource = mock(IRestaurantDataSource.class);
        userDataSource = mock(IUserDataSource.class);
        unitOfWork = mock(IUnitOfWork.class);

        when(unitOfWork.execute(any(Supplier.class))).thenAnswer(inv -> {
            Supplier<?> supplier = inv.getArgument(0);
            return supplier.get();
        });

        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0);
            runnable.run();
            return null;
        }).when(unitOfWork).execute(any(Runnable.class));

        controller = RestaurantController.create(restaurantDataSource, userDataSource, unitOfWork);

        restaurantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        AddressData addressData = new AddressData(addressId, "Rua A", "10", null, "Bairro", "Cidade", "SP", "01000000");
        userData = new UserData(userId, "Dono", "dono@x.com", "dono", "hash",
                Set.of(new RoleData(UUID.randomUUID(), RoleName.ROLE_OWNER.name())), List.of(addressData), null, null);

        restaurantData = new RestaurantData(restaurantId, userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Executa criacao de restaurante")
    void deveCriar() {
        when(userDataSource.findById(userId)).thenReturn(Optional.of(userData));
        when(restaurantDataSource.insert(any())).thenReturn(restaurantData);

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        RestaurantView view = controller.create(dto);

        assertNotNull(view);
    }

    @Test
    @DisplayName("Executa busca por ID")
    void deveBuscarPorId() {
        when(restaurantDataSource.findById(restaurantId)).thenReturn(Optional.of(restaurantData));

        RestaurantView view = controller.findById(restaurantId);

        assertNotNull(view);
    }

    @Test
    @DisplayName("Executa busca paginada")
    void deveBuscarPaginado() {
        PageRequest req = PageRequest.of(0, 10);
        PageResult<RestaurantData> page = new PageResult<>(List.of(restaurantData), 0, 10, 1);
        when(restaurantDataSource.search(any(), any())).thenReturn(page);

        PageResult<RestaurantView> view = controller.search("sabor", req);

        assertNotNull(view);
    }

    @Test
    @DisplayName("Executa atualizacao")
    void deveAtualizar() {
        when(restaurantDataSource.findById(restaurantId)).thenReturn(Optional.of(restaurantData));
        when(userDataSource.findById(userId)).thenReturn(Optional.of(userData));
        when(restaurantDataSource.update(any())).thenReturn(restaurantData);

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, addressId, "Novo", LocalTime.of(9, 0), LocalTime.of(23, 0));
        RestaurantView view = controller.update(dto);

        assertNotNull(view);
    }

    @Test
    @DisplayName("Executa delecao")
    void deveDeletar() {
        when(restaurantDataSource.findById(restaurantId)).thenReturn(Optional.of(restaurantData));

        controller.delete(restaurantId);

        verify(restaurantDataSource).delete(restaurantId);
    }

    @Test
    @DisplayName("Recusa parametros nulos no construtor/fabrica")
    void deveRecusarParametrosNulos() {
        assertThrows(IllegalArgumentException.class, () -> RestaurantController.create(null, userDataSource, unitOfWork));
        assertThrows(IllegalArgumentException.class, () -> RestaurantController.create(restaurantDataSource, null, unitOfWork));
        assertThrows(IllegalArgumentException.class, () -> RestaurantController.create(restaurantDataSource, userDataSource, null));
    }
}
