package com.postech.restaurantes.infrastructure.persistence.user;

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
    @EntityGraph(attributePaths = {"roles", "addresses"})
    Optional<UserJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"roles", "addresses"})
    Optional<UserJpaEntity> findByLogin(String login);

    @EntityGraph(attributePaths = {"roles", "addresses"})
    Optional<UserJpaEntity> findByEmail(String email);

    /**
     * Primeiro passo da busca paginada: só os ids da página. Paginar junto com o
     * {@code join fetch} das coleções faria o Hibernate trazer todas as linhas e recortar a
     * página em memória; separando em duas consultas, o banco continua paginando.
     */
    @Query("select u.id from UserJpaEntity u where lower(u.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByName(@Param("name") String name, Pageable pageable);

    /** Segundo passo: os usuários da página, com papéis e endereços em uma única consulta. */
    @EntityGraph(attributePaths = {"roles", "addresses"})
    List<UserJpaEntity> findByIdIn(Collection<UUID> ids, Sort sort);
}
