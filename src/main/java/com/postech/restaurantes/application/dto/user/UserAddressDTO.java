package com.postech.restaurantes.application.dto.user;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Endereço do usuário como chega aos casos de uso: o id de um endereço que o usuário já tem (para
 * mantê-lo; ausente cria um novo), rótulo opcional, a marca de padrão (opcional) e o endereço em si.
 */
public record UserAddressDTO(UUID id, String label, Boolean isDefault, AddressDTO address) {

    /**
     * Lista ausente é tratada como nenhum endereço; elemento nulo é entrada inválida. Se ninguém
     * foi marcado como padrão, o primeiro passa a ser — conveniência desta porta de entrada, para o
     * cliente não precisar marcar o óbvio. Marcar dois continua inválido, e quem recusa é o
     * {@code User}.
     */
    public static List<UserAddress> toEntities(List<UserAddressDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        Guard.require(dtos.stream().noneMatch(Objects::isNull), "Endereço inválido");
        boolean nenhumPadrao = dtos.stream().noneMatch(UserAddressDTO::marcadoComoPadrao);
        List<UserAddress> enderecos = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            UserAddressDTO dto = dtos.get(i);
            boolean padrao = dto.marcadoComoPadrao() || (nenhumPadrao && i == 0);
            enderecos.add(dto.toEntity(padrao));
        }
        return enderecos;
    }

    /** Com id, é um endereço que o usuário já tem — e o {@code User} confere que é mesmo dele. */
    private UserAddress toEntity(boolean padrao) {
        Address endereco = Guard.requireNonNull(address, "Endereço inválido").toEntity();
        return id == null
                ? UserAddress.create(label, padrao, endereco)
                : UserAddress.restore(id, label, padrao, endereco);
    }

    private boolean marcadoComoPadrao() {
        return Boolean.TRUE.equals(isDefault);
    }
}
