package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.PageRequest;
import com.postech.restaurantes.application.dto.PageResult;
import com.postech.restaurantes.application.dto.SortDirection;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** FindUserById, DeleteUser e SearchUsers: consultas e exclusão sobre o mesmo gateway. */
class UserQueryUseCasesTest {

    private IUserGateway userGateway;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
    }

    @Nested
    class FindUserById {

        @Test
        @DisplayName("Devolve o usuário quando existe")
        void deveDevolverQuandoExiste() {
            User user = existingUser();
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(user));

            assertSame(user, FindUserByIdUseCase.create(userGateway).run(USER_ID));
        }

        @Test
        @DisplayName("Lança ResourceNotFound quando não existe")
        void deveFalharQuandoNaoExiste() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> FindUserByIdUseCase.create(userGateway).run(USER_ID));
        }

        @Test
        @DisplayName("Recusa id nulo")
        void deveRecusarIdNulo() {
            assertThrows(IllegalArgumentException.class, () -> FindUserByIdUseCase.create(userGateway).run(null));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        @DisplayName("Exclui quando o usuário existe")
        void deveExcluirQuandoExiste() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

            DeleteUserUseCase.create(userGateway).run(USER_ID);

            verify(userGateway).delete(USER_ID);
        }

        @Test
        @DisplayName("Lança ResourceNotFound e não exclui quando não existe")
        void deveFalharQuandoNaoExiste() {
            when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> DeleteUserUseCase.create(userGateway).run(USER_ID));

            verify(userGateway, never()).delete(any());
        }

        @Test
        @DisplayName("Recusa id nulo")
        void deveRecusarIdNulo() {
            assertThrows(IllegalArgumentException.class, () -> DeleteUserUseCase.create(userGateway).run(null));
        }
    }

    @Nested
    class SearchUsers {

        private SearchUsersUseCase useCase;

        @BeforeEach
        void setUpUseCase() {
            useCase = SearchUsersUseCase.create(userGateway);
        }

        @Test
        @DisplayName("Repassa nome aparado e ordenação permitida")
        void deveRepassarOrdenacaoPermitida() {
            PageRequest request = PageRequest.of(0, 10).withSort("email", SortDirection.DESC);
            PageResult<User> page = new PageResult<>(List.of(existingUser()), 0, 10, 1);
            when(userGateway.search(eq("jo"), any())).thenReturn(page);

            PageResult<User> result = useCase.run("  jo ", request);

            assertSame(page, result);
            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userGateway).search(eq("jo"), captor.capture());
            assertEquals("email", captor.getValue().sortBy());
            assertEquals(SortDirection.DESC, captor.getValue().direction());
        }

        @Test
        @DisplayName("Propriedade não permitida (ex.: password) cai na ordenação padrão por nome")
        void deveIgnorarOrdenacaoNaoPermitida() {
            PageRequest request = PageRequest.of(1, 5).withSort("password", SortDirection.DESC);
            when(userGateway.search(isNull(), any())).thenReturn(PageResult.empty(request));

            useCase.run(null, request);

            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userGateway).search(isNull(), captor.capture());
            assertEquals(SearchUsersUseCase.DEFAULT_SORT, captor.getValue().sortBy());
            assertEquals(SortDirection.ASC, captor.getValue().direction());
            assertEquals(1, captor.getValue().page());
        }

        @Test
        @DisplayName("Sem ordenação informada usa a padrão; nome em branco vira nulo")
        void deveUsarOrdenacaoPadraoQuandoAusente() {
            when(userGateway.search(isNull(), any())).thenReturn(PageResult.empty(PageRequest.first()));

            useCase.run("   ", PageRequest.first());

            ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
            verify(userGateway).search(isNull(), captor.capture());
            assertEquals("name", captor.getValue().sortBy());
        }

        @Test
        @DisplayName("Recusa paginação nula")
        void deveRecusarPaginacaoNula() {
            assertThrows(IllegalArgumentException.class, () -> useCase.run("x", null));
        }
    }
}
