package com.wealthos.i18n;

import com.wealthos.common.error.NotFoundException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads messages*.properties so the frontend can use the same texts as the backend.
 * English is the base; the Spanish file overrides it key by key.
 */
@Component
public class MessageBundles {

    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    public Map<String, String> forLanguage(String lang) {
        if (!LocaleConfig.LANGUAGES.contains(lang)) throw new NotFoundException("error.notFound");
        return cache.computeIfAbsent(lang, this::load);
    }

    private Map<String, String> load(String lang) {
        Map<String, String> all = new TreeMap<>(read("messages.properties"));
        if (!lang.equals("en")) all.putAll(read("messages_" + lang + ".properties"));
        return Map.copyOf(all);
    }

    private static Map<String, String> read(String file) {
        Properties p = new Properties();
        try (Reader in = new InputStreamReader(new ClassPathResource(file).getInputStream(), StandardCharsets.UTF_8)) {
            p.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
        Map<String, String> map = new TreeMap<>();
        p.stringPropertyNames().forEach(k -> map.put(k, p.getProperty(k)));
        return map;
    }
}
