package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.datasource.IPasswordResetTokenDataSource;
import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.gateway.PasswordResetTokenGateway;
import com.postech.restaurantes.adapter.gateway.UserGateway;
import com.postech.restaurantes.adapter.presenter.AuthPresenter;
import com.postech.restaurantes.adapter.presenter.view.AuthView;
import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.application.usecase.auth.AuthenticateUseCase;
import com.postech.restaurantes.application.usecase.auth.ForgotPasswordUseCase;
import com.postech.restaurantes.application.usecase.auth.ResetPasswordUseCase;
import com.postech.restaurantes.domain.Guard;
import java.time.Clock;
import java.time.Duration;

/**
 * Controller de adaptação de autenticação e recuperação de senha. Os serviços técnicos
 * (hash, emissão de token, e-mail, geração de token seguro, relógio) chegam por interface e
 * são repassados aos casos de uso; as origens de dados viram gateways aqui.
 */
public final class AuthController {

    private final IUserDataSource userDataSource;
    private final IPasswordResetTokenDataSource tokenDataSource;
    private final IPasswordEncoder passwordEncoder;
    private final ITokenIssuer tokenIssuer;
    private final ISecureTokenGenerator tokenGenerator;
    private final IMailGateway mailGateway;
    private final Duration resetTokenValidity;
    private final Clock clock;
    private final IUnitOfWork unitOfWork;

    private AuthController(IUserDataSource userDataSource, IPasswordResetTokenDataSource tokenDataSource,
                           IPasswordEncoder passwordEncoder, ITokenIssuer tokenIssuer,
                           ISecureTokenGenerator tokenGenerator, IMailGateway mailGateway,
                           Duration resetTokenValidity, Clock clock, IUnitOfWork unitOfWork) {
        this.userDataSource = Guard.requireNonNull(userDataSource, "Origem de dados de usuário inválida");
        this.tokenDataSource = Guard.requireNonNull(tokenDataSource, "Origem de dados de token inválida");
        this.passwordEncoder = Guard.requireNonNull(passwordEncoder, "Codificador de senha inválido");
        this.tokenIssuer = Guard.requireNonNull(tokenIssuer, "Emissor de token inválido");
        this.tokenGenerator = Guard.requireNonNull(tokenGenerator, "Gerador de token inválido");
        this.mailGateway = Guard.requireNonNull(mailGateway, "Gateway de e-mail inválido");
        this.resetTokenValidity = Guard.requireNonNull(resetTokenValidity, "Validade do token inválida");
        this.clock = Guard.requireNonNull(clock, "Relógio inválido");
        this.unitOfWork = Guard.requireNonNull(unitOfWork, "Unidade de trabalho inválida");
    }

    public static AuthController create(IUserDataSource userDataSource, IPasswordResetTokenDataSource tokenDataSource,
                                        IPasswordEncoder passwordEncoder, ITokenIssuer tokenIssuer,
                                        ISecureTokenGenerator tokenGenerator, IMailGateway mailGateway,
                                        Duration resetTokenValidity, Clock clock, IUnitOfWork unitOfWork) {
        return new AuthController(userDataSource, tokenDataSource, passwordEncoder, tokenIssuer, tokenGenerator,
                mailGateway, resetTokenValidity, clock, unitOfWork);
    }

    public AuthView login(CredentialsDTO credentials) {
        var useCase = AuthenticateUseCase.create(userGateway(), passwordEncoder, tokenIssuer);
        return AuthPresenter.toView(unitOfWork.execute(() -> useCase.run(credentials)));
    }

    public void forgotPassword(String email) {
        var useCase = ForgotPasswordUseCase.create(userGateway(), tokenGateway(), tokenGenerator, mailGateway,
                resetTokenValidity, clock);
        unitOfWork.execute(() -> useCase.run(email));
    }

    public void resetPassword(ResetPasswordDTO dto) {
        var useCase = ResetPasswordUseCase.create(userGateway(), tokenGateway(), tokenGenerator, passwordEncoder, clock);
        unitOfWork.execute(() -> useCase.run(dto));
    }

    private UserGateway userGateway() {
        return UserGateway.create(userDataSource);
    }

    private PasswordResetTokenGateway tokenGateway() {
        return PasswordResetTokenGateway.create(tokenDataSource);
    }
}
