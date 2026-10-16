package com.postech.restaurantes.domain.entity.owner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Phone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OwnerProfileTest {

    @Test
    @DisplayName("Cria perfil de dono com CNPJ e telefone normalizados e razão social aparada")
    void deveCriarQuandoValido() {
        OwnerProfile profile = OwnerProfile.create("11.222.333/0001-81", "  Sabor & Arte Ltda ", "(11) 3123-4567");

        assertEquals(Cnpj.of("11222333000181"), profile.getCnpj());
        assertEquals("Sabor & Arte Ltda", profile.getLegalName());
        assertEquals(Phone.of("1131234567"), profile.getBusinessPhone());
    }

    @Test
    @DisplayName("Restaura com CNPJ alfanumérico, pela mesma validação")
    void deveRestaurarComCnpjAlfanumerico() {
        OwnerProfile profile = OwnerProfile.restore("12ABC34501DE35", "Sabor & Arte Ltda", "1131234567");

        assertEquals("12ABC34501DE35", profile.getCnpj().value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Recusa razão social em branco")
    void deveRecusarRazaoSocialEmBranco(String razaoSocial) {
        assertThrows(IllegalArgumentException.class,
                () -> OwnerProfile.create("11222333000181", razaoSocial, "1131234567"));
    }

    @Test
    @DisplayName("Recusa CNPJ e telefone inválidos; setters revalidam")
    void deveRecusarDocumentosInvalidos() {
        OwnerProfile profile = OwnerProfile.create("11222333000181", "Sabor & Arte Ltda", "1131234567");

        assertThrows(IllegalArgumentException.class,
                () -> OwnerProfile.create("11222333000182", "Sabor & Arte Ltda", "1131234567"));
        assertThrows(IllegalArgumentException.class,
                () -> OwnerProfile.create("11222333000181", "Sabor & Arte Ltda", "123"));
        assertThrows(IllegalArgumentException.class, () -> profile.setCnpj(null));
        assertThrows(IllegalArgumentException.class, () -> profile.setBusinessPhone(null));

        profile.setLegalName("Outra Razão");
        profile.setCnpj(Cnpj.of("12ABC34501DE35"));
        profile.setBusinessPhone(Phone.of("11912345678"));

        assertEquals("Outra Razão", profile.getLegalName());
        assertEquals("12ABC34501DE35", profile.getCnpj().value());
        assertEquals("11912345678", profile.getBusinessPhone().value());
    }
}
