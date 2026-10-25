package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import com.postech.restaurantes.adapter.controller.CuisineController;
import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.CuisineModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ApiDocumentation;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CreateCuisineRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateCuisineRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.CuisineResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.exception.ProblemType;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping(ApiRoutes.CUISINES)
@Tag(name = "Tipos de cozinha")
public class CuisineRestController {

    private static final String SOMENTE_ADMINISTRADOR = "hasRole('ADMIN')";

    private final CuisineController controller;
    private final CuisineModelAssembler assembler;

    public CuisineRestController(CuisineController controller, CuisineModelAssembler assembler) {
        this.controller = controller;
        this.assembler = assembler;
    }

    @PostMapping
    @PreAuthorize(SOMENTE_ADMINISTRADOR)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Cadastra um tipo de cozinha (só administrador)")
    @ApiResponse(responseCode = "201", description = "Tipo de cozinha criado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Quem chama não é administrador")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "Já existe um tipo de cozinha com esse nome")
    public ResponseEntity<EntityModel<CuisineResponse>> create(@Valid @RequestBody CreateCuisineRequest request) {
        CuisineView criado = controller.create(request.toDTO());
        return ResponseEntity.created(URI.create(assembler.selfLink(criado).getHref()))
                .body(assembler.toModel(criado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um tipo de cozinha por id")
    @ApiResponse(responseCode = "200", description = "Tipo de cozinha encontrado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Id não é um UUID")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Tipo de cozinha não encontrado")
    public EntityModel<CuisineResponse> findById(@PathVariable UUID id) {
        return assembler.toModel(controller.findById(id));
    }

    @GetMapping
    @Operation(summary = "Lista os tipos de cozinha, com busca por trecho do nome e paginação")
    @ApiResponse(responseCode = "200", description = "Página de tipos de cozinha")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Parâmetros de paginação inválidos")
    public PagedModel<EntityModel<CuisineResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return assembler.toPagedModel(
                controller.search(name, RestaurantRestController.paginacao(page, size, sort)), name, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize(SOMENTE_ADMINISTRADOR)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Altera o nome de um tipo de cozinha (só administrador)")
    @ApiResponse(responseCode = "200", description = "Tipo de cozinha alterado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Quem chama não é administrador")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Tipo de cozinha não encontrado")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "Já existe outro tipo de cozinha com esse nome")
    public EntityModel<CuisineResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody UpdateCuisineRequest request) {
        return assembler.toModel(controller.update(request.toDTO(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SOMENTE_ADMINISTRADOR)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Exclui um tipo de cozinha que nenhum restaurante usa (só administrador)")
    @ApiResponse(responseCode = "204", description = "Tipo de cozinha excluído")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Quem chama não é administrador")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Tipo de cozinha não encontrado")
    @ErrorResponse(type = ProblemType.RESOURCE_IN_USE, description = "Algum restaurante ainda usa este tipo de cozinha")
    public void delete(@PathVariable UUID id) {
        controller.delete(id);
    }
}