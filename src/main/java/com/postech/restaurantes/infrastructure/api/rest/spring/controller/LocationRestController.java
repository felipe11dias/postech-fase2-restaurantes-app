package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import com.postech.restaurantes.adapter.controller.LocationController;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.LocationModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.CityResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.StateResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.exception.ProblemType;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consulta pública do catálogo necessário ao cadastro de endereços. */
@RestController
@Tag(name = "Localizações")
public class LocationRestController {
    private final LocationController controller;
    private final LocationModelAssembler assembler;

    public LocationRestController(LocationController controller, LocationModelAssembler assembler) {
        this.controller = controller;
        this.assembler = assembler;
    }

    @GetMapping(ApiRoutes.STATES)
    @Operation(summary = "Lista os estados cadastrados")
    @ApiResponse(responseCode = "200", description = "Estados em ordem alfabética")
    public CollectionModel<StateResponse> findStates() {
        return assembler.toModel(controller.findStates());
    }

    @GetMapping(ApiRoutes.CITIES)
    @Operation(summary = "Consulta as cidades cadastradas", description = "Filtra por estado e nome, com paginação.")
    @ApiResponse(responseCode = "200", description = "Cidades em ordem alfabética")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Filtro ou paginação inválidos")
    public PagedModel<CityResponse> searchCities(
            @RequestParam(required = false) UUID stateId,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return assembler.toPagedModel(controller.searchCities(stateId, name, PageRequest.of(page, size)), stateId, name);
    }
}
