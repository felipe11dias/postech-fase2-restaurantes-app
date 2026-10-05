package com.postech.restaurantes.infrastructure.persistence.jpa.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório Spring Data de {@code users}. Detalhe de infraestrutura: o núcleo não o conhece. */
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"owner", "client", "courier", "admin", "addresses", "addresses.address"})
    Optional<UserJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"owner", "client", "courier", "admin", "addresses", "addresses.address"})
    Optional<UserJpaEntity> findByLogin(String login);

    @EntityGraph(attributePaths = {"owner", "client", "courier", "admin", "addresses", "addresses.address"})
    Optional<UserJpaEntity> findByEmail(String email);

    /**
     * Usuário cujo perfil de cliente ou de entregador tem o CPF. Os dois perfis do mesmo usuário têm o
     * mesmo CPF; outro usuário com ele não deveria existir, mas a lista não quebra se existir.
     */
    @EntityGraph(attributePaths = {"owner", "client", "courier", "admin", "addresses", "addresses.address"})
    @Query("select u from UserJpaEntity u left join u.client c left join u.courier k where c.cpf = :cpf or k.cpf = :cpf")
    List<UserJpaEntity> findByCpf(@Param("cpf") String cpf);

    @EntityGraph(attributePaths = {"owner", "client", "courier", "admin", "addresses", "addresses.address"})
    @Query("select u from UserJpaEntity u join u.owner o where o.cnpj = :cnpj")
    Optional<UserJpaEntity> findByCnpj(@Param("cnpj") String cnpj);

    /**
     * Primeiro passo da busca paginada: só os ids da página. Paginar junto com o
     * {@code join fetch} das coleções faria o Hibernate trazer todas as linhas e recortar a
     * página em memória; separando em duas consultas, o banco continua paginando.
     */
    @Query("select u.id from UserJpaEntity u where lower(u.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByName(@Param("name") String name, Pageable pageable);

    /** Segundo passo: os usuários da página, com perfis e endereços. */
    @EntityGraph(attributePaths = {"owner", "client", "courier", "admin", "addresses", "addresses.address"})
    List<UserJpaEntity> findByIdIn(Collection<UUID> ids, Sort sort);
}
