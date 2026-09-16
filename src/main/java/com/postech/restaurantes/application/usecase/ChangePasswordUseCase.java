package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.ChangePasswordDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Troca de senha pelo próprio usuário: exige a senha atual e a confirmação da nova. */
public final class ChangePasswordUseCase {

    private final IUserGateway userGateway;
    private final IPasswordEncoder passwordEncoder;

    private ChangePasswordUseCase(IUserGateway userGateway, IPasswordEncoder passwordEncoder) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
    }

    public static ChangePasswordUseCase create(IUserGateway userGateway, IPasswordEncoder passwordEncoder) {
        return new ChangePasswordUseCase(userGateway, passwordEncoder);
    }

    public void run(UUID id, ChangePasswordDTO dto) {
        Guard.requireNonNull(dto, "Dados de troca de senha inválidos");
        User user = userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        String currentPassword = dto.currentPassword();
        if (currentPassword == null || currentPassword.isBlank()
                || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("Senha atual incorreta");
        }
        String newPassword = Guard.requireNonBlank(dto.newPassword(), "Nova senha inválida");
        if (!newPassword.equals(dto.confirmPassword())) {
            throw new InvalidPasswordException("Confirmação de senha divergente");
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
        userGateway.update(user);
    }
}
