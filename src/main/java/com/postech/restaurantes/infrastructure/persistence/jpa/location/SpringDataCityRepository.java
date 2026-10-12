package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas Spring Data do catálogo de cidades. */
public interface SpringDataCityRepository extends JpaRepository<CityJpaEntity, UUID> {

    Optional<CityJpaEntity> findByNameIgnoreCaseAndState_Id(String name, UUID stateId);
}
