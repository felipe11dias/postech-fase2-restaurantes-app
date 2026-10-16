package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.dto.user.AdminProfileDTO;
import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import com.postech.restaurantes.application.dto.user.CourierProfileDTO;
import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import com.postech.restaurantes.application.dto.user.UserProfileDTO;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.application.policy.user.UniqueDocumentsPolicy;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inclui um perfil num usuário existente ou altera os dados do que ele já tem. As regras entre perfis
 * (o mesmo CPF para cliente e entregador; alterar o CPF de um perfil corrige o do outro) são do
 * domínio; as de aplicação ficam aqui: CPF e CNPJ não podem ser de outro cadastro
 * ({@link UniqueDocumentsPolicy}), e alterar o perfil de entregador não muda o status dele — status é
 * operação própria ({@link ChangeCourierStatusUseCase}). Quem pode incluir o perfil de administrador é
 * decidido na entrada (só um administrador).
 */
public final class SaveUserProfileUseCase {

    private final IUserGateway userGateway;
    private final Clock clock;
    private final UniqueDocumentsPolicy uniqueDocuments;

    private SaveUserProfileUseCase(IUserGateway userGateway, Clock clock) {
        this.userGateway = userGateway;
        this.clock = clock;
        this.uniqueDocuments = UniqueDocumentsPolicy.create(userGateway);
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
        uniqueDocuments.requireUnique(current, changed, id);
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
}
