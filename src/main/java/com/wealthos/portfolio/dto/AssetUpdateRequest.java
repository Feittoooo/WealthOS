package com.wealthos.portfolio.dto;

import com.wealthos.portfolio.domain.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Edits the asset as a whole. Units and prices paid are changed on each operation instead. */
public record AssetUpdateRequest(
        @NotBlank(message = "{val.position.ticker.required}")
        @Size(max = 20, message = "{val.position.ticker.size}")
        String ticker,

        @Pattern(regexp = "^$|^[A-Za-z]{2}[A-Za-z0-9]{9}[0-9]$", message = "{val.position.isin.pattern}")
        String isin,

        @Size(max = 120, message = "{val.position.name.size}")
        String name,

        @NotNull(message = "{val.position.type.required}")
        AssetType assetType,

        @DecimalMin(value = "0", inclusive = false, message = "{val.position.price.positive}")
        BigDecimal currentPrice,

        boolean autoPrice,

        @Size(max = 40, message = "{val.position.priceSymbol.size}")
        String priceSymbol) {
}
