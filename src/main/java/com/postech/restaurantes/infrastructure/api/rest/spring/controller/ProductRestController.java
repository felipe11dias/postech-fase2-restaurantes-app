package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import com.postech.restaurantes.adapter.controller.ProductController;
import com.postech.restaurantes.adapter.presenter.view.ProductView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.ProductModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ApiDocumentation;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ProductRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.ProductResponse;
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

/** API REST do catálogo de produtos, aninhada sob o restaurante para respeitar a posse do recurso. */
@RestController
@RequestMapping(ApiRoutes.PRODUCTS)
@Tag(name = "Produtos")
public class ProductRestController {

    private static final String PODE_MANTER = "hasRole('ADMIN') or "
            + "@restaurantSecurity.isOwner(#restaurantId, authentication)";

    private final ProductController controller;
    private final ProductModelAssembler assembler;

    public ProductRestController(ProductController controller, ProductModelAssembler assembler) {
        this.controller = controller;
        this.assembler = assembler;
    }

    @PostMapping
    @PreAuthorize(PODE_MANTER)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Cadastra um produto com imagens e grupos de opções")
    @ApiResponse(responseCode = "201", description = "Produto criado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Dados ou regras das opções inválidos")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Apenas o dono do restaurante ou administrador")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Restaurante não encontrado")
    public ResponseEntity<EntityModel<ProductResponse>> create(
            @PathVariable UUID restaurantId, @Valid @RequestBody ProductRequest request) {
        ProductView created = controller.create(restaurantId, request.toDTO());
        EntityModel<ProductResponse> body = assembler.toModel(created);
        return ResponseEntity.created(URI.create(assembler.selfLink(created).getHref())).body(body);
    }

    @GetMapping
    @Operation(summary = "Lista os produtos de um restaurante")
    @ApiResponse(responseCode = "200", description = "Página de produtos")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Parâmetros de paginação inválidos")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Restaurante não encontrado")
    public PagedModel<EntityModel<ProductResponse>> search(
            @PathVariable UUID restaurantId,
            @Parameter(description = "Página, a partir de 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página, de 1 a 100", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        PageResult<ProductView> result = controller.search(restaurantId, PageRequest.of(page, size));
        return assembler.toPagedModel(result, restaurantId);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Consulta um produto, suas imagens e opções")
    @ApiResponse(responseCode = "200", description = "Produto encontrado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Identificador inválido")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Produto não encontrado")
    public EntityModel<ProductResponse> findById(@PathVariable UUID restaurantId, @PathVariable UUID productId) {
        return assembler.toModel(controller.findById(restaurantId, productId));
    }

    @PutMapping("/{productId}")
    @PreAuthorize(PODE_MANTER)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Atualiza um produto e substitui suas imagens e grupos de opções")
    @ApiResponse(responseCode = "200", description = "Produto atualizado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Dados ou regras das opções inválidos")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Apenas o dono do restaurante ou administrador")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Produto não encontrado")
    public EntityModel<ProductResponse> update(@PathVariable UUID restaurantId, @PathVariable UUID productId,
                                                @Valid @RequestBody ProductRequest request) {
        return assembler.toModel(controller.update(restaurantId, productId, request.toDTO()));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize(PODE_MANTER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Exclui um produto e suas imagens e opções")
    @ApiResponse(responseCode = "204", description = "Produto excluído")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Apenas o dono do restaurante ou administrador")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Produto não encontrado")
    public void delete(@PathVariable UUID restaurantId, @PathVariable UUID productId) {
        controller.delete(restaurantId, productId);
    }
}
