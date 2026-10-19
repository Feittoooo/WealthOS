package com.wealthos;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

// Every key has to exist in both languages, so nothing shows up half translated.
class MessagesBundleTest {

    @Test
    void englishAndSpanishHaveTheSameKeys() throws IOException {
        Set<String> en = keys("/messages.properties");
        Set<String> es = keys("/messages_es.properties");

        Set<String> missingInSpanish = new TreeSet<>(en);
        missingInSpanish.removeAll(es);
        Set<String> missingInEnglish = new TreeSet<>(es);
        missingInEnglish.removeAll(en);

        assertThat(missingInSpanish).as("missing in messages_es.properties").isEmpty();
        assertThat(missingInEnglish).as("missing in messages.properties").isEmpty();
    }

    // Properties keeps the last one silently, so a key written twice is easy to miss
    @Test
    void noKeyIsWrittenTwice() throws IOException {
        for (String file : List.of("messages.properties", "messages_es.properties")) {
            Set<String> seen = new HashSet<>();
            Set<String> twice = new TreeSet<>();
            for (String line : Files.readAllLines(Path.of("src/main/resources", file), StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#") || !line.contains("=")) continue;
                String key = line.substring(0, line.indexOf('=')).trim();
                if (!seen.add(key)) twice.add(key);
            }
            assertThat(twice).as("keys written twice in " + file).isEmpty();
        }
    }

    private static Set<String> keys(String path) throws IOException {
        Properties p = new Properties();
        try (InputStream in = MessagesBundleTest.class.getResourceAsStream(path)) {
            assertThat(in).as(path).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return new TreeSet<>(p.stringPropertyNames());
    }
}
