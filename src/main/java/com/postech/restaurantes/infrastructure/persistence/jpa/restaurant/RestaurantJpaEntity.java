package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Tabela {@code restaurants}. Mapeamento JPA separado da entidade de domínio.
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

    @Column(name = "address_id", nullable = false)
    private UUID addressId;

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

    public UUID getAddressId() {
        return addressId;
    }

    public void setAddressId(UUID addressId) {
        this.addressId = addressId;
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
