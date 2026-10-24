package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.product.ProductDTO;
import com.postech.restaurantes.application.dto.product.ProductOptionGroupDTO;
import com.postech.restaurantes.application.dto.product.ProductOptionValueDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

/** Corpo da criação ou substituição completa de um produto e suas opções. */
public record ProductRequest(
        @Schema(example = "Pizza Margherita") @NotBlank @Size(max = 150) String name,
        @Schema(example = "Molho de tomate, mozzarella e manjericão")
        @Size(max = 1000) String description,
        @Schema(example = "49.90") @NotNull @PositiveOrZero Double price,
        @Schema(example = "false") boolean dineInOnly,
        @Schema(description = "Caminhos ou URLs das imagens do produto",
                example = "[\"/images/pizza.jpg\"]")
        @NotNull List<@NotBlank @Size(max = 2048) String> imagePaths,
        @Schema(description = "Grupos de opções configuráveis",
                example = "[{\"name\":\"Borda\",\"required\":false,\"minSelections\":0,"
                        + "\"maxSelections\":1,\"values\":[{\"name\":\"Recheada\","
                        + "\"additionalPrice\":5.00}]}]")
        @NotNull @Valid List<@Valid ProductOptionGroupRequest> optionGroups
) {

    public ProductDTO toDTO() {
        return new ProductDTO(name, description, price, dineInOnly, imagePaths,
                optionGroups.stream().map(ProductOptionGroupRequest::toDTO).toList());
    }

    public record ProductOptionGroupRequest(
            @Schema(example = "Borda") @NotBlank @Size(max = 150) String name,
            @Schema(example = "false") boolean required,
            @Schema(example = "0") @Min(0) int minSelections,
            @Schema(example = "1") @Min(1) int maxSelections,
            @Schema(example = "[{\"name\":\"Recheada\",\"additionalPrice\":5.00}]")
            @NotNull @Valid List<@Valid ProductOptionValueRequest> values
    ) {

        ProductOptionGroupDTO toDTO() {
            return new ProductOptionGroupDTO(name, required, minSelections, maxSelections,
                    values.stream().map(ProductOptionValueRequest::toDTO).toList());
        }
    }

    public record ProductOptionValueRequest(
            @Schema(example = "Borda recheada") @NotBlank @Size(max = 150) String name,
            @Schema(example = "5.00") @NotNull @DecimalMin("0.00") BigDecimal additionalPrice
    ) {

        ProductOptionValueDTO toDTO() {
            return new ProductOptionValueDTO(name, additionalPrice);
        }
    }
}
