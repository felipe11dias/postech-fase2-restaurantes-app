package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.CredentialsDTO;
import com.postech.restaurantes.application.dto.IssuedToken;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;

/**
 * Validação de login. Login inexistente e senha incorreta produzem a mesma exceção e a mesma
 * mensagem, para não revelar quais logins existem.
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
        User user = userGateway.findByLogin(Guard.requireNonBlank(credentials.login(), FAILURE))
                .orElseThrow(() -> new InvalidCredentialsException(FAILURE));
        if (!passwordEncoder.matches(credentials.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(FAILURE);
        }
        return tokenIssuer.issue(user);
    }
}
