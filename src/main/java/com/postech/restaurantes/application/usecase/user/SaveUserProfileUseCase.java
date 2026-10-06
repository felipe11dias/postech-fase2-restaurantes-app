package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.dto.user.AdminProfileDTO;
import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import com.postech.restaurantes.application.dto.user.CourierProfileDTO;
import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import com.postech.restaurantes.application.dto.user.UserProfileDTO;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inclui um perfil num usuário existente ou altera os dados do que ele já tem. As regras entre perfis
 * (o mesmo CPF para cliente e entregador) são do domínio; as de aplicação ficam aqui: CPF e CNPJ não
 * podem ser de outro cadastro, e alterar o perfil de entregador não muda o status dele — status é
 * operação própria ({@link ChangeCourierStatusUseCase}). Quem pode incluir o perfil de administrador é
 * decidido na entrada (só um administrador).
 */
public final class SaveUserProfileUseCase {

    private final IUserGateway userGateway;
    private final Clock clock;

    private SaveUserProfileUseCase(IUserGateway userGateway, Clock clock) {
        this.userGateway = userGateway;
        this.clock = clock;
    }

    public static SaveUserProfileUseCase create(IUserGateway userGateway, Clock clock) {
        return new SaveUserProfileUseCase(userGateway, Guard.requireNonNull(clock, "Relógio inválido"));
    }

    public User run(UUID id, UserProfileDTO profile) {
        Guard.requireNonNull(profile, "Perfil inválido");
        User user = userGateway.findById(Guard.requireNonNull(id, "Id inválido"))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        UserProfiles current = user.getProfiles();
        UserProfiles changed = switch (profile) {
            case OwnerProfileDTO owner -> current.withOwner(owner.toEntity());
            case ClientProfileDTO client -> current.withClient(client.toEntity(LocalDate.now(clock)));
            case CourierProfileDTO courier -> current.withCourier(keepingStatus(courier.toEntity(), current));
            case AdminProfileDTO admin -> current.withAdmin(admin.toEntity());
        };
        requireDocumentsNotOfOthers(changed, id);
        user.replaceProfiles(changed);
        return userGateway.update(user);
    }

    /** Entregador que já existe continua no status em que está; o novo começa fora de serviço. */
    private static CourierProfile keepingStatus(CourierProfile courier, UserProfiles current) {
        if (current.courier() != null) {
            courier.changeStatus(current.courier().getStatus());
        }
        return courier;
    }

    /** O mesmo do cadastro, mas o próprio usuário não conta: alterar o telefone mantém o CPF dele. */
    private void requireDocumentsNotOfOthers(UserProfiles profiles, UUID id) {
        if (profiles.cpf().flatMap(userGateway::findByCpf).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new DuplicateResourceException("CPF já cadastrado");
        }
        if (profiles.owner() != null && userGateway.findByCnpj(profiles.owner().getCnpj())
                .filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new DuplicateResourceException("CNPJ já cadastrado");
        }
    }
}
