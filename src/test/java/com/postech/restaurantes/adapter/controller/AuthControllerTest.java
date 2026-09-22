package com.postech.restaurantes.adapter.controller;

import static com.postech.restaurantes.adapter.AdapterFixtures.HASH;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.TOKEN_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.AdapterFixtures.CountingUnitOfWork;
import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.presenter.view.AuthView;
import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;
import com.postech.restaurantes.domain.vo.Email;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class AuthControllerTest {

    private static final Duration VALIDITY = Duration.ofMinutes(30);
    private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));

    private IUserDataSource userDataSource;
    private IPasswordResetTokenDataSource tokenDataSource;
    private IPasswordEncoder passwordEncoder;
    private ITokenIssuer tokenIssuer;
    private ISecureTokenGenerator tokenGenerator;
    private IMailGateway mailGateway;
    private CountingUnitOfWork unitOfWork;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userDataSource = mock(IUserDataSource.class);
        tokenDataSource = mock(IPasswordResetTokenDataSource.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        tokenIssuer = mock(ITokenIssuer.class);
        tokenGenerator = mock(ISecureTokenGenerator.class);
        mailGateway = mock(IMailGateway.class);
        unitOfWork = new CountingUnitOfWork();
        controller = create(userDataSource, tokenDataSource, passwordEncoder, tokenIssuer, tokenGenerator, mailGateway,
                VALIDITY, CLOCK, unitOfWork);
    }

    private static AuthController create(IUserDataSource u, IPasswordResetTokenDataSource t, IPasswordEncoder p,
                                         ITokenIssuer i, ISecureTokenGenerator g, IMailGateway m, Duration d,
                                         Clock c, IUnitOfWork w) {
        return AuthController.create(u, t, p, i, g, m, d, c, w);
    }

    static Stream<Arguments> dependenciasNulas() {
        return Stream.of(
                Arguments.of("origem de usuário", (Function<AuthControllerTest, AuthController>) t ->
                        create(null, t.tokenDataSource, t.passwordEncoder, t.tokenIssuer, t.tokenGenerator, t.mailGateway, VALIDITY, CLOCK, t.unitOfWork)),
                Arguments.of("origem de token", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, null, t.passwordEncoder, t.tokenIssuer, t.tokenGenerator, t.mailGateway, VALIDITY, CLOCK, t.unitOfWork)),
                Arguments.of("encoder", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, null, t.tokenIssuer, t.tokenGenerator, t.mailGateway, VALIDITY, CLOCK, t.unitOfWork)),
                Arguments.of("emissor", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, t.passwordEncoder, null, t.tokenGenerator, t.mailGateway, VALIDITY, CLOCK, t.unitOfWork)),
                Arguments.of("gerador", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, t.passwordEncoder, t.tokenIssuer, null, t.mailGateway, VALIDITY, CLOCK, t.unitOfWork)),
                Arguments.of("e-mail", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, t.passwordEncoder, t.tokenIssuer, t.tokenGenerator, null, VALIDITY, CLOCK, t.unitOfWork)),
                Arguments.of("validade", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, t.passwordEncoder, t.tokenIssuer, t.tokenGenerator, t.mailGateway, null, CLOCK, t.unitOfWork)),
                Arguments.of("relógio", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, t.passwordEncoder, t.tokenIssuer, t.tokenGenerator, t.mailGateway, VALIDITY, null, t.unitOfWork)),
                Arguments.of("unidade de trabalho", (Function<AuthControllerTest, AuthController>) t ->
                        create(t.userDataSource, t.tokenDataSource, t.passwordEncoder, t.tokenIssuer, t.tokenGenerator, t.mailGateway, VALIDITY, CLOCK, null)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dependenciasNulas")
    @DisplayName("Recusa qualquer dependência nula na criação")
    void deveRecusarDependenciaNula(String nome, Function<AuthControllerTest, AuthController> fabrica) {
        assertThrows(IllegalArgumentException.class, () -> fabrica.apply(this));
    }

    @Test
    @DisplayName("Login válido apresenta token Bearer com expiração")
    void deveAutenticar() {
        when(userDataSource.findByLogin("joao.silva")).thenReturn(Optional.of(USER_DATA));
        when(passwordEncoder.matches("senha", HASH)).thenReturn(true);
        when(tokenIssuer.issue(any())).thenReturn(new IssuedToken("jwt", NOW.plusHours(1)));

        AuthView view = controller.login(new CredentialsDTO("joao.silva", "senha"));

        assertEquals("jwt", view.token());
        assertEquals("Bearer", view.type());
        assertEquals(NOW.plusHours(1), view.expiresAt());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Login inválido propaga InvalidCredentials")
    void devePropagarCredenciaisInvalidas() {
        when(userDataSource.findByLogin(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> controller.login(new CredentialsDTO("x", "y")));
    }

    @Test
    @DisplayName("Esqueci minha senha: persiste o hash do token e envia o valor em claro por e-mail")
    void deveIniciarRecuperacao() {
        when(userDataSource.findByEmail("joao.silva@email.com")).thenReturn(Optional.of(USER_DATA));
        when(tokenGenerator.generate()).thenReturn("token-em-claro");
        when(tokenGenerator.hash("token-em-claro")).thenReturn("hash-do-token");
        when(tokenDataSource.insert(any())).thenReturn(TOKEN_DATA);

        controller.forgotPassword("Joao.Silva@Email.com");

        ArgumentCaptor<PasswordResetTokenData> captor = ArgumentCaptor.forClass(PasswordResetTokenData.class);
        verify(tokenDataSource).insert(captor.capture());
        assertNull(captor.getValue().id());
        assertEquals("hash-do-token", captor.getValue().tokenHash());
        assertEquals(NOW.plus(VALIDITY), captor.getValue().expiresAt());
        verify(mailGateway).sendPasswordReset(Email.of("joao.silva@email.com"), "token-em-claro");
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Esqueci minha senha com e-mail inexistente não toca token nem e-mail")
    void deveSilenciarEmailInexistente() {
        when(userDataSource.findByEmail(anyString())).thenReturn(Optional.empty());

        controller.forgotPassword("ninguem@email.com");

        verifyNoInteractions(tokenDataSource, mailGateway);
    }

    @Test
    @DisplayName("Redefinir senha: invalida o token, grava o novo hash, tudo na mesma unidade de trabalho")
    void deveRedefinirSenha() {
        when(tokenGenerator.hash("token-em-claro")).thenReturn("hash-do-token");
        when(tokenDataSource.findByTokenHash("hash-do-token")).thenReturn(Optional.of(TOKEN_DATA));
        when(userDataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));
        when(passwordEncoder.encode("nova")).thenReturn("novoHash");
        when(tokenDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userDataSource.update(any())).thenAnswer(inv -> inv.getArgument(0));

        controller.resetPassword(new ResetPasswordDTO("token-em-claro", "nova", "nova"));

        ArgumentCaptor<PasswordResetTokenData> token = ArgumentCaptor.forClass(PasswordResetTokenData.class);
        verify(tokenDataSource).update(token.capture());
        assertTrue(token.getValue().used());
        ArgumentCaptor<UserData> user = ArgumentCaptor.forClass(UserData.class);
        verify(userDataSource).update(user.capture());
        assertEquals("novoHash", user.getValue().passwordHash());
        assertEquals(1, unitOfWork.executions());
    }
}
