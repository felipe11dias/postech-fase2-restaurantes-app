package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import com.postech.restaurantes.adapter.datasource.ILocationDataSource;
import com.postech.restaurantes.adapter.datasource.data.CityData;
import com.postech.restaurantes.adapter.datasource.data.StateData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class LocationDataSourceJpa implements ILocationDataSource {
    private final SpringDataStateRepository states;
    private final SpringDataCityRepository cities;

    public LocationDataSourceJpa(SpringDataStateRepository states, SpringDataCityRepository cities) {
        this.states = states;
        this.cities = cities;
    }

    @Override
    public List<StateData> findStates() {
        return states.findAllByOrderByNameAsc().stream().map(LocationJpaMapping::toData).toList();
    }

    @Override
    public PageResult<CityData> searchCities(UUID stateId, String name, PageRequest request) {
        var pageable = org.springframework.data.domain.PageRequest.of(request.page(), request.size(),
                Sort.by("name", "id"));
        var page = cities.search(stateId, name == null ? "" : name, pageable);
        return new PageResult<>(page.stream().map(LocationJpaMapping::toData).toList(),
                request.page(), request.size(), page.getTotalElements());
    }
}
