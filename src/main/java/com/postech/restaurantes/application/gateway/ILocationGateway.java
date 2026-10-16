package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.location.city.City;
import com.postech.restaurantes.domain.entity.location.state.State;
import java.util.List;
import java.util.UUID;

/** Catálogo geográfico mantido por migrations, com acesso somente de leitura. */
public interface ILocationGateway {
    List<State> findStates();
    PageResult<City> searchCities(UUID stateId, String name, PageRequest request);
}
