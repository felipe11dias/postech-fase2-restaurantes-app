package com.postech.restaurantes.application.usecase.auth;

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

import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.user.User;
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
    @DisplayName("Login com espaços nas bordas é aparado antes da busca, como no cadastro")
    void deveApararLoginAntesDeBuscar() {
        User user = existingUser();
        when(userGateway.findByLogin("joao.silva")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha", HASH)).thenReturn(true);
        when(tokenIssuer.issue(user)).thenReturn(new IssuedToken("jwt", NOW.plusHours(1)));

        useCase.run(new CredentialsDTO("  joao.silva  ", "senha"));

        verify(userGateway).findByLogin("joao.silva");
    }

    @Test
    @DisplayName("Login inexistente pede ao encoder que gaste o tempo de uma comparação, sem comparar hash real")
    void deveSimularComparacaoQuandoLoginInexistente() {
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> useCase.run(new CredentialsDTO("ninguem", "senha")));

        verify(passwordEncoder).simulateMatch("senha");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    @DisplayName("Login existente compara contra o hash real e não simula")
    void naoDeveSimularQuandoLoginExiste() {
        when(userGateway.findByLogin("joao.silva")).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("senha", HASH)).thenReturn(true);
        when(tokenIssuer.issue(any())).thenReturn(new IssuedToken("jwt", NOW.plusHours(1)));

        useCase.run(new CredentialsDTO("joao.silva", "senha"));

        verify(passwordEncoder, never()).simulateMatch(anyString());
    }

    @Test
    @DisplayName("Senha nula ou em branco falha como credencial inválida, sem chegar ao encoder")
    void deveFalharQuandoSenhaEmBranco() {
        assertThrows(InvalidCredentialsException.class, () -> useCase.run(new CredentialsDTO("joao.silva", null)));
        assertThrows(InvalidCredentialsException.class, () -> useCase.run(new CredentialsDTO("joao.silva", "  ")));

        verify(passwordEncoder, never()).matches(any(), any());
        verify(userGateway, never()).findByLogin(anyString());
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
    @DisplayName("Credenciais nulas são erro de argumento; login em branco é credencial inválida")
    void deveRecusarEntradaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
        assertThrows(InvalidCredentialsException.class, () -> useCase.run(new CredentialsDTO(" ", "senha")));
        assertThrows(InvalidCredentialsException.class, () -> useCase.run(new CredentialsDTO(null, "senha")));
    }
}
