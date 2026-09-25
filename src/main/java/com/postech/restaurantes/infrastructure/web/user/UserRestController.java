package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.adapter.controller.UserController;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.SortDirection;
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
 */
@RestController
@RequestMapping(UserRestController.BASE_PATH)
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

    /** Autocadastro público. Pedir {@code ROLE_ADMIN} aqui é recusado pelo caso de uso. */
    @PostMapping
    public ResponseEntity<EntityModel<UserResponse>> register(@Valid @RequestBody NewUserRequest request) {
        UserView criado = controller.register(request.toDTO());
        EntityModel<UserResponse> corpo = assembler.toModel(criado);
        return ResponseEntity.created(URI.create(assembler.selfLink(criado).getHref())).body(corpo);
    }

    @GetMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    public EntityModel<UserResponse> findById(@PathVariable UUID id) {
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
    public PagedModel<EntityModel<UserResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        return assembler.toPagedModel(controller.search(name, paginacao(page, size, sort)));
    }

    @PutMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    public EntityModel<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return assembler.toModel(controller.update(id, request.toDTO()));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@PathVariable UUID id, @Valid @RequestBody ChangePasswordRequest request) {
        controller.changePassword(id, request.toDTO());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(DONO_OU_ADMIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
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
