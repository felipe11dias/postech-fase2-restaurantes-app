package com.postech.restaurantes.application.dto.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAddressDTOTest {

    private static final AddressDTO RUA_A = new AddressDTO("Rua A", null, null, null, "Cidade", "SP", "01001000");
    private static final AddressDTO RUA_B = new AddressDTO("Rua B", null, null, null, "Cidade", "RJ", "20000000");

    @Test
    @DisplayName("Lista ausente vira nenhum endereço")
    void deveConverterListaNulaEmVazia() {
        assertTrue(UserAddressDTO.toEntities(null).isEmpty());
    }

    @Test
    @DisplayName("Converte todos os itens, com rótulo e endereço")
    void deveConverterTodosOsItens() {
        List<UserAddress> enderecos = UserAddressDTO.toEntities(List.of(
                new UserAddressDTO(null, "Casa", true, RUA_A),
                new UserAddressDTO(null, "Trabalho", false, RUA_B)));

        assertEquals(2, enderecos.size());
        assertEquals("Casa", enderecos.get(0).getLabel());
        assertEquals("RJ", enderecos.get(1).getAddress().getState());
    }

    @Test
    @DisplayName("Sem nenhum marcado como padrão, o primeiro passa a ser")
    void devePromoverOPrimeiroQuandoNenhumEhPadrao() {
        List<UserAddress> enderecos = UserAddressDTO.toEntities(List.of(
                new UserAddressDTO(null, null, null, RUA_A),
                new UserAddressDTO(null, null, false, RUA_B)));

        assertTrue(enderecos.get(0).isDefault());
        assertFalse(enderecos.get(1).isDefault());
    }

    @Test
    @DisplayName("Com um marcado como padrão, a marca é respeitada e o primeiro não é promovido")
    void deveRespeitarOPadraoMarcado() {
        List<UserAddress> enderecos = UserAddressDTO.toEntities(List.of(
                new UserAddressDTO(null, null, null, RUA_A),
                new UserAddressDTO(null, null, true, RUA_B)));

        assertFalse(enderecos.get(0).isDefault());
        assertTrue(enderecos.get(1).isDefault());
    }

    @Test
    @DisplayName("Com id, o endereço é reconstruído com ele, para o usuário mantê-lo")
    void deveReconstruirComOId() {
        UUID id = UUID.randomUUID();

        List<UserAddress> enderecos = UserAddressDTO.toEntities(List.of(new UserAddressDTO(id, "Casa", true, RUA_A)));

        assertEquals(id, enderecos.get(0).getId());
        assertEquals("Rua A", enderecos.get(0).getAddress().getStreet());
    }

    @Test
    @DisplayName("Elemento nulo na lista é entrada inválida, não NPE")
    void deveRecusarElementoNuloNaLista() {
        List<UserAddressDTO> comNulo = new ArrayList<>();
        comNulo.add(null);

        assertThrows(IllegalArgumentException.class, () -> UserAddressDTO.toEntities(comNulo));
    }

    @Test
    @DisplayName("Item sem o endereço é entrada inválida, não NPE")
    void deveRecusarItemSemEndereco() {
        List<UserAddressDTO> semEndereco = List.of(new UserAddressDTO(null, "Casa", true, null));

        assertThrows(IllegalArgumentException.class, () -> UserAddressDTO.toEntities(semEndereco));
    }
}
