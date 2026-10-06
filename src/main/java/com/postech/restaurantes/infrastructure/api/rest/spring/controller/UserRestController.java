package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import com.postech.restaurantes.adapter.controller.UserController;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.UserModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ApiDocumentation;
import com.postech.restaurantes.infrastructure.api.rest.spring.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.AdminProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ChangePasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ClientProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CourierProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CourierStatusRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.NewUserRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.OwnerProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateUserRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.UserResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.exception.ProblemType;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta HTTP do agregado de usuário. Fina de propósito: valida a sintaxe do corpo, converte
 * para o DTO do caso de uso, delega ao controller de adaptação e devolve a representação com
 * links. Nenhuma regra de negócio — trocar REST por outro canal não exigiria reescrever nada
 * de dentro.
 *
 * <p><strong>Autorização por posse.</strong> As operações por id exigem ser o dono do cadastro
 * ou ter {@code ROLE_ADMIN}: conhecer o id de outra pessoa não basta para lê-la ou alterá-la.
 *
 * <p><strong>Documentação.</strong> Cada operação declara aqui o que devolve e, com
 * {@code @ErrorResponse}, em que casos falha — pela categoria do erro, de onde sai o código HTTP.
 * O {@code @SecurityRequirement} de cada operação precisa bater com o {@code @PreAuthorize} e
 * com a {@code SecurityConfig} — e há teste de integração que confere isso contra o
 * comportamento real.
 */
@RestController
@RequestMapping(ApiRoutes.USERS)
@Tag(name = "Usuários")
public class UserRestController {

    /** Só o dono do cadastro ou um administrador. Fecha a referência direta a objeto (IDOR). */
    private static final String DONO_OU_ADMIN =
            "hasRole('ADMIN') or @userSecurity.isSelf(#id, authentication)";

    /** Operação sobre o conjunto de cadastros, e não sobre um só: não há dono a verificar. */
    private static final String SO_ADMIN = "hasRole('ADMIN')";

    private final UserController controller;
    private final UserModelAssembler assembler;

    public UserRestController(UserController controller, UserModelAssembler assembler) {
        this.controller = controller;
        this.assembler = assembler;
    }

