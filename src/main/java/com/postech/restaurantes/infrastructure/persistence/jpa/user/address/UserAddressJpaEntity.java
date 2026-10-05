package com.postech.restaurantes.infrastructure.persistence.jpa.user.address;

import com.postech.restaurantes.infrastructure.persistence.jpa.address.AddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Tabela {@code user_addresses}: o vínculo entre o usuário e um endereço, com rótulo e a marca de
 * padrão. A coluna {@code user_id} é mapeada do lado do {@code UserJpaEntity} (associação
 * unidirecional), como na Etapa 17, para este pacote não depender do pacote do usuário.
 *
 * <p>O endereço é deste vínculo e de mais ninguém ({@code address_id} é único): removido o vínculo,
 * o endereço vai junto ({@code orphanRemoval}). O banco não faria isso sozinho, porque a chave
 * estrangeira aponta daqui para {@code addresses}.
 */
@Entity
@Table(name = "user_addresses")
public class UserAddressJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "label", length = 50)
    private String label;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private AddressJpaEntity address;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public AddressJpaEntity getAddress() {
        return address;
    }

    public void setAddress(AddressJpaEntity address) {
        this.address = address;
    }
}
