package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.AddressDTO;
import com.postech.restaurantes.application.dto.UpdateUserDTO;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.vo.Email;
import java.util.UUID;

/**
 * Atualização cadastral: nome, e-mail, login e endereços. Unicidade revalidada ignorando o
 * próprio registro. A senha não passa por aqui.
 */
public final class UpdateUserUseCase {

    private final IUserGateway userGateway;

    private UpdateUserUseCase(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static UpdateUserUseCase create(IUserGateway userGateway) {
        return new UpdateUserUseCase(userGateway);
    }

    public User run(UUID id, UpdateUserDTO dto) {
        Guard.requireNonNull(dto, "Dados de atualização inválidos");
        User user = userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        Email email = Email.of(dto.email());
        if (userGateway.findByEmail(email).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }
        String login = Guard.requireNonBlank(dto.login(), "Login inválido");
        if (userGateway.findByLogin(login).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new DuplicateResourceException("Login já cadastrado");
        }

        user.setName(dto.name());
        user.setEmail(email);
        user.setLogin(login);
        user.replaceAddresses(AddressDTO.toEntities(dto.addresses()));
        return userGateway.update(user);
    }
}
