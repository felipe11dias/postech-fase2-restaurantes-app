package com.postech.restaurantes.application.usecase.auth;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.role.RoleName;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindCurrentRolesUseCaseTest {

    private IUserGateway userGateway;
    private FindCurrentRolesUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        useCase = FindCurrentRolesUseCase.create(userGateway);
    }

    @Test
    @DisplayName("Devolve os papéis derivados dos perfis gravados agora")
    void deveDevolverOsPapeisAtuais() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

        assertEquals(Set.of(RoleName.ROLE_CLIENT), useCase.run(USER_ID));
    }

    @Test
    @DisplayName("Usuário que não existe mais não tem papel nenhum; id nulo é recusado")
    void deveDevolverVazioQuandoNaoExiste() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertTrue(useCase.run(USER_ID).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
