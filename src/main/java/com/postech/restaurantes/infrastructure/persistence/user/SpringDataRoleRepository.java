package com.postech.restaurantes.infrastructure.persistence.user;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório Spring Data de {@code roles}. */
public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {

    Set<RoleJpaEntity> findByNameIn(Collection<String> names);
}
