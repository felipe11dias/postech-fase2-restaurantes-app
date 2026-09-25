package com.postech.restaurantes.infrastructure.persistence.user;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.persistence.address.AddressJpaEntity;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação JPA da origem de dados de usuários: traduz {@link UserData} ↔
 * {@link UserJpaEntity} e é o único componente que sabe que existe um banco relacional
 * por trás. Trocá-la por outra tecnologia não toca em nenhuma camada de dentro.
 */
@Repository
public class UserDataSourceJpa implements IUserDataSource {

    /**
     * Propriedade de ordenação do núcleo → atributo da entidade JPA. A lista permitida já é
     * aplicada no caso de uso; o mapa é a tradução (e a segunda barreira: uma propriedade
     * desconhecida, {@code password} inclusive, nunca chega a virar {@code ORDER BY}).
     */
    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "name", "name",
            "email", "email",
            "login", "login",
            "createdAt", "createdAt",
            "lastUpdatedAt", "lastUpdatedAt");

    private static final String DEFAULT_SORT_PROPERTY = "name";

    private final SpringDataUserRepository users;
    private final SpringDataRoleRepository roles;

    public UserDataSourceJpa(SpringDataUserRepository users, SpringDataRoleRepository roles) {
        this.users = users;
        this.roles = roles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findById(UUID id) {
        return users.findById(id).map(UserDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findByLogin(String login) {
        return users.findByLogin(login).map(UserDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findByEmail(String email) {
        return users.findByEmail(email).map(UserDataSourceJpa::toData);
    }

    /**
     * Duas consultas: a primeira pagina os ids no banco, a segunda carrega os usuários da
     * página com papéis e endereços. O hash da senha vem junto porque o registro é traduzido
     * para o agregado inteiro — quem decide o que sai para o cliente é o presenter, e a
     * {@code UserView} não tem campo de senha.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<UserData> search(String name, PageRequest request) {
        Sort sort = toSort(request);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.page(), request.size(), sort);
        Page<UUID> ids = users.findIdsByName(name == null ? "" : name, pageable);
        List<UserData> content = ids.isEmpty()
                ? List.of()
                : users.findByIdIn(ids.getContent(), sort).stream().map(UserDataSourceJpa::toData).toList();
        return new PageResult<>(content, request.page(), request.size(), ids.getTotalElements());
    }

    /**
     * Grava e devolve o registro <em>com a auditoria já carimbada</em>. Por isso
     * {@code saveAndFlush}, e não {@code save}: o listener só escreve {@code last_updated_at}
     * quando a alteração é descarregada, e sem o flush o registro devolvido carregaria o
     * instante anterior à edição.
     */
    @Override
    @Transactional
    public UserData insert(UserData user) {
        UserJpaEntity entity = new UserJpaEntity();
        apply(entity, user);
        return toData(users.saveAndFlush(entity));
    }

    /** Ver {@link #insert}: o {@code saveAndFlush} garante o {@code last_updated_at} novo na volta. */
    @Override
    @Transactional
    public UserData update(UserData user) {
        UserJpaEntity entity = users.findById(user.id())
                .orElseThrow(() -> new IllegalStateException("Usuário inexistente para atualização: " + user.id()));
        apply(entity, user);
        return toData(users.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        users.deleteById(id);
    }

    /**
     * Copia o registro para a entidade gerenciada, resolvendo os papéis já persistidos.
     *
     * <p>As colunas de auditoria ficam de fora de propósito: quem as escreve é o listener do
     * Spring Data. Copiá-las do registro deixaria o núcleo definir "quando" — e no cadastro
     * ele não tem essa informação, porque {@code User.create} não recebe instante nenhum.
     */
    private void apply(UserJpaEntity entity, UserData data) {
        entity.setName(data.name());
        entity.setEmail(data.email());
        entity.setLogin(data.login());
        entity.setPassword(data.passwordHash());
        entity.replaceRoles(resolveRoles(data.roles()));
        entity.replaceAddresses(data.addresses().stream().map(UserDataSourceJpa::toEntity).toList());
    }

    /**
     * Papéis são catálogo: o vínculo N:M aponta para as linhas que já existem em
     * {@code roles}, nunca cria novas.
     */
    private Set<RoleJpaEntity> resolveRoles(Set<RoleData> wanted) {
        Collection<UUID> ids = wanted.stream().map(RoleData::id).toList();
        return new LinkedHashSet<>(roles.findAllById(ids));
    }

    /**
     * Endereço sempre nasce sem id: {@code orphanRemoval} apaga os antigos e o banco emite
     * os novos. É a tradução literal de {@code User.replaceAddresses} — a lista é substituída
     * como um todo, não reconciliada item a item.
     */
    private static AddressJpaEntity toEntity(AddressData data) {
        AddressJpaEntity entity = new AddressJpaEntity();
        entity.setStreet(data.street());
        entity.setNumber(data.number());
        entity.setComplement(data.complement());
        entity.setNeighborhood(data.neighborhood());
        entity.setCity(data.city());
        entity.setState(data.state());
        entity.setZipCode(data.zipCode());
        return entity;
    }

    static UserData toData(UserJpaEntity entity) {
        return new UserData(entity.getId(), entity.getName(), entity.getEmail(), entity.getLogin(),
                entity.getPassword(),
                entity.getRoles().stream()
                        .map(role -> new RoleData(role.getId(), role.getName()))
                        .collect(Collectors.toCollection(LinkedHashSet::new)),
                entity.getAddresses().stream().map(UserDataSourceJpa::toData).toList(),
                entity.getCreatedAt(), entity.getLastUpdatedAt());
    }

    private static AddressData toData(AddressJpaEntity entity) {
        return new AddressData(entity.getId(), entity.getStreet(), entity.getNumber(), entity.getComplement(),
                entity.getNeighborhood(), entity.getCity(), entity.getState(), entity.getZipCode());
    }

    private static Sort toSort(PageRequest request) {
        String property = request.hasSort()
                ? SORT_PROPERTIES.getOrDefault(request.sortBy(), DEFAULT_SORT_PROPERTY)
                : DEFAULT_SORT_PROPERTY;
        Sort.Direction direction = request.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
