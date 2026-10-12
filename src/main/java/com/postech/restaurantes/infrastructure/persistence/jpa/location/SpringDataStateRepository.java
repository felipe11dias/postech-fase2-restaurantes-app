package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas Spring Data do catálogo de estados. */
public interface SpringDataStateRepository extends JpaRepository<StateJpaEntity, UUID> {

    Optional<StateJpaEntity> findByAcronymIgnoreCase(String acronym);
}
