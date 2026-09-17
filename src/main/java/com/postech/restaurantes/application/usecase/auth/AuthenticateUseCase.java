package com.postech.restaurantes.application.usecase.auth;

import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;
import java.util.Optional;

/**
 * Validação de login. Login inexistente e senha incorreta produzem a mesma exceção, a mesma
 * mensagem e o mesmo custo de tempo, para não revelar quais logins existem. Como o custo é
 * igualado pelo próprio {@link IPasswordEncoder#simulateMatch}, o caso de uso não conhece o
 * algoritmo de hash.
 */
public final class AuthenticateUseCase {

    private static final String FAILURE = "Login ou senha incorretos";

    private final IUserGateway userGateway;
    private final IPasswordEncoder passwordEncoder;
    private final ITokenIssuer tokenIssuer;

    private AuthenticateUseCase(IUserGateway userGateway, IPasswordEncoder passwordEncoder, ITokenIssuer tokenIssuer) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    public static AuthenticateUseCase create(IUserGateway userGateway, IPasswordEncoder passwordEncoder,
                                             ITokenIssuer tokenIssuer) {
        return new AuthenticateUseCase(userGateway, passwordEncoder, tokenIssuer);
    }

    public IssuedToken run(CredentialsDTO credentials) {
        Guard.requireNonNull(credentials, "Credenciais inválidas");
        String login = credentials.login();
        String password = credentials.password();
        if (isBlank(login) || isBlank(password)) {
            throw new InvalidCredentialsException(FAILURE);
        }
        // Mesma normalização dos casos de uso de escrita (Guard.requireNonBlank apara o login).
        Optional<User> user = userGateway.findByLogin(login.trim());
        if (user.isEmpty()) {
            passwordEncoder.simulateMatch(password);
            throw new InvalidCredentialsException(FAILURE);
        }
        if (!passwordEncoder.matches(password, user.get().getPasswordHash())) {
            throw new InvalidCredentialsException(FAILURE);
        }
        return tokenIssuer.issue(user.get());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
