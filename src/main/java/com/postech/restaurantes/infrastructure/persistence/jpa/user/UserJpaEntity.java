package com.postech.restaurantes.infrastructure.persistence.jpa.user;

import com.postech.restaurantes.infrastructure.persistence.jpa.audit.AuditableJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.address.UserAddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.admin.AdminJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.client.ClientJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.courier.CourierJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.owner.OwnerJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tabela {@code users}. É uma classe <strong>separada</strong> de
 * {@link com.postech.restaurantes.domain.entity.user.User}: anotar a entidade de domínio
 * acoplaria as regras de negócio ao ciclo de vida do Hibernate, e o ORM é um detalhe.
 * Aqui não há invariante nenhuma — só mapeamento.
 *
 * <p>Os perfis ({@code owners}, {@code clients}, {@code couriers}, {@code admins}) compartilham a
 * chave primária do usuário. A associação é unidirecional, deste lado ({@code @PrimaryKeyJoinColumn}):
 * o perfil não referencia o usuário, senão o pacote de cada perfil e o do usuário dependeriam um do
 * outro. Por isso o id do perfil é atribuído pela origem de dados — o mesmo do usuário.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity extends AuditableJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "login", nullable = false, unique = true, length = 50)
    private String login;

    /** Hash da senha. A coluna se chama {@code password} por convenção do schema. */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private OwnerJpaEntity owner;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private ClientJpaEntity client;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private CourierJpaEntity courier;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private AdminJpaEntity admin;

    /**
     * Unidirecional: a chave estrangeira mora em {@code user_addresses}, mas só este lado a conhece
     * (ver {@link UserAddressJpaEntity}). {@code nullable = false} faz o Hibernate gravar o
     * {@code user_id} já no INSERT do vínculo, em vez de inserir nulo e atualizar depois.
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private List<UserAddressJpaEntity> addresses = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public OwnerJpaEntity getOwner() {
        return owner;
    }

    /** {@code null} remove o perfil: com {@code orphanRemoval}, a linha sai na descarga. */
    public void setOwner(OwnerJpaEntity owner) {
        this.owner = owner;
    }

    public ClientJpaEntity getClient() {
        return client;
    }

    public void setClient(ClientJpaEntity client) {
        this.client = client;
    }

    public CourierJpaEntity getCourier() {
        return courier;
    }

    public void setCourier(CourierJpaEntity courier) {
        this.courier = courier;
    }

    public AdminJpaEntity getAdmin() {
        return admin;
    }

    public void setAdmin(AdminJpaEntity admin) {
        this.admin = admin;
    }

    public List<UserAddressJpaEntity> getAddresses() {
        return addresses;
    }

    /**
     * Troca a coleção de vínculos pela lista dada, que pode reaproveitar instâncias já gerenciadas
     * (o vínculo mantido). Com {@code orphanRemoval = true}, os que saem da lista são apagados na
     * descarga da transação, sem DELETE explícito; os que ficam não são tocados.
     */
    public void replaceAddresses(List<UserAddressJpaEntity> newAddresses) {
        addresses.clear();
        addresses.addAll(newAddresses);
    }
}
