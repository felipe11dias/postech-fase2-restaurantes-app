package com.postech.restaurantes.infrastructure.persistence.jpa.user;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.persistence.jpa.address.AddressJpaMapping;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.address.UserAddressJpaEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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

    public UserDataSourceJpa(SpringDataUserRepository users) {
        this.users = users;
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

    /** O CPF pode estar no perfil de cliente ou no de entregador; o primeiro usuário achado basta. */
    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findByCpf(String cpf) {
        return users.findByCpf(cpf).stream().findFirst().map(UserDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserData> findByCnpj(String cnpj) {
        return users.findByCnpj(cnpj).map(UserDataSourceJpa::toData);
    }

    /**
     * Duas consultas: a primeira pagina os ids no banco, a segunda carrega os usuários da
     * página com perfis e endereços. O hash da senha vem junto porque o registro é traduzido
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
        // O {@code save} emite o id do usuário; só então os perfis, que o usam como chave, entram.
        UserJpaEntity gravado = users.save(entity);
        ProfileJpaMapping.apply(gravado, user);
        return toData(users.saveAndFlush(gravado));
    }

    /** Ver {@link #insert}: o {@code saveAndFlush} garante o {@code last_updated_at} novo na volta. */
    @Override
    @Transactional
    public UserData update(UserData user) {
        UserJpaEntity entity = users.findById(user.id())
                .orElseThrow(() -> new IllegalStateException("Usuário inexistente para atualização: " + user.id()));
        apply(entity, user);
        ProfileJpaMapping.apply(entity, user);
        return toData(users.saveAndFlush(entity));
    }

    /**
     * Os perfis saem antes do usuário. Com {@code @PrimaryKeyJoinColumn} do lado do usuário, o
     * Hibernate entende que é o usuário que referencia o perfil e apagaria o usuário primeiro; o
     * {@code ON DELETE CASCADE} do banco levaria o perfil junto, e o {@code DELETE} do perfil que vem
     * em seguida não acharia a linha ({@code StaleObjectStateException}). Tirar os perfis
     * ({@code orphanRemoval}) e descarregar antes faz a remoção seguir a ordem das chaves estrangeiras.
     */
    @Override
    @Transactional
    public void delete(UUID id) {
        users.findById(id).ifPresent(user -> {
            ProfileJpaMapping.removeAll(user);
            users.flush();
            users.delete(user);
        });
    }

    /**
     * Copia o registro para a entidade gerenciada (os perfis, que dependem do id, à parte: ver
     * {@link ProfileJpaMapping}).
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
        entity.replaceAddresses(reconcile(entity.getAddresses(), data.addresses()));
    }

    /**
     * Tradução de {@code User.replaceAddresses}: o registro com id é um vínculo que o usuário já
     * tem, atualizado na mesma linha (vínculo e endereço mantêm o id); sem id, é um vínculo novo; o
     * que não vier é removido pelo {@code orphanRemoval} (o vínculo e, em cascata, o endereço dele).
     * Assim o id que o cliente recebeu continua valendo depois de uma atualização.
     *
     * <p>Na descarga, o Hibernate pode inserir ou atualizar um padrão novo antes de desmarcar ou
     * apagar o antigo: por um instante há dois endereços padrão do mesmo usuário. Por isso a
     * restrição "um padrão por usuário" é conferida no commit ({@code DEFERRABLE INITIALLY
     * DEFERRED}, migration V4).
     */
    private static List<UserAddressJpaEntity> reconcile(List<UserAddressJpaEntity> current,
                                                        List<UserAddressData> wanted) {
        Map<UUID, UserAddressJpaEntity> byId = current.stream()
                .collect(Collectors.toMap(UserAddressJpaEntity::getId, Function.identity()));
        return wanted.stream()
                .map(data -> data.id() == null ? toEntity(data) : update(byId.get(data.id()), data))
                .toList();
    }

    /** O domínio só deixa passar id de um endereço do próprio usuário; ausência aqui é falha de estado. */
    private static UserAddressJpaEntity update(UserAddressJpaEntity existing, UserAddressData data) {
        if (existing == null) {
            throw new IllegalStateException("Endereço do usuário inexistente para atualização: " + data.id());
        }
        existing.setLabel(data.label());
        existing.setDefaultAddress(data.isDefault());
        AddressJpaMapping.copy(data.address(), existing.getAddress());
        return existing;
    }

    private static UserAddressJpaEntity toEntity(UserAddressData data) {
        UserAddressJpaEntity entity = new UserAddressJpaEntity();
        entity.setLabel(data.label());
        entity.setDefaultAddress(data.isDefault());
        entity.setAddress(AddressJpaMapping.toEntity(data.address()));
        return entity;
    }

    static UserData toData(UserJpaEntity entity) {
        return new UserData(entity.getId(), entity.getName(), entity.getEmail(), entity.getLogin(),
                entity.getPassword(), ProfileJpaMapping.toData(entity.getOwner()),
                ProfileJpaMapping.toData(entity.getClient()), ProfileJpaMapping.toData(entity.getCourier()),
                ProfileJpaMapping.toData(entity.getAdmin()),
                entity.getAddresses().stream().map(UserDataSourceJpa::toData).toList(),
                entity.getCreatedAt(), entity.getLastUpdatedAt());
    }

    private static UserAddressData toData(UserAddressJpaEntity entity) {
        return new UserAddressData(entity.getId(), entity.getLabel(), entity.isDefaultAddress(),
                AddressJpaMapping.toData(entity.getAddress()));
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
