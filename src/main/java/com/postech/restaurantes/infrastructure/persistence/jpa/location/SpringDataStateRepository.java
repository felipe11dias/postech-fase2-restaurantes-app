package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/** Catálogo somente de leitura: não expõe save nem delete. */
public interface SpringDataStateRepository extends Repository<StateJpaEntity, UUID> {
    List<StateJpaEntity> findAllByOrderByNameAsc();
}
