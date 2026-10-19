package com.wealthos.portfolio.dto;

import com.wealthos.portfolio.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// balance is only the money that isn't invested
public record AccountRequest(
        @NotBlank(message = "{val.account.name.required}")
        @Size(max = 80, message = "{val.account.name.size}")
        String name,

        @NotNull(message = "{val.account.type.required}")
        AccountType type,

        @Size(max = 80, message = "{val.account.institution.size}")
        String institution,

        @NotBlank(message = "{val.account.currency.required}")
        @Pattern(regexp = "[A-Za-z]{3}", message = "{val.account.currency.pattern}")
        String currency,

        @NotNull(message = "{val.account.balance.required}")
        BigDecimal balance) {
}
