package com.postech.restaurantes.domain.entity.user;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import java.util.UUID;

/**
 * Endereço do usuário: parte do agregado {@link User} que liga um {@link Address} ao dono,
 * com um rótulo opcional ("Casa", "Trabalho") e a marca de endereço padrão.
 *
 * <p>Quantos endereços são o padrão é regra do conjunto, não de um endereço isolado — por isso
 * quem a verifica é o {@link User}.
 */
public final class UserAddress {

    private final UUID id;
    private String label;
    private boolean defaultAddress;
    private Address address;

    private UserAddress(UUID id) {
        this.id = id;
    }

    /** Endereço novo do usuário, ainda sem id. */
    public static UserAddress create(String label, boolean defaultAddress, Address address) {
        return fill(new UserAddress(null), label, defaultAddress, address);
    }

    /** Endereço do usuário reconstruído a partir da origem de dados. */
    public static UserAddress restore(UUID id, String label, boolean defaultAddress, Address address) {
        UserAddress userAddress = new UserAddress(Guard.requireNonNull(id, "Id do endereço do usuário inválido"));
        return fill(userAddress, label, defaultAddress, address);
    }

    private static UserAddress fill(UserAddress userAddress, String label, boolean defaultAddress, Address address) {
        userAddress.setLabel(label);
        userAddress.defaultAddress = defaultAddress;
        userAddress.setAddress(address);
        return userAddress;
    }

    /** Rótulo opcional; em branco vira ausente. */
    public void setLabel(String label) {
        this.label = Guard.trimToNull(label);
    }

    public void setAddress(Address address) {
        this.address = Guard.requireNonNull(address, "Endereço inválido");
    }

    public UUID getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDefault() {
        return defaultAddress;
    }

    public Address getAddress() {
        return address;
    }
}
