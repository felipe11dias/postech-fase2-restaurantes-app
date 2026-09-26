package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.adapter.controller.UserController;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.web.doc.ApiDocumentation;
import com.postech.restaurantes.infrastructure.web.doc.ErrorResponse;
import com.postech.restaurantes.infrastructure.web.error.ProblemType;
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
@RequestMapping(UserRestController.BASE_PATH)
@Tag(name = "Usuários")
public class UserRestController {

    public static final String BASE_PATH = "/api/v1/users";

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
     * Autocadastro público. Pedir {@code ROLE_ADMIN} aqui é recusado pelo caso de uso.
     *
     * <p>O {@code @ResponseStatus} não muda o comportamento — quem define o 201 é o
     * {@code ResponseEntity.created} —, mas sem ele o springdoc documentaria 200.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Autocadastro",
            description = "Cria um usuário. Público. Pode pedir ROLE_OWNER e/ou ROLE_CUSTOMER; "
                    + "ROLE_ADMIN é recusado. A resposta traz o header Location do recurso criado.")
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @ErrorResponse(type = ProblemType.INVALID_REQUEST, description = "Campo inválido, papel inexistente ou endereço inconsistente")
    @ErrorResponse(type = ProblemType.FORBIDDEN_OPERATION, description = "Autocadastro pediu ROLE_ADMIN")
    @ErrorResponse(type = ProblemType.DATA_CONFLICT, description = "E-mail ou login já cadastrado")
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
