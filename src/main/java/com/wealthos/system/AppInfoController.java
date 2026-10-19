package com.wealthos.system;

import com.wealthos.common.format.NumberFormats;
import com.wealthos.i18n.LocaleConfig;
import com.wealthos.portfolio.dto.PriceSourceStatus;
import com.wealthos.portfolio.price.PriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** What the frontend needs to know before it starts: languages, formats, which extras are on. */
@RestController
@RequestMapping("/api/app")
public class AppInfoController {

    public record AppInfo(List<String> languages, String numberLocale, String baseCurrency,
                          boolean debugEnabled, PriceSourceStatus priceSource) {
    }

    private final NumberFormats formats;
    private final PriceService prices;
    private final boolean debugEnabled;

    public AppInfoController(NumberFormats formats, PriceService prices,
                             @Value("${wealthos.debug.enabled:true}") boolean debugEnabled) {
        this.formats = formats;
        this.prices = prices;
        this.debugEnabled = debugEnabled;
    }

    @GetMapping
    public AppInfo info() {
        return new AppInfo(LocaleConfig.LANGUAGES, formats.localeTag(), formats.baseCurrency(), debugEnabled,
                new PriceSourceStatus(prices.providerName(), prices.isAvailable()));
    }
}
