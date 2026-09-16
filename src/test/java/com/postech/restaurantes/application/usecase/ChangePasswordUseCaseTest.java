package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.HASH;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.ChangePasswordDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangePasswordUseCaseTest {

    private IUserGateway userGateway;
    private IPasswordEncoder passwordEncoder;
    private ChangePasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        useCase = ChangePasswordUseCase.create(userGateway, passwordEncoder);
    }

    @Test
    @DisplayName("Troca a senha quando a atual confere e a confirmação coincide")
    void deveTrocarQuandoValido() {
        User user = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("atual", HASH)).thenReturn(true);
        when(passwordEncoder.encode("nova")).thenReturn("novoHash");

        useCase.run(USER_ID, new ChangePasswordDTO("atual", "nova", "nova"));

        assertEquals("novoHash", user.getPasswordHash());
        verify(userGateway).update(user);
    }

    @Test
    @DisplayName("Recusa quando a senha atual está incorreta")
    void deveRecusarQuandoSenhaAtualIncorreta() {
        User user = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(InvalidPasswordException.class,
                () -> useCase.run(USER_ID, new ChangePasswordDTO("errada", "nova", "nova")));

        assertEquals(HASH, user.getPasswordHash());
        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Senha atual nula ou em branco é tratada como incorreta, sem chegar ao encoder")
    void deveRecusarQuandoSenhaAtualEmBranco() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

        assertThrows(InvalidPasswordException.class,
                () -> useCase.run(USER_ID, new ChangePasswordDTO(null, "nova", "nova")));
        assertThrows(InvalidPasswordException.class,
                () -> useCase.run(USER_ID, new ChangePasswordDTO("  ", "nova", "nova")));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Recusa quando a confirmação diverge da nova senha")
    void deveRecusarQuandoConfirmacaoDiverge() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(InvalidPasswordException.class,
                () -> useCase.run(USER_ID, new ChangePasswordDTO("atual", "nova", "outra")));

        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa nova senha em branco")
    void deveRecusarQuandoNovaSenhaEmBranco() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> useCase.run(USER_ID, new ChangePasswordDTO("atual", " ", " ")));
    }

    @Test
    @DisplayName("Recusa usuário inexistente")
    void deveRecusarQuandoInexistente() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.run(USER_ID, new ChangePasswordDTO("a", "b", "b")));
    }

    @Test
    @DisplayName("Recusa id ou dados nulos")
    void deveRecusarQuandoEntradaNula() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null, new ChangePasswordDTO("a", "b", "b")));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, null));
    }
}
