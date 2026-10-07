package com.postech.restaurantes.application.dto.user;

import com.postech.restaurantes.domain.entity.client.ClientProfile;
import java.time.LocalDate;

/** Perfil de cliente como chega aos casos de uso. A conversão para o domínio vive aqui. */
public record ClientProfileDTO(String cpf, String phone, LocalDate birthDate) implements UserProfileDTO {

    /** {@code today} é a referência para recusar nascimento no futuro; vem do relógio do caso de uso. */
    public ClientProfile toEntity(LocalDate today) {
        return ClientProfile.create(cpf, phone, birthDate, today);
    }
}
