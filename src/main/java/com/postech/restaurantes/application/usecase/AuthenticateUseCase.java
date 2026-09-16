package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.CredentialsDTO;
import com.postech.restaurantes.application.dto.IssuedToken;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;
import java.util.Optional;

/**
 * Validação de login. Login inexistente e senha incorreta produzem a mesma exceção, a mesma
 * mensagem e o mesmo custo de tempo, para não revelar quais logins existem.
 */
public final class AuthenticateUseCase {

    private static final String FAILURE = "Login ou senha incorretos";

    /**
     * Hash BCrypt válido de uma senha descartável. Quando o login não existe, a senha
     * informada é comparada contra ele para que o caminho "não encontrado" gaste o mesmo tempo
     * do caminho "senha errada" — sem isso, a latência revelaria quais logins existem.
     */
    static final String DUMMY_HASH = "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

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
        Optional<User> user = userGateway.findByLogin(login);
        String hash = user.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean matches = passwordEncoder.matches(password, hash);
        if (user.isEmpty() || !matches) {
            throw new InvalidCredentialsException(FAILURE);
        }
        return tokenIssuer.issue(user.get());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
