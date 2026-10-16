package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import com.postech.restaurantes.domain.exception.InvariantViolationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Compartilha a resolução da cidade entre usuários e restaurantes, sem criar dados do catálogo. */
@Component
public class CityReferenceResolver {
    private final SpringDataCityRepository cities;

    public CityReferenceResolver(SpringDataCityRepository cities) {
        this.cities = cities;
    }

    public CityJpaEntity resolve(UUID cityId) {
        if (cityId == null) {
            throw new InvariantViolationException("Id da cidade do endereço inválido");
        }
        return cities.findById(cityId).orElseThrow(() ->
                new ResourceNotFoundException("Cidade do endereço inexistente"));
    }
}
