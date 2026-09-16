package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.HASH;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.otherUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.AddressDTO;
import com.postech.restaurantes.application.dto.UpdateUserDTO;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateUserUseCaseTest {

    private static final UpdateUserDTO DTO = new UpdateUserDTO("João Atualizado", "Novo@Email.com", "joao.novo",
            List.of(new AddressDTO("Av. B", null, null, null, "Rio", "RJ", "20000000")));

    private IUserGateway userGateway;
    private UpdateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        useCase = UpdateUserUseCase.create(userGateway);
    }

    @Test
    @DisplayName("Atualiza nome, e-mail, login e endereços sem tocar na senha")
    void deveAtualizarQuandoValido() {
        User user = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(userGateway.update(user)).thenReturn(user);

        User result = useCase.run(USER_ID, DTO);

        assertSame(user, result);
        assertEquals("João Atualizado", user.getName());
        assertEquals("novo@email.com", user.getEmail().value());
        assertEquals("joao.novo", user.getLogin());
        assertEquals("RJ", user.getAddresses().get(0).getState());
        assertEquals(HASH, user.getPasswordHash());
    }

    @Test
    @DisplayName("Ignora o próprio registro ao revalidar unicidade de e-mail e login")
    void deveIgnorarProprioRegistroNaUnicidade() {
        User user = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userGateway.findByEmail(any())).thenReturn(Optional.of(user));
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.of(user));
        when(userGateway.update(user)).thenReturn(user);

        useCase.run(USER_ID, DTO);

        verify(userGateway).update(user);
    }

    @Test
    @DisplayName("Recusa e-mail que pertence a outro usuário")
    void deveRecusarQuandoEmailDeOutro() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(userGateway.findByEmail(any())).thenReturn(Optional.of(otherUser()));

        assertThrows(DuplicateResourceException.class, () -> useCase.run(USER_ID, DTO));

        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa login que pertence a outro usuário")
    void deveRecusarQuandoLoginDeOutro() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.of(otherUser()));

        assertThrows(DuplicateResourceException.class, () -> useCase.run(USER_ID, DTO));

        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa usuário inexistente")
    void deveRecusarQuandoInexistente() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(USER_ID, DTO));
    }

    @Test
    @DisplayName("Recusa id ou dados nulos")
    void deveRecusarQuandoEntradaNula() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null, DTO));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, null));
    }

    @Test
    @DisplayName("Lista de endereços ausente esvazia os endereços")
    void deveEsvaziarEnderecosQuandoAusentes() {
        User user = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(userGateway.update(user)).thenReturn(user);

        useCase.run(USER_ID, new UpdateUserDTO("Ana", "ana@x.com", "ana", null));

        assertTrue(user.getAddresses().isEmpty());
    }
}
