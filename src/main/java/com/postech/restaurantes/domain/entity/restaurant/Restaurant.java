package com.postech.restaurantes.domain.entity.restaurant;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Raiz do agregado de restaurante. Não existe instância inválida: {@link #create} e
 * {@link #restore} passam pela mesma validação.
 *
 * <p>Invariantes: nome não vazio; dono (userId) informado; endereço próprio presente — o
 * {@link Address} é parte deste agregado, e nenhum outro restaurante ou usuário o compartilha;
 * ao menos um horário de funcionamento ({@link OfficeHour}), sem dois que se sobreponham. A
 * sobreposição é regra do conjunto, e não de um horário isolado: por isso mora na raiz.
 *
 * <p>Os tipos de cozinha ({@link Cuisine}) são do catálogo, e não do restaurante: ele só escolhe entre os que já
 * existem. Pode ter nenhum, um ou vários, sem repetir o mesmo.
 */
public final class Restaurant {

    private static final Comparator<OfficeHour> ORDEM_DA_SEMANA =
            Comparator.comparing(OfficeHour::dayOfWeek).thenComparing(OfficeHour::startTime);

    /**
     * Ordem alfabética do português, sem diferenciar maiúsculas nem acentos: "Árabe" vem antes de "Italiana". A
     * comparação pelos códigos dos caracteres (a de {@code String}) mandaria toda inicial acentuada para o fim.
     */
    private static final Comparator<Cuisine> ORDEM_ALFABETICA =
            Comparator.comparing(Cuisine::getName, alphabeticalCollator());

    private final UUID id;
    private UUID userId;
    private Address address;
    private String name;
    private List<OfficeHour> officeHours;
    private List<Cuisine> cuisines;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUpdatedAt;

    private static Collator alphabeticalCollator() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        collator.setStrength(Collator.PRIMARY);
        return collator;
    }

    private Restaurant(UUID id, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /** Restaurante novo, ainda sem id nem auditoria. */
    public static Restaurant create(UUID userId, Address address, String name, List<OfficeHour> officeHours,
                                    List<Cuisine> cuisines) {
        return fill(new Restaurant(null, null, null), userId, address, name, officeHours, cuisines);
    }

    /** Restaurante reconstruído a partir da origem de dados, com id e auditoria conhecidos. */
    public static Restaurant restore(UUID id, UUID userId, Address address, String name,
                                     List<OfficeHour> officeHours, List<Cuisine> cuisines,
                                     LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        Restaurant restaurant = new Restaurant(
                Guard.requireNonNull(id, "Id do restaurante inválido"),
                createdAt,
                lastUpdatedAt);
        return fill(restaurant, userId, address, name, officeHours, cuisines);
    }

    private static Restaurant fill(Restaurant restaurant, UUID userId, Address address, String name,
                                   List<OfficeHour> officeHours, List<Cuisine> cuisines) {
        restaurant.setUserId(userId);
        restaurant.setAddress(address);
        restaurant.setName(name);
        restaurant.replaceOfficeHours(officeHours);
        restaurant.replaceCuisines(cuisines);
        return restaurant;
    }

    public void setUserId(UUID userId) {
        this.userId = Guard.requireNonNull(userId, "Id do dono do restaurante inválido");
    }

    public void setAddress(Address address) {
        this.address = Guard.requireNonNull(address, "Endereço do restaurante inválido");
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome do restaurante inválido");
    }

    /**
     * Troca a lista inteira de horários. Cada horário já é válido por si; aqui vale a regra do conjunto:
     * ao menos um, e nenhum par sobreposto — inclusive o que vira a meia-noite e invade o dia seguinte.
     * A lista fica na ordem da semana (dia, depois abertura).
     */
    public void replaceOfficeHours(List<OfficeHour> newOfficeHours) {
        Guard.requireNonNull(newOfficeHours, "Horários de funcionamento inválidos");
        Guard.require(newOfficeHours.stream().noneMatch(Objects::isNull), "Horário de funcionamento inválido");
        Guard.require(!newOfficeHours.isEmpty(), "Restaurante deve ter ao menos um horário de funcionamento");
        for (int i = 0; i < newOfficeHours.size(); i++) {
            for (int j = i + 1; j < newOfficeHours.size(); j++) {
                Guard.require(!newOfficeHours.get(i).overlaps(newOfficeHours.get(j)),
                        "Os horários de funcionamento não podem se sobrepor");
            }
        }
        this.officeHours = newOfficeHours.stream().sorted(ORDEM_DA_SEMANA).toList();
    }

    /**
     * Troca a lista inteira de tipos de cozinha. Cada tipo precisa já estar no catálogo (ter id), e o mesmo tipo não
     * entra duas vezes. Pode ficar vazia. A lista fica em ordem alfabética, sem diferenciar maiúsculas nem acentos.
     */
    public void replaceCuisines(List<Cuisine> newCuisines) {
        Guard.requireNonNull(newCuisines, "Tipos de cozinha inválidos");
        Guard.require(newCuisines.stream().noneMatch(Objects::isNull), "Tipo de cozinha inválido");
        Guard.require(newCuisines.stream().allMatch(cuisine -> cuisine.getId() != null),
                "Tipo de cozinha ainda não cadastrado no catálogo");
        Guard.require(newCuisines.stream().map(Cuisine::getId).distinct().count() == newCuisines.size(),
                "Os tipos de cozinha não podem se repetir");
        this.cuisines = newCuisines.stream().sorted(ORDEM_ALFABETICA).toList();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Address getAddress() {
        return address;
    }

    public String getName() {
        return name;
    }

    /** Lista imutável, na ordem da semana. */
    public List<OfficeHour> getOfficeHours() {
        return officeHours;
    }

    /** Lista imutável, em ordem alfabética. */
    public List<Cuisine> getCuisines() {
        return cuisines;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
