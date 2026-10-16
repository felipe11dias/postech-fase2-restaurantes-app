package com.postech.restaurantes.application.policy.user;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLIENT;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OWNER;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.otherUser;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Cpf;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UniqueDocumentsPolicyTest {

    private static final UserProfiles OUTRO_CLIENTE =
            new UserProfiles(null, ClientProfile.restore("11144477735", "11912345678", null), null, null);
    private static final UserProfiles OUTRO_DONO = new UserProfiles(
            OwnerProfile.restore("04252011000110", "Outra Ltda", "1131234567"), null, null, null);

    private IUserGateway userGateway;
    private UniqueDocumentsPolicy policy;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        policy = UniqueDocumentsPolicy.create(userGateway);
        when(userGateway.findByCpf(any())).thenReturn(Optional.empty());
        when(userGateway.findByCnpj(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("No cadastro, CPF e CNPJ são sempre consultados e qualquer outro dono deles é conflito")
    void deveConsultarTudoNoCadastro() {
        when(userGateway.findByCpf(Cpf.of("52998224725"))).thenReturn(Optional.of(existingUser()));
        when(userGateway.findByCnpj(Cnpj.of("11222333000181"))).thenReturn(Optional.of(existingUser()));

        DuplicateResourceException cpf =
                assertThrows(DuplicateResourceException.class, () -> policy.requireUnique(null, CLIENT, null));
        DuplicateResourceException cnpj =
                assertThrows(DuplicateResourceException.class, () -> policy.requireUnique(null, OWNER, null));

        assertEquals("CPF já cadastrado", cpf.getMessage());
        assertEquals("CNPJ já cadastrado", cnpj.getMessage());
    }

    @Test
    @DisplayName("Documento que não mudou não é consultado")
    void naoDeveConsultarDocumentoQueNaoMudou() {
        policy.requireUnique(CLIENT, CLIENT, USER_ID);
        policy.requireUnique(OWNER, OWNER, USER_ID);

        verify(userGateway, never()).findByCpf(any());
        verify(userGateway, never()).findByCnpj(any());
    }

    @Test
    @DisplayName("CPF novo que a busca encontra no próprio usuário não é conflito")
    void deveAceitarCpfNovoQueJaEDoProprioUsuario() {
        when(userGateway.findByCpf(Cpf.of("11144477735"))).thenReturn(Optional.of(existingUser()));

        assertDoesNotThrow(() -> policy.requireUnique(CLIENT, OUTRO_CLIENTE, USER_ID));
    }

    @Test
    @DisplayName("Documento novo de outro cadastro é conflito; do próprio usuário, não")
    void deveConferirODocumentoNovoContraOsOutros() {
        when(userGateway.findByCpf(Cpf.of("11144477735"))).thenReturn(Optional.of(otherUser()));
        when(userGateway.findByCnpj(Cnpj.of("04252011000110"))).thenReturn(Optional.of(existingUser()));

        assertThrows(DuplicateResourceException.class, () -> policy.requireUnique(CLIENT, OUTRO_CLIENTE, USER_ID));
        assertDoesNotThrow(() -> policy.requireUnique(OWNER, OUTRO_DONO, USER_ID));
        assertDoesNotThrow(() -> policy.requireUnique(CLIENT, OUTRO_DONO.withClient(
                ClientProfile.restore("52998224725", "11912345678", null)), USER_ID));
        assertThrows(DuplicateResourceException.class, () -> policy.requireUnique(OWNER,
                OWNER.withClient(ClientProfile.restore("11144477735", "11912345678", null)), USER_ID));
    }
}
