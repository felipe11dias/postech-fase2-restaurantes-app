package com.postech.restaurantes.infrastructure.persistence.jpa.cuisine;

import com.postech.restaurantes.adapter.datasource.ICuisineDataSource;
import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CuisineDataSourceJpa implements ICuisineDataSource {

    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "name", "name",
            "createdAt", "createdAt",
            "lastUpdatedAt", "lastUpdatedAt");

    private static final String DEFAULT_SORT_PROPERTY = "name";

    private final SpringDataCuisineRepository cuisines;

    public CuisineDataSourceJpa(SpringDataCuisineRepository cuisines) {
        this.cuisines = cuisines;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CuisineData> findById(UUID id) {
        return cuisines.findById(id).map(CuisineDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuisineData> findAllById(Collection<UUID> ids) {
        return cuisines.findAllById(ids).stream().map(CuisineDataSourceJpa::toData).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CuisineData> findByName(String name) {
        return cuisines.findByNameIgnoreCase(name).map(CuisineDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CuisineData> search(String name, PageRequest request) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.page(), request.size(),
                toSort(request));
        Page<CuisineJpaEntity> page = cuisines.findByNameContainingIgnoreCase(name == null ? "" : name, pageable);
        return new PageResult<>(page.getContent().stream().map(CuisineDataSourceJpa::toData).toList(),
                request.page(), request.size(), page.getTotalElements());
    }

    @Override
    @Transactional
    public CuisineData insert(CuisineData data) {
        CuisineJpaEntity entity = new CuisineJpaEntity();
        entity.setName(data.name());
        return toData(cuisines.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public CuisineData update(CuisineData data) {
        CuisineJpaEntity entity = cuisines.findById(data.id())
                .orElseThrow(() -> new IllegalStateException("Tipo de cozinha inexistente para atualização: " + data.id()));
        entity.setName(data.name());
        return toData(cuisines.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInRestaurants(UUID id) {
        return cuisines.existsInRestaurants(id);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        cuisines.deleteById(id);
    }

    public static CuisineData toData(CuisineJpaEntity entity) {
        return new CuisineData(entity.getId(), entity.getName(), entity.getCreatedAt(), entity.getLastUpdatedAt());
    }

    private static Sort toSort(PageRequest request) {
        String property = request.hasSort()
                ? SORT_PROPERTIES.getOrDefault(request.sortBy(), DEFAULT_SORT_PROPERTY)
                : DEFAULT_SORT_PROPERTY;
        Sort.Direction direction = request.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}