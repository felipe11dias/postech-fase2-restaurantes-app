package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Catálogo somente de leitura: não expõe save nem delete. */
public interface SpringDataCityRepository extends Repository<CityJpaEntity, UUID> {
    @EntityGraph(attributePaths = "state")
    Optional<CityJpaEntity> findById(UUID id);

    @EntityGraph(attributePaths = "state")
    @Query("select c from CityJpaEntity c where (:stateId is null or c.state.id = :stateId) "
            + "and lower(c.name) like lower(concat('%', :name, '%'))")
    Page<CityJpaEntity> search(UUID stateId, String name, Pageable pageable);
}
