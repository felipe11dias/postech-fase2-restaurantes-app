package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repositório Spring Data de {@code restaurants}. Detalhe de infraestrutura. */
public interface SpringDataRestaurantRepository extends JpaRepository<RestaurantJpaEntity, UUID> {

    @Query("select r.id from RestaurantJpaEntity r where lower(r.name) like lower(concat('%', :name, '%'))")
    Page<UUID> findIdsByName(@Param("name") String name, Pageable pageable);

    List<RestaurantJpaEntity> findByIdIn(Collection<UUID> ids, Sort sort);
}
