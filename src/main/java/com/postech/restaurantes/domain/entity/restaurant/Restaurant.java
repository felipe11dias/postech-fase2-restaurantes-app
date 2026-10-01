package com.postech.restaurantes.domain.entity.restaurant;

import com.postech.restaurantes.domain.Guard;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Raiz do agregado de restaurante. Não existe instância inválida: {@link #create} e
 * {@link #restore} passam pela mesma validação.
 *
 * <p>Invariantes: nome não vazio; dono (userId) e endereço (addressId) válidos; horários
 * de funcionamento informados e distintos.
 */
public final class Restaurant {

    private final UUID id;
    private UUID userId;
    private UUID addressId;
    private String name;
    private LocalTime officeHourStart;
    private LocalTime officeHourEnd;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUpdatedAt;

    private Restaurant(UUID id, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /** Restaurante novo, ainda sem id nem auditoria. */
    public static Restaurant create(UUID userId, UUID addressId, String name,
                                    LocalTime officeHourStart, LocalTime officeHourEnd) {
        return fill(new Restaurant(null, null, null), userId, addressId, name, officeHourStart, officeHourEnd);
    }

    /** Restaurante reconstruído a partir da origem de dados, com id e auditoria conhecidos. */
    public static Restaurant restore(UUID id, UUID userId, UUID addressId, String name,
                                     LocalTime officeHourStart, LocalTime officeHourEnd,
                                     LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        Restaurant restaurant = new Restaurant(
                Guard.requireNonNull(id, "Id do restaurante inválido"),
                createdAt,
                lastUpdatedAt);
        return fill(restaurant, userId, addressId, name, officeHourStart, officeHourEnd);
    }

    private static Restaurant fill(Restaurant restaurant, UUID userId, UUID addressId, String name,
                                   LocalTime officeHourStart, LocalTime officeHourEnd) {
        restaurant.setUserId(userId);
        restaurant.setAddressId(addressId);
        restaurant.setName(name);
        restaurant.setOfficeHours(officeHourStart, officeHourEnd);
        return restaurant;
    }

    public void setUserId(UUID userId) {
        this.userId = Guard.requireNonNull(userId, "Id do dono do restaurante inválido");
    }

    public void setAddressId(UUID addressId) {
        this.addressId = Guard.requireNonNull(addressId, "Id do endereço do restaurante inválido");
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome do restaurante inválido");
    }

    public void setOfficeHours(LocalTime officeHourStart, LocalTime officeHourEnd) {
        this.officeHourStart = Guard.requireNonNull(officeHourStart, "Horário de abertura inválido");
        this.officeHourEnd = Guard.requireNonNull(officeHourEnd, "Horário de fechamento inválido");
        Guard.require(!officeHourStart.equals(officeHourEnd),
                "Horários de abertura e fechamento não podem ser iguais");
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAddressId() {
        return addressId;
    }

    public String getName() {
        return name;
    }

    public LocalTime getOfficeHourStart() {
        return officeHourStart;
    }

    public LocalTime getOfficeHourEnd() {
        return officeHourEnd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
