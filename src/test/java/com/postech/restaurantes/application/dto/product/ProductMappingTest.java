package com.postech.restaurantes.application.dto.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductMappingTest {

    @Test
    @DisplayName("Converte caminhos de imagem e preço double para o produto")
    void deveConverterDadosDoProduto() {
        String image = "/images/pizza.jpg";
        ProductDTO dto = new ProductDTO("Pizza", null, 49.90, false, List.of(image), List.of());

        var product = dto.toProduct(UUID.randomUUID());

        assertEquals(49.90, product.getPrice());
        assertEquals(image, product.getImagePaths().getFirst());
    }

    @Test
    @DisplayName("Recusa preço nulo no DTO de produto")
    void naoDeveConverterPrecoNulo() {
        ProductDTO dto = new ProductDTO("Pizza", null, null, false, List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> dto.toProduct(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Converte grupos e valores para o domínio")
    void deveConverterGruposDeOpcoes() {
        List<ProductOptionGroupDTO> groups = List.of(new ProductOptionGroupDTO("Tamanho", false, 0, 1,
                List.of(new ProductOptionValueDTO("Grande", new BigDecimal("2.00")))));

        var mapped = ProductMapping.toGroups(groups);

        assertEquals("Tamanho", mapped.getFirst().name());
        assertEquals(new BigDecimal("2.00"), mapped.getFirst().values().getFirst().additionalPrice());
    }

    @Test
    @DisplayName("Recusa grupo, valores ou coleção ausentes")
    void deveRecusarDadosNulos() {
        assertThrows(IllegalArgumentException.class, () -> ProductMapping.toGroups(null));
        assertThrows(IllegalArgumentException.class,
                () -> ProductMapping.toGroups(Arrays.asList((ProductOptionGroupDTO) null)));
        assertThrows(IllegalArgumentException.class,
                () -> ProductMapping.toGroups(List.of(new ProductOptionGroupDTO("Grupo", false, 0, 1, null))));
        assertThrows(IllegalArgumentException.class,
                () -> ProductMapping.toGroups(List.of(new ProductOptionGroupDTO("Grupo", false, 0, 1,
                        Arrays.asList((ProductOptionValueDTO) null)))));
    }

}
