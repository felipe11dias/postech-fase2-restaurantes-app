package com.postech.restaurantes.application.usecase.user;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.User;
import java.util.Set;

/**
 * Listagem paginada com busca parcial por nome. A ordenação só é repassada se a propriedade
 * estiver na lista permitida; qualquer outra (inclusive {@code password}) cai na ordenação
 * padrão por nome.
 */
public final class SearchUsersUseCase {

    public static final String DEFAULT_SORT = "name";
    public static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "name", "email", "login", "createdAt", "lastUpdatedAt");

    private final IUserGateway userGateway;

    private SearchUsersUseCase(IUserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public static SearchUsersUseCase create(IUserGateway userGateway) {
        return new SearchUsersUseCase(userGateway);
    }

    public PageResult<User> run(String name, PageRequest request) {
        Guard.requireNonNull(request, "Paginação inválida");
        return userGateway.search(Guard.trimToNull(name), sanitizeSort(request));
    }

    private static PageRequest sanitizeSort(PageRequest request) {
        if (request.hasSort() && SORTABLE_PROPERTIES.contains(request.sortBy())) {
            return request;
        }
        return request.withSort(DEFAULT_SORT, SortDirection.ASC);
    }
}
