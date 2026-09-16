package com.postech.restaurantes.application.usecase;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.ADDRESS_DTO;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CUSTOMER;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OWNER;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.NewUserDTO;
import com.postech.restaurantes.application.gateway.IPasswordEncoder;
import com.postech.restaurantes.application.gateway.IRoleGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.RoleName;
import com.postech.restaurantes.domain.entity.User;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import com.postech.restaurantes.domain.vo.Email;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegisterUserUseCaseTest {

    private IUserGateway userGateway;
    private IRoleGateway roleGateway;
    private IPasswordEncoder passwordEncoder;
    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        roleGateway = mock(IRoleGateway.class);
        passwordEncoder = mock(IPasswordEncoder.class);
        useCase = RegisterUserUseCase.create(userGateway, roleGateway, passwordEncoder);
    }

    private static NewUserDTO dto(Set<RoleName> roles) {
        return new NewUserDTO("João Silva", "Joao.Silva@Email.com", "joao.silva", "senhaSegura123",
                roles, List.of(ADDRESS_DTO));
    }

    @Test
    @DisplayName("Cadastra usuário válido com e-mail normalizado e senha em hash")
    void deveCadastrarQuandoValido() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(roleGateway.findByNames(Set.of(RoleName.ROLE_CUSTOMER))).thenReturn(Set.of(CUSTOMER));
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("hash");
        User persisted = existingUser();
        when(userGateway.insert(any())).thenReturn(persisted);

        User result = useCase.run(dto(Set.of(RoleName.ROLE_CUSTOMER)));

        assertSame(persisted, result);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userGateway).insert(captor.capture());
        User created = captor.getValue();
        assertEquals(Email.of("joao.silva@email.com"), created.getEmail());
        assertEquals("hash", created.getPasswordHash());
        assertEquals(Set.of(CUSTOMER), created.getRoles());
        assertEquals(1, created.getAddresses().size());
        verify(userGateway).findByEmail(Email.of("joao.silva@email.com"));
    }

    @Test
    @DisplayName("Recusa autocadastro que solicita ROLE_ADMIN, sem consultar o banco")
    void deveRecusarQuandoSolicitaAdmin() {
        assertThrows(ForbiddenOperationException.class,
                () -> useCase.run(dto(Set.of(RoleName.ROLE_CUSTOMER, RoleName.ROLE_ADMIN))));

        verify(userGateway, never()).findByEmail(any());
        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa cadastro sem papéis (nulo ou vazio)")
    void deveRecusarQuandoSemPapeis() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(null)));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(Set.of())));
    }

    @Test
    @DisplayName("Recusa papel nulo dentro do conjunto com 400, não com NPE")
    void deveRecusarQuandoPapelNuloNoConjunto() {
        Set<RoleName> comNulo = new HashSet<>(Arrays.asList(RoleName.ROLE_CUSTOMER, null));

        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto(comNulo)));

        verify(userGateway, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Recusa dados nulos")
    void deveRecusarQuandoDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }

    @Test
    @DisplayName("Recusa e-mail já cadastrado")
    void deveRecusarQuandoEmailDuplicado() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.of(existingUser()));

        assertThrows(DuplicateResourceException.class, () -> useCase.run(dto(Set.of(RoleName.ROLE_CUSTOMER))));

        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa login já cadastrado")
    void deveRecusarQuandoLoginDuplicado() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin("joao.silva")).thenReturn(Optional.of(existingUser()));

        assertThrows(DuplicateResourceException.class, () -> useCase.run(dto(Set.of(RoleName.ROLE_CUSTOMER))));

        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa cadastro quando algum papel solicitado não existe na base")
    void deveRecusarQuandoPapelInexistente() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(roleGateway.findByNames(any())).thenReturn(Set.of(CUSTOMER));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.run(dto(Set.of(RoleName.ROLE_CUSTOMER, RoleName.ROLE_OWNER))));

        verify(userGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa senha em branco antes de calcular o hash")
    void deveRecusarQuandoSenhaEmBranco() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(roleGateway.findByNames(any())).thenReturn(Set.of(OWNER));
        NewUserDTO semSenha = new NewUserDTO("Ana", "ana@x.com", "ana", " ", Set.of(RoleName.ROLE_OWNER), null);

        assertThrows(IllegalArgumentException.class, () -> useCase.run(semSenha));

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Cadastro sem endereços resulta em lista vazia")
    void deveAceitarSemEnderecos() {
        when(userGateway.findByEmail(any())).thenReturn(Optional.empty());
        when(userGateway.findByLogin(anyString())).thenReturn(Optional.empty());
        when(roleGateway.findByNames(any())).thenReturn(Set.of(OWNER));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));
        NewUserDTO semEndereco = new NewUserDTO("Ana", "ana@x.com", "ana", "senha", Set.of(RoleName.ROLE_OWNER), null);

        User result = useCase.run(semEndereco);

        assertTrue(result.getAddresses().isEmpty());
    }
}
