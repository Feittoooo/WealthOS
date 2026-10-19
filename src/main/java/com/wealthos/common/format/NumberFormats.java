package com.wealthos.common.format;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Only used to write the arguments of activity log entries ("Bought 10 ACME at 89,10 €").
 * Everything the user sees on screen is formatted in the frontend.
 */
@Component
public class NumberFormats {

    private final Locale locale;
    private final String baseCurrency;

    public NumberFormats(@Value("${wealthos.locale:es-ES}") String locale,
                         @Value("${wealthos.base-currency:EUR}") String baseCurrency) {
        this.locale = Locale.forLanguageTag(locale);
        this.baseCurrency = baseCurrency;
    }

    public String localeTag() {
        return locale.toLanguageTag();
    }

    public String baseCurrency() {
        return baseCurrency;
    }

    public String qty(BigDecimal value) {
        NumberFormat nf = NumberFormat.getNumberInstance(locale);
        nf.setMaximumFractionDigits(8);
        return nf.format(value.stripTrailingZeros());
    }

    public String money(BigDecimal value, String currency) {
        NumberFormat nf = currencyFormat(currency);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    public String signedMoney(BigDecimal value, String currency) {
        return (value.signum() > 0 ? "+" : "") + money(value, currency);
    }

    public String price(BigDecimal value, String currency) {
        NumberFormat nf = currencyFormat(currency);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(value.abs().compareTo(BigDecimal.ONE) < 0 ? 6 : 4);
        return nf.format(value);
    }

    private NumberFormat currencyFormat(String code) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        try {
            nf.setCurrency(Currency.getInstance(code == null ? baseCurrency : code.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            nf.setCurrency(Currency.getInstance(baseCurrency));
        }
        return nf;
    }
}
