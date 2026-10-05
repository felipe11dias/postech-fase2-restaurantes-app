package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.UserAddressDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.vo.Email;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Autocadastro público. Regras de aplicação: por esta porta de entrada só se obtêm os perfis de
 * dono, cliente e entregador — o de administrador não existe no pedido —; e-mail, login, CPF e CNPJ
 * são únicos. As demais validações são das entidades (formato dos documentos, ao menos um perfil, o
 * mesmo CPF para cliente e entregador).
 */
public final class RegisterUserUseCase {

    private final IUserGateway userGateway;
    private final IPasswordEncoder passwordEncoder;
    private final Clock clock;

    private RegisterUserUseCase(IUserGateway userGateway, IPasswordEncoder passwordEncoder, Clock clock) {
        this.userGateway = userGateway;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public static RegisterUserUseCase create(IUserGateway userGateway, IPasswordEncoder passwordEncoder, Clock clock) {
        return new RegisterUserUseCase(userGateway, passwordEncoder, Guard.requireNonNull(clock, "Relógio inválido"));
    }

    public User run(NewUserDTO dto) {
        Guard.requireNonNull(dto, "Dados de cadastro inválidos");
        UserProfiles profiles = profiles(dto);
        Email email = Email.of(dto.email());
        if (userGateway.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }
        String login = Guard.requireNonBlank(dto.login(), "Login inválido");
        if (userGateway.findByLogin(login).isPresent()) {
            throw new DuplicateResourceException("Login já cadastrado");
        }
        requireUniqueDocuments(profiles);
        String rawPassword = Guard.requireNonBlank(dto.password(), "Senha inválida");
        User user = User.create(dto.name(), email.value(), login, passwordEncoder.encode(rawPassword),
                profiles, UserAddressDTO.toEntities(dto.addresses()));
        return userGateway.insert(user);
    }

    private UserProfiles profiles(NewUserDTO dto) {
        OwnerProfile owner = dto.owner() == null ? null : dto.owner().toEntity();
        ClientProfile client = dto.client() == null ? null : dto.client().toEntity(LocalDate.now(clock));
        CourierProfile courier = dto.courier() == null ? null : dto.courier().toEntity();
        return new UserProfiles(owner, client, courier, null);
    }

    /**
     * CPF e CNPJ identificam a pessoa e a empresa: outro cadastro com o mesmo documento é conflito,
     * com a mensagem certa — como e-mail e login —, e não a recusa genérica da restrição do banco.
     */
    private void requireUniqueDocuments(UserProfiles profiles) {
        if (profiles.cpf().flatMap(userGateway::findByCpf).isPresent()) {
            throw new DuplicateResourceException("CPF já cadastrado");
        }
        if (profiles.owner() != null && userGateway.findByCnpj(profiles.owner().getCnpj()).isPresent()) {
            throw new DuplicateResourceException("CNPJ já cadastrado");
        }
    }
}
