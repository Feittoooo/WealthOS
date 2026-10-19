package com.wealthos.portfolio.api;

import com.wealthos.common.error.BusinessException;
import com.wealthos.portfolio.dto.NetWorthSeries;
import com.wealthos.portfolio.service.NetWorthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Locale;

/** ?range=ALL|5Y|3Y|1Y|YTD, or ?range=CUSTOM&from=2025-01-01&to=2025-06-30 */
@RestController
@RequestMapping("/api/networth")
public class NetWorthController {

    private final NetWorthService netWorth;

    public NetWorthController(NetWorthService netWorth) {
        this.netWorth = netWorth;
    }

    @GetMapping
    public NetWorthSeries series(@RequestParam(defaultValue = "1Y") String range,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        NetWorthService.Range r = switch (range.toUpperCase(Locale.ROOT)) {
            case "ALL" -> NetWorthService.Range.ALL;
            case "5Y" -> NetWorthService.Range.Y5;
            case "3Y" -> NetWorthService.Range.Y3;
            case "1Y" -> NetWorthService.Range.Y1;
            case "YTD" -> NetWorthService.Range.YTD;
            case "CUSTOM" -> NetWorthService.Range.CUSTOM;
            default -> throw new BusinessException("error.badRequest");
        };
        if (r == NetWorthService.Range.CUSTOM && from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("error.rangeOrder");
        }
        return netWorth.series(r, from, to);
    }
}
