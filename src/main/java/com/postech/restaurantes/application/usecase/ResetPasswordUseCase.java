package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.ResetPasswordDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IPasswordResetTokenGateway;
import com.postech.restaurantes.application.gateway.ISecureTokenGenerator;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.PasswordResetToken;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Redefinição de senha com o token recebido por e-mail. O token só é consultado pelo hash;
 * precisa existir, não ter expirado e não ter sido usado. Após o sucesso é marcado como usado.
 */
public final class ResetPasswordUseCase {

    private static final String INVALID_TOKEN = "Token inválido ou expirado";

    private final IUserGateway userGateway;
    private final IPasswordResetTokenGateway tokenGateway;
    private final ISecureTokenGenerator tokenGenerator;
    private final IPasswordEncoder passwordEncoder;
    private final Clock clock;

    private ResetPasswordUseCase(IUserGateway userGateway, IPasswordResetTokenGateway tokenGateway,
                                 ISecureTokenGenerator tokenGenerator, IPasswordEncoder passwordEncoder, Clock clock) {
        this.userGateway = userGateway;
        this.tokenGateway = tokenGateway;
        this.tokenGenerator = tokenGenerator;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public static ResetPasswordUseCase create(IUserGateway userGateway, IPasswordResetTokenGateway tokenGateway,
                                              ISecureTokenGenerator tokenGenerator, IPasswordEncoder passwordEncoder,
                                              Clock clock) {
        return new ResetPasswordUseCase(userGateway, tokenGateway, tokenGenerator, passwordEncoder,
                Guard.requireNonNull(clock, "Relógio inválido"));
    }

    public void run(ResetPasswordDTO dto) {
        Guard.requireNonNull(dto, "Dados de redefinição inválidos");
        String rawToken = Guard.requireNonBlank(dto.token(), INVALID_TOKEN);
        PasswordResetToken token = tokenGateway.findByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> new InvalidOrExpiredTokenException(INVALID_TOKEN));
        if (!token.isUsable(LocalDateTime.now(clock))) {
            throw new InvalidOrExpiredTokenException(INVALID_TOKEN);
        }
        String newPassword = Guard.requireNonBlank(dto.newPassword(), "Nova senha inválida");
        if (!newPassword.equals(dto.confirmPassword())) {
            throw new InvalidPasswordException("Confirmação de senha divergente");
        }
        User user = userGateway.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userGateway.update(user);
        token.markUsed();
        tokenGateway.update(token);
    }
}
