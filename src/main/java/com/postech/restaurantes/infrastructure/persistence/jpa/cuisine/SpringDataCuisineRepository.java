package com.postech.restaurantes.infrastructure.persistence.jpa.cuisine;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataCuisineRepository extends JpaRepository<CuisineJpaEntity, UUID> {

    Optional<CuisineJpaEntity> findByNameIgnoreCase(String name);

    Page<CuisineJpaEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(value = "select exists (select 1 from restaurant_cuisines where cuisine_id = :id)", nativeQuery = true)
    boolean existsInRestaurants(@Param("id") UUID id);
}