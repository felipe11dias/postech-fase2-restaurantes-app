package com.postech.restaurantes.domain.entity.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductTest {

    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final ProductOptionGroup GROUP = new ProductOptionGroup(
            "Tamanho", true, 1, 1, List.of(new ProductOptionValue("Grande", new BigDecimal("4.50"))));

    @Test
    @DisplayName("Cria produto com imagens e grupos de opções")
    void deveCriarQuandoValido() {
        String image = "/images/pizza.jpg";
        Product product = Product.create(RESTAURANT_ID, "Pizza", "Descrição", new BigDecimal("49.90"),
                false, List.of(image), List.of(GROUP));

        assertNull(product.getId());
        assertEquals(RESTAURANT_ID, product.getRestaurantId());
        assertEquals("Pizza", product.getName());
        assertEquals("Descrição", product.getDescription());
        assertEquals(new BigDecimal("49.90"), product.getPrice());
        assertEquals(image, product.getImagePaths().getFirst());
        assertEquals(List.of(GROUP), product.getOptionGroups());
    }

    @Test
    @DisplayName("Restaura produto com identificador")
    void deveRestaurarProduto() {
        UUID id = UUID.randomUUID();
        Product product = Product.restore(id, RESTAURANT_ID, "Pizza", null, BigDecimal.ZERO,
                true, List.of(), List.of());

        assertEquals(id, product.getId());
        assertTrue(product.isDineInOnly());
    }

    @Test
    @DisplayName("Recusa id, restaurante, nome, preço, imagens ou grupos inválidos")
    void deveRecusarDadosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> Product.restore(null, RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(null, "Pizza", null, BigDecimal.ONE, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, " ", null, BigDecimal.ONE, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, null, false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, new BigDecimal("-0.01"), false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, new BigDecimal("0.001"), false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, new BigDecimal("100000000"), false, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false, null, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false,
                        Arrays.asList("/images/pizza.jpg", null), List.of()));
        assertThrows(IllegalArgumentException.class,
                        () -> Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false, List.of(" "), List.of()));
        assertThrows(IllegalArgumentException.class,
                        () -> Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false, List.of("x".repeat(2049)), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false, List.of(), null));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE, false, List.of(), Arrays.asList(GROUP, null)));
        assertThrows(IllegalArgumentException.class,
                () -> Product.restore(UUID.randomUUID(), null, "Pizza", null, BigDecimal.ONE, false, List.of(), List.of()));
    }

    @Test
    @DisplayName("Normaliza descrição e mantém listas imutáveis")
    void deveNormalizarDescricaoEImpedirAlteracaoDasListas() {
        String image = "/images/pizza.jpg";
        Product product = Product.create(RESTAURANT_ID, "Pizza", "  ", BigDecimal.ONE,
                false, new ArrayList<>(List.of(image)), List.of(GROUP));

        assertNull(product.getDescription());
        assertEquals(1, product.getImagePaths().size());
        assertEquals(image, product.getImagePaths().getFirst());
        assertThrows(UnsupportedOperationException.class, () -> product.getImagePaths().clear());
        assertThrows(UnsupportedOperationException.class, () -> product.getOptionGroups().clear());
    }

    @Test
    @DisplayName("Setters validam os novos valores antes de alterar o estado")
    void deveValidarAlteracoes() {
        Product product = Product.create(RESTAURANT_ID, "Pizza", null, BigDecimal.ONE,
                false, List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () -> product.setName(""));
        assertThrows(IllegalArgumentException.class, () -> product.setPrice(null));
        assertThrows(IllegalArgumentException.class, () -> product.setPrice(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> product.setPrice(new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class, () -> product.setPrice(new BigDecimal("100000000")));
        assertThrows(IllegalArgumentException.class, () -> product.replaceImagePaths(null));
        assertThrows(IllegalArgumentException.class, () -> product.replaceOptionGroups(null));
        assertEquals("Pizza", product.getName());
        assertEquals(BigDecimal.ONE, product.getPrice());
    }

    @Test
    @DisplayName("Grupo valida limites, obrigatoriedade e valores disponíveis")
    void deveValidarGrupoDeOpcoes() {
        ProductOptionValue value = new ProductOptionValue("Grande", BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("", false, 0, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", false, -1, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", false, 0, 0, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", false, 2, 1, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", true, 0, 1, List.of(value)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", false, 0, 1, null));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", false, 0, 1, Arrays.asList(value, null)));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionGroup("Tamanho", false, 1, 1, List.of()));
        ProductOptionGroup group = new ProductOptionGroup("Tamanho", true, 1, 2, List.of(value));
        assertEquals(List.of(value), group.values());
        assertThrows(UnsupportedOperationException.class, () -> group.values().clear());
    }

    @Test
    @DisplayName("Valor de opção valida nome e preço adicional")
    void deveValidarValorDeOpcao() {
        assertThrows(IllegalArgumentException.class, () -> new ProductOptionValue(" ", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new ProductOptionValue("Grande", null));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionValue("Grande", new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionValue("Grande", new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class,
                () -> new ProductOptionValue("Grande", new BigDecimal("123456789.00")));
        assertEquals(new BigDecimal("4.50"),
                new ProductOptionValue("Grande", new BigDecimal("4.50")).additionalPrice());
    }
}
