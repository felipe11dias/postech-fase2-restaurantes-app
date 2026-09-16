package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.gateway.IMailGateway;
import com.postech.restaurantes.application.gateway.IPasswordResetTokenGateway;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.PasswordResetToken;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.vo.Email;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * "Esqueci minha senha". Se o e-mail existir, gera um token de uso único, persiste apenas o
 * hash e envia o valor em claro por e-mail. Se não existir, não faz nada — e não avisa: a
 * resposta é idêntica nos dois casos, para não revelar quais e-mails estão cadastrados.
 */
public final class ForgotPasswordUseCase {

    private final IUserGateway userGateway;
    private final IPasswordResetTokenGateway tokenGateway;
    private final ISecureTokenGenerator tokenGenerator;
    private final IMailGateway mailGateway;
    private final Duration tokenValidity;
    private final Clock clock;

    private ForgotPasswordUseCase(IUserGateway userGateway, IPasswordResetTokenGateway tokenGateway,
                                  ISecureTokenGenerator tokenGenerator, IMailGateway mailGateway,
                                  Duration tokenValidity, Clock clock) {
        this.userGateway = userGateway;
        this.tokenGateway = tokenGateway;
        this.tokenGenerator = tokenGenerator;
        this.mailGateway = mailGateway;
        this.tokenValidity = tokenValidity;
        this.clock = clock;
    }

    public static ForgotPasswordUseCase create(IUserGateway userGateway, IPasswordResetTokenGateway tokenGateway,
                                               ISecureTokenGenerator tokenGenerator, IMailGateway mailGateway,
                                               Duration tokenValidity, Clock clock) {
        Guard.requireNonNull(tokenValidity, "Validade do token inválida");
        Guard.require(!tokenValidity.isNegative() && !tokenValidity.isZero(), "Validade do token deve ser positiva");
        return new ForgotPasswordUseCase(userGateway, tokenGateway, tokenGenerator, mailGateway, tokenValidity,
                Guard.requireNonNull(clock, "Relógio inválido"));
    }

    public void run(String emailValue) {
        Email email = Email.of(emailValue);
        Optional<User> user = userGateway.findByEmail(email);
        if (user.isEmpty()) {
            return;
        }
        String rawToken = tokenGenerator.generate();
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken token = PasswordResetToken.create(
                user.get().getId(), tokenGenerator.hash(rawToken), now.plus(tokenValidity), now);
        tokenGateway.insert(token);
        mailGateway.sendPasswordReset(email, rawToken);
    }
}
