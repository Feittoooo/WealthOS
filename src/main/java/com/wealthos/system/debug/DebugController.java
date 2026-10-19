package com.wealthos.system.debug;

import com.wealthos.activity.TransactionRepository;
import com.wealthos.portfolio.price.PriceRefreshJob;
import com.wealthos.portfolio.price.PriceService;
import com.wealthos.portfolio.repository.AccountRepository;
import com.wealthos.portfolio.repository.AssetOperationRepository;
import com.wealthos.portfolio.repository.AssetRepository;
import com.wealthos.tax.repository.TaxEntryRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend side of the in-app debug page. Turn it off on public deployments with
 * WEALTHOS_DEBUG=false (it shows logs and configuration).
 */
@RestController
@RequestMapping("/api/debug")
@ConditionalOnProperty(name = "wealthos.debug.enabled", havingValue = "true", matchIfMissing = true)
public class DebugController {

    // loggers worth changing from the UI; anything else can still be set by name
    private static final List<String> INTERESTING_LOGGERS = List.of(
            "ROOT", "com.wealthos", "org.hibernate.SQL", "org.hibernate.orm.jdbc.bind", "org.springframework.web");

    private final LogBuffer logs;
    private final RequestTraceFilter requests;
    private final LoggingSystem loggingSystem;
    private final PriceRefreshJob priceRefresh;
    private final PriceService prices;
    private final Environment env;
    private final ObjectProvider<BuildProperties> build;
    private final AccountRepository accounts;
    private final AssetRepository assets;
    private final TransactionRepository transactions;
    private final AssetOperationRepository operations;
    private final TaxEntryRepository taxEntries;

    public DebugController(LogBuffer logs, RequestTraceFilter requests, LoggingSystem loggingSystem,
                           PriceRefreshJob priceRefresh, PriceService prices, Environment env,
                           ObjectProvider<BuildProperties> build, AccountRepository accounts,
                           AssetRepository assets, TransactionRepository transactions,
                           AssetOperationRepository operations, TaxEntryRepository taxEntries) {
        this.logs = logs;
        this.requests = requests;
        this.loggingSystem = loggingSystem;
        this.priceRefresh = priceRefresh;
        this.prices = prices;
        this.env = env;
        this.build = build;
        this.accounts = accounts;
        this.assets = assets;
        this.transactions = transactions;
        this.operations = operations;
        this.taxEntries = taxEntries;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Runtime rt = Runtime.getRuntime();
        BuildProperties b = build.getIfAvailable();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("version", b == null ? "dev" : b.getVersion());
        info.put("builtAt", b == null ? null : b.getTime());
        info.put("java", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name"));
        info.put("uptime", Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime()).toString());
        info.put("memoryUsedMb", (rt.totalMemory() - rt.freeMemory()) / 1_048_576);
        info.put("memoryMaxMb", rt.maxMemory() / 1_048_576);
        info.put("profiles", List.of(env.getActiveProfiles()));
        info.put("datasource", env.getProperty("spring.datasource.url"));
        info.put("numberLocale", env.getProperty("wealthos.locale"));
        info.put("priceSource", Map.of("name", prices.providerName(), "available", prices.isAvailable()));
        info.put("authEnabled", !env.getProperty("wealthos.auth.password", "").isEmpty());
        info.put("rows", Map.of(
                "accounts", accounts.count(),
                "assets", assets.count(),
                "activity", transactions.count(),
                "operations", operations.count(),
                "taxEntries", taxEntries.count()));
        return info;
    }

    @GetMapping("/logs")
    public List<LogBuffer.Entry> logs(@RequestParam(defaultValue = "0") long after,
                                      @RequestParam(defaultValue = "TRACE") String level) {
        return logs.since(after, level);
    }

    @DeleteMapping("/logs")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearLogs() {
        logs.clear();
        requests.clear();
    }

    @GetMapping("/requests")
    public List<RequestTraceFilter.Trace> requests() {
        return requests.recent();
    }

    @GetMapping("/loggers")
    public List<Map<String, String>> loggers() {
        return INTERESTING_LOGGERS.stream().map(name -> {
            LoggerConfiguration c = loggingSystem.getLoggerConfiguration(name);
            String level = c == null || c.getEffectiveLevel() == null ? "INFO" : c.getEffectiveLevel().name();
            return Map.of("name", name, "level", level);
        }).toList();
    }

    @PutMapping("/loggers/{name}")
    public Map<String, String> setLevel(@PathVariable String name, @RequestBody Map<String, String> body) {
        LogLevel level = LogLevel.valueOf(body.getOrDefault("level", "INFO").toUpperCase());
        loggingSystem.setLogLevel(name, level);
        return Map.of("name", name, "level", level.name());
    }

    @PostMapping("/prices/refresh")
    public PriceRefreshJob.Result refreshPrices() {
        return priceRefresh.run();
    }
}
