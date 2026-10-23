package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.CityData;
import com.postech.restaurantes.adapter.datasource.data.StateData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.List;
import java.util.UUID;

public interface ILocationDataSource {
    List<StateData> findStates();
    PageResult<CityData> searchCities(UUID stateId, String name, PageRequest request);
}
