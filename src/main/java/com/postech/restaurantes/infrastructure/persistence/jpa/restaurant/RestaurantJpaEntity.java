package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import com.postech.restaurantes.infrastructure.persistence.jpa.address.AddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.cuisine.CuisineJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.restaurant.officehour.OfficeHourJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

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

    /**
     * Os horários são parte do restaurante: gravados, atualizados e removidos com ele. A chave fica na
     * tabela dos horários, mapeada daqui ({@code @JoinColumn} unidirecional), e o pacote dos horários não
     * conhece este. Ordenados pelo tipo {@code day_of_week}, que o PostgreSQL ordena de segunda a domingo.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false, updatable = false)
    @OrderBy("dayOfWeek ASC, startTime ASC")
    private List<OfficeHourJpaEntity> officeHours = new ArrayList<>();

    /**
     * Os tipos de cozinha são do catálogo: o restaurante só se liga a eles pela tabela {@code restaurant_cuisines},
     * sem cascata — gravar ou excluir o restaurante mexe só nas ligações, nunca nos tipos. Fica fora do
     * {@code @EntityGraph}: buscar um {@code Set} junto da lista de horários repetiria os horários; carregada à
     * parte, em lotes ({@code @BatchSize}), não faz uma consulta por restaurante na listagem.
     */
    @ManyToMany
    @JoinTable(name = "restaurant_cuisines",
            joinColumns = @JoinColumn(name = "restaurant_id"),
            inverseJoinColumns = @JoinColumn(name = "cuisine_id"))
    @BatchSize(size = 50)
    private Set<CuisineJpaEntity> cuisines = new LinkedHashSet<>();

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

    public List<OfficeHourJpaEntity> getOfficeHours() {
        return officeHours;
    }

    /** Troca a coleção pela lista dada, que pode reaproveitar instâncias já gerenciadas. */
    public void replaceOfficeHours(List<OfficeHourJpaEntity> newOfficeHours) {
        officeHours.clear();
        officeHours.addAll(newOfficeHours);
    }

    public Set<CuisineJpaEntity> getCuisines() {
        return cuisines;
    }

    /** Troca as ligações: o Hibernate apaga as que saíram e grava as novas, sem tocar nos tipos. */
    public void replaceCuisines(Collection<CuisineJpaEntity> newCuisines) {
        cuisines.clear();
        cuisines.addAll(newCuisines);
    }
}
