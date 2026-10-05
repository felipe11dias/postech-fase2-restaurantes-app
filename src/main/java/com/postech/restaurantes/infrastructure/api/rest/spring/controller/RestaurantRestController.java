package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import com.postech.restaurantes.adapter.controller.RestaurantController;
import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.RestaurantModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ApiDocumentation;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CreateRestaurantRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateRestaurantRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.RestaurantResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.exception.ProblemType;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta HTTP do agregado de restaurante.
 */
@RestController
@RequestMapping(ApiRoutes.RESTAURANTS)
@Tag(name = "Restaurantes")
public class RestaurantRestController {

    private static final String DONO_OU_ADMIN = "hasRole('OWNER') or hasRole('ADMIN')";

    private final RestaurantController controller;
    private final RestaurantModelAssembler assembler;

    public RestaurantRestController(RestaurantController controller, RestaurantModelAssembler assembler) {
        this.controller = controller;
        this.assembler = assembler;
    }

    @PostMapping
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Cadastra um restaurante",
            description = "Exige papel de dono de restaurante (ROLE_OWNER) ou de administrador (ROLE_ADMIN).")
    @ApiResponse(responseCode = "201", description = "Restaurante criado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Usuário sem perfil de dono/admin")
    @ErrorResponse(type = ProblemType.FORBIDDEN_OPERATION, description = "O dono indicado não tem perfil de dono")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Dono não encontrado")
    public ResponseEntity<EntityModel<RestaurantResponse>> create(@Valid @RequestBody CreateRestaurantRequest request) {
        RestaurantView criado = controller.create(request.toDTO());
        EntityModel<RestaurantResponse> corpo = assembler.toModel(criado);
        return ResponseEntity.created(URI.create(assembler.selfLink(criado).getHref())).body(corpo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um restaurante por ID")
    @ApiResponse(responseCode = "200", description = "Restaurante encontrado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Id não é um UUID")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Restaurante não encontrado")
    public EntityModel<RestaurantResponse> findById(@Parameter(description = "Id do restaurante") @PathVariable UUID id) {
        return assembler.toModel(controller.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista os restaurantes",
            description = "Paginada, com busca por nome de restaurante.")
    @ApiResponse(responseCode = "200", description = "Página de restaurantes")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Parâmetros de paginação inválidos")
    public PagedModel<EntityModel<RestaurantResponse>> search(
            @Parameter(description = "Trecho do nome do restaurante", example = "Sabor")
            @RequestParam(required = false) String name,
            @Parameter(description = "Página, a partir de 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página, de 1 a 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "propriedade,direcao — id, name, createdAt ou lastUpdatedAt; asc ou desc", example = "name,asc")
            @RequestParam(required = false) String sort) {
        return assembler.toPagedModel(controller.search(name, paginacao(page, size, sort)), name, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Atualiza um restaurante",
            description = "Exige papel de dono de restaurante (ROLE_OWNER) ou de administrador (ROLE_ADMIN).")
    @ApiResponse(responseCode = "200", description = "Restaurante atualizado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Usuário sem perfil de dono/admin")
    @ErrorResponse(type = ProblemType.FORBIDDEN_OPERATION, description = "O dono indicado não tem perfil de dono")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Restaurante ou dono não encontrado")
    public EntityModel<RestaurantResponse> update(
            @Parameter(description = "Id do restaurante") @PathVariable UUID id,
            @Valid @RequestBody UpdateRestaurantRequest request) {
        return assembler.toModel(controller.update(request.toDTO(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Exclui um restaurante",
            description = "Exige papel de dono de restaurante (ROLE_OWNER) ou de administrador (ROLE_ADMIN).")
    @ApiResponse(responseCode = "204", description = "Restaurante excluído")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Usuário sem perfil de dono/admin")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Restaurante não encontrado")
    public void delete(@Parameter(description = "Id do restaurante") @PathVariable UUID id) {
        controller.delete(id);
    }

    static PageRequest paginacao(int page, int size, String sort) {
        PageRequest pedido = new PageRequest(page, size, null, SortDirection.ASC);
        if (sort == null || sort.isBlank()) {
            return pedido;
        }
        String[] partes = sort.split(",", 2);
        SortDirection direcao = partes.length == 2 ? SortDirection.from(partes[1]) : SortDirection.ASC;
        return pedido.withSort(partes[0], direcao);
    }
}
