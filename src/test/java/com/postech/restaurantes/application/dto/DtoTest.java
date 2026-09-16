package com.postech.restaurantes.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.domain.entity.Address;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DtoTest {

    @Test
    @DisplayName("SortDirection: só 'desc' (em qualquer caixa) vira DESC")
    void deveConverterDirecao() {
        assertEquals(SortDirection.DESC, SortDirection.from("desc"));
        assertEquals(SortDirection.DESC, SortDirection.from(" DESC "));
        assertEquals(SortDirection.ASC, SortDirection.from("asc"));
        assertEquals(SortDirection.ASC, SortDirection.from("qualquer"));
        assertEquals(SortDirection.ASC, SortDirection.from(null));
    }

    @Test
    @DisplayName("AddressDTO converte para a entidade validada")
    void deveConverterEnderecoParaEntidade() {
        AddressDTO dto = new AddressDTO("Rua A", "1", null, "Centro", "Cidade", "sp", "01001-000");

        Address address = dto.toEntity();

        assertEquals("Rua A", address.getStreet());
        assertEquals("SP", address.getState());
        assertEquals("01001000", address.getZipCode().value());
    }

    @Test
    @DisplayName("AddressDTO: lista nula vira lista vazia; lista com itens converte todos")
    void deveConverterListaDeEnderecos() {
        assertTrue(AddressDTO.toEntities(null).isEmpty());

        List<Address> entities = AddressDTO.toEntities(List.of(
                new AddressDTO("Rua A", null, null, null, "Cidade", "SP", "01001000"),
                new AddressDTO("Rua B", null, null, null, "Cidade", "RJ", "20000000")));

        assertEquals(2, entities.size());
        assertEquals("RJ", entities.get(1).getState());
    }

    @Test
    @DisplayName("AddressDTO: elemento nulo na lista é entrada inválida, não NPE")
    void deveRecusarElementoNuloNaLista() {
        List<AddressDTO> comNulo = new ArrayList<>();
        comNulo.add(null);

        assertThrows(IllegalArgumentException.class, () -> AddressDTO.toEntities(comNulo));
    }

    @Test
    @DisplayName("IssuedToken guarda token aparado e expiração")
    void deveCriarTokenEmitido() {
        LocalDateTime expira = LocalDateTime.of(2026, 9, 16, 13, 0);

        IssuedToken token = new IssuedToken(" abc ", expira);

        assertEquals("abc", token.token());
        assertEquals(expira, token.expiresAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("IssuedToken recusa token em branco")
    void deveRecusarTokenEmBranco(String value) {
        assertThrows(IllegalArgumentException.class, () -> new IssuedToken(value, LocalDateTime.now()));
    }

    @Test
    @DisplayName("IssuedToken recusa expiração nula")
    void deveRecusarExpiracaoNula() {
        assertThrows(IllegalArgumentException.class, () -> new IssuedToken("abc", null));
    }

    @Test
    @DisplayName("Records de entrada apenas transportam dados")
    void deveTransportarDados() {
        NewUserDTO novo = new NewUserDTO("n", "e", "l", "p", null, null);
        UpdateUserDTO update = new UpdateUserDTO("n", "e", "l", null);
        ChangePasswordDTO change = new ChangePasswordDTO("a", "b", "b");
        CredentialsDTO cred = new CredentialsDTO("l", "p");
        ResetPasswordDTO reset = new ResetPasswordDTO("t", "n", "n");

        assertEquals("n", novo.name());
        assertNull(novo.roles());
        assertEquals("e", update.email());
        assertEquals("b", change.confirmPassword());
        assertEquals("p", cred.password());
        assertEquals("t", reset.token());
    }
}
