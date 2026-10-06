package com.postech.restaurantes.application.usecase.user;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangeCourierStatusUseCaseTest {

    private IUserGateway userGateway;
    private ChangeCourierStatusUseCase useCase;
    private User entregador;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        useCase = ChangeCourierStatusUseCase.create(userGateway);
        entregador = existingUser();
        entregador.replaceProfiles(entregador.getProfiles().withCourier(
                CourierProfile.create("52998224725", "11912345678", CourierVehicleType.ON_FOOT, null, null)));
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(entregador));
        when(userGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Troca o status do entregador, sem diferenciar maiúsculas, e grava")
    void deveTrocarOStatus() {
        User result = useCase.run(USER_ID, "available");

        assertEquals(CourierStatus.AVAILABLE, result.getProfiles().courier().getStatus());
        verify(userGateway).update(entregador);
    }

    @Test
    @DisplayName("Status desconhecido é recusado com a mensagem do domínio, antes de consultar o banco")
    void deveRecusarStatusDesconhecido() {
        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, "DORMINDO"));

        assertEquals("Status do entregador inválido: DORMINDO", erro.getMessage());
        verify(userGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Usuário sem perfil de entregador: 404")
    void deveRecusarQuemNaoEEntregador() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

        ResourceNotFoundException erro =
                assertThrows(ResourceNotFoundException.class, () -> useCase.run(USER_ID, "BUSY"));

        assertEquals("O usuário não tem perfil de entregador", erro.getMessage());
        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Usuário inexistente: 404; id nulo recusado")
    void deveRecusarUsuarioInexistente() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(USER_ID, "BUSY"));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null, "BUSY"));
    }
}
