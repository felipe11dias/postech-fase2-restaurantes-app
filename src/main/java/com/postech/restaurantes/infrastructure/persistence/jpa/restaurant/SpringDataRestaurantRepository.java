package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

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

/** Repositório Spring Data de {@code restaurants}. Detalhe de infraestrutura. */
public interface SpringDataRestaurantRepository extends JpaRepository<RestaurantJpaEntity, UUID> {

    /** Endereço e horários vêm na mesma consulta: o restaurante é sempre traduzido com eles. */
    @Override
    @EntityGraph(attributePaths = {"address", "officeHours"})
    Optional<RestaurantJpaEntity> findById(UUID id);

    @Query("select r.id from RestaurantJpaEntity r where lower(r.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByName(@Param("name") String name, Pageable pageable);

    @Query("select r.id from RestaurantJpaEntity r where r.userId = :userId "
            + "and lower(r.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByNameAndUserId(@Param("name") String name, @Param("userId") UUID userId, Pageable pageable);

    /** Só os restaurantes ligados ao tipo de cozinha, pela tabela restaurant_cuisines (cada par aparece uma vez). */
    @Query("select r.id from RestaurantJpaEntity r join r.cuisines c where c.id = :cuisineId "
            + "and lower(r.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByNameAndCuisineId(@Param("name") String name, @Param("cuisineId") UUID cuisineId,
                                         Pageable pageable);

    @Query("select r.id from RestaurantJpaEntity r join r.cuisines c where c.id = :cuisineId and r.userId = :userId "
            + "and lower(r.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByNameAndUserIdAndCuisineId(@Param("name") String name, @Param("userId") UUID userId,
                                                  @Param("cuisineId") UUID cuisineId, Pageable pageable);

    @EntityGraph(attributePaths = {"address", "officeHours"})
    List<RestaurantJpaEntity> findByIdIn(Collection<UUID> ids, Sort sort);

    boolean existsByUserId(UUID userId);

    List<RestaurantJpaEntity> findByUserId(UUID userId);
}
