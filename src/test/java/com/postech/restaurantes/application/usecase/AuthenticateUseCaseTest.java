package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.HASH;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.NOW;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.CredentialsDTO;
import com.postech.restaurantes.application.dto.IssuedToken;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthenticateUseCaseTest {

    private IUserGateway userGateway;
    private IPasswordEncoder passwordEncoder;
    private ITokenIssuer tokenIssuer;
    private AuthenticateUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        tokenIssuer = mock(ITokenIssuer.class);
        useCase = AuthenticateUseCase.create(userGateway, passwordEncoder, tokenIssuer);
    }

    @Test
    @DisplayName("Emite token quando login e senha conferem")
    void deveEmitirTokenQuandoCredenciaisValidas() {
        User user = existingUser();
        IssuedToken issued = new IssuedToken("jwt", NOW.plusHours(1));
        when(userGateway.findByLogin("joao.silva")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha", HASH)).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(issued);

        IssuedToken result = useCase.run(new CredentialsDTO("joao.silva", "senha"));

        assertSame(issued, result);
    }

    @Test
    @DisplayName("Login inexistente falha com a mesma mensagem de senha incorreta")
    void deveFalharQuandoLoginInexistente() {
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> useCase.run(new CredentialsDTO("ninguem", "senha")));

        assertEquals("Login ou senha incorretos", ex.getMessage());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    @DisplayName("Senha incorreta falha com a mesma mensagem de login inexistente")
    void deveFalharQuandoSenhaIncorreta() {
        when(userGateway.findByLogin("joao.silva")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class,
                () -> useCase.run(new CredentialsDTO("joao.silva", "errada")));

        assertEquals("Login ou senha incorretos", ex.getMessage());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    @DisplayName("Recusa credenciais nulas ou login em branco")
    void deveRecusarEntradaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(new CredentialsDTO(" ", "senha")));
    }
}
