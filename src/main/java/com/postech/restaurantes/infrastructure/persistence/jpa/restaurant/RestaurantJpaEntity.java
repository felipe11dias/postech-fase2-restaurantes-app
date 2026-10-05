package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

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
import java.time.LocalTime;
import java.util.UUID;

/**
 * Tabela {@code restaurants}. Mapeamento JPA separado da entidade de domínio.
 *
 * <p>O endereço é do restaurante e de mais ninguém ({@code address_id} é único): grava, atualiza
 * e remove junto com ele. A chave estrangeira aponta daqui para {@code addresses}, então o banco
 * não removeria o endereço sozinho — quem remove é o {@code orphanRemoval}.
 */
@Entity
@Table(name = "restaurants")
public class RestaurantJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private AddressJpaEntity address;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "office_hour_start", nullable = false)
    private LocalTime officeHourStart;

    @Column(name = "office_hour_end", nullable = false)
    private LocalTime officeHourEnd;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public AddressJpaEntity getAddress() {
        return address;
    }

    public void setAddress(AddressJpaEntity address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getOfficeHourStart() {
        return officeHourStart;
    }

    public void setOfficeHourStart(LocalTime officeHourStart) {
        this.officeHourStart = officeHourStart;
    }

    public LocalTime getOfficeHourEnd() {
        return officeHourEnd;
    }

    public void setOfficeHourEnd(LocalTime officeHourEnd) {
        this.officeHourEnd = officeHourEnd;
    }
}