    /**
     * Autocadastro público, com os perfis de dono, cliente e entregador. O de administrador não existe
     * no pedido: não se obtém por autocadastro.
     *
     * <p>O {@code @ResponseStatus} não muda o comportamento — quem define o 201 é o
     * {@code ResponseEntity.created} —, mas sem ele o springdoc documentaria 200.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Autocadastro",
            description = "Cria um usuário. Público. Traz ao menos um perfil — dono (owner), cliente (client), "
                    + "entregador (courier) —; os papéis (ROLE_OWNER, ROLE_CLIENT, ROLE_COURIER) saem dos perfis. "
                    + "O perfil de administrador não se obtém por aqui. A resposta traz o header Location do recurso criado.")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST,
            description = "Campo inválido, nenhum perfil, documento inválido ou endereço inconsistente")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "E-mail, login, CPF ou CNPJ já cadastrado")
    public ResponseEntity<EntityModel<UserResponse>> register(@Valid @RequestBody NewUserRequest request) {
        UserView criado = controller.register(request.toDTO());
        EntityModel<UserResponse> corpo = assembler.toModel(criado);
        return ResponseEntity.created(URI.create(assembler.selfLink(criado).getHref())).body(corpo);
    }

    @GetMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Consulta um cadastro", description = "Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro encontrado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Id não é um UUID")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    public EntityModel<UserResponse> findById(@Parameter(description = "Id do usuário") @PathVariable UUID id) {
        return assembler.toModel(controller.findById(id));
    }

    /**
     * Busca paginada. O parâmetro {@code sort} vem no formato {@code propriedade,direcao}; a
     * lista de propriedades aceitas é do caso de uso, e a tradução para coluna é da origem de
     * dados — a borda apenas repassa o que foi pedido.
     *
     * <p><strong>Só administrador.</strong> A listagem devolve e-mail, login e endereço de cada
     * cadastro; aberta a qualquer autenticado, ela entregaria de uma vez tudo o que a regra de
     * posse recusa um a um nas operações por id.
     */
    @GetMapping
    @PreAuthorize(SO_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Lista os cadastros",
            description = "Paginada, com busca parcial por nome sem diferenciar maiúsculas. Só administrador.")
    @ApiResponse(responseCode = "200", description = "Página de usuários")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Página ou tamanho fora dos limites")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Usuário não é administrador")
    public PagedModel<EntityModel<UserResponse>> search(
            @Parameter(description = "Trecho do nome, sem diferenciar maiúsculas", example = "silva")
            @RequestParam(required = false) String name,
            @Parameter(description = "Página, a partir de 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página, de 1 a 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "propriedade,direcao — id, name, email, login, createdAt ou lastUpdatedAt; "
                    + "asc ou desc. Propriedade fora da lista cai em name.", example = "name,asc")
            @RequestParam(required = false) String sort) {
        return assembler.toPagedModel(controller.search(name, paginacao(page, size, sort)), name, sort);
    }

    @PutMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Atualiza um cadastro",
            description = "Substitui nome, e-mail, login e a lista de endereços inteira. Não altera senha nem "
                    + "papéis. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro atualizado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido ou endereço inconsistente")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "E-mail ou login já usado por outro cadastro")
    public EntityModel<UserResponse> update(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return assembler.toModel(controller.update(id, request.toDTO()));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Troca a senha", description = "Exige a senha atual. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "204", description = "Senha trocada")
    @ErrorResponse(type = ProblemType.INVALID_PASSWORD, description = "Senha atual incorreta ou confirmação divergente")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo ausente ou senha nova fora das regras de tamanho")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    public void changePassword(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                               @Valid @RequestBody ChangePasswordRequest request) {
        controller.changePassword(id, request.toDTO());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Exclui um cadastro",
            description = "Remove também endereços e tokens de redefinição. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "204", description = "Cadastro excluído")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    public void delete(@Parameter(description = "Id do usuário") @PathVariable UUID id) {
        controller.delete(id);
    }

    // --- Perfis do cadastro -------------------------------------------------------------------
    // Os papéis do token são os do momento do login: incluir ou remover perfil vale para a
    // autorização a partir do próximo login.

    @PutMapping("/{id}/profiles/owner")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Inclui ou altera o perfil de dono",
            description = "Dá ao cadastro o papel ROLE_OWNER (no próximo login). Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro com o perfil de dono")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo ausente ou CNPJ inválido")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "CNPJ de outro cadastro")
    public EntityModel<UserResponse> saveOwnerProfile(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                                                      @Valid @RequestBody OwnerProfileRequest request) {
        return assembler.toModel(controller.saveProfile(id, request.toDTO()));
    }

    @PutMapping("/{id}/profiles/client")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Inclui ou altera o perfil de cliente",
            description = "Dá ao cadastro o papel ROLE_CLIENT (no próximo login). O CPF é o mesmo do perfil de "
                    + "entregador, se houver. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro com o perfil de cliente")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST,
            description = "Campo ausente, CPF inválido ou diferente do de entregador, nascimento futuro")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "CPF de outro cadastro")
    public EntityModel<UserResponse> saveClientProfile(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                                                       @Valid @RequestBody ClientProfileRequest request) {
        return assembler.toModel(controller.saveProfile(id, request.toDTO()));
    }

    @PutMapping("/{id}/profiles/courier")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Inclui ou altera o perfil de entregador",
            description = "Dá ao cadastro o papel ROLE_COURIER (no próximo login). Alterar não muda o status; o "
                    + "entregador novo começa OFFLINE. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro com o perfil de entregador")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST,
            description = "Campo ausente, CPF inválido ou diferente do de cliente, veículo desconhecido, CNH ou placa "
                    + "que não combinam com o veículo")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "CPF ou CNH de outro cadastro")
    public EntityModel<UserResponse> saveCourierProfile(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                                                        @Valid @RequestBody CourierProfileRequest request) {
        return assembler.toModel(controller.saveProfile(id, request.toDTO()));
    }

    @PutMapping("/{id}/profiles/admin")
    @PreAuthorize(SO_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Inclui ou altera o perfil de administrador",
            description = "Dá ao cadastro o papel ROLE_ADMIN (no próximo login). Só administrador — nem o próprio "
                    + "usuário se torna administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro com o perfil de administrador")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Código de funcionário ausente")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Usuário não é administrador")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "Código de funcionário de outro cadastro")
    public EntityModel<UserResponse> saveAdminProfile(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                                                      @Valid @RequestBody AdminProfileRequest request) {
        return assembler.toModel(controller.saveProfile(id, request.toDTO()));
    }

    @DeleteMapping("/{id}/profiles/{type}")
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Remove um perfil",
            description = "O último perfil não sai, nem o de dono enquanto houver restaurante do usuário, nem o de "
                    + "administrador do último administrador. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "204", description = "Perfil removido")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Tipo de perfil desconhecido ou último perfil")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado ou sem esse perfil")
    @ErrorResponse(type = ProblemType.RESOURCE_IN_USE,
            description = "Perfil de dono de quem tem restaurante, ou de administrador do último administrador")
    public void removeProfile(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                              @Parameter(description = "Tipo de perfil",
                                      schema = @Schema(allowableValues = {"owner", "client", "courier", "admin"}))
                              @PathVariable String type) {
        controller.removeProfile(id, type);
    }

    @PatchMapping("/{id}/profiles/courier/status")
    @PreAuthorize(DONO_OU_ADMIN)
    @SecurityRequirement(name = ApiDocumentation.BEARER_AUTH)
    @Operation(summary = "Troca o status do entregador",
            description = "OFFLINE, AVAILABLE ou BUSY. Só o próprio usuário ou um administrador.")
    @ApiResponse(responseCode = "200", description = "Cadastro com o status novo")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Status ausente ou desconhecido")
    @ErrorResponse(type = ProblemType.UNAUTHENTICATED, description = "Sem token ou token inválido")
    @ErrorResponse(type = ProblemType.ACCESS_DENIED, description = "Cadastro de outro usuário")
    @ErrorResponse(type = ProblemType.RESOURCE_NOT_FOUND, description = "Usuário não encontrado ou sem perfil de entregador")
    public EntityModel<UserResponse> changeCourierStatus(@Parameter(description = "Id do usuário") @PathVariable UUID id,
                                                         @Valid @RequestBody CourierStatusRequest request) {
        return assembler.toModel(controller.changeCourierStatus(id, request.status()));
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
