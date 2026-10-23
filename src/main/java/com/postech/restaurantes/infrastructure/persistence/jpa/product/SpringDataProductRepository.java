package com.postech.restaurantes.infrastructure.persistence.jpa.product;

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

/** Repositório Spring Data de produtos. */
public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {

    @EntityGraph(attributePaths = {"images", "optionGroups", "optionGroups.values"})
    Optional<ProductJpaEntity> findByIdAndRestaurantId(UUID id, UUID restaurantId);

    @Query("select p.id from ProductJpaEntity p where p.restaurantId = :restaurantId")
    Page<UUID> findIdsByRestaurantId(@Param("restaurantId") UUID restaurantId, Pageable pageable);

    @EntityGraph(attributePaths = {"images", "optionGroups", "optionGroups.values"})
    List<ProductJpaEntity> findByIdIn(Collection<UUID> ids, Sort sort);
}
